package hu.aleks.larvasecondslostextmod;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

/** Runner on a real worker thread with a synchronous "EDT" executor and a fake clock (E17-S12). */
public class LarvaMultiAnalysisRunnerTest {

    private static final long WAIT_MS = 10000L;

    private final List< String > events = Collections.synchronizedList( new ArrayList< String >() );

    private final List< String > workerThreadNames = Collections.synchronizedList( new ArrayList< String >() );

    private final LarvaFakeAggregationSource source = new LarvaFakeAggregationSource();

    private final Executor directExecutor = new Executor() {
        @Override
        public void execute( final Runnable command ) {
            command.run();
        }
    };

    private final ThreadFactory threads = new ThreadFactory() {
        @Override
        public Thread newThread( final Runnable task ) {
            final Thread t = new Thread( task, LarvaMultiAnalysisRunner.WORKER_THREAD_NAME );
            t.setDaemon( true );
            return t;
        }
    };

    private final LarvaClock steppingClock = new LarvaClock() {
        private long now;

        @Override
        public synchronized long nowMillis() {
            now += 1000L; // every query is a second later: nothing gets throttled
            return now;
        }
    };

    private final LarvaMultiAnalysisListener listener = new LarvaMultiAnalysisListener() {
        @Override
        public void onProgress( final int k, final int n, final String fileName, final int failedCount ) {
            events.add( "progress " + k + "/" + n + " " + fileName + " f=" + failedCount );
        }

        @Override
        public void onCompleted( final LarvaAggregationResult result, final LarvaMultiAnalysisViewModel viewModel ) {
            events.add( "completed eligible=" + result.getFilterSummary().getEligible() );
        }

        @Override
        public void onCancelled() {
            events.add( "cancelled" );
        }

        @Override
        public void onFailed( final Throwable error ) {
            events.add( "failed " + error.getMessage() );
        }
    };

    private LarvaMultiAnalysisRunner.SourceFactory factory( final LarvaAggregationSource created ) {
        return new LarvaMultiAnalysisRunner.SourceFactory() {
            @Override
            public LarvaAggregationSource create() {
                workerThreadNames.add( Thread.currentThread().getName() );
                return created;
            }
        };
    }

    private static List< Path > files( final String... names ) {
        final List< Path > files = new ArrayList<>();
        for ( final String name : names )
            files.add( Paths.get( name ) );
        return files;
    }

    private static LarvaAggregationGameRecord eligible( final String name ) {
        return new LarvaAggregationGameRecord.Builder().replayFile( Paths.get( name ) ).replayDate( new Date( 0L ) ).oneVsOne( true ).ladder( true )
                .lengthLoops( 10000 ).addZergPlayer( LarvaTestFixtures.player( "Me", LarvaMatchupBucket.ZVT ).build() ).build();
    }

    private LarvaMultiAnalysisRunner runner( final LarvaMultiAnalysisRunner.SourceFactory factory, final String... names ) {
        return new LarvaMultiAnalysisRunner( new StdErrLogger( false, false ), factory, directExecutor, threads, steppingClock, files( names ), true, true,
                listener );
    }

    @Test
    public void completesWithProgressThenCompletedAndCreatesTheSourceOnTheWorkerThread() throws Exception {
        source.add( "a.SC2Replay", eligible( "a.SC2Replay" ) ).add( "b.SC2Replay", eligible( "b.SC2Replay" ) );
        final LarvaMultiAnalysisRunner runner = runner( factory( source ), "a.SC2Replay", "b.SC2Replay" );
        runner.start();
        runner.join( WAIT_MS );
        assertEquals( "[progress 1/2 a.SC2Replay f=0, progress 2/2 b.SC2Replay f=0, completed eligible=2]", events.toString() );
        assertEquals( LarvaMultiAnalysisRunner.State.COMPLETED, runner.getState() );
        assertNotNull( runner.getResult() );
        assertEquals( "[" + LarvaMultiAnalysisRunner.WORKER_THREAD_NAME + "]", workerThreadNames.toString() );
        assertEquals( 2, runner.getCurrentK() );
    }

    @Test
    public void badFilesAreCountedAndTheRunStillCompletes() throws Exception {
        source.add( "a.SC2Replay", eligible( "a.SC2Replay" ) );
        source.metadata.put( "bad.SC2Replay", null );
        final LarvaMultiAnalysisRunner runner = runner( factory( source ), "bad.SC2Replay", "a.SC2Replay" );
        runner.start();
        runner.join( WAIT_MS );
        assertEquals( LarvaMultiAnalysisRunner.State.COMPLETED, runner.getState() );
        assertEquals( 1, runner.getFailedCount() );
        assertEquals( "completed eligible=1", events.get( events.size() - 1 ) );
    }

    @Test
    public void cancelStopsBeforeTheNextFileAndReportsCancelled() throws Exception {
        final CountDownLatch inSecondFile = new CountDownLatch( 1 );
        final CountDownLatch release = new CountDownLatch( 1 );
        for ( final String name : new String[] { "a.SC2Replay", "b.SC2Replay", "c.SC2Replay" } )
            source.add( name, eligible( name ) );
        source.onAnalyze = new Runnable() {
            @Override
            public void run() {
                if ( source.fullCalls.size() == 2 ) {
                    inSecondFile.countDown();
                    try {
                        release.await( WAIT_MS, TimeUnit.MILLISECONDS );
                    } catch ( final InterruptedException e ) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        };
        final LarvaMultiAnalysisRunner runner = runner( factory( source ), "a.SC2Replay", "b.SC2Replay", "c.SC2Replay" );
        runner.start();
        assertTrue( inSecondFile.await( WAIT_MS, TimeUnit.MILLISECONDS ) );
        runner.cancel();
        release.countDown();
        runner.join( WAIT_MS );
        assertEquals( LarvaMultiAnalysisRunner.State.CANCELLED, runner.getState() );
        assertEquals( 2, runner.getProcessedWhenCancelled() );
        assertEquals( "no third file", 2, source.fullCalls.size() );
        assertEquals( "cancelled", events.get( events.size() - 1 ) );
        assertEquals( "the last progress was delivered before the end callback", "progress 2/3 b.SC2Replay f=0", events.get( events.size() - 2 ) );
        assertEquals( null, runner.getResult() );
    }

    @Test
    public void failureOfTheWholeRunIsReportedAsFailed() throws Exception {
        final LarvaMultiAnalysisRunner runner = runner( new LarvaMultiAnalysisRunner.SourceFactory() {
            @Override
            public LarvaAggregationSource create() {
                throw new IllegalStateException( "no service" );
            }
        }, "a.SC2Replay" );
        runner.start();
        runner.join( WAIT_MS );
        assertEquals( LarvaMultiAnalysisRunner.State.FAILED, runner.getState() );
        assertEquals( "[failed no service]", events.toString() );
    }

    @Test( expected = IllegalStateException.class )
    public void aRunnerCanBeStartedOnlyOnce() throws Exception {
        final LarvaMultiAnalysisRunner runner = runner( factory( source ) );
        runner.start();
        runner.join( WAIT_MS );
        runner.start();
    }

    @Test
    public void cancelAfterTheEndIsHarmless() throws Exception {
        final LarvaMultiAnalysisRunner runner = runner( factory( source ) );
        runner.start();
        runner.join( WAIT_MS );
        runner.cancel();
        assertEquals( LarvaMultiAnalysisRunner.State.COMPLETED, runner.getState() );
    }
}
