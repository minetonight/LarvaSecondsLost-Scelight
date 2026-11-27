package hu.aleks.larvasecondslostextmod;

import hu.scelightapi.sc2.rep.model.details.IDetails;
import hu.scelightapi.sc2.rep.model.details.IPlayer;
import hu.scelightapi.sc2.rep.repproc.IRepProcessor;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

/**
 * Dev-only benchmark helper (E20-S02): the milestone loops of one player per list line, many replays in one JVM. Feeds the milestone
 * band regeneration ({@code ant MILESTONE_BANDS}, {@code benchmark-data/tools/milestone_bands.py}).
 *
 * <pre>
 * java ... hu.aleks.larvasecondslostextmod.LarvaMilestoneDumpCli --list &lt;tsv&gt; [--scelight-app-dir &lt;path&gt;]
 * </pre>
 *
 * <p>Input lines: {@code replay path<TAB>player name<TAB>cohort<TAB>matchup[<TAB>role]}, {@code ${NAME}} expanded as in
 * {@link LarvaAnalysisCheckCli}; role is {@code primary} when absent ({@code opponent} for the second Zerg of a ZvZ). Output CSV on
 * stdout, one line per list line: {@code file,cohort,matchup,role,player,result,replayLoops,lastLiveLoop} and one column per
 * {@link LarvaMilestone} (its name), holding the loop of {@link LarvaMilestoneCalculator} or empty when not reached. A replay that
 * fails or has no stats for the player gives a {@code #} line.</p>
 */
public final class LarvaMilestoneDumpCli {

    /** Role of a list line without a fifth column. */
    static final String PRIMARY = "primary";

    private LarvaMilestoneDumpCli() {
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
            csv.println( header() );
            for ( final String line : Files.readAllLines( list, StandardCharsets.UTF_8 ) ) {
                final String[] parts = line.split( "\t" );
                if ( line.trim().length() == 0 || line.startsWith( "#" ) || parts.length < 4 )
                    continue;
                final String role = parts.length > 4 && parts[ 4 ].trim().length() > 0 ? parts[ 4 ].trim() : PRIMARY;
                dumpOne( Paths.get( LarvaAnalysisCheckCli.expand( parts[ 0 ], System.getenv() ) ), parts[ 1 ], parts[ 2 ], parts[ 3 ], role, engine,
                        analyzer, csv );
            }
            System.exit( 0 );
        } catch ( final Exception e ) {
            System.err.println( "ERROR: " + e );
            System.exit( 1 );
        }
    }

    /** @return the CSV header line */
    static String header() {
        final StringBuilder header = new StringBuilder( "file,cohort,matchup,role,player,result,replayLoops,lastLiveLoop" );
        for ( final LarvaMilestone milestone : LarvaMilestone.values() )
            header.append( ',' ).append( milestone.name() );
        return header.toString();
    }

    /**
     * @return one CSV line; player names lose their commas
     */
    static String row( final String file, final String cohort, final String matchup, final String role, final String player, final String result,
            final int replayLoops, final List< LarvaPlayerResourceSnapshot > snapshots ) {
        final StringBuilder row = new StringBuilder();
        row.append( file ).append( ',' ).append( cohort ).append( ',' ).append( matchup ).append( ',' ).append( role ).append( ',' )
                .append( player.replace( ",", " " ) ).append( ',' ).append( result ).append( ',' ).append( replayLoops ).append( ',' )
                .append( lastLiveLoop( snapshots ) );
        final Map< LarvaMilestone, Integer > loops = LarvaMilestoneCalculator.compute( snapshots );
        for ( final LarvaMilestone milestone : LarvaMilestone.values() ) {
            final Integer loop = loops.get( milestone );
            row.append( ',' ).append( loop == null ? "" : loop.toString() );
        }
        return row.toString();
    }

    private static void dumpOne( final Path replay, final String player, final String cohort, final String matchup, final String role,
            final ReflectiveScelightRepParserEngine engine, final LarvaReplayAnalyzer analyzer, final PrintStream csv ) {
        try {
            final IRepProcessor repProc = engine.parseAndWrapReplay( replay );
            if ( repProc == null ) {
                csv.println( "# no parse " + replay.getFileName() );
                return;
            }
            final LarvaAnalysisReport report = analyzer.analyze( repProc, true );
            final List< LarvaPlayerResourceSnapshot > snapshots = report.getResourceSnapshotsByPlayerName().get( player );
            if ( snapshots == null || snapshots.isEmpty() ) {
                csv.println( "# no snapshots for " + player + " in " + replay.getFileName() );
                return;
            }
            csv.println( row( String.valueOf( replay.getFileName() ), cohort, matchup, role, player, result( repProc, player ), report.getReplayLengthLoops(),
                    snapshots ) );
        } catch ( final RuntimeException e ) {
            csv.println( "# ERROR " + replay.getFileName() + ": " + e );
        }
    }

    /** @return loop of the last sample with workers or supply (the player was still in the game), or -1 */
    private static int lastLiveLoop( final List< LarvaPlayerResourceSnapshot > snapshots ) {
        int last = -1;
        for ( final LarvaPlayerResourceSnapshot snapshot : snapshots ) {
            final boolean workers = snapshot.getWorkersActiveCount() != null && snapshot.getWorkersActiveCount().intValue() > 0;
            final boolean supply = snapshot.getFoodUsed() != null && snapshot.getFoodUsed().intValue() > 0;
            if ( workers || supply )
                last = Math.max( last, snapshot.getLoop() );
        }
        return last;
    }

    private static String result( final IRepProcessor repProc, final String player ) {
        final IDetails details = repProc.getReplay() == null ? null : repProc.getReplay().getDetails();
        if ( details == null || details.getPlayerList() == null )
            return "";
        for ( final IPlayer p : details.getPlayerList() )
            if ( p != null && player.equals( p.getName() ) )
                return p.getResult() == null ? "" : p.getResult().toString();
        return "";
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
