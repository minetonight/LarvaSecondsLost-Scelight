package hu.aleks.larvasecondslostextmod;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Result of the Epic 6 larva-to-hatchery assignment foundation analysis.
 */
public class LarvaAnalysisReport {

    /** Normal-speed relative value used by Scelight game-speed conversion. */
    private static final long NORMAL_GAME_SPEED_RELATIVE = 36L;

    /** Calibration used by the assignment heuristic. */
    private final LarvaHeuristicCalibration calibration;

    /** Per-hatchery count timelines. */
    private final List< HatcheryLarvaTimeline > timelineList;

    /** Story 11.01 answer for the replay-native inject signal question. */
    private final String injectSignalConclusion;

    /** Per-hatchery normalized inject-window diagnostics. */
    private final List< HatcheryInjectTimeline > injectTimelineList;

    /** Number of hatcheries tracked during replay analysis. */
    private final int trackedHatcheryCount;

    /** Number of larva birth events observed. */
    private final int larvaBirthCount;

    /** Number of larva assigned to a hatchery. */
    private final int assignedLarvaCount;

    /** Number of larva that remained ambiguous or unassigned. */
    private final int unassignedLarvaCount;

    /** Number of unassigned larva caused by ambiguous multi-hatchery matches. */
    private final int ambiguousLarvaCount;

    /** Number of unassigned larva caused by no eligible hatchery within range. */
    private final int noEligibleHatcheryLarvaCount;

    /** Direct creator-based assignments. */
    private final int directAssignmentCount;

    /** Inject-correlated assignments. */
    private final int injectCorrelatedAssignmentCount;

    /** Pure heuristic assignments. */
    private final int heuristicAssignmentCount;

    /** Number of hatchery morph continuations detected. */
    private final int hatcheryMorphCount;

    /** Number of tracker events available during analysis. */
    private final int trackerEventCount;

    /** Number of game events available during analysis. */
    private final int gameEventCount;

    /** Tells if full replay reparsing was required to get complete event streams. */
    private final boolean fullReplayParseUsed;

    /** Tells if replay times are currently shown in real time. */
    private final boolean realTime;

    /** Relative converter game speed used by Scelight for loop-to-time conversion. */
    private final long converterGameSpeedRelative;

    /** Replay length in raw game loops from the replay header. */
    private final int replayLengthLoops;

    /** Player resource snapshots collected from tracker events. */
    private final Map< String, List< LarvaPlayerResourceSnapshot > > resourceSnapshotsByPlayerName;

