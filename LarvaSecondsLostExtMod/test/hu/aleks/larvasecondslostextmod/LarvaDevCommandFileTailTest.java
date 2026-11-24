package hu.aleks.larvasecondslostextmod;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Collections;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class LarvaDevCommandFileTailTest {

    private static final Charset UTF8 = Charset.forName( "UTF-8" );

    private Path file;
    private LarvaDevCommandFileTail tail;

    @Before
    public void setUp() throws IOException {
        file = Files.createTempFile( "larva-dev-tail", ".txt" );
        tail = new LarvaDevCommandFileTail( file );
    }

    @After
    public void tearDown() throws IOException {
        Files.deleteIfExists( file );
    }

    private void append( final String text ) throws IOException {
        Files.write( file, text.getBytes( UTF8 ), StandardOpenOption.APPEND );
    }

    private void overwrite( final String text ) throws IOException {
        Files.write( file, text.getBytes( UTF8 ) );
    }

    @Test
    public void skipExistingContentIgnoresOldLines() throws IOException {
        append( "old1\nold2\n" );
        tail.skipExistingContent();
        assertTrue( tail.readNewLines().isEmpty() );
        append( "new1\n" );
        assertEquals( Collections.singletonList( "new1" ), tail.readNewLines() );
    }

    @Test
    public void withoutSkipExistingContentIsReadFromStart() throws IOException {
        append( "a\nb\n" );
        assertEquals( Arrays.asList( "a", "b" ), tail.readNewLines() );
    }

    @Test
    public void appendedLinesAreReadOnlyOnce() throws IOException {
        append( "a\n" );
        assertEquals( Collections.singletonList( "a" ), tail.readNewLines() );
        assertTrue( tail.readNewLines().isEmpty() );
        append( "b\nc\n" );
        assertEquals( Arrays.asList( "b", "c" ), tail.readNewLines() );
        assertTrue( tail.readNewLines().isEmpty() );
    }

    @Test
    public void partialLastLineIsHeldBackUntilNewline() throws IOException {
        append( "full\npart" );
        assertEquals( Collections.singletonList( "full" ), tail.readNewLines() );
        assertEquals( 5L, tail.getOffset() );
        assertTrue( tail.readNewLines().isEmpty() );
        append( "ial\n" );
        assertEquals( Collections.singletonList( "partial" ), tail.readNewLines() );
    }

    @Test
    public void crlfTerminatorsAreStripped() throws IOException {
        append( "a\r\nb\r\n\r\n" );
        assertEquals( Arrays.asList( "a", "b", "" ), tail.readNewLines() );
    }

    @Test
    public void utf8ContentIsDecoded() throws IOException {
        append( "\u00e1rv\u00edzt\u0171r\u0151\n" );
        assertEquals( Collections.singletonList( "\u00e1rv\u00edzt\u0171r\u0151" ), tail.readNewLines() );
    }

    @Test
    public void truncatedFileRestartsAtOffsetZero() throws IOException {
        append( "first line\nsecond line\n" );
        assertEquals( 2, tail.readNewLines().size() );
        overwrite( "x\n" );
        assertEquals( Collections.singletonList( "x" ), tail.readNewLines() );
        assertEquals( 2L, tail.getOffset() );
    }

    @Test
    public void missingFileYieldsNoLinesAndRecoversWhenCreated() throws IOException {
        Files.delete( file );
        tail.skipExistingContent();
        assertEquals( 0L, tail.getOffset() );
        assertTrue( tail.readNewLines().isEmpty() );
        overwrite( "late\n" );
        assertEquals( Collections.singletonList( "late" ), tail.readNewLines() );
    }
}
