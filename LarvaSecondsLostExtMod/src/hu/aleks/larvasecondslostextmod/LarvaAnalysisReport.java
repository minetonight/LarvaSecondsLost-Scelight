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

    /** Calibration used by the assignment heuristic. */
    private final LarvaHeuristicCalibration calibration;

    /** Per-hatchery count timelines. */
    private final List< HatcheryLarvaTimeline > timelineList;

    /** Story 11.01 answer for the replay-native inject signal question. */
    private final String injectSignalConclusion;

    /** Per-hatchery normalized inject-window diagnostics. */
    private final List< HatcheryInjectTimeline > injectTimelineList;

    /** How red (missed-inject) windows are made. */
    private final String idleInjectConclusion;

    /** Cells within which queen evidence belongs to a hatchery. */
    private final double idleInjectRadius;

    /** Per-hatchery normalized idle-inject diagnostics. */
    private final List< HatcheryIdleInjectTimeline > idleInjectTimelineList;

    /** Queens born in the replay (all players). */
    private final int trackedQueenCount;

    /** Queens with at least one tracker UnitPositions sample (damaged queens only). */
    private final int trackerObservedQueenCount;

    /** Queen spends attributed among the queens of a command selection (E19; the getter keeps its old name). */
    private final int commandSeededQueenCount;

    /** Commands given while at least one of the player's queens was selected (E19; the getter keeps its old name). */
    private final int queenCommandEvidenceCount;

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

    /** Replay length in raw game loops from the replay header. */
    private final int replayLengthLoops;

    /** Player resource snapshots collected from tracker events. */
    private final Map< String, List< LarvaPlayerResourceSnapshot > > resourceSnapshotsByPlayerName;

    /** Per-player Epic 12 phase tables. */
    private final Map< String, LarvaPlayerPhaseTable > playerPhaseTableByPlayerName;

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
     * @param replayLengthLoops replay length in raw game loops from the replay header
     * @param resourceSnapshotsByPlayerName player resource snapshots collected from tracker events
     * @param playerPhaseTableByPlayerName per-player phase tables
     */
        public LarvaAnalysisReport( final LarvaHeuristicCalibration calibration, final List< HatcheryLarvaTimeline > timelineList,
            final List< HatcheryInjectTimeline > injectTimelineList, final String injectSignalConclusion,
            final List< HatcheryIdleInjectTimeline > idleInjectTimelineList, final String idleInjectConclusion, final double idleInjectRadius,
            final int trackedQueenCount, final int trackerObservedQueenCount, final int commandSeededQueenCount,
            final int queenCommandEvidenceCount,
            final int trackedHatcheryCount, final int larvaBirthCount, final int assignedLarvaCount, final int unassignedLarvaCount,
            final int ambiguousLarvaCount, final int noEligibleHatcheryLarvaCount,
            final int directAssignmentCount, final int injectCorrelatedAssignmentCount, final int heuristicAssignmentCount,
             final int hatcheryMorphCount, final int trackerEventCount, final int gameEventCount, final boolean fullReplayParseUsed,
             final int replayLengthLoops,
             final Map< String, List< LarvaPlayerResourceSnapshot > > resourceSnapshotsByPlayerName,
             final Map< String, LarvaPlayerPhaseTable > playerPhaseTableByPlayerName ) {
        this.calibration = calibration;
        this.timelineList = Collections.unmodifiableList( new ArrayList<>( timelineList ) );
        this.injectTimelineList = Collections.unmodifiableList( new ArrayList<>( injectTimelineList ) );
        this.injectSignalConclusion = injectSignalConclusion;
        this.idleInjectTimelineList = Collections.unmodifiableList( new ArrayList<>( idleInjectTimelineList ) );
        this.idleInjectConclusion = idleInjectConclusion;
        this.idleInjectRadius = idleInjectRadius;
        this.trackedQueenCount = trackedQueenCount;
        this.trackerObservedQueenCount = trackerObservedQueenCount;
        this.commandSeededQueenCount = commandSeededQueenCount;
        this.queenCommandEvidenceCount = queenCommandEvidenceCount;
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
        this.replayLengthLoops = replayLengthLoops;
        this.resourceSnapshotsByPlayerName = copySnapshotMap( resourceSnapshotsByPlayerName );
        this.playerPhaseTableByPlayerName = copyPhaseTableMap( playerPhaseTableByPlayerName );
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

    public String getIdleInjectConclusion() {
        return idleInjectConclusion;
    }

    public double getIdleInjectRadius() {
        return idleInjectRadius;
    }

    public List< HatcheryIdleInjectTimeline > getIdleInjectTimelineList() {
        return idleInjectTimelineList;
    }

    public int getTrackedQueenCount() {
        return trackedQueenCount;
    }

    public int getTrackerObservedQueenCount() {
        return trackerObservedQueenCount;
    }

    public int getCommandSeededQueenCount() {
        return commandSeededQueenCount;
    }

    public int getQueenCommandEvidenceCount() {
        return queenCommandEvidenceCount;
    }

    public int getInjectEvidenceCount() {
        int injectEvidenceCount = 0;
        for ( final HatcheryInjectTimeline injectTimeline : injectTimelineList )
            injectEvidenceCount += injectTimeline.getRawInjectEvidenceCount();
        return injectEvidenceCount;
    }

    public int getInjectCommandCount() {
        return getInjectEvidenceCount();
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

    public int getIdleInjectWindowCount() {
        int idleInjectWindowCount = 0;
        for ( final HatcheryIdleInjectTimeline idleInjectTimeline : idleInjectTimelineList )
            idleInjectWindowCount += idleInjectTimeline.getKeptWindowCount();
        return idleInjectWindowCount;
    }

    public int getIdleInjectUncertaintyDiscardCount() {
        int idleInjectUncertaintyDiscardCount = 0;
        for ( final HatcheryIdleInjectTimeline idleInjectTimeline : idleInjectTimelineList )
            idleInjectUncertaintyDiscardCount += idleInjectTimeline.getUncertaintyDiscardCount();
        return idleInjectUncertaintyDiscardCount;
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

    public int getReplayLengthLoops() {
        return replayLengthLoops;
    }

    public Map< String, List< LarvaPlayerResourceSnapshot > > getResourceSnapshotsByPlayerName() {
        return resourceSnapshotsByPlayerName;
    }

    public Map< String, LarvaPlayerPhaseTable > getPlayerPhaseTableByPlayerName() {
        return playerPhaseTableByPlayerName;
    }

    public LarvaPlayerPhaseTable getPlayerPhaseTable( final String playerName ) {
        return playerName == null ? null : playerPhaseTableByPlayerName.get( playerName );
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
     * @param gameloop replay loop
     * @return milliseconds on the game clock ({@link LarvaGameClock})
     */
    public long loopToTimeMs( final int gameloop ) {
        return LarvaGameClock.loopToMs( gameloop );
    }

    /**
     * @param gameloop replay loop
     * @return game clock time label ({@link LarvaGameClock})
     */
    public String formatLoopTime( final int gameloop ) {
        return LarvaGameClock.format( gameloop );
    }

    /**
     * @param gameloop replay loop
     * @return game clock time label with tenths ({@link LarvaGameClock})
     */
    public String formatLoopTimeTenths( final int gameloop ) {
        return LarvaGameClock.formatTenths( gameloop );
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
        builder.append( "- Epic 12 phase tables: " ).append( formatPhaseTableCounts() ).append( '\n' );

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
        appendIdleInjectDiagnostics( builder );
        appendPhaseTableDiagnostics( builder );

        return builder.toString();
    }

    /**
     * Appends Epic 12 phase-table diagnostics.
     *
     * @param builder target builder
     */
    private void appendPhaseTableDiagnostics( final StringBuilder builder ) {
        builder.append( "Epic 12 per-phase summary:" ).append( '\n' );
        if ( playerPhaseTableByPlayerName.isEmpty() ) {
            builder.append( "- No player phase tables were derived yet." ).append( '\n' );
            return;
        }

        for ( final Map.Entry< String, LarvaPlayerPhaseTable > entry : playerPhaseTableByPlayerName.entrySet() ) {
            builder.append( "  * " ).append( entry.getKey() ).append( ": " );
            boolean first = true;
            for ( final LarvaGamePhase phase : LarvaGamePhase.values() ) {
                final LarvaPhaseStats stats = entry.getValue().getPhaseStats( phase );
                if ( !first )
                    builder.append( " | " );
                builder.append( phase.getDisplayLabel() )
                        .append( "=" )
                        .append( formatPhaseRate( stats == null ? null : stats.getMissedLarvaPerHatchPerMinute() ) )
                        .append( "/" )
                        .append( formatPhaseRate( stats == null ? null : stats.getMissedInjectLarvaPerHatchPerMinute() ) )
                        .append( "/" )
                        .append( formatPhaseRate( stats == null ? null : stats.getSpawnedLarvaPerHatchPerMinute() ) )
                        .append( "/" )
                        .append( formatPhasePercent( stats == null ? null : stats.getInjectUptimePercentage() ) );
                first = false;
            }
            builder.append( '\n' );
        }
    }

    /**
     * Appends Story 11.01 inject-window diagnostics.
     *
     * @param builder target builder
     */
    private void appendInjectDiagnostics( final StringBuilder builder ) {
        builder.append( "Epic 11 Story 11.01 inject-active reconstruction:" ).append( '\n' );
        builder.append( "- Inject signal answer: " ).append( injectSignalConclusion == null ? "n/a" : injectSignalConclusion ).append( '\n' );
        builder.append( "- Inject totals: inferred evidence=" ).append( getInjectEvidenceCount() )
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
                    .append( ") evidence=" )
                    .append( injectTimeline.getRawInjectEvidenceCount() )
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
     * Appends Story 11.03 idle-inject diagnostics.
     *
     * @param builder target builder
     */
    private void appendIdleInjectDiagnostics( final StringBuilder builder ) {
        builder.append( "Missed inject qualification (E19 queen model):" ).append( '\n' );
        builder.append( "- Idle inject answer: " ).append( idleInjectConclusion == null ? "n/a" : idleInjectConclusion ).append( '\n' );
        builder.append( "- Dedicated queen radius: " ).append( formatDouble( idleInjectRadius ) ).append( '\n' );
        builder.append( "- Queens: born=" ).append( trackedQueenCount )
            .append( ", with tracker positions=" ).append( trackerObservedQueenCount )
            .append( ", spends attributed from a command selection=" ).append( commandSeededQueenCount )
            .append( ", commands with queens selected=" ).append( queenCommandEvidenceCount )
            .append( '\n' );
        builder.append( "- Idle inject totals: kept windows=" ).append( getIdleInjectWindowCount() )
                .append( ", uncertainty discarded=" ).append( getIdleInjectUncertaintyDiscardCount() )
                .append( '\n' );

        if ( idleInjectTimelineList.isEmpty() ) {
            builder.append( "- No hatchery idle-inject timelines were available for Story 11.03 yet." );
            return;
        }

        builder.append( "- Per-hatchery idle-inject windows:" ).append( '\n' );
        for ( final HatcheryIdleInjectTimeline idleInjectTimeline : idleInjectTimelineList ) {
            builder.append( "  * " )
                    .append( idleInjectTimeline.getPlayerName() )
                    .append( " / " )
                    .append( idleInjectTimeline.getHatcheryType() )
                    .append( " (tag " )
                    .append( idleInjectTimeline.getHatcheryTagText() )
                    .append( ") kept=" )
                    .append( idleInjectTimeline.getKeptWindowCount() )
                    .append( ", queenEvidencePoints=" )
                    .append( idleInjectTimeline.getAttributedQueenCommandCount() )
                    .append( ", injectsWithCommand=" )
                    .append( idleInjectTimeline.getAttributedInjectCommandCount() )
                    .append( ", ambiguousSpends=" )
                    .append( idleInjectTimeline.getUncertaintyDiscardCount() )
                    .append( '\n' );
            builder.append( "    idle windows: " ).append( formatIdleInjectWindows( idleInjectTimeline.getIdleWindowList() ) ).append( '\n' );
            builder.append( "    idle diagnostics: " ).append( formatDiagnostics( idleInjectTimeline.getDiagnosticLineList() ) ).append( '\n' );
        }
    }

    /**
     * Formats idle-inject windows in compact deterministic form.
     *
     * @param idleWindowList idle windows to format
     * @return compact formatted text
     */
    private String formatIdleInjectWindows( final List< HatcheryIdleInjectWindow > idleWindowList ) {
        if ( idleWindowList == null || idleWindowList.isEmpty() )
            return "none";

        final StringBuilder builder = new StringBuilder();
        for ( int i = 0; i < idleWindowList.size(); i++ ) {
            if ( i > 0 )
                builder.append( ", " );

            final HatcheryIdleInjectWindow idleWindow = idleWindowList.get( i );
            builder.append( idleWindow.getStartTimeLabel() ).append( '-' ).append( idleWindow.getEndTimeLabel() )
                    .append( " [q=" ).append( idleWindow.getQualifyingQueenCount() ).append( ']' );
        }
        return builder.toString();
    }

    /**
     * Formats a decimal number with two visible fractional digits without locale dependence.
     *
     * @param value value to format
     * @return deterministic text
     */
    private String formatDouble( final double value ) {
        final long scaled = Math.round( value * 100.0d );
        final long whole = scaled / 100L;
        final long fraction = Math.abs( scaled % 100L );
        return String.valueOf( whole ) + '.' + ( fraction < 10L ? "0" : "" ) + fraction;
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
     * Copies the phase-table map into immutable containers.
     *
     * @param source source phase-table map
     * @return immutable copy
     */
    private Map< String, LarvaPlayerPhaseTable > copyPhaseTableMap( final Map< String, LarvaPlayerPhaseTable > source ) {
        final Map< String, LarvaPlayerPhaseTable > copy = new LinkedHashMap<>();
        if ( source == null || source.isEmpty() )
            return Collections.unmodifiableMap( copy );

        copy.putAll( source );
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

    /**
     * Formats how many phase tables were captured per player.
     *
     * @return summary text
     */
    private String formatPhaseTableCounts() {
        if ( playerPhaseTableByPlayerName.isEmpty() )
            return "no per-player phase tables derived";

        final StringBuilder builder = new StringBuilder();
        boolean first = true;
        for ( final Map.Entry< String, LarvaPlayerPhaseTable > entry : playerPhaseTableByPlayerName.entrySet() ) {
            if ( !first )
                builder.append( ", " );
            builder.append( entry.getKey() ).append( '=' ).append( entry.getValue().getPhaseStatsMap().size() );
            first = false;
        }

        return builder.toString();
    }

    /**
     * Formats a phase rate for diagnostics.
     */
    private String formatPhaseRate( final Double value ) {
        return value == null ? "n/a" : formatDouble( value.doubleValue() );
    }

    /**
     * Formats a phase percent for diagnostics.
     */
    private String formatPhasePercent( final Double value ) {
        return value == null ? "n/a" : formatDouble( value.doubleValue() ) + "%";
    }

}