    /**
     * Creates a new larva analysis report.
     *
     * @param calibration calibration used by the heuristic
     * @param timelineList per-hatchery count timelines
     * @param trackedHatcheryCount number of tracked hatcheries
     * @param larvaBirthCount number of larva birth events observed
     * @param assignedLarvaCount number of assigned larva
     * @param unassignedLarvaCount number of unassigned larva
    * @param ambiguousLarvaCount number of ambiguous larva births
    * @param noEligibleHatcheryLarvaCount number of larva births with no eligible hatchery
     * @param directAssignmentCount direct creator-based assignments
     * @param injectCorrelatedAssignmentCount inject-correlated assignments
     * @param heuristicAssignmentCount pure heuristic assignments
         * @param hatcheryMorphCount number of hatchery morph continuations detected
     * @param realTime tells if replay times are currently shown in real time
     * @param converterGameSpeedRelative relative converter game speed used by Scelight
     * @param replayLengthLoops replay length in raw game loops from the replay header
     * @param resourceSnapshotsByPlayerName player resource snapshots collected from tracker events
     */
        public LarvaAnalysisReport( final LarvaHeuristicCalibration calibration, final List< HatcheryLarvaTimeline > timelineList,
            final List< HatcheryInjectTimeline > injectTimelineList, final String injectSignalConclusion,
            final int trackedHatcheryCount, final int larvaBirthCount, final int assignedLarvaCount, final int unassignedLarvaCount,
            final int ambiguousLarvaCount, final int noEligibleHatcheryLarvaCount,
            final int directAssignmentCount, final int injectCorrelatedAssignmentCount, final int heuristicAssignmentCount,
             final int hatcheryMorphCount, final int trackerEventCount, final int gameEventCount, final boolean fullReplayParseUsed,
             final boolean realTime, final long converterGameSpeedRelative, final int replayLengthLoops,
             final Map< String, List< LarvaPlayerResourceSnapshot > > resourceSnapshotsByPlayerName ) {
        this.calibration = calibration;
        this.timelineList = Collections.unmodifiableList( new ArrayList<>( timelineList ) );
        this.injectTimelineList = Collections.unmodifiableList( new ArrayList<>( injectTimelineList ) );
        this.injectSignalConclusion = injectSignalConclusion;
        this.trackedHatcheryCount = trackedHatcheryCount;
        this.larvaBirthCount = larvaBirthCount;
        this.assignedLarvaCount = assignedLarvaCount;
        this.unassignedLarvaCount = unassignedLarvaCount;
        this.ambiguousLarvaCount = ambiguousLarvaCount;
        this.noEligibleHatcheryLarvaCount = noEligibleHatcheryLarvaCount;
        this.directAssignmentCount = directAssignmentCount;
        this.injectCorrelatedAssignmentCount = injectCorrelatedAssignmentCount;
        this.heuristicAssignmentCount = heuristicAssignmentCount;
        this.hatcheryMorphCount = hatcheryMorphCount;
        this.trackerEventCount = trackerEventCount;
        this.gameEventCount = gameEventCount;
        this.fullReplayParseUsed = fullReplayParseUsed;
        this.realTime = realTime;
        this.converterGameSpeedRelative = converterGameSpeedRelative;
        this.replayLengthLoops = replayLengthLoops;
        this.resourceSnapshotsByPlayerName = copySnapshotMap( resourceSnapshotsByPlayerName );
    }

    public LarvaHeuristicCalibration getCalibration() {
        return calibration;
    }

    public List< HatcheryLarvaTimeline > getTimelineList() {
        return timelineList;
    }

    public String getInjectSignalConclusion() {
        return injectSignalConclusion;
    }

    public List< HatcheryInjectTimeline > getInjectTimelineList() {
        return injectTimelineList;
    }

    public int getInjectCommandCount() {
        int injectCommandCount = 0;
        for ( final HatcheryInjectTimeline injectTimeline : injectTimelineList )
            injectCommandCount += injectTimeline.getRawInjectCommandCount();
        return injectCommandCount;
    }

    public int getInjectWindowCount() {
        int injectWindowCount = 0;
        for ( final HatcheryInjectTimeline injectTimeline : injectTimelineList )
            injectWindowCount += injectTimeline.getKeptWindowCount();
        return injectWindowCount;
    }

    public int getInjectOverlapDiscardCount() {
        int injectOverlapDiscardCount = 0;
        for ( final HatcheryInjectTimeline injectTimeline : injectTimelineList )
            injectOverlapDiscardCount += injectTimeline.getOverlapDiscardCount();
        return injectOverlapDiscardCount;
    }

    public int getInjectBoundsDiscardCount() {
        int injectBoundsDiscardCount = 0;
        for ( final HatcheryInjectTimeline injectTimeline : injectTimelineList )
            injectBoundsDiscardCount += injectTimeline.getBoundsDiscardCount();
        return injectBoundsDiscardCount;
    }

    public int getInjectTrimmedWindowCount() {
        int injectTrimmedWindowCount = 0;
        for ( final HatcheryInjectTimeline injectTimeline : injectTimelineList )
            injectTrimmedWindowCount += injectTimeline.getTrimmedWindowCount();
        return injectTrimmedWindowCount;
    }

    public int getTrackedHatcheryCount() {
        return trackedHatcheryCount;
    }

    public int getLarvaBirthCount() {
        return larvaBirthCount;
    }

    public int getAssignedLarvaCount() {
        return assignedLarvaCount;
    }

    public int getUnassignedLarvaCount() {
        return unassignedLarvaCount;
    }

    public int getAmbiguousLarvaCount() {
        return ambiguousLarvaCount;
    }

    public int getNoEligibleHatcheryLarvaCount() {
        return noEligibleHatcheryLarvaCount;
    }

    public int getDirectAssignmentCount() {
        return directAssignmentCount;
    }

