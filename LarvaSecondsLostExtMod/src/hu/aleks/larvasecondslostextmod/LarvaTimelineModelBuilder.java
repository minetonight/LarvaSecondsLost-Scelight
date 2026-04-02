package hu.aleks.larvasecondslostextmod;

import java.awt.Color;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds the normalized module-owned timeline model used by the supported Larva chart.
 */
public class LarvaTimelineModelBuilder {

    /** Maximum allowed distance between a hovered point and a resource snapshot. */
    private static final int SNAPSHOT_MATCH_WINDOW_LOOPS = 10 * 16;

    /** Minimum width of a placeholder interval so it remains visible on short replays. */
    private static final long MIN_PLACEHOLDER_WINDOW_MS = 10000L;

    /** Timeline title. */
    private static final String TITLE = "Larva pressure timeline";

    /** Timeline subtitle. */
    private static final String SUBTITLE = "Red bars show 3+ larva windows for completed Zerg hatcheries.";

    /** Timeline legend. */
    private static final String MODE_LABEL = "Black ticks mark each 11-second missed-inject threshold.";

    /** Empty message used when no rows are available yet. */
    private static final String EMPTY_MESSAGE = "No qualifying Zerg hatcheries were found in this replay.";

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
        final int replayLengthLoops = larvaAnalysisReport == null ? 0 : larvaAnalysisReport.getReplayLengthLoops();
        final String replayLengthLabel = replaySummary == null ? "0:00" : replaySummary.getLength();

        final List< LarvaTimelineRow > rowList = new ArrayList<>();
        if ( replaySummary != null && replayLengthMs > 0L && larvaAnalysisReport != null && !larvaAnalysisReport.getTimelineList().isEmpty() )
            populateReplayDerivedRows( replayLengthMs, replayLengthLoops, larvaAnalysisReport, rowList );

        if ( rowList.isEmpty() && replaySummary != null && replayLengthMs > 0L )
            rowList.add( createFallbackRow( replayLengthMs, fallbackPreviewStartMs, fallbackPreviewEndMs ) );

