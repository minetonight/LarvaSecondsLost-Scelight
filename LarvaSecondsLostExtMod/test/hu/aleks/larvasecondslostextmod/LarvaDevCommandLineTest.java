package hu.aleks.larvasecondslostextmod;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Test;

public class LarvaDevCommandLineTest {

    @Test
    public void parsesIdCommandAndArgs() {
        final LarvaDevCommandLine l = LarvaDevCommandLine.parse( "c1 open-replay /a/b.SC2Replay extra" );
        assertEquals( "c1", l.getId() );
        assertEquals( "open-replay", l.getCommand() );
        assertEquals( Arrays.asList( "/a/b.SC2Replay", "extra" ), l.getArgs() );
    }

    @Test
    public void idOnlyLineHasEmptyCommandAndNoArgs() {
        final LarvaDevCommandLine l = LarvaDevCommandLine.parse( "c1" );
        assertEquals( "c1", l.getId() );
        assertEquals( "", l.getCommand() );
        assertTrue( l.getArgs().isEmpty() );
    }

    @Test
    public void collapsesRepeatedWhitespaceAndTrims() {
        final LarvaDevCommandLine l = LarvaDevCommandLine.parse( "  c1 \t cmd   a    b  " );
        assertEquals( "cmd", l.getCommand() );
        assertEquals( Arrays.asList( "a", "b" ), l.getArgs() );
    }

    @Test
    public void quotedArgumentKeepsSpaces() {
        final LarvaDevCommandLine l = LarvaDevCommandLine.parse( "c1 cmd \"a path/with space.txt\" x" );
        assertEquals( Arrays.asList( "a path/with space.txt", "x" ), l.getArgs() );
    }

    @Test
    public void escapedQuoteAndBackslashInsideQuotes() {
        final LarvaDevCommandLine l = LarvaDevCommandLine.parse( "c1 cmd \"say \\\"hi\\\" C:\\\\dir\"" );
        assertEquals( Arrays.asList( "say \"hi\" C:\\dir" ), l.getArgs() );
    }

    @Test
    public void otherBackslashesStayLiteral() {
        final LarvaDevCommandLine l = LarvaDevCommandLine.parse( "c1 cmd \"a\\nb\"" );
        assertEquals( Arrays.asList( "a\\nb" ), l.getArgs() );
    }

    @Test
    public void emptyQuotedArgumentIsKept() {
        final LarvaDevCommandLine l = LarvaDevCommandLine.parse( "c1 cmd \"\" z" );
        assertEquals( Arrays.asList( "", "z" ), l.getArgs() );
    }

    @Test
    public void blankAndCommentLinesReturnNull() {
        assertNull( LarvaDevCommandLine.parse( null ) );
        assertNull( LarvaDevCommandLine.parse( "" ) );
        assertNull( LarvaDevCommandLine.parse( "   \t " ) );
        assertNull( LarvaDevCommandLine.parse( "# comment" ) );
        assertNull( LarvaDevCommandLine.parse( "   # indented comment" ) );
    }

    @Test( expected = IllegalArgumentException.class )
    public void unterminatedQuoteThrows() {
        LarvaDevCommandLine.parse( "c1 cmd \"never closed" );
    }

    @Test( expected = IllegalArgumentException.class )
    public void quoteEndingWithEscapedQuoteIsUnterminated() {
        LarvaDevCommandLine.parse( "c1 cmd \"abc\\\"" );
    }
}
