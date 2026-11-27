package hu.aleks.larvasecondslostextmod.queens;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

/** Remove masks of selection deltas, as measured on replays (E19-S11). */
public class SelectionMaskDecodingTest {

    private static String removed( final int count, final int... chunks ) {
        final byte[] bytes = new byte[ chunks.length ];
        for ( int i = 0; i < chunks.length; i++ )
            bytes[ i ] = (byte) chunks[ i ];
        final boolean[] flags = SelectionMaskDecoding.removedFlags( count, bytes );
        final StringBuilder b = new StringBuilder();
        for ( int i = 0; i < flags.length; i++ )
            if ( flags[ i ] )
                b.append( b.length() == 0 ? "" : "," ).append( i );
        return b.toString();
    }

    @Test
    public void oneChunkReadsBitIAtIndexI() {
        assertEquals( "0", removed( 1, 0x01 ) );
        assertEquals( "1,2", removed( 3, 0x06 ) );
        assertEquals( "5,6", removed( 7, 0x60 ) );
        assertEquals( "0,7", removed( 8, 0x81 ) );
    }

    /** Larva morphing to eggs two by two in the scenario replay: the removed indices are the last two of each mask. */
    @Test
    public void twoChunksFollowTheMeasuredLayout() {
        assertEquals( "7,8", removed( 9, 0x81, 0x00 ) );
        assertEquals( "8,9", removed( 10, 0x03, 0x00 ) );
        assertEquals( "9,10", removed( 11, 0x06, 0x00 ) );
        assertEquals( "10,11", removed( 12, 0x0c, 0x00 ) );
        assertEquals( "11,12", removed( 13, 0x0c, 0x00 ) );
        assertEquals( "12,13", removed( 14, 0x0c, 0x00 ) );
        assertEquals( "12,13,14", removed( 15, 0x0e, 0x00 ) );
    }

    /** A mask is cut after its highest index, so index n-1 is set in every mask; these are the chunk bits replays always have set. */
    @Test
    public void highestIndexSitsWhereReplaysHaveIt() {
        assertArrayEquals( new int[] { 0, 0 }, SelectionMaskDecoding.chunkPositionOf( 8, 9 ) );
        assertArrayEquals( new int[] { 0, 3 }, SelectionMaskDecoding.chunkPositionOf( 11, 12 ) );
        assertArrayEquals( new int[] { 0, 3 }, SelectionMaskDecoding.chunkPositionOf( 15, 16 ) );
        assertArrayEquals( new int[] { 1, 0 }, SelectionMaskDecoding.chunkPositionOf( 16, 17 ) );
        assertArrayEquals( new int[] { 1, 3 }, SelectionMaskDecoding.chunkPositionOf( 23, 24 ) );
        assertArrayEquals( new int[] { 2, 0 }, SelectionMaskDecoding.chunkPositionOf( 24, 25 ) );
        assertArrayEquals( new int[] { 2, 3 }, SelectionMaskDecoding.chunkPositionOf( 31, 32 ) );
        assertEquals( "32", removed( 33, 0, 0, 0, 0, 0x01 ) );
        assertEquals( "39", removed( 40, 0, 0, 0, 0, 0x80 ) );
    }

    @Test
    public void layoutUsesEveryChunkBitOnce() {
        for ( int n = 9; n <= SelectionMaskDecoding.LAYOUT_MAX_BITS; n++ ) {
            final Set< Integer > used = new HashSet<>();
            final int chunks = ( n + 7 ) / 8;
            final int lastBits = n % 8 == 0 ? 8 : n % 8;
            for ( int i = 0; i < n; i++ ) {
                final int[] position = SelectionMaskDecoding.chunkPositionOf( i, n );
                assertTrue( "n=" + n + " i=" + i, position[ 0 ] >= 0 && position[ 0 ] < chunks && position[ 1 ] >= 0
                        && position[ 1 ] < ( position[ 0 ] == chunks - 1 ? lastBits : 8 ) );
                assertTrue( "n=" + n + " i=" + i + " twice", used.add( Integer.valueOf( position[ 0 ] * 8 + position[ 1 ] ) ) );
            }
        }
    }

    @Test
    public void missingChunksRemoveNothing() {
        assertEquals( 3, SelectionMaskDecoding.removedFlags( 3, null ).length );
        assertEquals( "4,5,6,7,8,9,10,11", removed( 12, 0xff ) );
    }
}
