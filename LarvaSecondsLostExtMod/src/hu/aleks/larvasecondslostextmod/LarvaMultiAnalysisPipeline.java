package hu.aleks.larvasecondslostextmod;

import hu.scelightapibase.service.log.ILogger;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Headless batch pipeline of the multi-replay analysis (E17-S12, FR-6, FR-15): per file optional metadata pre-filter, full
 * analysis, eligibility; at the end tracked-player selection and aggregation. No Swing, no threads; the caller provides the
 * worker. Only the compact records of eligible games are kept (R-4), reports and processors die with each file.
 *
 * <p>Failure policy (user decision): a replay that cannot be parsed (null record, null processor, any
 * {@link RuntimeException}) is never dropped silently. It is counted as {@link LarvaEligibility#FAILED_TO_PARSE} and listed
 * in the skipped list with a reason, in the pre-filter path and in the full path alike.</p>
 */
final class LarvaMultiAnalysisPipeline {

    static final String LOG_PREFIX = "[larva-multi] ";

    private final ILogger logger;

    private final LarvaAggregationSource source;

    private final boolean usePreFilter;

    private final LarvaEligibilityFilter filter = new LarvaEligibilityFilter();

    /**
     * @param logger module logger
     * @param source per-replay record source, used on the calling thread only
     * @param usePreFilter <code>true</code> to read cheap metadata first and skip the full analysis of ineligible files
     */
    LarvaMultiAnalysisPipeline( final ILogger logger, final LarvaAggregationSource source, final boolean usePreFilter ) {
        if ( logger == null || source == null )
            throw new IllegalArgumentException( "Logger and source are required." );
        this.logger = logger;
        this.source = source;
        this.usePreFilter = usePreFilter;
    }

    /**
     * Runs the batch on the calling thread.
     *
     * @param files replay files; processed in list order, which is also the order of the result
     * @param ladderOnly <code>true</code> to accept only ladder games
     * @param sink progress receiver (called before each file, on this thread)
     * @param token cancellation token, checked before each file
     * @return the aggregated result
     * @throws LarvaBatchCancelledException if the token was cancelled; no new file was started after that
     */
    LarvaAggregationResult run( final List< Path > files, final boolean ladderOnly, final LarvaProgressSink sink,
            final LarvaCancellationToken token ) {
        if ( files == null || sink == null || token == null )
            throw new IllegalArgumentException( "Files, progress sink and cancellation token are required." );
        final int total = files.size();
        final Run run = new Run();
        for ( int index = 0; index < total; index++ ) {
            if ( token.isCancelled() )
                throw new LarvaBatchCancelledException( index, total );
            final Path file = files.get( index );
            sink.onProgress( index + 1, total, fileNameOf( file ), run.failedCount );
            processFile( file, ladderOnly, run );
        }
        final LarvaFilterSummary summary = run.summary.build();
        final List< LarvaTrackedPlayer > tracked = new LarvaTrackedPlayerSelector().select( run.eligible );
        return new LarvaAggregator().aggregate( run.eligible, tracked, summary, run.skipped );
    }

    private void processFile( final Path file, final boolean ladderOnly, final Run run ) {
        try {
            if ( usePreFilter ) {
                final LarvaEligibility pre = filter.evaluate( source.readMetadata( file ), ladderOnly );
                if ( pre != LarvaEligibility.ELIGIBLE ) {
                    recordExcluded( file, pre, run );
                    return;
                }
            }
            final LarvaAggregationGameRecord record = source.analyze( file );
            final LarvaEligibility result = filter.evaluate( record, ladderOnly );
            if ( result == LarvaEligibility.ELIGIBLE ) {
                run.summary.add( result );
                run.eligible.add( record );
            } else
                recordExcluded( file, result, run );
        } catch ( final RuntimeException e ) {
            recordFailure( file, e, run );
        }
    }

    private void recordExcluded( final Path file, final LarvaEligibility reason, final Run run ) {
        run.summary.add( reason );
        if ( reason == LarvaEligibility.FAILED_TO_PARSE ) {
            run.failedCount++;
            run.skipped.add( new LarvaSkippedReplay( fileNameOf( file ), LarvaSkippedReplay.REASON_FAILED_TO_PARSE ) );
            logger.debug( LOG_PREFIX + "replay could not be parsed (no record): " + file );
            return;
        }
        logger.debug( LOG_PREFIX + "excluded (" + reason.getLabel() + "): " + file );
    }

    private void recordFailure( final Path file, final RuntimeException e, final Run run ) {
        run.summary.add( LarvaEligibility.FAILED_TO_PARSE );
        run.failedCount++;
        final String message = e.getMessage();
        final boolean expected = e instanceof IllegalStateException && ReplaySummaryService.MESSAGE_COULD_NOT_PARSE.equals( message );
        if ( expected )
            logger.debug( LOG_PREFIX + "replay could not be parsed (unsupported by the Scelight parser): " + file );
        else
            logger.error( LOG_PREFIX + "analysis failed for " + file, e );
        final String detail = message == null || message.length() == 0 ? e.getClass().getSimpleName() : message;
        run.skipped.add( new LarvaSkippedReplay( fileNameOf( file ),
                expected ? LarvaSkippedReplay.REASON_FAILED_TO_PARSE : LarvaSkippedReplay.REASON_FAILED_TO_PARSE + ": " + detail ) );
    }

    private static String fileNameOf( final Path file ) {
        return file.getFileName() == null ? file.toString() : file.getFileName().toString();
    }

    /** Mutable state of one run. */
    private static final class Run {

        final LarvaFilterSummary.Builder summary = new LarvaFilterSummary.Builder();

        final List< LarvaAggregationGameRecord > eligible = new ArrayList<>();

        final List< LarvaSkippedReplay > skipped = new ArrayList<>();

        int failedCount;
    }
}
