package hu.aleks.larvasecondslostextmod;

import hu.scelightapibase.service.log.ILogger;

import java.nio.file.Path;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * The module-wide FIFO queue for single-replay analyses: one daemon worker thread ({@code larva-replay-loader}) owns
 * the one {@link ReplaySummaryService} (not thread-safe), so analyses never run concurrently. Results are posted to the
 * EDT; work and results of closed pages are dropped.
 */
final class LarvaSingleReplayAnalysisQueue implements LarvaReplayAnalysisQueue {

    static final String WORKER_THREAD_NAME = "larva-replay-loader";

    /** The worker waits at most this long for a short EDT task (auto-open admission check). */
    private static final long EDT_WAIT_SECONDS = 10L;

    private final ILogger logger;
    private final LarvaReplayAnalysisRunner runner;
    private final Executor edtExecutor;
    private final DevDiagnosticDumpWriter devDump;
    private final ExecutorService worker;
    private final LarvaAutoOpenPendingLimit pendingAutoOpen = new LarvaAutoOpenPendingLimit( LarvaAutoOpenPolicy.MAX_PENDING_AUTO_REQUESTS );
    private final LarvaAutoOpenStats autoOpenStats = new LarvaAutoOpenStats();
    private volatile LarvaAutoOpenHost autoOpenHost;
    private volatile boolean shutdown;

    /**
     * @param logger module logger
     * @param runner analysis implementation, used on the worker thread only
     * @param edtExecutor runs result handling on the EDT (must not block the caller)
     * @param devDump development dump writer; <code>null</code> or disabled for no dump
     */
    LarvaSingleReplayAnalysisQueue( final ILogger logger, final LarvaReplayAnalysisRunner runner, final Executor edtExecutor,
            final DevDiagnosticDumpWriter devDump ) {
        this.logger = logger;
        this.runner = runner;
        this.edtExecutor = edtExecutor;
        this.devDump = devDump != null && devDump.isEnabled() ? devDump : null;
        this.worker = Executors.newSingleThreadExecutor( new ThreadFactory() {
            @Override
            public Thread newThread( final Runnable task ) {
                final Thread thread = new Thread( task, WORKER_THREAD_NAME );
                thread.setDaemon( true );
                return thread;
            }
        } );
    }

    @Override
    public void submit( final LarvaReplayPageController controller, final String sourceDescription, final long requestId ) {
        if ( shutdown ) {
            logger.debug( "[larva-replay] queue stopped, request dropped: " + controller.getReplayFile() );
            return;
        }
        worker.execute( new Runnable() {
            @Override
            public void run() {
                process( controller, sourceDescription, requestId );
            }
        } );
    }

    /** Sets the host that creates auto-opened pages; without it auto-open requests are skipped (hub not installed yet). */
    void setAutoOpenHost( final LarvaAutoOpenHost host ) {
        this.autoOpenHost = host;
    }

    LarvaAutoOpenStats getAutoOpenStats() {
        return autoOpenStats;
    }

    LarvaAutoOpenPendingLimit getPendingAutoOpen() {
        return pendingAutoOpen;
    }

    /**
     * Auto-open (E17-S07, D17-f): queues the Zerg pre-check and analysis of a newly detected replay and returns
     * immediately (called on Scelight's replay-handler job thread). More than
     * {@link LarvaAutoOpenPolicy#MAX_PENDING_AUTO_REQUESTS} waiting requests are dropped (D17-e).
     *
     * @param replayFile the final replay path from the monitor event
     */
    void submitAutoOpen( final Path replayFile ) {
        autoOpenStats.recordDetected();
        if ( shutdown ) {
            logger.debug( "[larva-auto] queue stopped, replay ignored: " + replayFile );
            return;
        }
        if ( !pendingAutoOpen.tryAcquire() ) {
            autoOpenStats.recordDroppedPendingLimit( String.valueOf( replayFile ) );
            logger.debug( "[larva-auto] dropped, pending limit " + pendingAutoOpen.getMax() + " reached: " + replayFile );
            return;
        }
        try {
            worker.execute( new Runnable() {
                @Override
                public void run() {
                    pendingAutoOpen.release();
                    processAutoOpen( replayFile );
                }
            } );
        } catch ( final RuntimeException e ) { // RejectedExecutionException after shutdown
            pendingAutoOpen.release();
            logger.debug( "[larva-auto] queue stopped, replay ignored: " + replayFile );
        }
    }

    /** Stops the worker; queued requests are discarded, a running analysis finishes but its result is dropped. */
    void shutdown() {
        shutdown = true;
        worker.shutdownNow();
    }

    private void process( final LarvaReplayPageController controller, final String sourceDescription, final long requestId ) {
        if ( controller.isDisposed() || shutdown ) {
            logger.debug( "[larva-replay] queued analysis dropped, page closed: " + controller.getReplayFile() );
            return;
        }
        try {
            final LarvaReplayPageSummary summary = analyzeAndDump( controller, sourceDescription );
            postResult( new Runnable() {
                @Override
                public void run() {
                    controller.onAnalysisSucceeded( requestId, summary );
                }
            } );
            logger.debug( "[larva-replay] replay diagnostics ready for: " + controller.getReplayFile() );
        } catch ( final RuntimeException e ) {
            fail( controller, sourceDescription, requestId, e );
        } catch ( final Error e ) {
            // The page must not stay busy forever; the failure is logged with the throwable.
            fail( controller, sourceDescription, requestId, e );
        }
    }