    public int getInjectCorrelatedAssignmentCount() {
        return injectCorrelatedAssignmentCount;
    }

    public int getHeuristicAssignmentCount() {
        return heuristicAssignmentCount;
    }

    public int getHatcheryMorphCount() {
        return hatcheryMorphCount;
    }

    public int getTrackerEventCount() {
        return trackerEventCount;
    }

    public int getGameEventCount() {
        return gameEventCount;
    }

    public boolean isFullReplayParseUsed() {
        return fullReplayParseUsed;
    }

    public boolean isRealTime() {
        return realTime;
    }

    public long getConverterGameSpeedRelative() {
        return converterGameSpeedRelative;
    }

    public int getReplayLengthLoops() {
        return replayLengthLoops;
    }

    public Map< String, List< LarvaPlayerResourceSnapshot > > getResourceSnapshotsByPlayerName() {
        return resourceSnapshotsByPlayerName;
    }

    /**
     * Resolves the latest player resource snapshot at or before a specified marker loop.
     *
     * @param playerName player name
     * @param loop marker loop
     * @return latest matching snapshot; may be <code>null</code>
     */
    public LarvaPlayerResourceSnapshot findLatestResourceSnapshot( final String playerName, final int loop ) {
        if ( playerName == null || playerName.length() == 0 )
            return null;

        final List< LarvaPlayerResourceSnapshot > snapshotList = resourceSnapshotsByPlayerName.get( playerName );
        if ( snapshotList == null || snapshotList.isEmpty() )
            return null;

        LarvaPlayerResourceSnapshot latestSnapshot = null;
        for ( final LarvaPlayerResourceSnapshot snapshot : snapshotList ) {
            if ( snapshot.getLoop() > loop )
                break;
            latestSnapshot = snapshot;
        }

        return latestSnapshot;
    }

    /**
     * Resolves the earliest player resource snapshot after a specified marker loop.
     *
     * @param playerName player name
     * @param loop marker loop
     * @return earliest future snapshot; may be <code>null</code>
     */
    public LarvaPlayerResourceSnapshot findEarliestFutureResourceSnapshot( final String playerName, final int loop ) {
        if ( playerName == null || playerName.length() == 0 )
            return null;

        final List< LarvaPlayerResourceSnapshot > snapshotList = resourceSnapshotsByPlayerName.get( playerName );
        if ( snapshotList == null || snapshotList.isEmpty() )
            return null;

        for ( final LarvaPlayerResourceSnapshot snapshot : snapshotList )
            if ( snapshot.getLoop() > loop )
                return snapshot;

        return null;
    }

    /**
     * Converts replay loops to milliseconds using the same time basis as Scelight.
     *
     * @param gameloop replay loop
     * @return converted milliseconds
     */
    public long loopToTimeMs( final int gameloop ) {
        if ( gameloop <= 0 )
            return 0L;

        long gameMs = ( gameloop * 125L ) / 2L;
        if ( realTime && converterGameSpeedRelative != NORMAL_GAME_SPEED_RELATIVE )
            gameMs = gameMs * converterGameSpeedRelative / NORMAL_GAME_SPEED_RELATIVE;
        return gameMs;
    }

    /**
     * Formats replay loops using the same time basis as Scelight.
     *
     * @param gameloop replay loop
     * @return formatted time label
     */
    public String formatLoopTime( final int gameloop ) {
        return formatDuration( loopToTimeMs( gameloop ), false );
    }

    /**
     * Formats replay loops with tenth-of-a-second precision using the same time basis as Scelight.
     *
     * @param gameloop replay loop
     * @return formatted time label with tenths
     */
    public String formatLoopTimeTenths( final int gameloop ) {
        return formatDuration( loopToTimeMs( gameloop ), true );
    }

