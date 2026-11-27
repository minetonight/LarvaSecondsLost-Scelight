package hu.aleks.larvasecondslostextmod.queens;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Half-open loop intervals {@code [start, end)} as {@code int[2]}, and the set operations the missed-inject model needs. Inputs
 * may be unsorted and overlapping; outputs are sorted, merged and never contain empty intervals.
 */
public final class LoopIntervals {

    private static final Comparator< int[] > BY_START = new Comparator< int[] >() {
        @Override
        public int compare( final int[] a, final int[] b ) {
            return a[ 0 ] != b[ 0 ] ? ( a[ 0 ] < b[ 0 ] ? -1 : 1 ) : ( a[ 1 ] < b[ 1 ] ? -1 : a[ 1 ] == b[ 1 ] ? 0 : 1 );
        }
    };

    private LoopIntervals() {
    }

    /** @return the intervals sorted, with overlapping and touching ones merged and empty ones dropped */
    public static List< int[] > normalize( final List< int[] > intervals ) {
        final List< int[] > sorted = new ArrayList<>();
        for ( final int[] interval : intervals )
            if ( interval[ 1 ] > interval[ 0 ] )
                sorted.add( new int[] { interval[ 0 ], interval[ 1 ] } );
        Collections.sort( sorted, BY_START );
        final List< int[] > merged = new ArrayList<>();
        for ( final int[] interval : sorted ) {
            final int[] last = merged.isEmpty() ? null : merged.get( merged.size() - 1 );
            if ( last != null && interval[ 0 ] <= last[ 1 ] )
                last[ 1 ] = Math.max( last[ 1 ], interval[ 1 ] );
            else
                merged.add( interval );
        }
        return merged;
    }

    /** @return the loops that are in both {@code a} and {@code b} */
    public static List< int[] > intersect( final List< int[] > a, final List< int[] > b ) {
        final List< int[] > left = normalize( a );
        final List< int[] > right = normalize( b );
        final List< int[] > result = new ArrayList<>();
        int i = 0;
        int j = 0;
        while ( i < left.size() && j < right.size() ) {
            final int start = Math.max( left.get( i )[ 0 ], right.get( j )[ 0 ] );
            final int end = Math.min( left.get( i )[ 1 ], right.get( j )[ 1 ] );
            if ( end > start )
                result.add( new int[] { start, end } );
            if ( left.get( i )[ 1 ] < right.get( j )[ 1 ] )
                i++;
            else
                j++;
        }
        return result;
    }

    /** @return the loops of {@code a} that are not in {@code b} */
    public static List< int[] > subtract( final List< int[] > a, final List< int[] > b ) {
        final List< int[] > cut = normalize( b );
        final List< int[] > result = new ArrayList<>();
        for ( final int[] interval : normalize( a ) ) {
            int cursor = interval[ 0 ];
            for ( final int[] hole : cut ) {
                if ( hole[ 1 ] <= cursor || hole[ 0 ] >= interval[ 1 ] )
                    continue;
                if ( hole[ 0 ] > cursor )
                    result.add( new int[] { cursor, hole[ 0 ] } );
                cursor = Math.max( cursor, hole[ 1 ] );
            }
            if ( cursor < interval[ 1 ] )
                result.add( new int[] { cursor, interval[ 1 ] } );
        }
        return result;
    }

    /** @return the total length in loops */
    public static long length( final List< int[] > intervals ) {
        long total = 0L;
        for ( final int[] interval : normalize( intervals ) )
            total += interval[ 1 ] - interval[ 0 ];
        return total;
    }
}