    private void processAutoOpen( final Path replayFile ) {
        final String file = String.valueOf( replayFile );
        final LarvaAutoOpenHost host = autoOpenHost;
        if ( shutdown || host == null ) {
            autoOpenStats.recordSkipped( "not ready", file );
            logger.debug( "[larva-auto] skipped, module not ready: " + file );
            return;
        }
        try {
            final LarvaAutoOpenPolicy.Admission admission = onEdt( new Callable< LarvaAutoOpenPolicy.Admission >() {
                @Override
                public LarvaAutoOpenPolicy.Admission call() {
                    return host.admit( replayFile );
                }
            } );
            if ( admission != LarvaAutoOpenPolicy.Admission.OPEN ) {
                recordSkip( admission, file );
                return;
            }
            final LarvaAutoOpenOutcome outcome = runner.analyzeForAutoOpen( replayFile, LarvaAutoOpenPolicy.SOURCE_MONITOR );
            handleOutcome( host, replayFile, outcome );
        } catch ( final InterruptedException e ) {
            Thread.currentThread().interrupt();
            logger.debug( "[larva-auto] interrupted, replay ignored: " + file );
        } catch ( final RuntimeException e ) {
            logger.error( "[larva-auto] auto-open failed, no page created: " + file, e );
        } catch ( final Error e ) {
            logger.error( "[larva-auto] auto-open failed, no page created: " + file, e );
        }
    }

    private void handleOutcome( final LarvaAutoOpenHost host, final Path replayFile, final LarvaAutoOpenOutcome outcome ) {
        final String file = String.valueOf( replayFile );
        switch ( outcome.getKind() ) {
        case NOT_ZERG:
            autoOpenStats.recordNotZerg( file );
            logger.debug( "[larva-auto] no Zerg player, no page: " + file );
            return;
        case PRECHECK_FAILED:
            autoOpenStats.recordPrecheckFailed( file );
            logger.debug( "[larva-auto] pre-check failed (" + outcome.getMessage() + "), no page: " + file );
            return;
        default:
            if ( devDump != null )
                dumpAutoOpen( replayFile, outcome );
            postResult( new Runnable() {
                @Override
                public void run() {
                    host.open( replayFile, outcome );
                }
            } );
        }
    }

    private void dumpAutoOpen( final Path replayFile, final LarvaAutoOpenOutcome outcome ) {
        devDump.recordAnalysisStart( replayFile, LarvaAutoOpenPolicy.SOURCE_MONITOR );
        if ( outcome.getSummary() != null )
            devDump.recordAnalysisSuccess( outcome.getSummary() );
        else
            devDump.recordAnalysisFailure( replayFile, LarvaAutoOpenPolicy.SOURCE_MONITOR, outcome.getMessage() );
    }

    private void recordSkip( final LarvaAutoOpenPolicy.Admission admission, final String file ) {
        final String reason = admission == LarvaAutoOpenPolicy.Admission.SKIP_CAP ? "cap of " + LarvaAutoOpenPolicy.MAX_AUTO_OPEN_SINGLE_PAGES + " pages"
                : admission == LarvaAutoOpenPolicy.Admission.SKIP_ALREADY_OPEN ? "already open" : "not ready";
        autoOpenStats.recordSkipped( reason, file );
        logger.debug( "[larva-auto] skipped, " + reason + ": " + file );
    }

    /** Runs a short task on the EDT and waits (never called while holding a lock); gives up after a timeout. */
    private < T > T onEdt( final Callable< T > task ) throws InterruptedException {
        final FutureTask< T > future = new FutureTask< T >( task );
        edtExecutor.execute( future );
        try {
            return future.get( EDT_WAIT_SECONDS, TimeUnit.SECONDS );
        } catch ( final java.util.concurrent.ExecutionException e ) {
            throw new IllegalStateException( "EDT task failed", e.getCause() );
        } catch ( final java.util.concurrent.TimeoutException e ) {
            throw new IllegalStateException( "EDT did not answer within " + EDT_WAIT_SECONDS + " s", e );
        }
    }

    private LarvaReplayPageSummary analyzeAndDump( final LarvaReplayPageController controller, final String sourceDescription ) {
        if ( devDump != null )
            devDump.recordAnalysisStart( controller.getReplayFile(), sourceDescription );
        final LarvaReplayPageSummary summary = runner.analyze( controller.getReplayFile(), sourceDescription );
        if ( devDump != null )
            devDump.recordAnalysisSuccess( summary );
        return summary;
    }

    private void fail( final LarvaReplayPageController controller, final String sourceDescription, final long requestId, final Throwable error ) {
        logger.error( "Failed to analyze replay for the Larva page: " + controller.getReplayFile(), error );
        final String message = error.getMessage(); // null is shown as "Unknown error" by the page, as before
        if ( devDump != null )
            devDump.recordAnalysisFailure( controller.getReplayFile(), sourceDescription, error.getMessage() );
        postResult( new Runnable() {
            @Override
            public void run() {
                controller.onAnalysisFailed( requestId, message );
            }
        } );
    }

    private void postResult( final Runnable result ) {
        if ( shutdown )
            return;
        edtExecutor.execute( result );
    }

}
