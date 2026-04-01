package hu.aleks.larvasecondslostextmod;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Result of the Epic 6 larva-to-hatchery assignment foundation analysis.
 */
public class LarvaAnalysisReport {

    /** Calibration used by the assignment heuristic. */
    private final LarvaHeuristicCalibration calibration;

    /** Per-hatchery count timelines. */
    private final List< HatcheryLarvaTimeline > timelineList;

    /** Number of hatcheries tracked during replay analysis. */
    private final int trackedHatcheryCount;

    /** Number of larva birth events observed. */
    private final int larvaBirthCount;

    /** Number of larva assigned to a hatchery. */
    private final int assignedLarvaCount;

    /** Number of larva that remained ambiguous or unassigned. */
    private final int unassignedLarvaCount;

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

    /**
     * Creates a new larva analysis report.
     *
     * @param calibration calibration used by the heuristic
     * @param timelineList per-hatchery count timelines
     * @param trackedHatcheryCount number of tracked hatcheries
     * @param larvaBirthCount number of larva birth events observed
     * @param assignedLarvaCount number of assigned larva
     * @param unassignedLarvaCount number of unassigned larva
     * @param directAssignmentCount direct creator-based assignments
     * @param injectCorrelatedAssignmentCount inject-correlated assignments
     * @param heuristicAssignmentCount pure heuristic assignments
     * @param hatcheryMorphCount number of hatchery morph continuations detected
     */
    public LarvaAnalysisReport( final LarvaHeuristicCalibration calibration, final List< HatcheryLarvaTimeline > timelineList,
            final int trackedHatcheryCount, final int larvaBirthCount, final int assignedLarvaCount, final int unassignedLarvaCount,
            final int directAssignmentCount, final int injectCorrelatedAssignmentCount, final int heuristicAssignmentCount,
            final int hatcheryMorphCount, final int trackerEventCount, final int gameEventCount, final boolean fullReplayParseUsed ) {
        this.calibration = calibration;
        this.timelineList = Collections.unmodifiableList( new ArrayList<>( timelineList ) );
        this.trackedHatcheryCount = trackedHatcheryCount;
        this.larvaBirthCount = larvaBirthCount;
        this.assignedLarvaCount = assignedLarvaCount;
        this.unassignedLarvaCount = unassignedLarvaCount;
        this.directAssignmentCount = directAssignmentCount;
        this.injectCorrelatedAssignmentCount = injectCorrelatedAssignmentCount;
        this.heuristicAssignmentCount = heuristicAssignmentCount;
        this.hatcheryMorphCount = hatcheryMorphCount;
        this.trackerEventCount = trackerEventCount;
        this.gameEventCount = gameEventCount;
        this.fullReplayParseUsed = fullReplayParseUsed;
    }

    public LarvaHeuristicCalibration getCalibration() {
        return calibration;
    }

    public List< HatcheryLarvaTimeline > getTimelineList() {
        return timelineList;
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
        builder.append( "- Assigned larva total: " ).append( assignedLarvaCount ).append( '\n' );

        if ( timelineList.isEmpty() ) {
            builder.append( "- No per-hatchery timelines were derived from this replay yet." );
            return builder.toString();
        }

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
                    .append( ", direct=" )
                    .append( timeline.getDirectAssignmentCount() )
                    .append( ", inject=" )
                    .append( timeline.getInjectCorrelatedAssignmentCount() )
                    .append( ", heuristic=" )
                    .append( timeline.getHeuristicAssignmentCount() )
                    .append( '\n' );
            builder.append( "    points: " ).append( formatPoints( timeline.getCountPointList() ) ).append( '\n' );
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
     * Formats a decimal with 2 digits.
     *
     * @param value value to format
     * @return formatted decimal text
     */
    private String formatDecimal( final double value ) {
        final long scaled = Math.round( value * 100.0 );
        return String.valueOf( scaled / 100.0 );
    }

}