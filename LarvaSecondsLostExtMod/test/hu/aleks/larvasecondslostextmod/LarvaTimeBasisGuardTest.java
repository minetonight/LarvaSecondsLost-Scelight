package hu.aleks.larvasecondslostextmod;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.junit.Test;

/**
 * One time basis for the whole module: loops become seconds and minutes only in {@link LarvaGameClock}. Before 2026-11-26 the module
 * mixed three bases (Normal-speed seconds of 16 loops, Scelight's 22.15 loops per second and its real-time setting, and the game clock
 * of 22.4), so times ran 1.1 % or 40 % off and "per minute" rates were per 42.9 s. This test scans {@code src} and fails on any code
 * outside {@code LarvaGameClock} that converts loops itself or asks Scelight for time.
 */
public class LarvaTimeBasisGuardTest {

    private static final String[][] RULES = {
            { "\\w*[Pp]roc\\w*(\\(\\))?\\.(formatLoopTime|loopToTime|getLengthMs)\\s*\\(", "Scelight's time conversion (1.1 % off, follows its real-time setting)" },
            { "\\b(convertToRealTime|convertToGameTime|isRealTime|getConverterGameSpeed)\\s*\\(", "Scelight's game speed conversion" },
            { "[*/]\\s*(16|22\\.4)(\\.0)?[dDfFlL]?\\b(?!\\s*\\.)", "a loops-per-second factor" },
            { "\\b(16|22\\.4)(\\.0)?[dDfFlL]?\\s*[*/](?!/)", "a loops-per-second factor" },
            { "(?<![\\w.])(960|1344|672|5376)[lL]?\\b", "a loop count written as a number of seconds or minutes" },
            { "(?<![\\w.])(26|36)(\\.0)?[dDlL]\\b", "Scelight's relative game speed (26 Faster, 36 Normal)" } };

    private static final Pattern STRING_LITERAL = Pattern.compile( "\"(\\\\.|[^\"\\\\])*\"" );

    /** @return the reason if the code line breaks a rule, else null */
    static String violation( final String codeLine ) {
        final String code = STRING_LITERAL.matcher( codeLine ).replaceAll( "\"\"" ).split( "//" )[ 0 ];
        for ( final String[] rule : RULES )
            if ( Pattern.compile( rule[ 0 ] ).matcher( code ).find() )
                return rule[ 1 ];
        return null;
    }

    @Test
    public void onlyTheGameClockConvertsLoops() throws IOException {
        final List< String > violations = new ArrayList<>();
        Files.walkFileTree( Paths.get( "src" ), new SimpleFileVisitor< Path >() {
            @Override
            public FileVisitResult visitFile( final Path file, final BasicFileAttributes attributes ) throws IOException {
                if ( file.toString().endsWith( ".java" ) && !file.getFileName().toString().equals( "LarvaGameClock.java" ) )
                    scan( file, violations );
                return FileVisitResult.CONTINUE;
            }
        } );
        assertEquals( "Use LarvaGameClock for every loop/time conversion:\n" + join( violations ), 0, violations.size() );
    }

    /** The old conversions this test exists to keep out. */
    @Test
    public void theRulesCatchTheOldConversions() {
        assertNotNull( violation( "final String label = repProc.formatLoopTime( loop );" ) );
        assertNotNull( violation( "final long ms = context.repProc.getLengthMs();" ) );
        assertNotNull( violation( "final long relative = repProc.getConverterGameSpeed().getRelativeSpeed();" ) );
        assertNotNull( violation( "return count * 60.0d * 16.0d / loops;" ) );
        assertNotNull( violation( "return (int) ( seconds * 16 * ( 36.0d / relative ) );" ) );
        assertNotNull( violation( "static final double MAX = 4.5d / 22.4d;" ) );
        assertNotNull( violation( "static final int WINDOW = 10 * 16;" ) );
        assertNotNull( violation( "static final int MAX_AGE_LOOPS = 1344;" ) );
        assertNull( violation( "g.drawString( label, x, y + 16 );" ) );
        assertNull( violation( "private static final int COLUMN_GAP = 16;" ) );
        assertNull( violation( "final String label = report.formatLoopTime( loop );" ) );
        assertNull( violation( "final String text = \"16 * 60\"; // 960 loops" ) );
    }

    private static void scan( final Path file, final List< String > violations ) throws IOException {
        boolean inComment = false;
        int lineNumber = 0;
        for ( final String line : Files.readAllLines( file, StandardCharsets.UTF_8 ) ) {
            lineNumber++;
            final String trimmed = line.trim();
            if ( inComment ) {
                inComment = !trimmed.contains( "*/" );
                continue;
            }
            if ( trimmed.startsWith( "/*" ) ) {
                inComment = !trimmed.contains( "*/" );
                continue;
            }
            if ( trimmed.startsWith( "*" ) || trimmed.startsWith( "//" ) )
                continue;
            final String reason = violation( line );
            if ( reason != null )
                violations.add( file + ":" + lineNumber + " (" + reason + "): " + trimmed );
        }
    }

    private static String join( final List< String > lines ) {
        final StringBuilder b = new StringBuilder();
        for ( final String line : lines )
            b.append( line ).append( '\n' );
        return b.toString();
    }
}
