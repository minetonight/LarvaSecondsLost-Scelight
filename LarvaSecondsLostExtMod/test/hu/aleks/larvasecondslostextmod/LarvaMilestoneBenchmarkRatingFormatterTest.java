package hu.aleks.larvasecondslostextmod;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;

import org.junit.Test;

/**
 * Milestone rankings (E20-S05): lower loops are better, {@code aim for m:ss} past the weakest tier (E20-S12), n/a without a band; a
 * one-tier band is a pace (E20-S10). Inside the bands the league label is marked (E20-S12).
 */
public class LarvaMilestoneBenchmarkRatingFormatterTest {

    private static LarvaBenchmarkTiers tiers( final String key, final String value ) {
        final Properties properties = new Properties();
        properties.setProperty( key, value );
        return LarvaMilestoneBenchmarkRatingFormatter.parse( properties ).get( key );
    }

    private static final LarvaBenchmarkTiers SUPPLY_100 = tiers( "SUPPLY_100.ALL", "dia:10800,masters:9760,GM:9280,pro:8720" );

    private static String rate( final int loop ) {
        return LarvaMilestoneBenchmarkRatingFormatter.formatRanking( SUPPLY_100, Integer.valueOf( loop ) );
    }

    @Test
    public void earlierIsBetterAndTierStartsBelongToTheirTier() {
        assertEquals( "top 100% dia", rate( 10800 ) );
        assertEquals( "top 55% dia", rate( 10280 ) ); // halfway to masters
        assertEquals( "top 100% masters", rate( 9760 ) );
        assertEquals( "top 100% GM", rate( 9280 ) );
        assertEquals( "top 100% pro", rate( 8720 ) );
    }

    @Test
    public void theBestTierIsCappedAtTopFivePercent() {
        assertEquals( "top 5% pro", rate( 6000 ) );
    }

    /** OQ-5: no percentage for a time later than the weakest tier; E20-S12: the time where it starts, without a label. */
    @Test
    public void laterThanTheWeakestTierAimsForItsStart() {
        assertEquals( "aim for 8:02", rate( 10801 ) ); // dia starts at 10800
        assertEquals( "aim for 8:02", rate( 20000 ) );
        assertFalse( LarvaMilestoneBenchmarkRatingFormatter.rankLoop( SUPPLY_100, Integer.valueOf( 20000 ) ).hasLabel() );
    }

    /** E20-S12: inside the bands the league label is marked for bold, also at the weakest tier start. */
    @Test
    public void insideTheBandsTheLabelIsMarked() {
        final LarvaRanking ranking = LarvaMilestoneBenchmarkRatingFormatter.rankLoop( SUPPLY_100, Integer.valueOf( 10280 ) );
        assertEquals( "top 55% dia", ranking.getText() );
        assertEquals( "dia", ranking.getText().substring( ranking.getLabelStart(), ranking.getLabelEnd() ) );
        assertTrue( LarvaMilestoneBenchmarkRatingFormatter.rankLoop( SUPPLY_100, Integer.valueOf( 10800 ) ).hasLabel() );
        assertFalse( LarvaRanking.NOT_AVAILABLE.hasLabel() );
    }

    @Test
    public void noBandOrNoTimeIsNotAvailable() {
        assertEquals( "n/a", LarvaMilestoneBenchmarkRatingFormatter.formatRanking( null, Integer.valueOf( 9000 ) ) );
        assertEquals( "n/a", LarvaMilestoneBenchmarkRatingFormatter.formatRanking( SUPPLY_100, null ) );
        assertEquals( "n/a", new LarvaMilestoneBenchmarkRatingFormatter().format( LarvaMilestone.DRONES_80, LarvaMatchupBucket.ZVZ,
                Integer.valueOf( 9000 ) ) );
        assertEquals( "n/a", new LarvaMilestoneBenchmarkRatingFormatter().format( null, LarvaMatchupBucket.ALL, Integer.valueOf( 9000 ) ) );
    }

    /** The test classpath has the fixture bands (COMPILE_TESTS), so this does not change with ant MILESTONE_BANDS. */
    @Test
    public void formatUsesTheBandOfTheMilestoneAndBucket() {
        assertTrue( LarvaMilestoneBenchmarkRatingFormatter.hasBands() );
        final LarvaMilestoneBenchmarkRatingFormatter formatter = new LarvaMilestoneBenchmarkRatingFormatter();
        assertEquals( "top 100% masters", formatter.format( LarvaMilestone.SUPPLY_100, LarvaMatchupBucket.ALL, Integer.valueOf( 9760 ) ) );
        assertEquals( "aim for 7:53", formatter.format( LarvaMilestone.SUPPLY_100, LarvaMatchupBucket.ZVZ, Integer.valueOf( 10700 ) ) );
    }

