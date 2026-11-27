package hu.aleks.larvasecondslostextmod.queens;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Test;

/**
 * The scripted scenarios of the epic (section 4, S-a to S-d) as synthetic evidence, run through attribution and the red-window
 * calculation (E19-S10). Real scenario replays replace the numbers here once they exist (E19-S02).
 */
public class MissedInjectScenarioTest {

    static final int MAIN = 100;
    static final int NATURAL = 200;
    static final int QUEEN_A = 1;
    static final int QUEEN_B = 2;
    static final int QUEEN_C = 3;
    static final int END = 30000;
    /** Inject window length (29 s at Faster in the module's loop basis). */
    static final int INJECT = 642;
    static final int HOLD = 642;
    static final int BRIDGE = 1993;
    /** Red loops of the on-time inject queen from rounding (about 3 s over 12 injects). */
    static final long ON_TIME_SLIVER_LOOPS = 63L;

    /** Builds one player's evidence: main at (100, 100), natural 30 cells east. */
    static final class Game {
        final MissedInjectInput input = new MissedInjectInput();
        final List< int[] > mainInjects = new ArrayList<>();
        final List< int[] > naturalInjects = new ArrayList<>();
        int unlockLoop = 0;
        boolean multiSelect;

        Game queen( final int tag, final int bornLoop, final int hatch ) {
            final double x = hatch == MAIN ? 100 : 130;
            input.addQueen( new MissedInjectInput.Queen( tag, "q" + tag, bornLoop, x, 103, -1, 0, 0 ) );
            return this;
        }

        /** An inject window on the hatchery, with a Spawn Larva command from the queen (or from all queens if multi-select). */
        Game inject( final int hatch, final int loop, final int queenTag ) {
            ( hatch == MAIN ? mainInjects : naturalInjects ).add( new int[] { loop, loop + INJECT } );
            input.addInjectCommand( new MissedInjectInput.InjectCommand( loop, hatch, selection( queenTag ) ) );
            return this;
        }

        Game tumor( final int loop, final double x, final double y, final int queenTag ) {
            input.addTumor( new QueenSpend( loop, QueenSpend.Kind.TUMOR, -1, x, y, selection( queenTag ) ) );
            return this;
        }

        private int[] selection( final int queenTag ) {
            return multiSelect ? new int[] { QUEEN_A, QUEEN_B, QUEEN_C } : new int[] { queenTag };
        }

        Map< Integer, MissedInjectCalculator.HatchResult > run() {
            input.addHatch( new MissedInjectInput.Hatch( MAIN, 100, 100, 0, -1, mainInjects ) );
            input.addHatch( new MissedInjectInput.Hatch( NATURAL, 130, 100, 0, -1, naturalInjects ) );
            final QueenSpendAttributor.Result attribution = QueenSpendAttributor.attribute( input, new QueenSpendAttributor.Settings( 12.0d, END, 886,
                    32 ) );
            return MissedInjectCalculator.compute( input, attribution, new MissedInjectCalculator.Settings( HOLD, BRIDGE, unlockLoop, END,
                    new MissedInjectCalculator.TimeFormatter() {
                        @Override
                        public String format( final int loop ) {
                            return String.valueOf( loop );
                        }
                    } ) );
        }
    }

    static long redLoops( final Map< Integer, MissedInjectCalculator.HatchResult > results, final int hatch, final int from, final int to ) {
        final List< int[] > red = new ArrayList<>();
        for ( final MissedInjectCalculator.RedWindow window : results.get( Integer.valueOf( hatch ) ).getWindows() )
            red.add( new int[] { window.getStart(), window.getEnd() } );
        final List< int[] > range = new ArrayList<>();
        range.add( new int[] { from, to } );
        return LoopIntervals.length( LoopIntervals.intersect( red, range ) );
    }

    /**
     * Queen A injects the main every 712 loops (as soon as she has 25 energy), from loop 1000 to 9544. 712 loops give 25.03 energy,
     * so the surplus adds up and she is ready a few loops before each inject: {@link #ON_TIME_SLIVER_LOOPS} in total.
     */
    static Game onTimeMainQueen( final Game game ) {
        game.queen( QUEEN_A, 1000, MAIN );
        for ( int loop = 1000; loop < 10000; loop += 712 )
            game.inject( MAIN, loop, QUEEN_A );
        return game;
    }

    @Test
    public void injectQueenOnTimeHasOnlyRoundingSlivers() {
        final Map< Integer, MissedInjectCalculator.HatchResult > results = onTimeMainQueen( new Game() ).run();
        assertEquals( ON_TIME_SLIVER_LOOPS, redLoops( results, MAIN, 0, 9544 ) );
        for ( final MissedInjectCalculator.RedWindow window : results.get( Integer.valueOf( MAIN ) ).getWindows() )
            assertTrue( window.getEnd() - window.getStart() <= 16 );
    }

