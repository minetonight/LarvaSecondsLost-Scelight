package hu.aleks.larvasecondslostextmod;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * One parsed line of the dev command file: {@code <id> <command> [args...]}.
 *
 * <p>Pure value class without any Scelight dependency so it can be unit tested on its own.
 * Arguments that contain spaces are wrapped in double quotes; inside quotes a backslash escapes
 * a double quote or a backslash.</p>
 */
final class LarvaDevCommandLine {

    /** Command id chosen by the caller, echoed in every log line. */
    private final String id;

    /** Command name, may be empty if the line had only an id. */
    private final String command;

    /** Command arguments (unquoted). */
    private final List< String > args;

    private LarvaDevCommandLine( final String id, final String command, final List< String > args ) {
        this.id = id;
        this.command = command;
        this.args = Collections.unmodifiableList( args );
    }

    String getId() {
        return id;
    }

    String getCommand() {
        return command;
    }

    List< String > getArgs() {
        return args;
    }

    /**
     * Parses a line.
     *
     * @param line raw line
     * @return parsed line, or <code>null</code> for blank lines and {@code #} comments
     * @throws IllegalArgumentException if a quote is not closed
     */
    static LarvaDevCommandLine parse( final String line ) {
        if ( line == null )
            return null;
        final String trimmed = line.trim();
        if ( trimmed.isEmpty() || trimmed.charAt( 0 ) == '#' )
            return null;

        final List< String > tokens = tokenize( trimmed );
        final String id = tokens.get( 0 );
        final String command = tokens.size() > 1 ? tokens.get( 1 ) : "";
        final List< String > args = tokens.size() > 2 ? new ArrayList< String >( tokens.subList( 2, tokens.size() ) ) : new ArrayList< String >();
        return new LarvaDevCommandLine( id, command, args );
    }

    private static List< String > tokenize( final String text ) {
        final List< String > tokens = new ArrayList< String >();
        final StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        boolean hasToken = false;

        for ( int i = 0; i < text.length(); i++ ) {
            final char c = text.charAt( i );
            if ( inQuotes ) {
                if ( c == '\\' && i + 1 < text.length() && ( text.charAt( i + 1 ) == '"' || text.charAt( i + 1 ) == '\\' ) )
                    current.append( text.charAt( ++i ) );
                else if ( c == '"' )
                    inQuotes = false;
                else
                    current.append( c );
            } else if ( c == '"' ) {
                inQuotes = true;
                hasToken = true;
            } else if ( Character.isWhitespace( c ) ) {
                if ( hasToken ) {
                    tokens.add( current.toString() );
                    current.setLength( 0 );
                    hasToken = false;
                }
            } else {
                current.append( c );
                hasToken = true;
            }
        }
        if ( inQuotes )
            throw new IllegalArgumentException( "unterminated double quote" );
        if ( hasToken )
            tokens.add( current.toString() );
        return tokens;
    }

}
