package hu.aleks.larvasecondslostextmod;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.EnumMap;
import java.util.Map;

import org.junit.Test;

/** Milestone table of the single-replay page (E20-S09): one column per milestone, time and ranking of the game's matchup. */
public class LarvaMilestoneTableViewModelsTest {

    private static Map< LarvaMilestone, Integer > loops() {
        final Map< LarvaMilestone, Integer > loops = new EnumMap<>( LarvaMilestone.class );
        loops.put( LarvaMilestone.DRONES_44, Integer.valueOf( 6720 ) ); // 5:00
        loops.put( LarvaMilestone.SUPPLY_100, Integer.valueOf( 10240 ) ); // 7:37
        return loops;
    }

    @Test
    public void columnsAreTheMilestonesInGameOrder() {
        final LarvaTableViewModel table = LarvaMilestoneTableViewModels.fromPlayerMilestones( new LarvaPlayerMilestones( LarvaMatchupBucket.ZVZ, loops() ) );
        assertNull( table.getTitle() );
        assertEquals( "Milestone", table.getCornerLabel() );
        assertEquals( LarvaMilestone.values().length, table.getColumnCount() );
        assertEquals( "44 drones", table.getColumnHeader( 0 ) );
        assertEquals( "55 drones", table.getColumnHeader( 1 ) );
        assertEquals( "100 supply", table.getColumnHeader( 2 ) );
        assertEquals( "200 supply", table.getColumnHeader( 6 ) );
        assertEquals( LarvaMilestoneTableViewModels.BODY_ROW_COUNT, table.getRowCount() );
        assertEquals( "Reached at", table.getRowLabel( 0 ) );
        assertEquals( "ranking (ZvZ)", table.getRowLabel( 1 ) );
    }

    @Test
    public void timesUseTheGameClockAndRankingsTheMatchupBand() {
        final LarvaTableViewModel table = LarvaMilestoneTableViewModels.fromPlayerMilestones( new LarvaPlayerMilestones( LarvaMatchupBucket.ZVZ, loops() ) );
        assertEquals( "5:00", table.getCell( 0, 0 ) );
        assertEquals( "n/a", table.getCell( 0, 1 ) ); // 55 drones not reached
        assertEquals( "7:37", table.getCell( 0, 2 ) );
        // fixture DRONES_44.ZVZ: masters 8280, GM 7280, pro 6840
        assertEquals( "top 75% pro", table.getCell( 1, 0 ) );
        assertEquals( "pro", table.getBoldText( 1, 0 ) ); // E20-S12: the league is bold, the time row is not
        assertEquals( "", table.getBoldText( 0, 0 ) );
        assertEquals( "n/a", table.getCell( 1, 1 ) );
        // fixture SUPPLY_100.ZVZ: masters 10600, GM 9880
        assertEquals( "top 55% masters", table.getCell( 1, 2 ) );
        assertEquals( "n/a", table.getCell( 1, 3 ) ); // 66 drones: not reached, and ZvZ has no band
    }

    @Test
    public void anUnknownMatchupIsRatedAgainstAllGames() {
        final LarvaPlayerMilestones milestones = new LarvaPlayerMilestones( null, loops() );
        assertEquals( LarvaMatchupBucket.ALL, milestones.getRatingBucket() );
        final LarvaTableViewModel table = LarvaMilestoneTableViewModels.fromPlayerMilestones( milestones );
        assertEquals( "ranking (All)", table.getRowLabel( 1 ) );
        // fixture DRONES_44.ALL: masters 6960, GM 6560
        assertEquals( "top 45% masters", table.getCell( 1, 0 ) );
    }

    /** E20-S10: a one-tier band reads as a pace on the single-replay page too. */
    @Test
    public void aOneTierBandReadsAsAPace() {
        final Map< LarvaMilestone, Integer > loops = loops();
        loops.put( LarvaMilestone.SUPPLY_200, Integer.valueOf( 12800 ) ); // 9:31
        final LarvaTableViewModel table = LarvaMilestoneTableViewModels.fromPlayerMilestones( new LarvaPlayerMilestones( LarvaMatchupBucket.ZVZ, loops ) );
        // fixture SUPPLY_200.ZVZ: GM/pro pace 13120
        assertEquals( "GM/pro pace 9:45", table.getCell( 1, 6 ) );
        assertEquals( "GM/pro", table.getBoldText( 1, 6 ) );
        loops.put( LarvaMilestone.SUPPLY_200, Integer.valueOf( 13280 ) );
        final LarvaTableViewModel slower = LarvaMilestoneTableViewModels.fromPlayerMilestones( new LarvaPlayerMilestones( LarvaMatchupBucket.ZVZ, loops ) );
        assertEquals( "aim for 9:45", slower.getCell( 1, 6 ) );
        assertEquals( "", slower.getBoldText( 1, 6 ) );
    }

    @Test( expected = IllegalArgumentException.class )
    public void allIsNotAMatchup() {
        new LarvaPlayerMilestones( LarvaMatchupBucket.ALL, loops() );
    }
}
