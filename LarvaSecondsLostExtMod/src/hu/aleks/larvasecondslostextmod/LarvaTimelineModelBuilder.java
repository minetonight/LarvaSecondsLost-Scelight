package hu.aleks.larvasecondslostextmod;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds the normalized module-owned timeline model used by the Epic 03 preview chart.
 */
public class LarvaTimelineModelBuilder {

    /** Replay loops per second in SC2 tracker/game timelines. */
    private static final long LOOPS_PER_SECOND = 16L;

    /** Minimum width of a placeholder interval so it remains visible on short replays. */
    private static final long MIN_PLACEHOLDER_WINDOW_MS = 10000L;

    /** Timeline title. */
    private static final String TITLE = "Epic 03 module-owned larva timeline";

    /** Timeline subtitle. */
    private static final String SUBTITLE = "Replay-derived placeholder intervals rendered on the supported Larva page fallback surface.";

    /** Empty message used when no rows are available yet. */
    private static final String EMPTY_MESSAGE = "Load a replay to populate the module-owned fallback timeline preview.";

    /**
     * Builds a timeline model from replay metadata and current larva diagnostics.
     *
     * @param replaySummary replay metadata summary
     * @param integrationMode fallback integration mode label
     * @param larvaAnalysisReport larva analysis report; may be <code>null</code>
     * @param fallbackPreviewStartMs fallback preview interval start
     * @param fallbackPreviewEndMs fallback preview interval end
     * @return normalized timeline model
     */
    public LarvaTimelineModel build( final ReplaySummary replaySummary, final String integrationMode, final LarvaAnalysisReport larvaAnalysisReport,
            final long fallbackPreviewStartMs, final long fallbackPreviewEndMs ) {
        final long replayLengthMs = replaySummary == null ? 0L : replaySummary.getLengthMs();
        final String replayLengthLabel = replaySummary == null ? "0:00" : replaySummary.getLength();

        final List< LarvaTimelineRow > rowList = new ArrayList<>();
        if ( replaySummary != null && replayLengthMs > 0L && larvaAnalysisReport != null && !larvaAnalysisReport.getTimelineList().isEmpty() )
            populateReplayDerivedRows( replayLengthMs, larvaAnalysisReport, rowList );

        if ( rowList.isEmpty() && replaySummary != null && replayLengthMs > 0L )
            rowList.add( createFallbackRow( replayLengthMs, fallbackPreviewStartMs, fallbackPreviewEndMs ) );

        return new LarvaTimelineModel( TITLE, SUBTITLE, integrationMode, replayLengthMs, replayLengthLabel, EMPTY_MESSAGE, rowList );
    }

    /**
     * Builds replay-derived placeholder rows from per-hatchery timelines.
     *
     * @param replayLengthMs replay length in milliseconds
     * @param larvaAnalysisReport larva analysis report
     * @param rowList target row list
     */
    private void populateReplayDerivedRows( final long replayLengthMs, final LarvaAnalysisReport larvaAnalysisReport,
            final List< LarvaTimelineRow > rowList ) {
        for ( final HatcheryLarvaTimeline timeline : larvaAnalysisReport.getTimelineList() ) {
            final LarvaTimelineRow row = createReplayDerivedRow( replayLengthMs, timeline );
            if ( row != null )
                rowList.add( row );
        }
    }

