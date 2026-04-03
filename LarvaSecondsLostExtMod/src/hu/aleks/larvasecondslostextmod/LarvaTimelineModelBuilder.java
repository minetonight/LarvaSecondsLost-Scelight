package hu.aleks.larvasecondslostextmod;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds the normalized module-owned timeline model used by the supported Larva chart.
 */
public class LarvaTimelineModelBuilder {

    /** Maximum allowed distance between a hovered point and a resource snapshot. */
    private static final int SNAPSHOT_MATCH_WINDOW_LOOPS = 10 * 16;

    /** Replay loops per second used by SC2 timing. */
    private static final int REPLAY_LOOPS_PER_SECOND = 16;

    /** Dot-column spacing in visible gameplay seconds for 1- and 2-larva rhythm hints. */
    private static final int LARVA_DOT_STEP_SECONDS = 2;

    /** Scelight normal-speed relative value. */
    private static final double NORMAL_GAME_SPEED_RELATIVE = 36.0d;

    /** Scelight Faster-speed relative value used as the fallback default. */
    private static final long DEFAULT_GAME_SPEED_RELATIVE = 26L;

    /** First cumulative label shown inside a 3+ larva window. */
    private static final int ACCUMULATION_LABEL_START = 6;

    /** Step between cumulative labels shown inside a 3+ larva window. */
    private static final int ACCUMULATION_LABEL_STEP = 3;

    /** Minimum width of a placeholder interval so it remains visible on short replays. */
    private static final long MIN_PLACEHOLDER_WINDOW_MS = 10000L;

    /** Timeline title. */
    private static final String TITLE = "Larva pressure timeline";

    /** Timeline subtitle. */
    private static final String SUBTITLE = "Red bars show 3+ larva windows; a green lane shows injected status windows; a lower dark red lane shows missed inject potential windows with black 29-second threshold ticks.";

    /** Timeline legend. */
    private static final String MODE_LABEL = "Green lanes show injected status windows; lower dark red lanes show conservative missed inject potential windows; black ticks mark 11-second larva-loss thresholds on the main rail and 29-second inject-loss thresholds on the dark red lane.";

    /** Empty message used when no rows are available yet. */
    private static final String EMPTY_MESSAGE = "No qualifying Zerg hatcheries were found in this replay.";

    /** Suffix used for per-hatchery totals. */
    private static final String POTENTIAL_LARVA_MISSED_SUFFIX = " potential larva missed";

    /** Suffix used for per-hatchery injected-larva totals. */
    private static final String POTENTIAL_INJECTED_LARVA_MISSED_SUFFIX = " potential injected larva missed";

    /** Visible gameplay seconds represented by one missed-inject threshold. */
    private static final int MISSED_INJECT_THRESHOLD_SECONDS = 29;

    /** Potential injected larva counted for each missed-inject threshold. */
    private static final int MISSED_INJECT_LARVA_PER_THRESHOLD = 3;

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

        return new LarvaTimelineModel( TITLE, SUBTITLE, resolveModeLabel( integrationMode ), buildGroupOverviewLabelMap( rowList ),
            buildGroupColorMap( replaySummary, rowList ), replayLengthMs, replayLengthLabel, EMPTY_MESSAGE, rowList );
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
        final int visibleEndLoop = saturationWindowCalculator.resolveVisibleEndLoop( timeline, larvaAnalysisReport.getReplayLengthLoops() );
        if ( visibleStartLoop < 0 || visibleEndLoop <= visibleStartLoop )
            return null;

        final long startMs = clampToReplay( toTimelineMs( larvaAnalysisReport, visibleStartLoop ), replayLengthMs );
        final long endMs = clampToReplay( toTimelineMs( larvaAnalysisReport, visibleEndLoop ), replayLengthMs );
        if ( endMs <= startMs )
            return null;

        final List< LarvaSaturationWindow > saturationWindowList = saturationWindowCalculator.buildWindows( timeline, larvaAnalysisReport.getReplayLengthLoops() );
        final String playerName = safeText( timeline.getPlayerName(), "Unknown player" );
        final String hatcheryType = safeText( timeline.getHatcheryType(), "Hatchery" );
        final String hatcheryTagText = safeText( timeline.getHatcheryTagText(), String.valueOf( timeline.getHatcheryTag() ) );