    /**
     * Formats milliseconds as $m:ss$ or $h:mm:ss$, optionally with tenths.
     *
     * @param ms duration in milliseconds
     * @param includeTenths tells if tenth-of-a-second precision should be included
     * @return formatted duration text
     */
    private String formatDuration( final long ms, final boolean includeTenths ) {
        final long totalSeconds = ms / 1000L;
        final long hours = totalSeconds / 3600L;
        final long minutes = ( totalSeconds % 3600L ) / 60L;
        final long seconds = totalSeconds % 60L;
        final long tenths = ( ms % 1000L ) / 100L;
        final String suffix = includeTenths ? "." + tenths : "";
        if ( hours > 0L )
            return hours + ":" + padTwoDigits( minutes ) + ":" + padTwoDigits( seconds ) + suffix;
        return minutes + ":" + padTwoDigits( seconds ) + suffix;
    }

    /**
     * Pads a time component to two digits.
     *
     * @param value value to pad
     * @return padded text
     */
    private String padTwoDigits( final long value ) {
        return value < 10L ? "0" + value : String.valueOf( value );
    }

    /**
     * Renders a readable diagnostics section for the Larva page.
     *
     * @return formatted diagnostics text
     */
    public String toDisplayText() {
        final StringBuilder builder = new StringBuilder();
        builder.append( "Epic 6 larva-to-hatchery assignment foundation:" ).append( '\n' );
        builder.append( "- Event streams: tracker=" ).append( trackerEventCount )
            .append( ", game=" ).append( gameEventCount )
            .append( ", replay load=" ).append( fullReplayParseUsed ? "full reparse" : "cached/full direct" )
            .append( '\n' );
        builder.append( "- Calibration source: " ).append( calibration.isCalibratedFromReplay() ? "opening replay sample" : "fallback defaults" ).append( '\n' );
        builder.append( "- Calibration samples: " ).append( calibration.getSampleCount() )
                .append( ", average offset dx=" ).append( formatDecimal( calibration.getAverageDx() ) )
                .append( ", dy=" ).append( formatDecimal( calibration.getAverageDy() ) )
                .append( ", radius=" ).append( formatDecimal( calibration.getRecommendedAssignmentDistance() ) )
                .append( '\n' );
        builder.append( "- Calibration note: " ).append( calibration.getNote() ).append( '\n' );
        builder.append( "- Hatcheries tracked: " ).append( trackedHatcheryCount )
                .append( ", larva births observed: " ).append( larvaBirthCount )
                .append( ", hatchery morph continuations: " ).append( hatcheryMorphCount )
                .append( '\n' );
        builder.append( "- Assignments: direct=" ).append( directAssignmentCount )
                .append( ", inject-correlated=" ).append( injectCorrelatedAssignmentCount )
                .append( ", heuristic=" ).append( heuristicAssignmentCount )
                .append( ", unassigned=" ).append( unassignedLarvaCount )
                .append( '\n' );
        builder.append( "- Unassigned details: ambiguous=" ).append( ambiguousLarvaCount )
            .append( ", no eligible hatchery=" ).append( noEligibleHatcheryLarvaCount )
            .append( '\n' );
        builder.append( "- Assigned larva total: " ).append( assignedLarvaCount ).append( '\n' );
        builder.append( "- Resource snapshot support: " ).append( formatResourceSnapshotCounts() ).append( '\n' );

        if ( timelineList.isEmpty() ) {
            builder.append( "- No per-hatchery timelines were derived from this replay yet." ).append( '\n' );
        } else {
            builder.append( "- Per-hatchery count timelines:" ).append( '\n' );
            for ( final HatcheryLarvaTimeline timeline : timelineList ) {
                builder.append( "  * " )
                        .append( timeline.getPlayerName() )
                        .append( " / " )
                        .append( timeline.getHatcheryType() )
                        .append( " (tag " )
                        .append( timeline.getHatcheryTagText() )
                        .append( ") max=" )
                        .append( timeline.getMaxLarvaCount() )
                    .append( ", completed=" )
                    .append( timeline.isCompleted() )
                    .append( ", larva=" )
                    .append( timeline.getCreatedLarvaCount() )
                        .append( ", direct=" )
                        .append( timeline.getDirectAssignmentCount() )
                        .append( ", inject=" )
                        .append( timeline.getInjectCorrelatedAssignmentCount() )
                        .append( ", heuristic=" )
                        .append( timeline.getHeuristicAssignmentCount() )
                        .append( '\n' );
                builder.append( "    lifecycle: completion=" )
                    .append( formatOptionalTimeLabel( timeline.getCompletionTimeLabel() ) )
                    .append( ", first larva=" )
                    .append( formatOptionalTimeLabel( timeline.getFirstLarvaTimeLabel() ) )
                    .append( ", destroyed=" )
                    .append( formatOptionalTimeLabel( timeline.getDestroyedTimeLabel() ) )
                    .append( '\n' );
                builder.append( "    points: " ).append( formatPoints( timeline.getCountPointList() ) ).append( '\n' );
                builder.append( "    milestones: " ).append( formatLarvaCountMilestones( timeline ) ).append( '\n' );
            }
        }

        appendInjectDiagnostics( builder );

        return builder.toString();
    }

