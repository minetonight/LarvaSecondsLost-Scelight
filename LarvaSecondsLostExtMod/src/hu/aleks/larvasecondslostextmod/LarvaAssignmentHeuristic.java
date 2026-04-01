package hu.aleks.larvasecondslostextmod;

import hu.scelightapi.sc2.balancedata.model.IAbility;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Assigns larva births to hatcheries using direct creator tags when available, then inject correlation and calibrated spatial fallback.
 */
public class LarvaAssignmentHeuristic {

    /** Confidence level of an assignment. */
    public enum Confidence {
        DIRECT,
        INJECT_CORRELATED,
        HEURISTIC,
        UNASSIGNED
    }

    /** Result of one larva assignment attempt. */
    public static class AssignmentResult {

        /** Assigned hatchery tag. */
        private final Integer hatcheryTag;

        /** Confidence of the assignment. */
        private final Confidence confidence;

        /** Explanation of the result. */
        private final String reason;

        /**
         * Creates a new assignment result.
         *
         * @param hatcheryTag assigned hatchery tag
         * @param confidence assignment confidence
         * @param reason explanation of the result
         */
        public AssignmentResult( final Integer hatcheryTag, final Confidence confidence, final String reason ) {
            this.hatcheryTag = hatcheryTag;
            this.confidence = confidence;
            this.reason = reason;
        }

        public Integer getHatcheryTag() {
            return hatcheryTag;
        }

        public Confidence getConfidence() {
            return confidence;
        }

        public String getReason() {
            return reason;
        }

        public boolean isAssigned() {
            return hatcheryTag != null;
        }

    }

    /** Signal window for matching a larva birth to a recent Spawn Larva command. */
    private static final int DEFAULT_INJECT_WINDOW_LOOPS = 40 * 16;

    /** Minimum score advantage required over the second-best hatchery. */
    private static final double AMBIGUITY_MARGIN = 2.0;

    /** Calibration used by the heuristic. */
    private final LarvaHeuristicCalibration calibration;

    /**
     * Creates a new larva assignment heuristic.
     *
     * @param calibration calibration used by the heuristic
     */
    public LarvaAssignmentHeuristic( final LarvaHeuristicCalibration calibration ) {
        this.calibration = calibration;
    }

    /**
     * Assigns a larva to a hatchery.
     *
     * @param loop larva birth loop
     * @param larvaPlayerId larva owner player id
     * @param larvaX larva x coordinate
     * @param larvaY larva y coordinate
     * @param creatorTag creator hatchery tag if available
     * @param creatorAbilityName creator ability name if available
     * @param hatcheryStates known hatchery states
     * @param injectLoopsByTag Spawn Larva command loops by hatchery tag
     * @return assignment result
     */
    public AssignmentResult assignLarva( final int loop, final Integer larvaPlayerId, final Integer larvaX, final Integer larvaY,
            final Integer creatorTag, final String creatorAbilityName, final Collection< LarvaReplayAnalyzer.HatcherySnapshot > hatcheryStates,
            final Map< Integer, List< Integer > > injectLoopsByTag ) {
        if ( creatorTag != null && IAbility.ID_SPAWN_LARVA.equals( creatorAbilityName ) ) {
            for ( final LarvaReplayAnalyzer.HatcherySnapshot hatcheryState : hatcheryStates ) {
                if ( hatcheryState.getHatcheryTag() == creatorTag.intValue() && hatcheryState.isAlive() && hatcheryState.isCompleted() )
                    return new AssignmentResult( creatorTag, Confidence.DIRECT, "creatorUnitTag matched SpawnLarva" );
            }
        }

        Candidate best = null;
        Candidate second = null;
        final double maxDistance = calibration.getRecommendedAssignmentDistance();

        for ( final LarvaReplayAnalyzer.HatcherySnapshot hatcheryState : hatcheryStates ) {
            if ( !hatcheryState.isAlive() || !hatcheryState.isCompleted() )
                continue;
            if ( larvaPlayerId != null && hatcheryState.getPlayerId() != null && !larvaPlayerId.equals( hatcheryState.getPlayerId() ) )
                continue;
            if ( larvaX == null || larvaY == null || hatcheryState.getX() == null || hatcheryState.getY() == null )
                continue;

            final double dx = larvaX.intValue() - hatcheryState.getX().intValue();
            final double dy = larvaY.intValue() - hatcheryState.getY().intValue();
            final double distance = Math.sqrt( dx * dx + dy * dy );
            if ( distance > maxDistance )
                continue;

            final double offsetDx = dx - calibration.getAverageDx();
            final double offsetDy = dy - calibration.getAverageDy();
            double score = distance * 0.35 + Math.sqrt( offsetDx * offsetDx + offsetDy * offsetDy );

            final boolean injectCorrelated = isInjectCorrelated( loop, hatcheryState.getHatcheryTag(), injectLoopsByTag );
            if ( injectCorrelated )
                score -= 1.5;

            final Candidate candidate = new Candidate( hatcheryState, score, injectCorrelated );
            if ( best == null || candidate.score < best.score ) {
                second = best;
                best = candidate;
            } else if ( second == null || candidate.score < second.score ) {
                second = candidate;
            }
        }

        if ( best == null )
            return new AssignmentResult( null, Confidence.UNASSIGNED, "no eligible hatchery within calibrated radius" );

        if ( second != null && second.score - best.score < AMBIGUITY_MARGIN )
            return new AssignmentResult( null, Confidence.UNASSIGNED, "multiple hatcheries matched the larva birth with similar score" );

        return new AssignmentResult( Integer.valueOf( best.hatcheryState.getHatcheryTag() ),
                best.injectCorrelated ? Confidence.INJECT_CORRELATED : Confidence.HEURISTIC,
                best.injectCorrelated ? "recent SpawnLarva command strengthened the spatial match" : "nearest calibrated hatchery offset match" );
    }

    /**
     * Tells if a hatchery had a recent Spawn Larva command near the specified loop.
     *
     * @param loop larva birth loop
     * @param hatcheryTag hatchery tag
     * @param injectLoopsByTag Spawn Larva command loops by hatchery tag
     * @return true if a recent inject exists; false otherwise
     */
    private boolean isInjectCorrelated( final int loop, final int hatcheryTag, final Map< Integer, List< Integer > > injectLoopsByTag ) {
        final List< Integer > injectLoops = injectLoopsByTag.get( Integer.valueOf( hatcheryTag ) );
        if ( injectLoops == null )
            return false;

        for ( int i = injectLoops.size() - 1; i >= 0; i-- ) {
            final int injectLoop = injectLoops.get( i ).intValue();
            if ( injectLoop > loop )
                continue;
            if ( loop - injectLoop <= DEFAULT_INJECT_WINDOW_LOOPS )
                return true;
            break;
        }

        return false;
    }

    /** Candidate hatchery during heuristic matching. */
    private static class Candidate {

        /** Hatchery snapshot. */
        private final LarvaReplayAnalyzer.HatcherySnapshot hatcheryState;

        /** Aggregate match score. */
        private final double score;

        /** Tells if a recent Spawn Larva command strengthened the match. */
        private final boolean injectCorrelated;

        /**
         * Creates a new candidate.
         *
         * @param hatcheryState candidate hatchery
         * @param score aggregate match score
         * @param injectCorrelated tells if a recent inject strengthened the match
         */
        private Candidate( final LarvaReplayAnalyzer.HatcherySnapshot hatcheryState, final double score, final boolean injectCorrelated ) {
            this.hatcheryState = hatcheryState;
            this.score = score;
            this.injectCorrelated = injectCorrelated;
        }

    }

}