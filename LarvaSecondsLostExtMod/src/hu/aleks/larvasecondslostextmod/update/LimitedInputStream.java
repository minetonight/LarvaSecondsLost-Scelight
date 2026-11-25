package hu.aleks.larvasecondslostextmod.update;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Fails with an {@link IOException} once more than a fixed number of bytes was read, so a wrong URL cannot fill the disk or
 * the memory.
 */
final class LimitedInputStream extends FilterInputStream {

    private final long limit;
    private long count;

    LimitedInputStream( final InputStream in, final long limit ) {
        super( in );
        this.limit = limit;
    }

    @Override
    public int read() throws IOException {
        final int b = super.read();
        if ( b >= 0 )
            count( 1 );
        return b;
    }

    @Override
    public int read( final byte[] buffer, final int offset, final int length ) throws IOException {
        final int n = super.read( buffer, offset, length );
        if ( n > 0 )
            count( n );
        return n;
    }

    @Override
    public long skip( final long n ) throws IOException {
        final long skipped = super.skip( n );
        count( skipped );
        return skipped;
    }

    private void count( final long n ) throws IOException {
        count += n;
        if ( count > limit )
            throw new IOException( "download is larger than " + limit + " bytes" );
    }

}
