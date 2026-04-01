package hu.aleks.larvasecondslostextmod;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds the normalized module-owned timeline model used by the supported Larva chart.
 */
public class LarvaTimelineModelBuilder {

    /** Minimum width of a placeholder interval so it remains visible on short replays. */
    private static final long MIN_PLACEHOLDER_WINDOW_MS = 10000L;

    /** Timeline title. */
    private static final String TITLE = "Larva timeline";

    /** Timeline subtitle. */
    private static final String SUBTITLE = "Replay-derived 3+ larva windows rendered on the supported module-owned Larva page.";

    /** Empty message used when no rows are available yet. */
    private static final String EMPTY_MESSAGE = "Load a replay to populate the supported larva timeline.";

    /** Suffix used for per-hatchery totals. */
    private static final String POTENTIAL_LARVA_MISSED_SUFFIX = " potential larva missed";

    /** Stable 3+ larva window calculator. */
    private final LarvaSaturationWindowCalculator saturationWindowCalculator = new LarvaSaturationWindowCalculator();

    /** Missed-larva threshold marker calculator. */
    private final LarvaMissedLarvaMarkerCalculator missedLarvaMarkerCalculator = new LarvaMissedLarvaMarkerCalculator();

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

        return new LarvaTimelineModel( TITLE, SUBTITLE, integrationMode, buildGroupOverviewLabelMap( rowList ), replayLengthMs, replayLengthLabel,
            EMPTY_MESSAGE, rowList );
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
        if ( timeline.getCountPointList().isEmpty() || !timeline.isCompleted() || timeline.getCreatedLarvaCount() <= 0 )
            return null;

        final int visibleStartLoop = saturationWindowCalculator.resolveVisibleStartLoop( timeline );
        final int visibleEndLoop = saturationWindowCalculator.resolveVisibleEndLoop( timeline, replayLengthMs );
        if ( visibleStartLoop < 0 || visibleEndLoop <= visibleStartLoop )
            return null;

        final long startMs = clampToReplay( saturationWindowCalculator.loopsToMs( visibleStartLoop ), replayLengthMs );
        final long endMs = clampToReplay( saturationWindowCalculator.loopsToMs( visibleEndLoop ), replayLengthMs );
        if ( endMs <= startMs )
            return null;

        final List< LarvaSaturationWindow > saturationWindowList = saturationWindowCalculator.buildWindows( timeline, replayLengthMs );
        final List< LarvaTimelineMarker > markerList = missedLarvaMarkerCalculator.buildMarkers( saturationWindowList );

        final List< LarvaTimelineSegment > segmentList = new ArrayList<>();
        for ( final LarvaSaturationWindow window : saturationWindowList )
            segmentList.add( new LarvaTimelineSegment( window.getStartMs(), window.getEndMs(),
                    "3+ larva " + window.getStartTimeLabel() + "-" + window.getEndTimeLabel(), LarvaTimelineSegment.Kind.SATURATION_WINDOW ) );

        final String rowLabel = timeline.getHatcheryType() + " (tag " + timeline.getHatcheryTagText() + ")";
        final String detailLabel = markerList.size() + POTENTIAL_LARVA_MISSED_SUFFIX;
        return new LarvaTimelineRow( timeline.getPlayerName(), rowLabel, detailLabel, startMs, endMs, markerList.size(), segmentList, markerList );
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
        return new LarvaTimelineRow( "Replay context", "Larva fallback preview rail", "placeholder timing before replay-derived hatchery rows exist",
            0L, replayLengthMs, 0, segmentList, new ArrayList< LarvaTimelineMarker >() );
    }

    /**
     * Builds the per-player overview messages from visible hatchery rows.
     *
     * @param rowList visible timeline rows
     * @return per-player overview messages keyed by player name
     */
    private Map< String, String > buildGroupOverviewLabelMap( final List< LarvaTimelineRow > rowList ) {
        final Map< String, Integer > playerTotals = new LinkedHashMap<>();

        for ( final LarvaTimelineRow row : rowList ) {
            if ( row.getMissedLarvaCount() < 0 )
                continue;

            final String playerName = row.getGroupLabel();
            if ( playerName == null || playerName.length() == 0 || "Replay context".equals( playerName ) )
                continue;

            final Integer currentTotal = playerTotals.get( playerName );
            playerTotals.put( playerName, Integer.valueOf( ( currentTotal == null ? 0 : currentTotal.intValue() ) + row.getMissedLarvaCount() ) );
        }

        final Map< String, String > groupOverviewLabelMap = new LinkedHashMap<>();
        for ( final Map.Entry< String, Integer > entry : playerTotals.entrySet() ) {
            groupOverviewLabelMap.put( entry.getKey(), entry.getValue().intValue() + POTENTIAL_LARVA_MISSED_SUFFIX );
        }

        return groupOverviewLabelMap;
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