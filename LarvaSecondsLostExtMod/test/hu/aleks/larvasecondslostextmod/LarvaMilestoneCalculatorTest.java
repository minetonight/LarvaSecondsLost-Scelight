package hu.aleks.larvasecondslostextmod;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Test;

/** Unit tests for {@link LarvaMilestoneCalculator} (synthetic data only). */
public class LarvaMilestoneCalculatorTest {

    private static final int SCALE = 4096;

    private static LarvaPlayerResourceSnapshot snap( final int loop, final Integer foodUsed, final Integer workers ) {
        return new LarvaPlayerResourceSnapshot( "P", loop, "t", 0, 0, foodUsed, null, workers );
    }

    private static Map< LarvaMilestone, Integer > compute( final List< LarvaPlayerResourceSnapshot > s ) {
        return LarvaMilestoneCalculator.compute( s );
    }

    @Test
    public void supply100IsFirstSnapshotAtOrAboveThreshold() {
        final Map< LarvaMilestone, Integer > m = compute( Arrays.asList( snap( 1000, 99 * SCALE, 10 ), snap( 1160, 100 * SCALE, 10 ),
                snap( 1320, 120 * SCALE, 10 ) ) );
        assertEquals( Integer.valueOf( 1160 ), m.get( LarvaMilestone.SUPPLY_100 ) );
    }

    @Test
    public void fixedPointBoundaryJustBelowThresholdDoesNotQualify() {
        final Map< LarvaMilestone, Integer > m = compute( Arrays.asList( snap( 1000, 100 * SCALE - 1, 10 ) ) );
        assertTrue( m.isEmpty() );
    }

    @Test
    public void supply200NeverReachedIsAbsent() {
        final Map< LarvaMilestone, Integer > m = compute( Arrays.asList( snap( 1000, 150 * SCALE, 10 ), snap( 2000, 199 * SCALE, 10 ) ) );
        assertEquals( Integer.valueOf( 1000 ), m.get( LarvaMilestone.SUPPLY_100 ) );
        assertTrue( !m.containsKey( LarvaMilestone.SUPPLY_200 ) );
    }

    @Test
    public void supply200ReachedExactlyAtThreshold() {
        final Map< LarvaMilestone, Integer > m = compute( Arrays.asList( snap( 5000, 200 * SCALE, 10 ) ) );
        assertEquals( Integer.valueOf( 5000 ), m.get( LarvaMilestone.SUPPLY_200 ) );
    }

    @Test
    public void emptyAndNullListsGiveNoMilestones() {
        assertTrue( compute( Collections.< LarvaPlayerResourceSnapshot >emptyList() ).isEmpty() );
        assertTrue( compute( null ).isEmpty() );
        assertTrue( compute( Arrays.asList( snap( 1, null, null ) ) ).isEmpty() );
    }

    @Test
    public void droneSnapshotScanUsesGreaterOrEqualAndFirstReachWins() {
        final Map< LarvaMilestone, Integer > m = compute( Arrays.asList( snap( 100, 0, 65 ), snap( 200, 0, 66 ), snap( 300, 0, 60 ),
                snap( 400, 0, 70 ), snap( 500, 0, 80 ) ) );
        assertEquals( Integer.valueOf( 200 ), m.get( LarvaMilestone.DRONES_66 ) );
        assertEquals( Integer.valueOf( 500 ), m.get( LarvaMilestone.DRONES_80 ) );
    }

    @Test
    public void droneMilestonesIgnoreTheZeroSamplesAfterAPlayerLeft() {
        final Map< LarvaMilestone, Integer > m = compute( Arrays.asList( snap( 100, 0, 70 ), snap( 260, 0, 0 ), snap( 420, 0, 0 ) ) );
        assertEquals( Integer.valueOf( 100 ), m.get( LarvaMilestone.DRONES_66 ) );
        assertNull( m.get( LarvaMilestone.DRONES_80 ) );
    }

    @Test
    public void unsortedSnapshotsStillGiveEarliestLoop() {
        final Map< LarvaMilestone, Integer > m = compute( Arrays.asList( snap( 900, 100 * SCALE, 0 ), snap( 300, 101 * SCALE, 0 ) ) );
        assertEquals( Integer.valueOf( 300 ), m.get( LarvaMilestone.SUPPLY_100 ) );
    }

    @Test
    public void resultIsInDisplayOrder() {
        final List< LarvaMilestone > order = new ArrayList<>( Arrays.asList( LarvaMilestone.values() ) );
        assertEquals( Arrays.asList( LarvaMilestone.SUPPLY_100, LarvaMilestone.DRONES_66, LarvaMilestone.DRONES_80, LarvaMilestone.SUPPLY_200 ), order );
        final Map< LarvaMilestone, Integer > m = compute( Arrays.asList( snap( 1, 200 * SCALE, 80 ) ) );
        assertEquals( order, new ArrayList<>( m.keySet() ) );
    }
}