        return new LarvaTimelineModel( TITLE, SUBTITLE, resolveModeLabel( integrationMode ), buildGroupOverviewLabelMap( rowList ),
            buildGroupColorMap( replaySummary, rowList ), replayLengthMs, replayLengthLabel, EMPTY_MESSAGE, rowList );
    }

    /**
     * Builds replay-derived placeholder rows from per-hatchery timelines.
     *
     * @param replayLengthMs replay length in milliseconds
     * @param replayLengthLoops replay length in raw loops
     * @param larvaAnalysisReport larva analysis report
     * @param rowList target row list
     */
    private void populateReplayDerivedRows( final long replayLengthMs, final int replayLengthLoops, final LarvaAnalysisReport larvaAnalysisReport,
            final List< LarvaTimelineRow > rowList ) {
        for ( final HatcheryLarvaTimeline timeline : larvaAnalysisReport.getTimelineList() ) {
            final LarvaTimelineRow row = createReplayDerivedRow( replayLengthMs, replayLengthLoops, timeline, larvaAnalysisReport );
            if ( row != null )
                rowList.add( row );
        }
    }

    /**
     * Creates one replay-derived placeholder row from a hatchery timeline.
     *
     * @param replayLengthMs replay length in milliseconds
     * @param replayLengthLoops replay length in raw loops
     * @param timeline hatchery larva timeline
     * @param larvaAnalysisReport larva analysis report
     * @return timeline row; may be <code>null</code>
     */
    private LarvaTimelineRow createReplayDerivedRow( final long replayLengthMs, final int replayLengthLoops, final HatcheryLarvaTimeline timeline,
            final LarvaAnalysisReport larvaAnalysisReport ) {
        if ( timeline.getCountPointList().isEmpty() || !timeline.isCompleted() || timeline.getCreatedLarvaCount() <= 0 )
            return null;

        final int visibleStartLoop = saturationWindowCalculator.resolveVisibleStartLoop( timeline );
        final int visibleEndLoop = saturationWindowCalculator.resolveVisibleEndLoop( timeline, replayLengthLoops );
        if ( visibleStartLoop < 0 || visibleEndLoop <= visibleStartLoop )
            return null;

        final long startMs = clampToReplay( toTimelineMs( larvaAnalysisReport, visibleStartLoop ), replayLengthMs );
        final long endMs = clampToReplay( toTimelineMs( larvaAnalysisReport, visibleEndLoop ), replayLengthMs );
        if ( endMs <= startMs )
            return null;

        final List< LarvaSaturationWindow > saturationWindowList = saturationWindowCalculator.buildWindows( timeline, replayLengthLoops );
        final String playerName = safeText( timeline.getPlayerName(), "Unknown player" );
        final String hatcheryType = safeText( timeline.getHatcheryType(), "Hatchery" );
        final String hatcheryTagText = safeText( timeline.getHatcheryTagText(), String.valueOf( timeline.getHatcheryTag() ) );

        final List< LarvaTimelineMarker > markerList = attachMarkerHoverData( missedLarvaMarkerCalculator.buildMarkers( saturationWindowList ),
            playerName, larvaAnalysisReport );

        final List< LarvaTimelineSegment > segmentList = new ArrayList<>();
        for ( final LarvaSaturationWindow window : saturationWindowList )
            segmentList.add( new LarvaTimelineSegment( toTimelineMs( larvaAnalysisReport, window.getStartLoop() ), toTimelineMs( larvaAnalysisReport, window.getEndLoop() ),
                "3+ larva " + formatTimelineLoop( larvaAnalysisReport, window.getStartLoop() ) + "-" + formatTimelineLoop( larvaAnalysisReport, window.getEndLoop() ), LarvaTimelineSegment.Kind.SATURATION_WINDOW,
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
            final SnapshotSelection snapshotSelection = selectSnapshot( larvaAnalysisReport, playerName, marker.getLoop() );
            final LarvaPlayerResourceSnapshot snapshot = snapshotSelection.snapshot;
            final LarvaMarkerHoverData hoverData = snapshot == null ? null
                : new LarvaMarkerHoverData( playerName, snapshot.getLoop(), snapshot.getTimeLabel(), snapshot.getMineralsCurrent(), snapshot.getGasCurrent(),
                    snapshot.getFoodUsed(), snapshot.getFoodMade(), snapshotSelection.futureSnapshot );
            final String markerLabel = buildMarkerLabel( marker, larvaAnalysisReport );
            enrichedMarkerList.add( new LarvaTimelineMarker( marker.getLoop(), toTimelineMs( larvaAnalysisReport, marker.getLoop() ), markerLabel, marker.getKind(),
                hoverData, buildMarkerTooltipText( markerLabel, marker.getLoop(), hoverData, larvaAnalysisReport ) ) );
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

        final SnapshotSelection snapshotSelection = larvaAnalysisReport == null ? SnapshotSelection.NONE : selectSnapshot( larvaAnalysisReport, playerName,
                window.getStartLoop() );

        final StringBuilder builder = new StringBuilder( "<html><b>3+ larva window</b><br/>" );
        builder.append( "Window: " ).append( formatTooltipLoop( larvaAnalysisReport, window.getStartLoop() ) ).append( " - " )
            .append( formatTooltipLoop( larvaAnalysisReport, window.getEndLoop() ) ).append( "<br/>" );
        builder.append( buildSnapshotTooltipLines( "Window start", formatTooltipLoop( larvaAnalysisReport, window.getStartLoop() ), snapshotSelection ) );
        builder.append( "</html>" );
        return builder.toString();
    }

    /**
     * Builds the tooltip text for a missed-larva marker.
     *
     * @param markerLabel visible missed-larva label
         * @param markerLoop replay loop of the marker
         * @param hoverData attached hover metadata; may be <code>null</code>
     * @param larvaAnalysisReport analysis report containing replay time conversion
     * @return tooltip text
     */
        private String buildMarkerTooltipText( final String markerLabel, final int markerLoop, final LarvaMarkerHoverData hoverData,
            final LarvaAnalysisReport larvaAnalysisReport ) {
        if ( markerLabel == null )
            return null;

        final StringBuilder builder = new StringBuilder( "<html><b>Potential larva missed</b><br/>" );
        builder.append( markerLabel ).append( "<br/>" );
        builder.append( buildSnapshotTooltipLines( "Missed moment", formatTooltipLoop( larvaAnalysisReport, markerLoop ), hoverData, larvaAnalysisReport ) );
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
    private String buildSnapshotTooltipLines( final String pointLabel, final String pointTimeLabel, final SnapshotSelection snapshotSelection ) {
        final StringBuilder builder = new StringBuilder();
        builder.append( pointLabel ).append( ": " ).append( safeText( pointTimeLabel, "n/a" ) ).append( "<br/>" );

        final LarvaPlayerResourceSnapshot snapshot = snapshotSelection == null ? null : snapshotSelection.snapshot;
        if ( snapshot == null ) {
            builder.append( "Unknown at this timestamp" );
            return builder.toString();
        }

        if ( snapshotSelection.futureSnapshot )
            builder.append( "<font color='#c62828'>" );
        builder.append( snapshotSelection.futureSnapshot ? "Near-future snapshot: " : "Resource snapshot: " )
            .append( formatSnapshotLoopTime( snapshotSelection.formatter, snapshot ) ).append( "<br/>" );
        builder.append( "Minerals: " ).append( formatInt( snapshot.getMineralsCurrent() ) )
                .append( ", Gas: " ).append( formatInt( snapshot.getGasCurrent() ) ).append( "<br/>" );
        builder.append( "Supply: " ).append( formatSupply( snapshot.getFoodUsed(), snapshot.getFoodMade() ) );
        if ( snapshotSelection.futureSnapshot )
            builder.append( "</font>" );
        return builder.toString();
    }

    /**
     * Builds the snapshot lines for HTML tooltips.
     *
     * @param pointLabel label describing the relevant replay point
     * @param pointTimeLabel formatted replay time of the relevant point
     * @param hoverData hover metadata to render; may be <code>null</code>
     * @param larvaAnalysisReport analysis report containing replay time conversion
     * @return HTML snippet without surrounding html/body tags
     */
    private String buildSnapshotTooltipLines( final String pointLabel, final String pointTimeLabel, final LarvaMarkerHoverData hoverData,
            final LarvaAnalysisReport larvaAnalysisReport ) {
        final StringBuilder builder = new StringBuilder();
        builder.append( pointLabel ).append( ": " ).append( safeText( pointTimeLabel, "n/a" ) ).append( "<br/>" );
        if ( hoverData == null ) {
            builder.append( "Unknown at this timestamp" );
            return builder.toString();
        }

        if ( hoverData.isFutureSnapshot() )
            builder.append( "<font color='#c62828'>" );
        builder.append( hoverData.isFutureSnapshot() ? "Near-future snapshot: " : "Resource snapshot: " )
            .append( formatSnapshotLoopTime( larvaAnalysisReport, hoverData.getSnapshotLoop(), hoverData.getSnapshotTimeLabel() ) ).append( "<br/>" );
        builder.append( "Minerals: " ).append( formatInt( hoverData.getMineralsCurrent() ) )
                .append( ", Gas: " ).append( formatInt( hoverData.getGasCurrent() ) ).append( "<br/>" );
        builder.append( "Supply: " ).append( formatSupply( hoverData.getFoodUsed(), hoverData.getFoodMade() ) );
        if ( hoverData.isFutureSnapshot() )
            builder.append( "</font>" );
        return builder.toString();
    }

    /**
     * Formats how old the resource snapshot is compared to the event point.
     *
     * @param pointTimeLabel formatted event time
     * @param snapshotTimeLabel formatted snapshot time
     * @param snapshotLoop snapshot loop if available; negative if unknown
     * @return readable age text
     */
    /**
     * Selects the best snapshot for a hovered point.
     *
     * <p>If a future snapshot is within 10 seconds, it is preferred over an older past snapshot.
     * Otherwise the latest past snapshot is used only if it is within 10 seconds. Anything older
     * is treated as unknown at this timestamp.</p>
     *
     * @param larvaAnalysisReport analysis report holding resource snapshots
     * @param playerName player name
     * @param pointLoop hovered point loop
     * @return selected snapshot description
     */
    private SnapshotSelection selectSnapshot( final LarvaAnalysisReport larvaAnalysisReport, final String playerName, final int pointLoop ) {
        if ( larvaAnalysisReport == null || playerName == null || playerName.length() == 0 )
            return SnapshotSelection.NONE;

        final LarvaPlayerResourceSnapshot futureSnapshot = larvaAnalysisReport.findEarliestFutureResourceSnapshot( playerName, pointLoop );
        if ( futureSnapshot != null && futureSnapshot.getLoop() - pointLoop <= SNAPSHOT_MATCH_WINDOW_LOOPS )
            return new SnapshotSelection( futureSnapshot, true, larvaAnalysisReport );

        final LarvaPlayerResourceSnapshot latestSnapshot = larvaAnalysisReport.findLatestResourceSnapshot( playerName, pointLoop );
        if ( latestSnapshot != null && pointLoop - latestSnapshot.getLoop() <= SNAPSHOT_MATCH_WINDOW_LOOPS )
            return new SnapshotSelection( latestSnapshot, false, larvaAnalysisReport );

        return SnapshotSelection.NONE;
    }

    /** Selected snapshot plus its relative position to the hovered point. */
    private static class SnapshotSelection {

        /** Empty selection instance. */
        private static final SnapshotSelection NONE = new SnapshotSelection( null, false, null );

        /** Selected snapshot. */
        private final LarvaPlayerResourceSnapshot snapshot;

        /** Tells if the snapshot is from shortly after the hovered point. */
        private final boolean futureSnapshot;

        /** Analysis report used to format replay-time values. */
        private final LarvaAnalysisReport formatter;

        /**
         * Creates a new snapshot selection.
         *
         * @param snapshot selected snapshot
         * @param futureSnapshot tells if the snapshot is from shortly after the hovered point
         * @param formatter analysis report used to format replay-time values
         */
        private SnapshotSelection( final LarvaPlayerResourceSnapshot snapshot, final boolean futureSnapshot, final LarvaAnalysisReport formatter ) {
            this.snapshot = snapshot;
            this.futureSnapshot = futureSnapshot;
            this.formatter = formatter;
        }

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
     * Builds the visible marker label using Scelight-consistent time formatting.
     *
     * @param marker marker to label
     * @return formatted marker label
     */
    private String buildMarkerLabel( final LarvaTimelineMarker marker, final LarvaAnalysisReport larvaAnalysisReport ) {
        if ( marker == null )
            return null;

        final String prefix = marker.getLabel() == null ? "Missed larva" : marker.getLabel();
        final int atIndex = prefix.lastIndexOf( " at " );
        final String countPrefix = atIndex < 0 ? prefix : prefix.substring( 0, atIndex );
        return countPrefix + " at " + formatTimelineLoop( larvaAnalysisReport, marker.getLoop() );
    }

    /**
     * Converts a loop to timeline milliseconds using Scelight-consistent time conversion when available.
     *
     * @param larvaAnalysisReport analysis report containing time-conversion settings
     * @param loop replay loop
     * @return converted milliseconds
     */
    private long toTimelineMs( final LarvaAnalysisReport larvaAnalysisReport, final int loop ) {
        return larvaAnalysisReport == null ? saturationWindowCalculator.loopsToMs( loop ) : larvaAnalysisReport.loopToTimeMs( loop );
    }

    /**
     * Formats a loop with the same time basis Scelight uses for replay pages.
     *
     * @param larvaAnalysisReport analysis report containing time-conversion settings
     * @param loop replay loop
     * @return formatted time label
     */
    private String formatTimelineLoop( final LarvaAnalysisReport larvaAnalysisReport, final int loop ) {
        return larvaAnalysisReport == null ? saturationWindowCalculator.formatLoopTime( loop ) : larvaAnalysisReport.formatLoopTime( loop );
    }

    /**
     * Formats a loop with tenth-of-a-second precision for tooltip text.
     *
     * @param larvaAnalysisReport analysis report containing time-conversion settings
     * @param loop replay loop
     * @return formatted tooltip time label
     */
    private String formatTooltipLoop( final LarvaAnalysisReport larvaAnalysisReport, final int loop ) {
        return larvaAnalysisReport == null ? saturationWindowCalculator.formatLoopTime( loop ) : larvaAnalysisReport.formatLoopTimeTenths( loop );
    }

    /**
     * Formats a snapshot time using the replay time basis when possible.
     *
     * @param formatter analysis report containing time-conversion settings
     * @param snapshot resource snapshot to format
     * @return formatted snapshot time label
     */
    private String formatSnapshotLoopTime( final LarvaAnalysisReport formatter, final LarvaPlayerResourceSnapshot snapshot ) {
        if ( snapshot == null )
            return "n/a";

        return formatter == null ? safeText( snapshot.getTimeLabel(), "n/a" ) : formatter.formatLoopTimeTenths( snapshot.getLoop() );
    }

    /**
     * Formats a snapshot time from raw loop data with a label fallback.
     *
     * @param formatter analysis report containing time-conversion settings
     * @param snapshotLoop snapshot loop
     * @param fallbackLabel fallback label when no formatter is available
     * @return formatted snapshot time label
     */
    private String formatSnapshotLoopTime( final LarvaAnalysisReport formatter, final int snapshotLoop, final String fallbackLabel ) {
        return formatter == null ? safeText( fallbackLabel, "n/a" ) : formatter.formatLoopTimeTenths( snapshotLoop );
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
        return new LarvaTimelineRow( "Replay context", "Replay overview", "No 3+ larva windows reached the warning threshold",
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
     * Builds player display colors for the visible group labels.
     *
     * @param replaySummary replay summary holding player colors
     * @param rowList visible timeline rows
     * @return group color map keyed by player name
     */
    private Map< String, Color > buildGroupColorMap( final ReplaySummary replaySummary, final List< LarvaTimelineRow > rowList ) {
        final Map< String, Color > groupColorMap = new LinkedHashMap<>();
        if ( replaySummary == null || rowList == null || rowList.isEmpty() )
            return groupColorMap;

        for ( final LarvaTimelineRow row : rowList ) {
            final String playerName = row.getGroupLabel();
            if ( playerName == null || playerName.length() == 0 || groupColorMap.containsKey( playerName ) )
                continue;

            final Color playerColor = replaySummary.getPlayerColor( playerName );
            if ( playerColor != null )
                groupColorMap.put( playerName, adjustForReadability( playerColor ) );
        }

        return groupColorMap;
    }

    /**
     * Keeps very bright player colors readable on a white background.
     *
     * @param color player color
     * @return adjusted color
     */
    private Color adjustForReadability( final Color color ) {
        if ( color == null )
            return null;

        final int brightness = color.getRed() + color.getGreen() + color.getBlue();
        if ( brightness > 540 )
            return color.darker().darker();
        if ( brightness > 440 )
            return color.darker();
        return color;
    }

    /**
     * Returns the label shown below the chart title.
     *
     * @param integrationMode integration-mode label passed by the page summary
     * @return user-facing legend label
     */
    private String resolveModeLabel( final String integrationMode ) {
        return integrationMode == null || integrationMode.length() == 0 ? MODE_LABEL : MODE_LABEL;
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