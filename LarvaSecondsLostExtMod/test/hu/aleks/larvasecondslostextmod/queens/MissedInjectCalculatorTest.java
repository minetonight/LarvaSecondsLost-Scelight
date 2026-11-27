package hu.aleks.larvasecondslostextmod.queens;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Test;

/**
 * Red windows across hatcheries (E19-S02 scenario: a queen between a main and a macro hatchery). Distances are between hatchery
 * centers; a hatchery is 5 by 5 cells, so two touching hatcheries are 5 cells apart. Any hatchery within the 12-cell radius counts.
 */
public class MissedInjectCalculatorTest {

    private static final int MAIN = 100;
    private static final int MACRO = 200;
    private static final int END = 50000;

    /** Touching hatcheries (5 by 5 each), the closest a macro hatchery can be. */
    private static final int TOUCHING = 5;

    /** A macro hatchery with a gap of 3 cells. */
    private static final int GAP = 8;

    private static String red( final int macroX, final List< int[] > mainInjects ) {
        final MissedInjectInput input = new MissedInjectInput().addHatch( new MissedInjectInput.Hatch( MAIN, 0, 0, 0, -1, mainInjects ) )
                .addHatch( new MissedInjectInput.Hatch( MACRO, macroX, 0, 0, -1, Collections.< int[] >emptyList() ) )
                .addQueen( new MissedInjectInput.Queen( 1, "q", 0, 1, 0, -1, 0, 0 ) );
        final QueenSpendAttributor.Result attribution = QueenSpendAttributor.attribute( input, new QueenSpendAttributor.Settings( 12.0d, END, 886, 32 ) );
        final Map< Integer, MissedInjectCalculator.HatchResult > results = MissedInjectCalculator.compute( input, attribution,
                new MissedInjectCalculator.Settings( 600, 2000, 0, END, new MissedInjectCalculator.TimeFormatter() {
                    @Override
                    public String format( final int loop ) {
                        return String.valueOf( loop );
                    }
                } ) );
        final StringBuilder b = new StringBuilder();
        for ( final int hatch : new int[] { MAIN, MACRO } )
            for ( final MissedInjectCalculator.RedWindow window : results.get( Integer.valueOf( hatch ) ).getWindows() )
                b.append( hatch == MAIN ? "main" : "macro" ).append( '[' ).append( window.getStart() ).append( ',' ).append( window.getEnd() ).append( ')' );
        return b.toString();
    }

    /** Her energy keeps up with one inject schedule: only the nearer hatchery is red. */
    @Test
    public void aQueenNearTwoHatcheriesCountsForTheNearerOne() {
        assertEquals( "main[0,600)", red( GAP, Collections.< int[] >emptyList() ) );
        assertEquals( "main[0,600)", red( TOUCHING, Collections.< int[] >emptyList() ) );
    }

    /** While the main is injected she is missing the macro hatchery's inject. */
    @Test
    public void whileTheNearerHatcheryIsInjectedTheOtherOneIsRed() {
        assertEquals( "macro[0,600)", red( GAP, Arrays.asList( new int[] { -100, 1000 } ) ) );
        assertEquals( "macro[0,600)", red( TOUCHING, Arrays.asList( new int[] { -100, 1000 } ) ) );
    }
}