    /**
     * Appends Story 11.01 inject-window diagnostics.
     *
     * @param builder target builder
     */
    private void appendInjectDiagnostics( final StringBuilder builder ) {
        builder.append( "Epic 11 Story 11.01 inject-active reconstruction:" ).append( '\n' );
        builder.append( "- Inject signal answer: " ).append( injectSignalConclusion == null ? "n/a" : injectSignalConclusion ).append( '\n' );
        builder.append( "- Inject totals: commands=" ).append( getInjectCommandCount() )
                .append( ", kept windows=" ).append( getInjectWindowCount() )
                .append( ", overlap discarded=" ).append( getInjectOverlapDiscardCount() )
                .append( ", bounds discarded=" ).append( getInjectBoundsDiscardCount() )
                .append( ", trimmed=" ).append( getInjectTrimmedWindowCount() )
                .append( '\n' );

        if ( injectTimelineList.isEmpty() ) {
            builder.append( "- No hatchery inject timelines were available for Story 11.01 yet." );
            return;
        }

        builder.append( "- Per-hatchery inject windows:" ).append( '\n' );
        for ( final HatcheryInjectTimeline injectTimeline : injectTimelineList ) {
            builder.append( "  * " )
                    .append( injectTimeline.getPlayerName() )
                    .append( " / " )
                    .append( injectTimeline.getHatcheryType() )
                    .append( " (tag " )
                    .append( injectTimeline.getHatcheryTagText() )
                    .append( ") commands=" )
                    .append( injectTimeline.getRawInjectCommandCount() )
                    .append( ", kept=" )
                    .append( injectTimeline.getKeptWindowCount() )
                    .append( ", overlapDiscarded=" )
                    .append( injectTimeline.getOverlapDiscardCount() )
                    .append( ", boundsDiscarded=" )
                    .append( injectTimeline.getBoundsDiscardCount() )
                    .append( ", trimmed=" )
                    .append( injectTimeline.getTrimmedWindowCount() )
                    .append( '\n' );
            builder.append( "    inject windows: " ).append( formatInjectWindows( injectTimeline.getInjectWindowList() ) ).append( '\n' );
            builder.append( "    inject diagnostics: " ).append( formatDiagnostics( injectTimeline.getDiagnosticLineList() ) ).append( '\n' );
        }
    }

    /**
     * Formats inject windows in compact deterministic form.
     *
     * @param injectWindowList inject windows to format
     * @return compact formatted text
     */
    private String formatInjectWindows( final List< HatcheryInjectWindow > injectWindowList ) {
        if ( injectWindowList == null || injectWindowList.isEmpty() )
            return "none";

        final StringBuilder builder = new StringBuilder();
        for ( int i = 0; i < injectWindowList.size(); i++ ) {
            if ( i > 0 )
                builder.append( ", " );

            final HatcheryInjectWindow injectWindow = injectWindowList.get( i );
            builder.append( injectWindow.getStartTimeLabel() ).append( '-' ).append( injectWindow.getEndTimeLabel() );
            if ( injectWindow.isTrimmedAtStart() || injectWindow.isTrimmedAtEnd() )
                builder.append( " [trimmed]" );
        }
        return builder.toString();
    }

