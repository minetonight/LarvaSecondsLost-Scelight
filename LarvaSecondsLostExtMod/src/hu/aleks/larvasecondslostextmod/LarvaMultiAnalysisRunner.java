package hu.aleks.larvasecondslostextmod;

import hu.scelightapibase.service.log.ILogger;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;

/**
 * One background run of the batch pipeline (E17-S12, FR-15). {@link #start()} spawns one daemon worker thread (S02: one
 * worker is enough, about 17 ms per replay); the worker creates its own {@link LarvaAggregationSource}, hence its own
 * {@link ReplaySummaryService}. Listener callbacks are posted to the EDT through the injected executor; progress posts are
 * throttled to one per 100 ms, the last one is always delivered before the end callback. The runner is single-use.
 */
final class LarvaMultiAnalysisRunner {

    static final String WORKER_THREAD_NAME = "larva-multi-analysis";

    /** Creates the per-worker source; called on the worker thread. */
    interface SourceFactory {
        LarvaAggregationSource create();
    }

    /** Lifecycle of a run. */
    enum State {
        /** Not started yet. */
        NEW,
        /** The worker is processing files. */
        RUNNING,
        /** Finished with a result. */
        COMPLETED,
        /** Stopped by {@link #cancel()}. */
        CANCELLED,
        /** Stopped by an unexpected error. */
        FAILED
    }

    private final ILogger logger;
    private final SourceFactory sourceFactory;
    private final Executor edtExecutor;
    private final ThreadFactory threadFactory;
    private final LarvaClock clock;
    private final List< Path > files;
    private final boolean ladderOnly;
    private final boolean usePreFilter;
    private final LarvaMultiAnalysisListener listener;
    private final LarvaCancellationToken token = new LarvaCancellationToken();

    private volatile State state = State.NEW;
    private volatile int currentK;
    private volatile int failedCount;
    private volatile LarvaAggregationResult result;
    private volatile Throwable error;
    private volatile long startedMs;
    private volatile long finishedMs;
    private volatile int processedWhenCancelled = -1;
    private Thread worker;

    /**
     * @param logger module logger
     * @param sourceFactory creates the record source on the worker thread
     * @param edtExecutor runs listener callbacks on the EDT without blocking the caller
     * @param threadFactory creates the worker thread (daemon)
     * @param clock time source for the throttling
     * @param files replay files in processing order (copied)
     * @param ladderOnly <code>true</code> to accept only ladder games
     * @param usePreFilter <code>true</code> to use the metadata pre-filter
     * @param listener callbacks on the EDT
     */
    LarvaMultiAnalysisRunner( final ILogger logger, final SourceFactory sourceFactory, final Executor edtExecutor,
            final ThreadFactory threadFactory, final LarvaClock clock, final List< Path > files, final boolean ladderOnly,
            final boolean usePreFilter, final LarvaMultiAnalysisListener listener ) {
        this.logger = logger;
        this.sourceFactory = sourceFactory;
        this.edtExecutor = edtExecutor;
        this.threadFactory = threadFactory;
        this.clock = clock;
        this.files = Collections.unmodifiableList( new ArrayList<>( files ) );
        this.ladderOnly = ladderOnly;
        this.usePreFilter = usePreFilter;
        this.listener = listener;
    }

    /** Starts the worker. May be called once. */
    synchronized void start() {
        if ( state != State.NEW )
            throw new IllegalStateException( "Runner already started." );
        state = State.RUNNING;
        startedMs = clock.nowMillis();
        worker = threadFactory.newThread( new Runnable() {
            @Override
            public void run() {
                runBatch();
            }
        } );
        worker.start();
    }

    /** Asks the worker to stop before the next file. Returns immediately; safe from any thread and when already finished. */
    void cancel() {
        token.cancel();
    }

    State getState() {
        return state;
    }

    /** @return 1-based index of the file being analyzed (0 before the first) */
    int getCurrentK() {
        return currentK;
    }

    int getTotal() {
        return files.size();
    }

