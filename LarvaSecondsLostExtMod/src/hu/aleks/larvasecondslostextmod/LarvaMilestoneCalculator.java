package hu.aleks.larvasecondslostextmod;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Computes milestone loops for one player and game from resource snapshots (E17-S08, D-4, D-10). Pure and stateless.
 *
 * <p>A milestone that was not reached is absent from the result: it is never treated as game end (no censoring).</p>
 */
public final class LarvaMilestoneCalculator {

    /** {@code PlayerStats.foodUsed} is fixed-point with this scale (OQ-11). */
    static final int FOOD_FIXED_POINT_SCALE = 4096;

    private LarvaMilestoneCalculator() {
    }

    /**
     * Computes the loop at which each reached milestone was first reached.
     *
     * @param snapshots snapshots of one player, may be null or empty
     * @param exactDrone66Loop exact loop where the alive drone count first reached 66, or null to scan the snapshots
     * @param exactDrone80Loop exact loop where the alive drone count first reached 80, or null to scan the snapshots
     * @return reached milestones in display order mapped to their loop, never null
     */
    public static Map< LarvaMilestone, Integer > compute( final List< LarvaPlayerResourceSnapshot > snapshots, final Integer exactDrone66Loop,
            final Integer exactDrone80Loop ) {
        final Map< LarvaMilestone, Integer > result = new EnumMap<>( LarvaMilestone.class );
        for ( final LarvaMilestone milestone : LarvaMilestone.values() ) {
            final Integer loop = resolveLoop( milestone, snapshots, exactDrone66Loop, exactDrone80Loop );
            if ( loop != null )
                result.put( milestone, loop );
        }
        return result;
    }

    private static Integer resolveLoop( final LarvaMilestone milestone, final List< LarvaPlayerResourceSnapshot > snapshots,
            final Integer exactDrone66Loop, final Integer exactDrone80Loop ) {
        // Exact analyzer loops win over the coarse (about 10 s) snapshot scan.
        if ( milestone == LarvaMilestone.DRONES_66 && exactDrone66Loop != null )
            return exactDrone66Loop;
        if ( milestone == LarvaMilestone.DRONES_80 && exactDrone80Loop != null )
            return exactDrone80Loop;
        return firstSnapshotLoop( milestone, snapshots );
    }

    private static Integer firstSnapshotLoop( final LarvaMilestone milestone, final List< LarvaPlayerResourceSnapshot > snapshots ) {
        if ( snapshots == null )
            return null;
        Integer firstLoop = null;
        for ( final LarvaPlayerResourceSnapshot snapshot : snapshots ) {
            if ( snapshot == null || !isReached( milestone, snapshot ) )
                continue;
            // Do not rely on list order: the earliest loop wins.
            if ( firstLoop == null || snapshot.getLoop() < firstLoop.intValue() )
                firstLoop = Integer.valueOf( snapshot.getLoop() );
        }
        return firstLoop;
    }

    private static boolean isReached( final LarvaMilestone milestone, final LarvaPlayerResourceSnapshot snapshot ) {
        if ( milestone.getKind() == LarvaMilestone.Kind.SUPPLY ) {
            final Integer foodUsed = snapshot.getFoodUsed();
            // Integer compare in fixed-point: 100 * 4096 - 1 does not count as 100 supply.
            return foodUsed != null && foodUsed.longValue() >= (long) milestone.getThreshold() * FOOD_FIXED_POINT_SCALE;
        }
        final Integer workers = snapshot.getWorkersActiveCount();
        return workers != null && workers.intValue() >= milestone.getThreshold();
    }
}
