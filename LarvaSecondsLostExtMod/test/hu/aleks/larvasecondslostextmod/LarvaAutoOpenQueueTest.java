package hu.aleks.larvasecondslostextmod;

import static org.junit.Assert.assertEquals;
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

/** Auto-open flow of the queue with a fake runner and host: pending limit, decisions, lifecycle. */
public class LarvaAutoOpenQueueTest {

    private static final long TIMEOUT_SECONDS = 10L;

    private final List< String > analyzed = new CopyOnWriteArrayList< String >();
    private final List< String > opened = new CopyOnWriteArrayList< String >();
    private final CountDownLatch gate = new CountDownLatch( 1 );
    private final CountDownLatch firstStarted = new CountDownLatch( 1 );
    private volatile LarvaAutoOpenPolicy.Admission admission = LarvaAutoOpenPolicy.Admission.OPEN;
    private volatile LarvaAutoOpenOutcome outcome = LarvaAutoOpenOutcome.analyzed( null );
    private LarvaSingleReplayAnalysisQueue queue;

    @Before
    public void setUp() {
        queue = new LarvaSingleReplayAnalysisQueue( new StdErrLogger( false, false ), new LarvaReplayAnalysisRunner() {
            @Override
            public LarvaReplayPageSummary analyze( final Path replayFile, final String sourceDescription ) {
                throw new AssertionError( "manual path not used" );
            }

            @Override
            public LarvaAutoOpenOutcome analyzeForAutoOpen( final Path replayFile, final String sourceDescription ) {
                analyzed.add( replayFile.getFileName().toString() );
                firstStarted.countDown();
                try {
                    gate.await( TIMEOUT_SECONDS, TimeUnit.SECONDS );
                } catch ( final InterruptedException e ) {
                    Thread.currentThread().interrupt();
                }
                return outcome;
            }
        }, new Executor() {
            @Override
            public void execute( final Runnable command ) {
                command.run();
            }
        }, null );
        queue.setAutoOpenHost( new LarvaAutoOpenHost() {
            @Override
            public LarvaAutoOpenPolicy.Admission admit( final Path replayFile ) {
                return admission;
            }

            @Override
            public LarvaReplayPageController open( final Path replayFile, final LarvaAutoOpenOutcome result ) {
                opened.add( replayFile.getFileName().toString() );
                return null;
            }
        } );
    }

    @After
    public void tearDown() {
        gate.countDown();
        queue.shutdown();
    }

    private static Path file( final int i ) {
        return Paths.get( "/replays/r" + i + ".SC2Replay" );
    }

    private void waitFor( final List< String > list, final int size ) throws InterruptedException {
        final long deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis( TIMEOUT_SECONDS );
        while ( list.size() < size && System.currentTimeMillis() < deadline )
            Thread.sleep( 5L );
        assertEquals( size, list.size() );
    }

    @Test
    public void burstOfTenProducesAtMostOneRunningPlusFivePending() throws Exception {
        queue.submitAutoOpen( file( 0 ) );
        assertTrue( firstStarted.await( TIMEOUT_SECONDS, TimeUnit.SECONDS ) );
        for ( int i = 1; i < 10; i++ )
            queue.submitAutoOpen( file( i ) );
        gate.countDown();
        waitFor( opened, 6 );
        Thread.sleep( 100L );
        assertEquals( 6, opened.size() );
        assertEquals( 6, analyzed.size() );
        assertTrue( queue.getAutoOpenStats().summary().contains( "droppedPendingLimit=4" ) );
        assertEquals( 0, queue.getPendingAutoOpen().getPending() );
    }

    @Test
    public void skippedAdmissionsAreNotAnalyzedOrOpened() throws Exception {
        admission = LarvaAutoOpenPolicy.Admission.SKIP_ALREADY_OPEN;
        gate.countDown();
        queue.submitAutoOpen( file( 1 ) );
        waitForSummary( "skipped=1" );
        assertTrue( analyzed.isEmpty() );
        assertTrue( opened.isEmpty() );
    }

    @Test
    public void nonZergAndFailedPrecheckCreateNoPage() throws Exception {
        gate.countDown();
        outcome = LarvaAutoOpenOutcome.notZerg();
        queue.submitAutoOpen( file( 1 ) );
        waitForSummary( "notZerg=1" );
        outcome = LarvaAutoOpenOutcome.precheckFailed( "corrupt" );
        queue.submitAutoOpen( file( 2 ) );
        waitForSummary( "precheckFailed=1" );
        assertTrue( opened.isEmpty() );
    }

    @Test
    public void failedAnalysisOfAZergReplayStillOpensAPage() throws Exception {
        gate.countDown();
        outcome = LarvaAutoOpenOutcome.analysisFailed( "boom" );
        queue.submitAutoOpen( file( 1 ) );
        waitFor( opened, 1 );
    }

    @Test
    public void withoutHostEventsAreSkippedAndAfterShutdownIgnored() throws Exception {
        gate.countDown();
        queue.setAutoOpenHost( null );
        queue.submitAutoOpen( file( 1 ) );
        waitForSummary( "skipped=1" );
        queue.shutdown();
        queue.submitAutoOpen( file( 2 ) ); // must not throw
        Thread.sleep( 50L );
        assertTrue( analyzed.isEmpty() );
        assertTrue( opened.isEmpty() );
        assertEquals( 0, queue.getPendingAutoOpen().getPending() );
    }

    private void waitForSummary( final String fragment ) throws InterruptedException {
        final long deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis( TIMEOUT_SECONDS );
        while ( !queue.getAutoOpenStats().summary().contains( fragment ) && System.currentTimeMillis() < deadline )
            Thread.sleep( 5L );
        assertTrue( queue.getAutoOpenStats().summary(), queue.getAutoOpenStats().summary().contains( fragment ) );
    }

}