        final HatcheryInjectTimeline injectTimeline = findInjectTimeline( larvaAnalysisReport, timeline.getHatcheryTag() );
        final HatcheryIdleInjectTimeline idleInjectTimeline = findIdleInjectTimeline( larvaAnalysisReport, timeline.getHatcheryTag() );

        final List< LarvaTimelineMarker > rawMarkerList = new ArrayList<>();
        rawMarkerList.addAll( missedLarvaMarkerCalculator.buildMarkers( saturationWindowList, larvaAnalysisReport.getConverterGameSpeedRelative() ) );
        rawMarkerList.addAll( buildMissedInjectMarkers( idleInjectTimeline, larvaAnalysisReport ) );
        sortMarkersByLoop( rawMarkerList );

        final List< LarvaTimelineMarker > markerList = attachMarkerHoverData( rawMarkerList, playerName, larvaAnalysisReport );
        final List< LarvaTimelineDecoration > decorationList = buildDecorations( timeline, visibleStartLoop, visibleEndLoop, larvaAnalysisReport );

        final List< LarvaTimelineSegment > segmentList = new ArrayList<>();
        for ( final LarvaSaturationWindow window : saturationWindowList )
            segmentList.add( new LarvaTimelineSegment( toTimelineMs( larvaAnalysisReport, window.getStartLoop() ), toTimelineMs( larvaAnalysisReport, window.getEndLoop() ),
                "3+ larva " + formatTimelineLoop( larvaAnalysisReport, window.getStartLoop() ) + "-" + formatTimelineLoop( larvaAnalysisReport, window.getEndLoop() ), LarvaTimelineSegment.Kind.SATURATION_WINDOW,
                buildWindowTooltipText( window, playerName, larvaAnalysisReport ) ) );

        appendInjectSegments( segmentList, injectTimeline, larvaAnalysisReport );
        appendIdleInjectSegments( segmentList, idleInjectTimeline, larvaAnalysisReport );