    /**
     * Formats deterministic inject diagnostics on a single line.
     *
     * @param diagnosticLineList diagnostic lines to format
     * @return compact formatted diagnostics
     */
    private String formatDiagnostics( final List< String > diagnosticLineList ) {
        if ( diagnosticLineList == null || diagnosticLineList.isEmpty() )
            return "none";

        final StringBuilder builder = new StringBuilder();
        for ( int i = 0; i < diagnosticLineList.size(); i++ ) {
            if ( i > 0 )
                builder.append( " | " );
            builder.append( diagnosticLineList.get( i ) );
        }
        return builder.toString();
    }

    /**
     * Formats count points in compact form.
     *
     * @param pointList points to format
     * @return compact formatted list
     */
    private String formatPoints( final List< HatcheryLarvaTimeline.CountPoint > pointList ) {
        if ( pointList.isEmpty() )
            return "none";

        final StringBuilder builder = new StringBuilder();
        final int limit = Math.min( pointList.size(), 10 );
        for ( int i = 0; i < limit; i++ ) {
            if ( i > 0 )
                builder.append( ", " );
            final HatcheryLarvaTimeline.CountPoint point = pointList.get( i );
            builder.append( point.getTimeLabel() ).append( '=' ).append( point.getLarvaCount() );
        }

        if ( pointList.size() > limit )
            builder.append( ", ... (" ).append( pointList.size() - limit ).append( " more)" );

        return builder.toString();
    }

    /**
     * Formats hatchery moments when the tracked larva count reaches 3, 6, 9, 12, ...
     *
     * @param timeline hatchery timeline
     * @return formatted milestone list
     */
    private String formatLarvaCountMilestones( final HatcheryLarvaTimeline timeline ) {
        if ( timeline == null || timeline.getCountPointList().isEmpty() )
            return "none";

        final StringBuilder builder = new StringBuilder();
        boolean first = true;
        for ( final HatcheryLarvaTimeline.CountPoint point : timeline.getCountPointList() ) {
            if ( point == null || point.getLarvaCount() <= 0 || point.getLarvaCount() % 3 != 0 )
                continue;

            if ( !first )
                builder.append( ", " );
            builder.append( "tag " )
                .append( timeline.getHatcheryTagText() )
                .append( ' ' )
                .append( point.getTimeLabel() )
                .append( '=' )
                .append( point.getLarvaCount() );
            first = false;
        }

        return first ? "none" : builder.toString();
    }

    /**
     * Formats a decimal with 2 digits.
     *
     * @param value value to format
     * @return formatted decimal text
     */
    private String formatDecimal( final double value ) {
        final long scaled = Math.round( value * 100.0 );
        return String.valueOf( scaled / 100.0 );
    }

    /**
     * Formats an optional time label.
     *
     * @param timeLabel time label
     * @return display text
     */
    private String formatOptionalTimeLabel( final String timeLabel ) {
        return timeLabel == null || timeLabel.length() == 0 ? "n/a" : timeLabel;
    }

    /**
     * Copies the player resource snapshot map into immutable containers.
     *
     * @param source source snapshot map
     * @return immutable copy
     */
    private Map< String, List< LarvaPlayerResourceSnapshot > > copySnapshotMap( final Map< String, List< LarvaPlayerResourceSnapshot > > source ) {
        final Map< String, List< LarvaPlayerResourceSnapshot > > copy = new LinkedHashMap<>();
        if ( source == null || source.isEmpty() )
            return Collections.unmodifiableMap( copy );

        for ( final Map.Entry< String, List< LarvaPlayerResourceSnapshot > > entry : source.entrySet() )
            copy.put( entry.getKey(), Collections.unmodifiableList( new ArrayList<>( entry.getValue() ) ) );

        return Collections.unmodifiableMap( copy );
    }

    /**
     * Formats how many resource snapshots were captured per player.
     *
     * @return summary text
     */
    private String formatResourceSnapshotCounts() {
        if ( resourceSnapshotsByPlayerName.isEmpty() )
            return "no tracker-based player resource snapshots captured";

        final StringBuilder builder = new StringBuilder();
        boolean first = true;
        for ( final Map.Entry< String, List< LarvaPlayerResourceSnapshot > > entry : resourceSnapshotsByPlayerName.entrySet() ) {
            if ( !first )
                builder.append( ", " );
            builder.append( entry.getKey() ).append( '=' ).append( entry.getValue().size() );
            first = false;
        }

        return builder.toString();
    }

}