    /**
     * Creates one replay-derived placeholder row from a hatchery timeline.
     *
     * @param replayLengthMs replay length in milliseconds
     * @param timeline hatchery larva timeline
     * @return timeline row; may be <code>null</code>
     */
    private LarvaTimelineRow createReplayDerivedRow( final long replayLengthMs, final HatcheryLarvaTimeline timeline ) {
        if ( timeline.getCountPointList().isEmpty() )
            return null;

        final HatcheryLarvaTimeline.CountPoint firstPoint = timeline.getCountPointList().get( 0 );
        final HatcheryLarvaTimeline.CountPoint lastPoint = timeline.getCountPointList().get( timeline.getCountPointList().size() - 1 );
        final long firstPointMs = clampToReplay( loopsToMs( firstPoint.getLoop() ), replayLengthMs );
        final long lastPointMs = clampToReplay( loopsToMs( lastPoint.getLoop() ), replayLengthMs );
        final long startMs;
        final long endMs;
        final LarvaTimelineSegment.Kind kind;

        if ( lastPointMs > firstPointMs ) {
            startMs = firstPointMs;
            endMs = ensureVisibleWindow( firstPointMs, lastPointMs, replayLengthMs );
            kind = LarvaTimelineSegment.Kind.PREVIEW_INTERVAL;
        } else {
            final long markerStartMs = Math.max( 0L, firstPointMs - MIN_PLACEHOLDER_WINDOW_MS / 2L );
            startMs = markerStartMs;
            endMs = ensureVisibleWindow( markerStartMs, firstPointMs + MIN_PLACEHOLDER_WINDOW_MS / 2L, replayLengthMs );
            kind = LarvaTimelineSegment.Kind.PREVIEW_MARKER;
        }

        final List< LarvaTimelineSegment > segmentList = new ArrayList<>();
        segmentList.add( new LarvaTimelineSegment( startMs, endMs,
                "Replay-derived placeholder span, max larva=" + timeline.getMaxLarvaCount(), kind ) );

        final String rowLabel = timeline.getHatcheryType() + " (tag " + timeline.getHatcheryTagText() + ")";
        final String detailLabel = "points=" + timeline.getCountPointList().size() + ", max=" + timeline.getMaxLarvaCount();
        return new LarvaTimelineRow( timeline.getPlayerName(), rowLabel, detailLabel, segmentList );
    }

    /**
     * Creates the length-derived single-row fallback interval used before replay-derived rows exist.
     *
     * @param replayLengthMs replay length in milliseconds
     * @param previewStartMs fallback preview start
     * @param previewEndMs fallback preview end
     * @return fallback row
     */
    private LarvaTimelineRow createFallbackRow( final long replayLengthMs, final long previewStartMs, final long previewEndMs ) {
        final long startMs = clampToReplay( previewStartMs, replayLengthMs );
        final long endMs = ensureVisibleWindow( startMs, clampToReplay( previewEndMs, replayLengthMs ), replayLengthMs );
        final List< LarvaTimelineSegment > segmentList = new ArrayList<>();
        segmentList.add( new LarvaTimelineSegment( startMs, endMs, "Replay-length-derived placeholder interval",
                LarvaTimelineSegment.Kind.PREVIEW_INTERVAL ) );
        return new LarvaTimelineRow( "Replay context", "Larva fallback preview rail", "placeholder timing before real larva windows", segmentList );
    }

    /**
     * Converts tracker loops to milliseconds.
     *
     * @param loops replay loops
     * @return milliseconds
     */
    private long loopsToMs( final int loops ) {
        return ( loops * 1000L ) / LOOPS_PER_SECOND;
    }

    /**
     * Clamps a replay time to the replay length.
     *
     * @param valueMs value to clamp
     * @param replayLengthMs replay length
     * @return clamped value
     */
    private long clampToReplay( final long valueMs, final long replayLengthMs ) {
        if ( replayLengthMs <= 0L )
            return 0L;
        if ( valueMs < 0L )
            return 0L;
        if ( valueMs > replayLengthMs )
            return replayLengthMs;
        return valueMs;
    }

    /**
     * Ensures a placeholder interval stays visible.
     *
     * @param startMs interval start
     * @param endMs interval end
     * @param replayLengthMs replay length
     * @return adjusted end time
     */
    private long ensureVisibleWindow( final long startMs, final long endMs, final long replayLengthMs ) {
        final long clampedEndMs = clampToReplay( endMs, replayLengthMs );
        if ( clampedEndMs > startMs )
            return clampedEndMs;
        return clampToReplay( startMs + MIN_PLACEHOLDER_WINDOW_MS, replayLengthMs );
    }

}