    @Test
    public void thePackagedBandFileLoads() throws IOException {
        final Properties properties = new Properties();
        try ( final InputStream in = new FileInputStream( "src/hu/aleks/larvasecondslostextmod/" + LarvaMilestoneBenchmarkRatingFormatter.BANDS_RESOURCE ) ) {
            properties.load( in );
        }
        final Map< String, LarvaBenchmarkTiers > bands = LarvaMilestoneBenchmarkRatingFormatter.parse( properties );
        assertFalse( bands.isEmpty() );
        // 44 drones is rated in every league overall (F-2); a band file without it is broken
        assertTrue( bands.containsKey( "DRONES_44.ALL" ) );
    }

    @Test( expected = IllegalArgumentException.class )
    public void aLaterTierIsRejected() {
        tiers( "SUPPLY_100.ALL", "gold:9000,plat:9100" );
    }

    /** E20-S10: one tier has no scale; at or before its start it is a pace (label marked), later it aims for the pace. */
    @Test
    public void oneTierIsAPace() {
        final LarvaBenchmarkTiers pace = tiers( "DRONES_66.ZVZ", "GM/pro:9120" );
        assertFalse( pace.hasScale() );
        assertEquals( "GM/pro pace 6:47", LarvaMilestoneBenchmarkRatingFormatter.formatRanking( pace, Integer.valueOf( 9120 ) ) );
        assertEquals( "GM/pro pace 6:47", LarvaMilestoneBenchmarkRatingFormatter.formatRanking( pace, Integer.valueOf( 6000 ) ) );
        assertEquals( "aim for 6:47", LarvaMilestoneBenchmarkRatingFormatter.formatRanking( pace, Integer.valueOf( 9121 ) ) );
        final LarvaRanking atPace = LarvaMilestoneBenchmarkRatingFormatter.rankLoop( pace, Integer.valueOf( 9000 ) );
        assertEquals( "GM/pro", atPace.getText().substring( atPace.getLabelStart(), atPace.getLabelEnd() ) );
    }

    /** E20-S10: a merged label is shown as written, also in the slower-than text. */
    @Test
    public void aLabelNamingSeveralLeaguesIsShownWhole() {
        final LarvaMilestoneBenchmarkRatingFormatter formatter = new LarvaMilestoneBenchmarkRatingFormatter();
        assertEquals( "top 100% masters/GM", formatter.format( LarvaMilestone.SUPPLY_150, LarvaMatchupBucket.ZVP, Integer.valueOf( 12000 ) ) );
        assertEquals( "top 65% silver/gold", LarvaMilestoneBenchmarkRatingFormatter.formatRanking(
                tiers( "SUPPLY_100.ALL", "silver/gold:11760,plat:11600" ), Integer.valueOf( 11700 ) ) );
        assertEquals( "GM/pro pace 9:45", formatter.format( LarvaMilestone.SUPPLY_200, LarvaMatchupBucket.ZVZ, Integer.valueOf( 12800 ) ) );
    }

    @Test( expected = IllegalStateException.class )
    public void oneTierHasNoTopPercent() {
        tiers( "DRONES_66.ZVZ", "GM/pro:9120" ).formatTopPercent( 9000, false );
    }

    @Test( expected = IllegalArgumentException.class )
    public void anEmptyBandIsRejected() {
        tiers( "SUPPLY_100.ALL", "" );
    }

    @Test( expected = IllegalArgumentException.class )
    public void anUnknownMilestoneIsRejected() {
        tiers( "SUPPLY_120.ALL", "gold:9000,plat:8000" );
    }

    @Test( expected = IllegalArgumentException.class )
    public void anUnknownBucketIsRejected() {
        tiers( "SUPPLY_100.ZVX", "gold:9000,plat:8000" );
    }

    @Test( expected = IllegalArgumentException.class )
    public void aKeyWithoutBucketIsRejected() {
        tiers( "SUPPLY_100", "gold:9000,plat:8000" );
    }

    @Test( expected = IllegalArgumentException.class )
    public void aMalformedTierIsRejected() {
        tiers( "SUPPLY_100.ALL", "gold9000,plat:8000" );
    }
}
