package hu.aleks.larvasecondslostextmod;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;

import javax.swing.SwingUtilities;

/** Wires a {@link LarvaMultiAnalysisRunner} with the production dependencies of the running module (E17-S12; used by S14 too). */
final class LarvaMultiAnalysisRunnerFactory {

    /** The worker runs slightly below normal priority so the EDT stays responsive on a busy machine. */
    private static final int WORKER_PRIORITY = Thread.NORM_PRIORITY - 1;

    private static final Executor EDT_EXECUTOR = new Executor() {
        @Override
        public void execute( final Runnable command ) {
            SwingUtilities.invokeLater( command ); // never invokeAndWait: the worker must not block on the EDT
        }
    };

    private LarvaMultiAnalysisRunnerFactory() {
    }

    /**
     * Creates, but does not start, a runner. Its worker creates its own {@link ReplaySummaryService}.
     *
     * @param module the running module
     * @param files replay files in processing order
     * @param ladderOnly <code>true</code> to accept only ladder games
     * @param listener callbacks on the EDT
     * @return the runner; call {@link LarvaMultiAnalysisRunner#start()}
     */
    static LarvaMultiAnalysisRunner create( final LarvaSecondsLostModule module, final List< Path > files, final boolean ladderOnly,
            final LarvaMultiAnalysisListener listener ) {
        return create( module, files, ladderOnly, true, listener );
    }

    /** Same, with an explicit pre-filter switch (dev measurements). */
    static LarvaMultiAnalysisRunner create( final LarvaSecondsLostModule module, final List< Path > files, final boolean ladderOnly,
            final boolean usePreFilter, final LarvaMultiAnalysisListener listener ) {
        final LarvaMultiAnalysisRunner.SourceFactory sourceFactory = new LarvaMultiAnalysisRunner.SourceFactory() {
            @Override
            public LarvaAggregationSource create() {
                return new LarvaServiceAggregationSource( new ReplaySummaryService( module ) );
            }
        };
        final ThreadFactory threads = new ThreadFactory() {
            @Override
            public Thread newThread( final Runnable task ) {
                final Thread thread = new Thread( task, LarvaMultiAnalysisRunner.WORKER_THREAD_NAME );
                thread.setDaemon( true );
                thread.setPriority( WORKER_PRIORITY );
                return thread;
            }
        };
        return new LarvaMultiAnalysisRunner( module.logger, sourceFactory, EDT_EXECUTOR, threads, LarvaClock.SYSTEM, files, ladderOnly, usePreFilter,
                listener );
    }
}
