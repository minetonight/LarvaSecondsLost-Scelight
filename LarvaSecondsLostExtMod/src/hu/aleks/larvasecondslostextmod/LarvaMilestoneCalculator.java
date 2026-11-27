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
     * Computes the loop at which each reached milestone was first reached. Drone milestones use the snapshots' workers count,
     * which is {@code PlayerStats.workersActiveCount} (Scelight's Workers Active chart, E19-S04), so a milestone loop is the loop of
     * the first {@code PlayerStats} sample (every 160 loops) at or above the threshold.
     *
     * @param snapshots snapshots of one player, may be null or empty
     * @return reached milestones in display order mapped to their loop, never null
     */
    public static Map< LarvaMilestone, Integer > compute( final List< LarvaPlayerResourceSnapshot > snapshots ) {
        final Map< LarvaMilestone, Integer > result = new EnumMap<>( LarvaMilestone.class );
        for ( final LarvaMilestone milestone : LarvaMilestone.values() ) {
            final Integer loop = firstSnapshotLoop( milestone, snapshots );
            if ( loop != null )
                result.put( milestone, loop );
        }
        return result;
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