        final String rowLabel = hatcheryType + " (tag " + hatcheryTagText + ")";
        final int missedLarvaCount = countMarkers( markerList, LarvaTimelineMarker.Kind.MISSED_LARVA );
        final int potentialInjectedLarvaMissedCount = countMarkers( markerList, LarvaTimelineMarker.Kind.MISSED_INJECT_LARVA ) * MISSED_INJECT_LARVA_PER_THRESHOLD;
        final String detailLabel = missedLarvaCount + POTENTIAL_LARVA_MISSED_SUFFIX;
        final String secondaryDetailLabel = potentialInjectedLarvaMissedCount + POTENTIAL_INJECTED_LARVA_MISSED_SUFFIX;
        return new LarvaTimelineRow( playerName, rowLabel, detailLabel, secondaryDetailLabel, startMs, endMs, missedLarvaCount,
            potentialInjectedLarvaMissedCount, segmentList, markerList, decorationList );
    }

    /**
     * Rebuilds gray dot and accumulation-label decorations from hatchery count points.
     *
     * @param timeline hatchery timeline
     * @param larvaAnalysisReport analysis report used for loop-to-time conversion
     * @return immutable-ready decoration list
     */
    private List< LarvaTimelineDecoration > buildDecorations( final HatcheryLarvaTimeline timeline, final int visibleStartLoop,
            final int visibleEndLoop, final LarvaAnalysisReport larvaAnalysisReport ) {
        final List< HatcheryLarvaTimeline.CountPoint > pointList = normalizeCountPoints( timeline, visibleEndLoop );
        if ( pointList.isEmpty() )
            return Collections.emptyList();

        final List< LarvaTimelineDecoration > decorationList = new ArrayList<>();
        int previousLarvaCount = 0;

        for ( int i = 0; i < pointList.size(); i++ ) {
            final HatcheryLarvaTimeline.CountPoint point = pointList.get( i );
            final int intervalStartLoop = Math.max( point.getLoop(), visibleStartLoop );
            final int nextLoop = i + 1 < pointList.size() ? pointList.get( i + 1 ).getLoop() : visibleEndLoop;
            final int intervalEndLoop = Math.min( nextLoop, visibleEndLoop );
            if ( intervalEndLoop <= intervalStartLoop )
                continue;

            final int larvaCount = point.getLarvaCount();
            if ( larvaCount == 1 || larvaCount == 2 )
                addDotDecorations( decorationList, intervalStartLoop, intervalEndLoop, larvaCount, larvaAnalysisReport );
            if ( larvaCount >= 3 )
                addLarvaCountLabels( decorationList, intervalStartLoop, previousLarvaCount, larvaCount, larvaAnalysisReport );
            previousLarvaCount = larvaCount;
        }

        return decorationList;
    }

    /**
     * Adds one- or two-larva dot columns for a visible interval.
     *
     * @param decorationList target decoration list
     * @param startLoop visible interval start loop
     * @param endLoop visible interval end loop
     * @param larvaCount larva count represented by the dots
     * @param larvaAnalysisReport analysis report used for time conversion
     */
    private void addDotDecorations( final List< LarvaTimelineDecoration > decorationList, final int startLoop, final int endLoop,
            final int larvaCount, final LarvaAnalysisReport larvaAnalysisReport ) {
        final int larvaDotStepLoops = resolveLarvaDotStepLoops( larvaAnalysisReport == null ? 0L : larvaAnalysisReport.getConverterGameSpeedRelative() );
        final int durationLoops = endLoop - startLoop;
        if ( durationLoops <= 0 || larvaDotStepLoops <= 0 )
            return;

        if ( durationLoops <= larvaDotStepLoops ) {
            decorationList.add( new LarvaTimelineDecoration( LarvaTimelineDecoration.Kind.LARVA_DOT_COLUMN,
                    toTimelineMs( larvaAnalysisReport, startLoop + durationLoops / 2 ), larvaCount, null ) );
            return;
        }

        for ( int loop = startLoop + larvaDotStepLoops / 2; loop < endLoop; loop += larvaDotStepLoops )
            decorationList.add( new LarvaTimelineDecoration( LarvaTimelineDecoration.Kind.LARVA_DOT_COLUMN,
                    toTimelineMs( larvaAnalysisReport, loop ), larvaCount, null ) );
    }

    /**
     * Resolves the periodic gray-dot spacing in replay loops for the replay game speed.
     *
     * @param gameSpeedRelative replay game-speed relative value
     * @return gray-dot spacing in replay loops
     */
    private int resolveLarvaDotStepLoops( final long gameSpeedRelative ) {
        final double effectiveGameSpeedRelative = gameSpeedRelative <= 0L ? DEFAULT_GAME_SPEED_RELATIVE : gameSpeedRelative;
        return (int) ( LARVA_DOT_STEP_SECONDS * REPLAY_LOOPS_PER_SECOND * ( NORMAL_GAME_SPEED_RELATIVE / effectiveGameSpeedRelative ) );
    }

    /**
     * Adds actual 6/9/12... larva-count labels when a hatchery reaches those counts.
     *
     * @param decorationList target decoration list
     * @param loop loop where the current larva count becomes active
     * @param previousLarvaCount previous larva count before the change
     * @param larvaCount current larva count after the change
     * @param larvaAnalysisReport analysis report used for time conversion
     */
    private void addLarvaCountLabels( final List< LarvaTimelineDecoration > decorationList, final int loop, final int previousLarvaCount,
            final int larvaCount, final LarvaAnalysisReport larvaAnalysisReport ) {
        if ( larvaCount < ACCUMULATION_LABEL_START || larvaCount <= previousLarvaCount )
            return;

        for ( int labelValue = ACCUMULATION_LABEL_START; labelValue <= larvaCount; labelValue += ACCUMULATION_LABEL_STEP ) {
            if ( labelValue <= previousLarvaCount )
                continue;

            decorationList.add( new LarvaTimelineDecoration( LarvaTimelineDecoration.Kind.ACCUMULATION_LABEL,
                    toTimelineMs( larvaAnalysisReport, loop ), 0, String.valueOf( labelValue ) ) );
        }
    }

    /**
     * Normalizes count points for deterministic row-decoration placement.
     *
     * @param timeline hatchery timeline
     * @param visibleEndLoop visible row end loop
     * @return normalized point list
     */
    private List< HatcheryLarvaTimeline.CountPoint > normalizeCountPoints( final HatcheryLarvaTimeline timeline, final int visibleEndLoop ) {
        if ( timeline == null || timeline.getCountPointList().isEmpty() )
            return Collections.emptyList();

        final List< HatcheryLarvaTimeline.CountPoint > sortedPointList = new ArrayList<>();
        for ( final HatcheryLarvaTimeline.CountPoint point : timeline.getCountPointList() ) {
            if ( point == null || point.getLoop() < 0 || point.getLoop() > visibleEndLoop )
                continue;
            sortedPointList.add( point );
        }

        if ( sortedPointList.isEmpty() )
            return Collections.emptyList();

        Collections.sort( sortedPointList, new Comparator< HatcheryLarvaTimeline.CountPoint >() {
            @Override
            public int compare( final HatcheryLarvaTimeline.CountPoint left, final HatcheryLarvaTimeline.CountPoint right ) {
                return left.getLoop() < right.getLoop() ? -1 : left.getLoop() == right.getLoop() ? 0 : 1;
            }
        } );

        final List< HatcheryLarvaTimeline.CountPoint > normalizedPointList = new ArrayList<>( sortedPointList.size() );
        for ( final HatcheryLarvaTimeline.CountPoint point : sortedPointList ) {
            if ( !normalizedPointList.isEmpty() && normalizedPointList.get( normalizedPointList.size() - 1 ).getLoop() == point.getLoop() )
                normalizedPointList.set( normalizedPointList.size() - 1, point );
            else
                normalizedPointList.add( point );
        }

        return normalizedPointList;
    }

    /**
     * Resolves the inject timeline for a hatchery tag.
     *
     * @param larvaAnalysisReport analysis report
     * @param hatcheryTag hatchery tag to resolve
     * @return matching inject timeline; may be <code>null</code>
     */
    private HatcheryInjectTimeline findInjectTimeline( final LarvaAnalysisReport larvaAnalysisReport, final int hatcheryTag ) {
        if ( larvaAnalysisReport == null || larvaAnalysisReport.getInjectTimelineList() == null )
            return null;

        for ( final HatcheryInjectTimeline injectTimeline : larvaAnalysisReport.getInjectTimelineList() )
            if ( injectTimeline != null && injectTimeline.getHatcheryTag() == hatcheryTag )
                return injectTimeline;

        return null;
    }

    /**
     * Resolves the idle-inject timeline for a hatchery tag.
     *
     * @param larvaAnalysisReport analysis report
     * @param hatcheryTag hatchery tag to resolve
     * @return matching idle-inject timeline; may be <code>null</code>
     */
    private HatcheryIdleInjectTimeline findIdleInjectTimeline( final LarvaAnalysisReport larvaAnalysisReport, final int hatcheryTag ) {
        if ( larvaAnalysisReport == null || larvaAnalysisReport.getIdleInjectTimelineList() == null )
            return null;

        for ( final HatcheryIdleInjectTimeline idleInjectTimeline : larvaAnalysisReport.getIdleInjectTimelineList() )
            if ( idleInjectTimeline != null && idleInjectTimeline.getHatcheryTag() == hatcheryTag )
                return idleInjectTimeline;

        return null;
    }

    /**
     * Appends normalized inject-window segments to a row segment list.
     *
     * @param segmentList target segment list
     * @param injectTimeline inject timeline for the hatchery
     * @param larvaAnalysisReport analysis report used for snapshot lookup
     */
    private void appendInjectSegments( final List< LarvaTimelineSegment > segmentList, final HatcheryInjectTimeline injectTimeline,
            final LarvaAnalysisReport larvaAnalysisReport ) {
        if ( segmentList == null || injectTimeline == null || injectTimeline.getInjectWindowList().isEmpty() )
            return;

        for ( final HatcheryInjectWindow injectWindow : injectTimeline.getInjectWindowList() ) {
            if ( injectWindow == null )
                continue;

            segmentList.add( new LarvaTimelineSegment( injectWindow.getStartMs(), injectWindow.getEndMs(),
                    "Inject " + injectWindow.getStartTimeLabel() + "-" + injectWindow.getEndTimeLabel(), LarvaTimelineSegment.Kind.INJECT_WINDOW,
                    buildInjectTooltipText( injectWindow, injectTimeline, larvaAnalysisReport ) ) );
        }
    }

    /**
     * Appends normalized idle-inject segments to a row segment list.
     *
     * @param segmentList target segment list
     * @param idleInjectTimeline idle-inject timeline for the hatchery
     * @param larvaAnalysisReport analysis report used for snapshot lookup
     */
    private void appendIdleInjectSegments( final List< LarvaTimelineSegment > segmentList,
            final HatcheryIdleInjectTimeline idleInjectTimeline, final LarvaAnalysisReport larvaAnalysisReport ) {
        if ( segmentList == null || idleInjectTimeline == null || idleInjectTimeline.getIdleWindowList().isEmpty() )
            return;

        for ( final HatcheryIdleInjectWindow idleWindow : idleInjectTimeline.getIdleWindowList() ) {
            if ( idleWindow == null )
                continue;

            final long startMs = idleWindow.getStartTimeLabel() == null ? toTimelineMs( larvaAnalysisReport, idleWindow.getStartLoop() ) : idleWindow.getStartMs();
            final long endMs = idleWindow.getEndTimeLabel() == null ? toTimelineMs( larvaAnalysisReport, idleWindow.getEndLoop() ) : idleWindow.getEndMs();
            final String startTimeLabel = idleWindow.getStartTimeLabel() == null ? formatTimelineLoop( larvaAnalysisReport, idleWindow.getStartLoop() ) : idleWindow.getStartTimeLabel();
            final String endTimeLabel = idleWindow.getEndTimeLabel() == null ? formatTimelineLoop( larvaAnalysisReport, idleWindow.getEndLoop() ) : idleWindow.getEndTimeLabel();
            segmentList.add( new LarvaTimelineSegment( startMs, endMs,
                    "Idle inject " + startTimeLabel + "-" + endTimeLabel, LarvaTimelineSegment.Kind.IDLE_INJECT_WINDOW,
                    buildIdleInjectTooltipText( idleWindow, idleInjectTimeline, larvaAnalysisReport, startTimeLabel, endTimeLabel ) ) );
        }
    }

    /**
     * Builds black threshold markers for accumulated missed inject windows.
     *
     * @param idleInjectTimeline idle inject timeline
     * @param larvaAnalysisReport analysis report used for timing conversion
     * @return chronological marker list
     */
    private List< LarvaTimelineMarker > buildMissedInjectMarkers( final HatcheryIdleInjectTimeline idleInjectTimeline,
            final LarvaAnalysisReport larvaAnalysisReport ) {
        if ( idleInjectTimeline == null || idleInjectTimeline.getIdleWindowList().isEmpty() )
            return Collections.emptyList();

        final List< HatcheryIdleInjectWindow > sortedWindowList = new ArrayList<>( idleInjectTimeline.getIdleWindowList() );
        Collections.sort( sortedWindowList, new Comparator< HatcheryIdleInjectWindow >() {
            @Override
            public int compare( final HatcheryIdleInjectWindow left, final HatcheryIdleInjectWindow right ) {
                if ( left.getStartLoop() != right.getStartLoop() )
                    return left.getStartLoop() < right.getStartLoop() ? -1 : 1;
                return left.getEndLoop() < right.getEndLoop() ? -1 : left.getEndLoop() == right.getEndLoop() ? 0 : 1;
            }
        } );

        final List< LarvaTimelineMarker > markerList = new ArrayList<>();
        final int thresholdLoops = resolveMissedInjectThresholdLoops( larvaAnalysisReport == null ? 0L : larvaAnalysisReport.getConverterGameSpeedRelative() );
        int accumulatedLoops = 0;
        int potentialInjectedLarvaMissedCount = 0;
        int coveredUntilLoop = Integer.MIN_VALUE;

        for ( final HatcheryIdleInjectWindow idleWindow : sortedWindowList ) {
            if ( idleWindow == null )
                continue;

            final int effectiveStartLoop = Math.max( idleWindow.getStartLoop(), coveredUntilLoop );
            final int effectiveEndLoop = idleWindow.getEndLoop();
            if ( effectiveEndLoop <= effectiveStartLoop )
                continue;

            int loopsRemainingInWindow = effectiveEndLoop - effectiveStartLoop;
            int markerLoop = effectiveStartLoop;

            while ( accumulatedLoops + loopsRemainingInWindow >= thresholdLoops ) {
                final int loopsUntilThreshold = thresholdLoops - accumulatedLoops;
                markerLoop += loopsUntilThreshold;
                potentialInjectedLarvaMissedCount += MISSED_INJECT_LARVA_PER_THRESHOLD;
                markerList.add( new LarvaTimelineMarker( markerLoop, toTimelineMs( larvaAnalysisReport, markerLoop ),
                        "Potential injected larva missed " + potentialInjectedLarvaMissedCount + " at " + formatTimelineLoop( larvaAnalysisReport, markerLoop ),
                        LarvaTimelineMarker.Kind.MISSED_INJECT_LARVA ) );

                loopsRemainingInWindow -= loopsUntilThreshold;
                accumulatedLoops = 0;
            }

            accumulatedLoops += loopsRemainingInWindow;
            coveredUntilLoop = Math.max( coveredUntilLoop, effectiveEndLoop );
        }

        return markerList;
    }

    /**
     * Resolves the loop threshold corresponding to 29 visible game-timer seconds.
     *
     * @param gameSpeedRelative replay game-speed relative value
     * @return threshold in replay loops
     */
    private int resolveMissedInjectThresholdLoops( final long gameSpeedRelative ) {
        final double effectiveGameSpeedRelative = gameSpeedRelative <= 0L ? DEFAULT_GAME_SPEED_RELATIVE : gameSpeedRelative;
        return (int) ( MISSED_INJECT_THRESHOLD_SECONDS * REPLAY_LOOPS_PER_SECOND * ( NORMAL_GAME_SPEED_RELATIVE / effectiveGameSpeedRelative ) );
    }

    /**
     * Sorts markers chronologically, keeping main-rail markers before dark-red-lane markers on ties.
     *
     * @param markerList marker list to sort in place
     */
    private void sortMarkersByLoop( final List< LarvaTimelineMarker > markerList ) {
        Collections.sort( markerList, new Comparator< LarvaTimelineMarker >() {
            @Override
            public int compare( final LarvaTimelineMarker left, final LarvaTimelineMarker right ) {
                if ( left.getLoop() != right.getLoop() )
                    return left.getLoop() < right.getLoop() ? -1 : 1;
                if ( left.getKind() == right.getKind() )
                    return 0;
                return left.getKind() == LarvaTimelineMarker.Kind.MISSED_LARVA ? -1 : 1;
            }
        } );
    }

    /**
     * Counts markers of the specified kind.
     *
     * @param markerList marker list
     * @param kind marker kind
     * @return marker count
     */
    private int countMarkers( final List< LarvaTimelineMarker > markerList, final LarvaTimelineMarker.Kind kind ) {
        int count = 0;
        for ( final LarvaTimelineMarker marker : markerList )
            if ( marker != null && marker.getKind() == kind )
                count++;
        return count;
    }

    /**
     * Builds the tooltip text for a replay-derived idle-inject window.
     */
    private String buildIdleInjectTooltipText( final HatcheryIdleInjectWindow idleWindow, final HatcheryIdleInjectTimeline idleInjectTimeline,
            final LarvaAnalysisReport larvaAnalysisReport, final String startTimeLabel, final String endTimeLabel ) {
        final StringBuilder builder = new StringBuilder( "<html><b>Missed inject window</b><br/>" );
        builder.append( "Hatchery: " ).append( safeText( idleInjectTimeline == null ? null : idleInjectTimeline.getHatcheryTagText(), "n/a" ) ).append( "<br/>" );
        builder.append( "Window: " ).append( safeText( startTimeLabel, "n/a" ) ).append( " - " ).append( safeText( endTimeLabel, "n/a" ) ).append( "<br/>" );
        builder.append( "This hatchery appears to have been ready for an inject during this period.<br/>" );
        builder.append( "Each full 29-second stretch without an inject is worth 3 potential larva.<br/>" );
        builder.append( "This is an estimate reconstructed from replay events." );
        builder.append( "</html>" );
        return builder.toString();
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
                hoverData, buildMarkerTooltipText( marker, markerLabel, hoverData ) ) );
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
        builder.append( "Window: " ).append( formatTimelineLoop( larvaAnalysisReport, window.getStartLoop() ) ).append( " - " )
            .append( formatTimelineLoop( larvaAnalysisReport, window.getEndLoop() ) ).append( "<br/>" );
        builder.append( buildSnapshotTooltipLines( "Window start", formatTimelineLoop( larvaAnalysisReport, window.getStartLoop() ), snapshotSelection ) );
        builder.append( "</html>" );
        return builder.toString();
    }

    /**
     * Builds the tooltip text for a replay-derived inject-active window.
     *
     * @param injectWindow inject window
     * @param injectTimeline timeline owning the window
     * @param larvaAnalysisReport analysis report containing resource snapshots
     * @return tooltip text
     */
    private String buildInjectTooltipText( final HatcheryInjectWindow injectWindow, final HatcheryInjectTimeline injectTimeline,
            final LarvaAnalysisReport larvaAnalysisReport ) {
        if ( injectWindow == null )
            return null;

        final SnapshotSelection snapshotSelection = larvaAnalysisReport == null || injectTimeline == null ? SnapshotSelection.NONE
                : selectSnapshot( larvaAnalysisReport, injectTimeline.getPlayerName(), injectWindow.getStartLoop() );

        final StringBuilder builder = new StringBuilder( "<html><b>Inject active</b><br/>" );
        builder.append( "Window: " ).append( safeText( injectWindow.getStartTimeLabel(), "n/a" ) ).append( " - " )
                .append( safeText( injectWindow.getEndTimeLabel(), "n/a" ) ).append( "<br/>" );
        builder.append( "Command: " ).append( safeText( injectWindow.getCommandTimeLabel(), "n/a" ) ).append( "<br/>" );
        builder.append( buildSnapshotTooltipLines( "Inject start", safeText( injectWindow.getStartTimeLabel(), "n/a" ), snapshotSelection ) );
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
    private String buildMarkerTooltipText( final LarvaTimelineMarker marker, final String markerLabel, final LarvaMarkerHoverData hoverData ) {
        if ( markerLabel == null )
            return null;

        if ( marker != null && marker.getKind() == LarvaTimelineMarker.Kind.MISSED_INJECT_LARVA ) {
            final StringBuilder injectBuilder = new StringBuilder( "<html><b>Potential injected larva missed</b><br/>" );
            injectBuilder.append( markerLabel ).append( "<br/>" );
            injectBuilder.append( "This black tick marks another full missed inject cycle.<br/>" );
            injectBuilder.append( "Value: 3 potential larva from one hatchery over 29 seconds.<br/>" );
            injectBuilder.append( buildSnapshotTooltipLines( "Replay moment", extractTimeFromMarkerLabel( markerLabel ), hoverData ) );
            injectBuilder.append( "</html>" );
            return injectBuilder.toString();
        }

        final StringBuilder builder = new StringBuilder( "<html><b>" );
        builder.append( marker != null && marker.getKind() == LarvaTimelineMarker.Kind.MISSED_INJECT_LARVA ? "Potential injected larva missed"
                : "Potential larva missed" );
        builder.append( "</b><br/>" );
        builder.append( markerLabel ).append( "<br/>" );
        builder.append( buildSnapshotTooltipLines( "Missed moment", extractTimeFromMarkerLabel( markerLabel ), hoverData ) );
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
                .append( safeText( snapshot.getTimeLabel(), "n/a" ) ).append( "<br/>" );
        builder.append( snapshotSelection.futureSnapshot ? "Minerals: " : "Minerals: " ).append( formatInt( snapshot.getMineralsCurrent() ) )
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
     * @return HTML snippet without surrounding html/body tags
     */
    private String buildSnapshotTooltipLines( final String pointLabel, final String pointTimeLabel, final LarvaMarkerHoverData hoverData ) {
        final StringBuilder builder = new StringBuilder();
        builder.append( pointLabel ).append( ": " ).append( safeText( pointTimeLabel, "n/a" ) ).append( "<br/>" );
        if ( hoverData == null ) {
            builder.append( "Unknown at this timestamp" );
            return builder.toString();
        }

        if ( hoverData.isFutureSnapshot() )
            builder.append( "<font color='#c62828'>" );
        builder.append( hoverData.isFutureSnapshot() ? "Near-future snapshot: " : "Resource snapshot: " )
                .append( safeText( hoverData.getSnapshotTimeLabel(), "n/a" ) ).append( "<br/>" );
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
            return new SnapshotSelection( futureSnapshot, true );

        final LarvaPlayerResourceSnapshot latestSnapshot = larvaAnalysisReport.findLatestResourceSnapshot( playerName, pointLoop );
        if ( latestSnapshot != null && pointLoop - latestSnapshot.getLoop() <= SNAPSHOT_MATCH_WINDOW_LOOPS )
            return new SnapshotSelection( latestSnapshot, false );

        return SnapshotSelection.NONE;
    }

    /** Selected snapshot plus its relative position to the hovered point. */
    private static class SnapshotSelection {

        /** Empty selection instance. */
        private static final SnapshotSelection NONE = new SnapshotSelection( null, false );

        /** Selected snapshot. */
        private final LarvaPlayerResourceSnapshot snapshot;

        /** Tells if the snapshot is from shortly after the hovered point. */
        private final boolean futureSnapshot;

        /**
         * Creates a new snapshot selection.
         *
         * @param snapshot selected snapshot
         * @param futureSnapshot tells if the snapshot is from shortly after the hovered point
         */
        private SnapshotSelection( final LarvaPlayerResourceSnapshot snapshot, final boolean futureSnapshot ) {
            this.snapshot = snapshot;
            this.futureSnapshot = futureSnapshot;
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
        final Map< String, Integer > playerInjectedTotals = new LinkedHashMap<>();

        for ( final LarvaTimelineRow row : rowList ) {
            if ( row.getMissedLarvaCount() < 0 )
                continue;

            final String playerName = row.getGroupLabel();
            if ( playerName == null || playerName.length() == 0 || "Replay context".equals( playerName ) )
                continue;

            final Integer currentTotal = playerTotals.get( playerName );
            playerTotals.put( playerName, Integer.valueOf( ( currentTotal == null ? 0 : currentTotal.intValue() ) + row.getMissedLarvaCount() ) );

        final Integer currentInjectedTotal = playerInjectedTotals.get( playerName );
        playerInjectedTotals.put( playerName,
            Integer.valueOf( ( currentInjectedTotal == null ? 0 : currentInjectedTotal.intValue() ) + row.getPotentialInjectedLarvaMissedCount() ) );
        }

        final Map< String, String > groupOverviewLabelMap = new LinkedHashMap<>();
        for ( final Map.Entry< String, Integer > entry : playerTotals.entrySet() ) {
        final Integer injectedTotal = playerInjectedTotals.get( entry.getKey() );
        groupOverviewLabelMap.put( entry.getKey(), entry.getValue().intValue() + POTENTIAL_LARVA_MISSED_SUFFIX + "; "
            + ( injectedTotal == null ? 0 : injectedTotal.intValue() ) + POTENTIAL_INJECTED_LARVA_MISSED_SUFFIX );
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