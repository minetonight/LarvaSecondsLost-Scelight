package hu.aleks.larvasecondslostextmod;

import static hu.aleks.larvasecondslostextmod.LarvaTestFixtures.game;
import static hu.aleks.larvasecondslostextmod.LarvaTestFixtures.player;
import static hu.aleks.larvasecondslostextmod.LarvaTestFixtures.stats;
import static hu.aleks.larvasecondslostextmod.LarvaTestFixtures.summaryOf;
import static hu.aleks.larvasecondslostextmod.LarvaTestFixtures.tracked;
import static hu.aleks.larvasecondslostextmod.LarvaTestFixtures.withEnd;
import static hu.aleks.larvasecondslostextmod.LarvaTestFixtures.withMilestone;
import static hu.aleks.larvasecondslostextmod.LarvaTestFixtures.withStats;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import org.junit.Test;

/**
 * Criterion 8 on synthetic data: the S12 dump and the view-model dump of the same result agree cell by cell, and the check
 * notices a difference. The real {@code queensMoved} run is {@code ant TEST_GOLDEN_MULTI}.
 */
public class LarvaMultiViewGoldenCheckTest {

    private static LarvaAggregationResult result() {
        final LarvaAggregationPlayerRecord.Builder b = withStats( player( "A", LarvaMatchupBucket.ZVT ),
                stats( LarvaGamePhase.EARLY, 7, 6, 66, 12391, 8267, 4528 ) );
        withStats( b, stats( LarvaGamePhase.MID, 77, 66, 123, 31723, 31723, 14132 ) );
        withEnd( b, LarvaGamePhase.EARLY, 411666 );
        withMilestone( b, LarvaMilestone.SUPPLY_100, 678888 );
        return new LarvaAggregator().aggregate( Arrays.asList( game( 1, b.build() ), game( 2, b.build() ) ), Arrays.asList( tracked( "A", 2 ) ),
                summaryOf( 2 ) );
    }

    private static String golden() {
        return new LarvaMultiAnalysisDumpFormatter().format( result() );
    }

    private static String view() {
        return LarvaMultiAnalysisViewModelBuilder.build( result(), Locale.US, TimeZone.getTimeZone( "UTC" ) ).toText();
    }

    @Test
    public void viewModelCellsEqualTheGoldenDumpValues() {
        final List< String > problems = LarvaMultiViewGoldenCheck.compare( golden(), view() );
        assertEquals( "[]", problems.toString() );
        assertTrue( golden().contains( "player 1: name=A" ) );
    }

    @Test
    public void aChangedCellIsReported() {
        final List< String > problems = LarvaMultiViewGoldenCheck.compare( golden(), view().replace( "6:51 (2/2)", "6:52 (2/2)" ) );
        assertFalse( problems.isEmpty() );
        assertTrue( problems.get( 0 ), problems.get( 0 ).contains( "golden '6:51 (2/2)'" ) );
    }

    @Test
    public void aMissingPlayerIsReported() {
        assertFalse( LarvaMultiViewGoldenCheck.compare( golden(), "header: x\nskipped (0):\n" ).isEmpty() );
    }
}
