package hu.aleks.larvasecondslostextmod;

import hu.scelightapi.sc2.rep.repproc.IRepProcessor;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

/**
 * Dev-only benchmark helper (E19-S05): per-phase larva rates of one player per replay, many replays in one JVM. Feeds the band
 * regeneration ({@code ant BENCHMARK_BANDS}, {@code benchmark-data/tools/benchmark_bands.py}).
 *
 * <pre>
 * java ... hu.aleks.larvasecondslostextmod.LarvaPhaseRateDumpCli --list &lt;tsv&gt; [--scelight-app-dir &lt;path&gt;]
 * </pre>
 *
 * <p>Input lines: {@code replay path<TAB>player name<TAB>cohort<TAB>matchup}, {@code ${NAME}} expanded as in
 * {@link LarvaAnalysisCheckCli}. Output CSV on stdout: one line per replay and phase.</p>
 */
public final class LarvaPhaseRateDumpCli {

    private LarvaPhaseRateDumpCli() {
    }

    /** CLI main entry point. */
    public static void main( final String[] args ) {
        try {
            final Path list = Paths.get( value( args, "--list" ) ).toAbsolutePath();
            final String appDir = optional( args, "--scelight-app-dir" );
            final PrintStream csv = new PrintStream( new FileOutputStream( FileDescriptor.out ), true, "UTF-8" );
            System.setOut( System.err );
            new StandaloneScelightBootstrap().ensureInitialized( appDir == null ? null : Paths.get( appDir ) );
            final ReflectiveScelightRepParserEngine engine = new ReflectiveScelightRepParserEngine();
            final LarvaReplayAnalyzer analyzer = new LarvaReplayAnalyzer( new ReflectiveScelightFactory().createFactory() );
            csv.println( "file,cohort,matchup,player,phase,startLoop,endLoop,hatchEligibleLoops,spawnedLarvaPerHatchPerMinute,larvaMissedPerHatchPerMinute" );
            for ( final String line : Files.readAllLines( list, StandardCharsets.UTF_8 ) ) {
                final String[] parts = line.split( "\t" );
                if ( line.trim().length() == 0 || line.startsWith( "#" ) || parts.length < 4 )
                    continue;
                dumpOne( Paths.get( LarvaAnalysisCheckCli.expand( parts[ 0 ], System.getenv() ) ), parts[ 1 ], parts[ 2 ], parts[ 3 ], engine, analyzer,
                        csv );
            }
            System.exit( 0 );
        } catch ( final Exception e ) {
            System.err.println( "ERROR: " + e );
            System.exit( 1 );
        }
    }

    private static void dumpOne( final Path replay, final String player, final String cohort, final String matchup,
            final ReflectiveScelightRepParserEngine engine, final LarvaReplayAnalyzer analyzer, final PrintStream csv ) {
        try {
            final IRepProcessor repProc = engine.parseAndWrapReplay( replay );
            final LarvaPlayerPhaseTable table = repProc == null ? null : analyzer.analyze( repProc, true ).getPlayerPhaseTable( player );
            if ( table == null ) {
                csv.println( "# no phase table for " + player + " in " + replay.getFileName() );
                return;
            }
            for ( final LarvaPhaseStats stats : table.getPhaseStatsMap().values() )
                csv.println( replay.getFileName() + "," + cohort + "," + matchup + "," + player.replace( ",", " " ) + "," + stats.getPhase() + ","
                        + stats.getInterval().getStartLoop() + "," + stats.getInterval().getEndLoop() + "," + stats.getHatchEligibleLoops() + ","
                        + rate( stats.getSpawnedLarvaPerHatchPerMinute() ) + "," + rate( stats.getMissedLarvaPerHatchPerMinute() ) );
        } catch ( final RuntimeException e ) {
            csv.println( "# ERROR " + replay.getFileName() + ": " + e );
        }
    }

    private static String rate( final Double value ) {
        return value == null ? "" : String.format( Locale.ROOT, "%.6f", value );
    }

    private static String value( final String[] args, final String flag ) {
        final String value = optional( args, flag );
        if ( value == null )
            throw new IllegalArgumentException( flag + " is required." );
        return value;
    }

    private static String optional( final String[] args, final String flag ) {
        for ( int i = 0; i + 1 < args.length; i++ )
            if ( flag.equals( args[ i ] ) )
                return args[ i + 1 ];
        return null;
    }
}