    int getFailedCount() {
        return failedCount;
    }

    /** @return the result once {@link State#COMPLETED}, else null */
    LarvaAggregationResult getResult() {
        return result;
    }

    Throwable getError() {
        return error;
    }

    /** @return files fully processed when the run was cancelled, or -1 */
    int getProcessedWhenCancelled() {
        return processedWhenCancelled;
    }

    /** @return wall time of the run so far or in total, in ms */
    long getElapsedMs() {
        final long end = finishedMs == 0L ? clock.nowMillis() : finishedMs;
        return startedMs == 0L ? 0L : end - startedMs;
    }

    /** Waits for the worker to end (tests, dev tooling). */
    void join( final long timeoutMs ) throws InterruptedException {
        final Thread t = worker;
        if ( t != null )
            t.join( timeoutMs );
    }

    private void runBatch() {
        final LarvaThrottledProgressSink sink = new LarvaThrottledProgressSink( new LarvaProgressSink() {
            @Override
            public void onProgress( final int k, final int n, final String fileName, final int failed ) {
                postProgress( k, n, fileName, failed );
            }
        }, clock );
        logger.info( LarvaMultiAnalysisPipeline.LOG_PREFIX + "started " + files.size() + " files, ladderOnly=" + ladderOnly + ", preFilter=" + usePreFilter );
        try {
            final LarvaMultiAnalysisPipeline pipeline = new LarvaMultiAnalysisPipeline( logger, sourceFactory.create(), usePreFilter );
            final LarvaAggregationResult aggregated = pipeline.run( files, ladderOnly, trackingSink( sink ), token );
            sink.flush();
            // Off the EDT: the page only paints this immutable model (E17-S15).
            final LarvaMultiAnalysisViewModel viewModel = LarvaMultiAnalysisViewModelBuilder.build( aggregated, Locale.getDefault(),
                    TimeZone.getDefault() );
            result = aggregated;
            failedCount = aggregated.getFilterSummary().getCount( LarvaEligibility.FAILED_TO_PARSE );
            finish( State.COMPLETED );
            logger.info( LarvaMultiAnalysisPipeline.LOG_PREFIX + "completed " + files.size() + " files in " + getElapsedMs() + " ms: "
                    + aggregated.getFilterSummary().toText() );
            post( new Runnable() {
                @Override
                public void run() {
                    listener.onCompleted( aggregated, viewModel );
                }
            } );
        } catch ( final LarvaBatchCancelledException e ) {
            sink.flush();
            processedWhenCancelled = e.getProcessed();
            finish( State.CANCELLED );
            logger.info( LarvaMultiAnalysisPipeline.LOG_PREFIX + "cancelled after " + e.getProcessed() + "/" + e.getTotal() );
            post( new Runnable() {
                @Override
                public void run() {
                    listener.onCancelled();
                }
            } );
        } catch ( final Throwable t ) { // never let the worker die silently
            error = t;
            finish( State.FAILED );
            logger.error( LarvaMultiAnalysisPipeline.LOG_PREFIX + "run failed", t );
            post( new Runnable() {
                @Override
                public void run() {
                    listener.onFailed( t );
                }
            } );
        }
    }

    /** Keeps the unthrottled counters for {@link #getCurrentK()} and {@link #getFailedCount()} exact. */
    private LarvaProgressSink trackingSink( final LarvaProgressSink throttled ) {
        return new LarvaProgressSink() {
            @Override
            public void onProgress( final int k, final int n, final String fileName, final int failed ) {
                currentK = k;
                failedCount = failed;
                throttled.onProgress( k, n, fileName, failed );
            }
        };
    }

    private void finish( final State end ) {
        finishedMs = clock.nowMillis();
        state = end;
    }

    private void postProgress( final int k, final int n, final String fileName, final int failed ) {
        post( new Runnable() {
            @Override
            public void run() {
                listener.onProgress( k, n, fileName, failed );
            }
        } );
    }

    private void post( final Runnable callback ) {
        edtExecutor.execute( callback );
    }
}
