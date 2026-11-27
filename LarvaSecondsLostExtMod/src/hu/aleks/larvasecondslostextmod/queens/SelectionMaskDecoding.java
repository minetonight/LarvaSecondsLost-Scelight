package hu.aleks.larvasecondslostextmod.queens;

/**
 * Decodes the {@code Mask} remove mask of a selection delta from the chunks Scelight reads (E19-S11). Pure, so it can be unit tested.
 *
 * <p>Scelight reads an n-bit mask as {@code (n + 7) / 8} chunks in stream order: whole 8-bit chunks, then the last {@code n % 8}
 * bits, and its {@code getBit(i)} returns chunk {@code i / 8}, bit {@code i % 8}. For masks of one chunk that is right. For longer
 * masks SC2 stores the bits in another layout. Measured on 2018 to 2026 replays (old and new builds alike), with these
 * facts: a mask is cut after its highest set index, so index {@code n - 1} is always set; morphing units (larva to egg) are removed
 * and added back under the same tag, which pins the removed indices.</p>
 *
 * <p>Read the chunks as one big-endian value V (first chunk on top, as the stream composes them). With {@code r = n % 8} (8 when
 * 0) and {@code a = min(r, 4)}, V holds from its lowest bit up: the lowest {@code a} mask bits, then the bits of the last mask
 * byte ({@code r} bits), then the middle mask bytes in order, then the rest of the first mask byte. That fits masks of 9 to 16 bits
 * (85 to 94 % of morph checks, the rest are earlier tracking errors) and puts index {@code n - 1} where the replays have it for 17
 * to 32 bits. Masks longer than 32 bits keep their highest index in the last chunk, as in Scelight's reading, so they are read
 * Scelight's way. Such long partial removals are rare and almost never hold queens.</p>
 */
public final class SelectionMaskDecoding {

    /** Above this length the mask is read as Scelight reads it. */
    static final int LAYOUT_MAX_BITS = 32;

    private SelectionMaskDecoding() {
    }

    /**
     * @param count mask length in bits
     * @param chunks the chunks as Scelight read them ({@code IBitArray.getArray()})
     * @return true at each removed index
     */
    public static boolean[] removedFlags( final int count, final byte[] chunks ) {
        final boolean[] flags = new boolean[ Math.max( 0, count ) ];
        if ( chunks == null )
            return flags;
        final int chunkCount = ( count + 7 ) / 8;
        for ( int i = 0; i < count; i++ ) {
            final int[] position = chunkCount <= 1 || count > LAYOUT_MAX_BITS ? new int[] { i / 8, i % 8 } : chunkPositionOf( i, count );
            flags[ i ] = position[ 0 ] < chunks.length && ( chunks[ position[ 0 ] ] >> position[ 1 ] & 1 ) != 0;
        }
        return flags;
    }

    /** @return {chunk, bit} holding mask index i of an n-bit mask in the multi-chunk layout */
    static int[] chunkPositionOf( final int i, final int n ) {
        final int valueBit = valueBitOf( i, n );
        final int rest = n % 8;
        final int chunkCount = ( n + 7 ) / 8;
        final int chunk = ( n - 1 - valueBit ) / 8;
        if ( rest != 0 && chunk == chunkCount - 1 )
            return new int[] { chunk, valueBit };
        return new int[] { chunk, valueBit - ( n - 8 * ( chunk + 1 ) ) };
    }

    /** @return the bit of the composed value V that holds mask index i */
    static int valueBitOf( final int i, final int n ) {
        final int lastBytes = ( n + 7 ) / 8 - 1;
        final int lastLength = n % 8 == 0 ? 8 : n % 8;
        final int low = Math.min( lastLength, 4 );
        if ( i < low )
            return i;
        final int lastStart = 8 * lastBytes;
        if ( i >= lastStart )
            return low + ( i - lastStart );
        if ( i >= 8 )
            return low + lastLength + ( i - 8 );
        return low + lastLength + 8 * ( lastBytes - 1 ) + ( i - low );
    }
}
