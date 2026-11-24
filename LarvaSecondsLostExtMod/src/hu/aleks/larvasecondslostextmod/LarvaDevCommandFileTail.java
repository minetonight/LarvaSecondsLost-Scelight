package hu.aleks.larvasecondslostextmod;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads only the lines appended to a file since the last call, remembering a byte offset.
 *
 * <p>Pure I/O helper without Scelight dependencies. Lines that exist when {@link #skipExistingContent()}
 * is called are never returned. A file that shrinks (truncated or replaced) restarts at offset 0.
 * A trailing line without newline is held back until its newline arrives, so half-written
 * commands are never executed.</p>
 */
final class LarvaDevCommandFileTail {

    private static final Charset UTF8 = Charset.forName( "UTF-8" );

    private final Path file;

    /** Byte offset of the first unread byte. */
    private long offset;

    LarvaDevCommandFileTail( final Path file ) {
        this.file = file;
    }

    long getOffset() {
        return offset;
    }

    /** Marks everything currently in the file as already processed (offset 0 if the file does not exist). */
    void skipExistingContent() throws IOException {
        offset = Files.exists( file ) ? Files.size( file ) : 0L;
    }

    /**
     * Returns the complete lines appended since the last call.
     *
     * @return new lines without line terminators; empty if there are none or the file is missing
     * @throws IOException if reading fails
     */
    List< String > readNewLines() throws IOException {
        final List< String > lines = new ArrayList< String >();
        if ( !Files.exists( file ) )
            return lines;

        final long size = Files.size( file );
        if ( size < offset )
            offset = 0L;
        if ( size == offset )
            return lines;

        final byte[] data = new byte[ (int) ( size - offset ) ];
        final RandomAccessFile raf = new RandomAccessFile( file.toFile(), "r" );
        try {
            raf.seek( offset );
            raf.readFully( data );
        } finally {
            raf.close();
        }

        int lineStart = 0;
        for ( int i = 0; i < data.length; i++ ) {
            if ( data[ i ] == '\n' ) {
                int lineEnd = i;
                if ( lineEnd > lineStart && data[ lineEnd - 1 ] == '\r' )
                    lineEnd--;
                lines.add( new String( data, lineStart, lineEnd - lineStart, UTF8 ) );
                lineStart = i + 1;
            }
        }
        offset += lineStart;
        return lines;
    }

}
