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
            final LarvaTimelineRow row = createReplayDerivedRow( replayLengthMs, timeline, larvaAnalysisReport );
            if ( row != null )
                rowList.add( row );
        }
    }

    /**
     * Creates one replay-derived placeholder row from a hatchery timeline.
     *
     * @param replayLengthMs replay length in milliseconds
         * @param timeline hatchery larva timeline
         * @param larvaAnalysisReport larva analysis report
     * @return timeline row; may be <code>null</code>
     */
        private LarvaTimelineRow createReplayDerivedRow( final long replayLengthMs, final HatcheryLarvaTimeline timeline,
            final LarvaAnalysisReport larvaAnalysisReport ) {
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
        final String playerName = safeText( timeline.getPlayerName(), "Unknown player" );
        final String hatcheryType = safeText( timeline.getHatcheryType(), "Hatchery" );
        final String hatcheryTagText = safeText( timeline.getHatcheryTagText(), String.valueOf( timeline.getHatcheryTag() ) );

        final List< LarvaTimelineMarker > markerList = attachMarkerHoverData( missedLarvaMarkerCalculator.buildMarkers( saturationWindowList ),
            playerName, larvaAnalysisReport );

        final List< LarvaTimelineSegment > segmentList = new ArrayList<>();
        for ( final LarvaSaturationWindow window : saturationWindowList )
            segmentList.add( new LarvaTimelineSegment( window.getStartMs(), window.getEndMs(),
                "3+ larva " + window.getStartTimeLabel() + "-" + window.getEndTimeLabel(), LarvaTimelineSegment.Kind.SATURATION_WINDOW,
                buildWindowTooltipText( window, playerName, larvaAnalysisReport ) ) );

        final String rowLabel = hatcheryType + " (tag " + hatcheryTagText + ")";
        final String detailLabel = markerList.size() + POTENTIAL_LARVA_MISSED_SUFFIX;
        return new LarvaTimelineRow( playerName, rowLabel, detailLabel, startMs, endMs, markerList.size(), segmentList, markerList );
    }

    /**
     * Attaches immutable hover metadata to marker models without leaking replay parser objects into Swing.
     *
     * @param markerList base marker list
     * @param playerName player owning the markers
     * @param larvaAnalysisReport replay analysis report containing tracker-based resource snapshots
     * @return marker list enriched with hover metadata
     */
    private List< LarvaTimelineMarker > attachMarkerHoverData( final List< LarvaTimelineMarker > markerList, final String playerName,
            final LarvaAnalysisReport larvaAnalysisReport ) {
        if ( markerList == null || markerList.isEmpty() || larvaAnalysisReport == null )
            return markerList;

        final List< LarvaTimelineMarker > enrichedMarkerList = new ArrayList<>( markerList.size() );
        for ( final LarvaTimelineMarker marker : markerList ) {
            final LarvaPlayerResourceSnapshot snapshot = larvaAnalysisReport.findLatestResourceSnapshot( playerName, marker.getLoop() );
            final LarvaMarkerHoverData hoverData = snapshot == null ? null
                    : new LarvaMarkerHoverData( playerName, snapshot.getLoop(), snapshot.getTimeLabel(), snapshot.getMineralsCurrent(), snapshot.getGasCurrent(),
                            snapshot.getFoodUsed(), snapshot.getFoodMade() );
            enrichedMarkerList.add( new LarvaTimelineMarker( marker.getLoop(), marker.getTimeMs(), marker.getLabel(), marker.getKind(), hoverData,
                    buildMarkerTooltipText( marker, hoverData ) ) );
        }

        return enrichedMarkerList;
    }

    /**
     * Builds the tooltip text for a replay-derived 3+ larva window.
     *
     * @param window saturation window
     * @param playerName player owning the window
     * @param larvaAnalysisReport analysis report containing resource snapshots
     * @return tooltip text
     */
    private String buildWindowTooltipText( final LarvaSaturationWindow window, final String playerName, final LarvaAnalysisReport larvaAnalysisReport ) {
        if ( window == null )
            return null;

        final LarvaPlayerResourceSnapshot snapshot = larvaAnalysisReport == null ? null
                : larvaAnalysisReport.findLatestResourceSnapshot( playerName, window.getStartLoop() );

        final StringBuilder builder = new StringBuilder( "<html><b>3+ larva window</b><br/>" );
        builder.append( "Window: " ).append( window.getStartTimeLabel() ).append( " - " ).append( window.getEndTimeLabel() ).append( "<br/>" );
        builder.append( buildSnapshotTooltipLines( "Window start", window.getStartTimeLabel(), snapshot ) );
        builder.append( "</html>" );
        return builder.toString();
    }

    /**
     * Builds the tooltip text for a missed-larva marker.
     *
     * @param marker missed-larva marker
     * @param hoverData attached hover metadata; may be <code>null</code>
     * @return tooltip text
     */
    private String buildMarkerTooltipText( final LarvaTimelineMarker marker, final LarvaMarkerHoverData hoverData ) {
        if ( marker == null )
            return null;

        final StringBuilder builder = new StringBuilder( "<html><b>Potential larva missed</b><br/>" );
        builder.append( marker.getLabel() ).append( "<br/>" );
        builder.append( buildSnapshotTooltipLines( "Missed moment", extractTimeFromMarkerLabel( marker.getLabel() ), hoverData ) );
        builder.append( "</html>" );
        return builder.toString();
    }

    /**
     * Builds the snapshot lines for HTML tooltips.
     *
     * @param pointLabel label describing the relevant replay point
     * @param pointTimeLabel formatted replay time of the relevant point
     * @param snapshot snapshot to render; may be <code>null</code>
     * @return HTML snippet without surrounding html/body tags
     */
    private String buildSnapshotTooltipLines( final String pointLabel, final String pointTimeLabel, final LarvaPlayerResourceSnapshot snapshot ) {
        final StringBuilder builder = new StringBuilder();
        builder.append( pointLabel ).append( ": " ).append( safeText( pointTimeLabel, "n/a" ) ).append( "<br/>" );
        if ( snapshot == null ) {
            builder.append( "Resources: unknown<br/>Supply: unknown" );
            return builder.toString();
        }

        builder.append( "Resource snapshot: " ).append( safeText( snapshot.getTimeLabel(), "n/a" ) ).append( "<br/>" );
        builder.append( "Minerals: " ).append( formatInt( snapshot.getMineralsCurrent() ) )
                .append( ", Gas: " ).append( formatInt( snapshot.getGasCurrent() ) ).append( "<br/>" );
        builder.append( "Supply: " ).append( formatSupply( snapshot.getFoodUsed(), snapshot.getFoodMade() ) );
        return builder.toString();
    }

    /**
     * Builds the snapshot lines for HTML tooltips.
     *
     * @param pointLabel label describing the relevant replay point
     * @param pointTimeLabel formatted replay time of the relevant point
     * @param hoverData hover metadata to render; may be <code>null</code>
     * @return HTML snippet without surrounding html/body tags
     */
    private String buildSnapshotTooltipLines( final String pointLabel, final String pointTimeLabel, final LarvaMarkerHoverData hoverData ) {
        final StringBuilder builder = new StringBuilder();
        builder.append( pointLabel ).append( ": " ).append( safeText( pointTimeLabel, "n/a" ) ).append( "<br/>" );
        if ( hoverData == null ) {
            builder.append( "Resources: unknown<br/>Supply: unknown" );
            return builder.toString();
        }

        builder.append( "Resource snapshot: " ).append( safeText( hoverData.getSnapshotTimeLabel(), "n/a" ) ).append( "<br/>" );
        builder.append( "Minerals: " ).append( formatInt( hoverData.getMineralsCurrent() ) )
                .append( ", Gas: " ).append( formatInt( hoverData.getGasCurrent() ) ).append( "<br/>" );
        builder.append( "Supply: " ).append( formatSupply( hoverData.getFoodUsed(), hoverData.getFoodMade() ) );
        return builder.toString();
    }

    /**
     * Extracts the trailing time portion from a marker label.
     *
     * @param label marker label
     * @return extracted time text
     */
    private String extractTimeFromMarkerLabel( final String label ) {
        if ( label == null )
            return "n/a";

        final int atIndex = label.lastIndexOf( " at " );
        return atIndex < 0 ? label : label.substring( atIndex + 4 );
    }

    /**
     * Formats an integer value or a fallback when unknown.
     *
     * @param value value to format
     * @return formatted text
     */
    private String formatInt( final Integer value ) {
        return value == null ? "unknown" : String.valueOf( value.intValue() );
    }

    /**
     * Formats fixed-point supply values as used/made.
     *
     * @param foodUsed fixed-point food used
     * @param foodMade fixed-point food made
     * @return formatted supply text
     */
    private String formatSupply( final Integer foodUsed, final Integer foodMade ) {
        if ( foodUsed == null || foodMade == null )
            return "unknown";

        return formatFixedPointSupply( foodUsed.intValue() ) + "/" + formatFixedPointSupply( foodMade.intValue() );
    }

    /**
     * Formats a fixed-point supply value.
     *
     * @param fixedPointValue raw tracker value
     * @return formatted supply component
     */
    private String formatFixedPointSupply( final int fixedPointValue ) {
        final int whole = fixedPointValue / 4096;
        final int remainder = Math.abs( fixedPointValue % 4096 );
        if ( remainder == 0 )
            return String.valueOf( whole );
        if ( remainder == 2048 )
            return whole + ".5";
        return String.valueOf( Math.round( fixedPointValue / 4096.0d * 10.0d ) / 10.0d );
    }

    /**
     * Returns a non-empty string.
     *
     * @param value value to sanitize
     * @param fallback fallback text
     * @return sanitized text
     */
    private String safeText( final String value, final String fallback ) {
        return value == null || value.length() == 0 ? fallback : value;
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