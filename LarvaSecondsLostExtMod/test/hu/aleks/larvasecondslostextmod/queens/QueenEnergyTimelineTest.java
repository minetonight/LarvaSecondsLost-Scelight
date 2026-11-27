package hu.aleks.larvasecondslostextmod.queens;

import static hu.aleks.larvasecondslostextmod.queens.LoopIntervalsTest.text;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** Queen energy: regeneration, cap, spends, and spare energy for an inject (E19-S09). */
public class QueenEnergyTimelineTest {

    private static final int H = 7;
    private static final int OTHER = 8;
    /** Loops to regenerate 25 energy. */
    private static final int R25 = QueenEnergy.loopsToRegenerate( 25.0d );

    private static QueenSpend inject( final int loop, final int hatch ) {
        return new QueenSpend( loop, QueenSpend.Kind.INJECT, hatch, Double.NaN, Double.NaN, null );
    }

    private static QueenSpend tumor( final int loop ) {
        return new QueenSpend( loop, QueenSpend.Kind.TUMOR, -1, 0, 0, null );
    }

    @Test
    public void regenerationMatchesGameDataAndCaps() {
        assertEquals( 712, R25 ); // 31.7 s at Faster
        final QueenEnergyTimeline queen = new QueenEnergyTimeline( 1000, 100000 );
        assertEquals( 25.0d, queen.energyAt( 1000 ), 1e-9 );
        assertEquals( 25.0d + 0.5625d, queen.energyAt( 1016 ), 1e-9 ); // one game second
        assertEquals( 50.0d, queen.energyAt( 1000 + R25 ), 0.05d );
        assertEquals( QueenEnergy.CAP, queen.energyAt( 99999 ), 1e-9 );
        assertEquals( 0.0d, queen.energyAt( 999 ), 1e-9 );
    }

    @Test
    public void spendsLowerEnergyAndUnaffordableSpendIsAConflict() {
        final QueenEnergyTimeline queen = new QueenEnergyTimeline( 0, 100000 );
        queen.addSpend( inject( 10, H ) );
        assertEquals( 25.0d + 10 * QueenEnergy.PER_LOOP - 25.0d, queen.energyNow( 10 ), 1e-9 );
        assertFalse( queen.canAfford( 20, 25.0d ) );
        queen.addSpend( tumor( 20 ) );
        assertEquals( 1, queen.getConflictCount() );
        assertEquals( 0.0d, queen.energyAt( 20 ), 1e-9 );
        assertTrue( queen.canAfford( 20 + R25, 25.0d ) );
    }

    @Test( expected = IllegalArgumentException.class )
    public void spendsMustComeInLoopOrder() {
        final QueenEnergyTimeline queen = new QueenEnergyTimeline( 0, 100000 );
        queen.addSpend( inject( 100, H ) );
        queen.addSpend( inject( 50, H ) );
    }

    @Test
    public void lateInjectOnTheSameHatcheryIsSpareFromReadyUntilTheInject() {
        final QueenEnergyTimeline queen = new QueenEnergyTimeline( 0, 10000 );
        queen.addSpend( inject( 0, H ) ); // 0 energy left
        queen.addSpend( inject( 2000, H ) ); // about 70 - 25 = 45 left: spare again right away
        assertEquals( "[" + R25 + ",10000)", text( queen.spareIntervals( H ) ) );
    }

    @Test
    public void energyThatGoesIntoATumorSoonAfterIsNotSpare() {
        final QueenEnergyTimeline queen = new QueenEnergyTimeline( 0, R25 + 200 );
        queen.addSpend( tumor( 100 ) ); // 25 + 100 loops of regen - 25: about 3.5 left
        queen.addSpend( tumor( R25 + 150 ) ); // spends the next 25 as soon as she has it
        assertEquals( "", text( queen.spareIntervals( H ) ) );
    }

    @Test
    public void bankedEnergyLaterSpentOnTumorsIsNotSpare() {
        final QueenEnergyTimeline queen = new QueenEnergyTimeline( 0, 4000 );
        final double banked = 25.0d + 3000 * QueenEnergy.PER_LOOP; // about 130
        for ( int i = 0; i < 5; i++ )
            queen.addSpend( tumor( 3000 ) ); // about 5 left
        final int readyAgain = 3000 + QueenEnergy.loopsToRegenerate( 25.0d - ( banked - 125.0d ) );
        assertEquals( "[" + readyAgain + ",4000)", text( queen.spareIntervals( H ) ) );
    }

    @Test
    public void energyLeftOverAfterLaterTumorsIsSpare() {
        final QueenEnergyTimeline queen = new QueenEnergyTimeline( 0, 4000 );
        for ( int i = 0; i < 4; i++ )
            queen.addSpend( tumor( 3000 ) ); // about 130 - 100 = 30 left: one inject more was affordable all along
        assertEquals( "[0,4000)", text( queen.spareIntervals( H ) ) );
    }

    @Test
    public void injectOnAnotherHatcheryCountsAgainstSpareEnergy() {
        final QueenEnergyTimeline queen = new QueenEnergyTimeline( 0, 800 );
        queen.addSpend( inject( 500, OTHER ) ); // about 17.6 left: spare again only after regenerating
        final int readyAgain = 500 + QueenEnergy.loopsToRegenerate( 25.0d - ( 500 * QueenEnergy.PER_LOOP ) );
        assertEquals( "[" + readyAgain + ",800)", text( queen.spareIntervals( H ) ) );
        assertEquals( "[0,500)[" + readyAgain + ",800)", text( queen.spareIntervals( OTHER ) ) );
    }

    @Test
    public void energyAtTheCapIsSpareEvenIfSpentLater() {
        final QueenEnergyTimeline queen = new QueenEnergyTimeline( 0, 20000 );
        final int capLoop = (int) Math.ceil( ( QueenEnergy.CAP - QueenEnergy.START ) / QueenEnergy.PER_LOOP );
        for ( int i = 0; i < 8; i++ )
            queen.addSpend( tumor( capLoop + 1000 ) ); // dumps everything at once, long after the cap
        assertEquals( "[0," + ( capLoop + 1000 ) + ")[" + ( capLoop + 1000 + R25 ) + ",20000)", text( queen.spareIntervals( H ) ) );
    }

    /** Scenario third base: a queen hoards energy at a skipped hatchery, then spends it all in a creep burst much later. */
    @Test
    public void withAHorizonOnlySpendsCloseAheadBlockSpareEnergy() {
        final QueenEnergyTimeline queen = new QueenEnergyTimeline( 0, 100000 );
        for ( int loop = 2844; loop <= 2848; loop++ )
            queen.addSpend( tumor( loop ) ); // 125 energy, five tumors: the fifth leaves almost nothing
        final java.util.List< int[] > withHorizon = queen.spareIntervals( H, 642 );
        assertEquals( 0, withHorizon.get( 0 )[ 0 ] );
        assertEquals( 2848 - 642, withHorizon.get( 0 )[ 1 ] );
        assertTrue( queen.spareIntervals( H ).get( 0 )[ 0 ] >= 2848 );
    }
}