    /** S-a: queen B injects the natural, walks 40 cells away and drops tumors, comes back and injects again. */
    static Game creepAway( final Game game ) {
        onTimeMainQueen( game ).queen( QUEEN_B, 1000, NATURAL );
        for ( int loop = 1000; loop < 4000; loop += 712 )
            game.inject( NATURAL, loop, QUEEN_B );
        for ( int loop = 4600; loop < 7400; loop += 712 )
            game.tumor( loop, 170, 100, QUEEN_B );
        game.inject( NATURAL, 7800, QUEEN_B );
        return game;
    }

    @Test
    public void creepAwayGivesNoRedWhileTheQueenIsAway() {
        final Map< Integer, MissedInjectCalculator.HatchResult > results = creepAway( new Game() ).run();
        // last inject before leaving at 3848: window to 4490, hold ends at 4490 too; away from the first tumor at 4600 to 7800
        assertEquals( 0L, redLoops( results, NATURAL, 3848, 7800 ) );
        assertEquals( ON_TIME_SLIVER_LOOPS, redLoops( results, MAIN, 0, 9544 ) );
    }

    /** S-b: queen C stays within 8 cells of the main and drops a tumor each time she has 25 energy. */
    @Test
    public void creepQueenNextToTheMainSpendingAllHerEnergyGivesNoRed() {
        final Game game = onTimeMainQueen( new Game() ).queen( QUEEN_C, 1000, MAIN );
        for ( int loop = 1100; loop < 10000; loop += 712 )
            game.tumor( loop, 106, 104, QUEEN_C );
        assertEquals( ON_TIME_SLIVER_LOOPS, redLoops( game.run(), MAIN, 0, 9544 ) );
    }

    /** S-c: a queen with spare energy stands at her hatchery for 90 s after an inject, then injects again. */
    static Game idle( final Game game ) {
        game.queen( QUEEN_A, 0, MAIN ); // 60 energy at loop 1000: spare right after her inject
        game.inject( MAIN, 1000, QUEEN_A );
        game.inject( MAIN, 1000 + INJECT + 1993, QUEEN_A ); // 90 s (displayed) late
        return game;
    }

    @Test
    public void idleQueenNextToHerHatcheryIsRedUpToTheBridgeLimit() {
        final Map< Integer, MissedInjectCalculator.HatchResult > results = idle( new Game() ).run();
        // red from the end of the inject (1642) until the bridge limit after the inject (1000 + 1993): about 61 s, 2 missed injects
        assertEquals( 1000 + BRIDGE - ( 1000 + INJECT ), redLoops( results, MAIN, 1000, 1000 + INJECT + 1993 ) );
        // window 0 is the walk from birth (loop 0) to the first inject; window 1 is the idle time
        final String note = results.get( Integer.valueOf( MAIN ) ).getWindows().get( 1 ).getNote();
        assertTrue( note, note.contains( "queen q1 at the hatchery from inject 1000" ) && note.contains( "counted until 2993" ) );
    }

    /** S-d: S-a and S-c again, but every command was given with all queens selected. */
    @Test
    public void multiSelectGivesTheSameResultsAsSingleSelect() {
        final Game single = creepAway( new Game() );
        final Game multi = new Game();
        multi.multiSelect = true;
        creepAway( multi );
        assertEquals( redLoops( single.run(), NATURAL, 0, END ), redLoops( multi.run(), NATURAL, 0, END ) );
        final Game idleMulti = new Game();
        idleMulti.multiSelect = true;
        assertEquals( redLoops( idle( new Game() ).run(), MAIN, 0, END ), redLoops( idle( idleMulti ).run(), MAIN, 0, END ) );
    }

    @Test
    public void nothingIsRedBeforeInjectIsUnlocked() {
        final Game game = idle( new Game() );
        game.unlockLoop = 2500;
        assertEquals( 1000 + BRIDGE - 2500, redLoops( game.run(), MAIN, 0, END ) );
    }

    @Test
    public void hatcheryWithoutQueenEvidenceHasNoRedAndSaysWhy() {
        final Map< Integer, MissedInjectCalculator.HatchResult > results = onTimeMainQueen( new Game() ).run();
        final MissedInjectCalculator.HatchResult natural = results.get( Integer.valueOf( NATURAL ) );
        assertTrue( natural.getWindows().isEmpty() );
        assertTrue( natural.getDiagnostics().get( 0 ).startsWith( "No queen evidence" ) );
    }
}
