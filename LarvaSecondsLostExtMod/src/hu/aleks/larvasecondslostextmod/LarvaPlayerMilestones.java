package hu.aleks.larvasecondslostextmod;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/** Immutable milestones of one Zerg player in one replay, for the single-replay page (E20-S09). Loops, not ms. */
final class LarvaPlayerMilestones {

    private final LarvaMatchupBucket matchup;

    private final Map< LarvaMilestone, Integer > loops;

    /**
     * @param matchup {@code ZVP}, {@code ZVT} or {@code ZVZ}; null if unknown (not a two-player replay); {@code ALL} is rejected
     * @param loops first loop at or above each reached milestone ({@link LarvaMilestoneCalculator}); copied
     */
    LarvaPlayerMilestones( final LarvaMatchupBucket matchup, final Map< LarvaMilestone, Integer > loops ) {
        if ( matchup == LarvaMatchupBucket.ALL )
            throw new IllegalArgumentException( "ALL is not a matchup." );
        this.matchup = matchup;
        final EnumMap< LarvaMilestone, Integer > copy = new EnumMap<>( LarvaMilestone.class );
        if ( loops != null )
            copy.putAll( loops );
        this.loops = Collections.unmodifiableMap( copy );
    }

    /** @return the game's matchup, or null if unknown */
    LarvaMatchupBucket getMatchup() {
        return matchup;
    }

    /** @return the bucket whose bands rate this game: its matchup, or {@code ALL} when the matchup is unknown */
    LarvaMatchupBucket getRatingBucket() {
        return matchup == null ? LarvaMatchupBucket.ALL : matchup;
    }

    /** @return loop at which the milestone was first reached, or null if it was not reached */
    Integer getLoop( final LarvaMilestone milestone ) {
        return loops.get( milestone );
    }
}
