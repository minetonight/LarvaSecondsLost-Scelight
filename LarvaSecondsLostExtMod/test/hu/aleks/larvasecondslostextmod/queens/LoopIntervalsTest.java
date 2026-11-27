package hu.aleks.larvasecondslostextmod.queens;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

/** Interval set operations of the missed-inject model. */
public class LoopIntervalsTest {

    static List< int[] > list( final int... bounds ) {
        final List< int[] > result = new ArrayList<>();
        for ( int i = 0; i + 1 < bounds.length; i += 2 )
            result.add( new int[] { bounds[ i ], bounds[ i + 1 ] } );
        return result;
    }

    static String text( final List< int[] > intervals ) {
        final StringBuilder b = new StringBuilder();
        for ( final int[] interval : intervals )
            b.append( '[' ).append( interval[ 0 ] ).append( ',' ).append( interval[ 1 ] ).append( ')' );
        return b.toString();
    }

    @Test
    public void normalizeSortsMergesTouchingAndDropsEmpty() {
        assertEquals( "[0,30)[40,50)", text( LoopIntervals.normalize( list( 20, 30, 0, 10, 10, 20, 40, 50, 60, 60 ) ) ) );
    }

    @Test
    public void intersectKeepsCommonLoops() {
        assertEquals( "[5,10)[20,25)", text( LoopIntervals.intersect( list( 0, 10, 20, 30 ), list( 5, 25 ) ) ) );
        assertEquals( "", text( LoopIntervals.intersect( list( 0, 10 ), list( 10, 20 ) ) ) );
    }

    @Test
    public void subtractCutsHoles() {
        assertEquals( "[0,5)[10,20)[25,30)", text( LoopIntervals.subtract( list( 0, 30 ), list( 5, 10, 20, 25 ) ) ) );
        assertEquals( "", text( LoopIntervals.subtract( list( 0, 30 ), list( -5, 40 ) ) ) );
        assertEquals( "[0,30)", text( LoopIntervals.subtract( list( 0, 30 ), Arrays.< int[] >asList() ) ) );
    }

    @Test
    public void lengthCountsMergedLoops() {
        assertEquals( 30L, LoopIntervals.length( list( 0, 10, 5, 20, 30, 40 ) ) );
    }
}
