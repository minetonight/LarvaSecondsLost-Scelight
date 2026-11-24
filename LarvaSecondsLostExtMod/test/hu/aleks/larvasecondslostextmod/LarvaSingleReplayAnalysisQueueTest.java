package hu.aleks.larvasecondslostextmod;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class LarvaSingleReplayAnalysisQueueTest {

    private static final long TIMEOUT_SECONDS = 10L;
    private static final LarvaReplayPageSummary SUMMARY = new LarvaReplayPageSummary( null, "mode", null, 0L, 0L, null );

    /** Stands in for the EDT: runs results inline and counts them. */
    private static final class InlineExecutor implements Executor {
        @Override
        public void execute( final Runnable command ) {
            command.run();
        }
    }

    private final List< String > analyzed = new CopyOnWriteArrayList< String >();
    private final List< String > threads = new CopyOnWriteArrayList< String >();
    private final CountDownLatch gate = new CountDownLatch( 1 );
    private final CountDownLatch firstStarted = new CountDownLatch( 1 );
    private LarvaSingleReplayAnalysisQueue queue;
    private volatile boolean failNext;

    @Before
    public void setUp() {
        queue = new LarvaSingleReplayAnalysisQueue( new StdErrLogger( false, false ), new LarvaReplayAnalysisRunner() {
            @Override
            public LarvaReplayPageSummary analyze( final Path replayFile, final String sourceDescription ) {
                analyzed.add( replayFile.getFileName().toString() );
                threads.add( Thread.currentThread().getName() );
                firstStarted.countDown();
                await( gate );
                if ( failNext )
                    throw new IllegalStateException( "corrupt replay" );
                return SUMMARY;
            }

            @Override
            public LarvaAutoOpenOutcome analyzeForAutoOpen( final Path replayFile, final String sourceDescription ) {
                return LarvaAutoOpenOutcome.notZerg();
            }
        }, new InlineExecutor(), null );
    }

    @After
    public void tearDown() {
        gate.countDown();
        queue.shutdown();
    }

    private static void await( final CountDownLatch latch ) {
        try {
            latch.await( TIMEOUT_SECONDS, TimeUnit.SECONDS );
        } catch ( final InterruptedException e ) {
            Thread.currentThread().interrupt();
        }
    }

    private LarvaReplayPageController controllerFor( final String name ) {
        return new LarvaReplayPageController( Paths.get( "/replays/" + name ).toAbsolutePath(), queue, new StdErrLogger( false, false ) );
    }

    private static void waitUntil( final LarvaReplayPageController controller, final LarvaReplayPageController.State state ) throws InterruptedException {
        final long deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis( TIMEOUT_SECONDS );
        while ( controller.getState() != state && System.currentTimeMillis() < deadline )
            Thread.sleep( 5L );
        assertEquals( state, controller.getState() );
    }

    @Test
    public void analysesRunFifoOnTheSingleNamedWorker() throws Exception {
        final LarvaReplayPageController a = controllerFor( "a.SC2Replay" );
        final LarvaReplayPageController b = controllerFor( "b.SC2Replay" );
        a.requestAnalysis( "s" );
        b.requestAnalysis( "s" );
        gate.countDown();
        waitUntil( a, LarvaReplayPageController.State.READY );
        waitUntil( b, LarvaReplayPageController.State.READY );
        assertEquals( "[a.SC2Replay, b.SC2Replay]", analyzed.toString() );
        assertEquals( LarvaSingleReplayAnalysisQueue.WORKER_THREAD_NAME, threads.get( 0 ) );
        assertEquals( threads.get( 0 ), threads.get( 1 ) );
    }

    @Test
    public void queuedAnalysisOfAClosedPageIsDroppedBeforeItRuns() throws Exception {
        final LarvaReplayPageController running = controllerFor( "running.SC2Replay" );
        final LarvaReplayPageController closed = controllerFor( "closed.SC2Replay" );
        final LarvaReplayPageController last = controllerFor( "last.SC2Replay" );
        running.requestAnalysis( "s" );
        assertTrue( firstStarted.await( TIMEOUT_SECONDS, TimeUnit.SECONDS ) );
        closed.requestAnalysis( "s" );
        last.requestAnalysis( "s" );
        closed.dispose();
        gate.countDown();
        waitUntil( last, LarvaReplayPageController.State.READY );
        assertEquals( "[running.SC2Replay, last.SC2Replay]", analyzed.toString() );
        assertFalse( analyzed.contains( "closed.SC2Replay" ) );
    }

    @Test
    public void resultOfARunningAnalysisIsDroppedWhenThePageWasClosedMeanwhile() throws Exception {
        final LarvaReplayPageController page = controllerFor( "running.SC2Replay" );
        final LarvaReplayPageController next = controllerFor( "next.SC2Replay" );
        page.requestAnalysis( "s" );
        assertTrue( firstStarted.await( TIMEOUT_SECONDS, TimeUnit.SECONDS ) );
        next.requestAnalysis( "s" );
        page.dispose();
        gate.countDown();
        waitUntil( next, LarvaReplayPageController.State.READY );
        assertEquals( LarvaReplayPageController.State.BUSY, page.getState() );
        assertTrue( page.isDisposed() );
    }

    @Test
    public void failedAnalysisPutsOnlyThatPageIntoErrorAndTheWorkerSurvives() throws Exception {
        final LarvaReplayPageController bad = controllerFor( "bad.SC2Replay" );
        failNext = true;
        gate.countDown();
        bad.requestAnalysis( "s" );
        waitUntil( bad, LarvaReplayPageController.State.ERROR );
        assertEquals( "corrupt replay", bad.getErrorMessage() );

        failNext = false;
        final LarvaReplayPageController good = controllerFor( "good.SC2Replay" );
        good.requestAnalysis( "s" );
        waitUntil( good, LarvaReplayPageController.State.READY );
    }

    @Test
    public void requestsAfterShutdownAreDropped() throws Exception {
        queue.shutdown();
        final LarvaReplayPageController late = controllerFor( "late.SC2Replay" );
        late.requestAnalysis( "s" );
        gate.countDown();
        Thread.sleep( 100L );
        assertTrue( analyzed.isEmpty() );
        assertEquals( LarvaReplayPageController.State.BUSY, late.getState() );
    }

}
