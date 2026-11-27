package hu.aleks.larvasecondslostextmod;

import hu.scelightapi.sc2.rep.repproc.IRepProcessor;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Dev-only regression dump (E19-S02): inject windows and red (missed-inject) windows per hatchery, for replays with a written
 * ground truth ({@code benchmark-data/queens-scenarios/ground-truth.txt}). The golden is {@code test/golden/epic19-scenario-red.txt}.
 *
 * <pre>
 * java ... hu.aleks.larvasecondslostextmod.LarvaRedWindowDumpCli [--scelight-app-dir &lt;path&gt;] &lt;replay&gt;...
 * </pre>
 */
public final class LarvaRedWindowDumpCli {

    private LarvaRedWindowDumpCli() {
    }

    /** CLI main entry point. */
    public static void main( final String[] args ) {
        try {
            String appDir = null;
            final List< Path > replays = new ArrayList<>();
            for ( int i = 0; i < args.length; i++ )
                if ( "--scelight-app-dir".equals( args[ i ] ) && i + 1 < args.length )
                    appDir = args[ ++i ];
                else
                    replays.add( Paths.get( args[ i ] ) );
            final PrintStream out = new PrintStream( new FileOutputStream( FileDescriptor.out ), true, "UTF-8" );
            System.setOut( System.err );
            new StandaloneScelightBootstrap().ensureInitialized( appDir == null ? null : Paths.get( appDir ) );
            final ReflectiveScelightRepParserEngine engine = new ReflectiveScelightRepParserEngine();
            final LarvaReplayAnalyzer analyzer = new LarvaReplayAnalyzer( new ReflectiveScelightFactory().createFactory() );
            out.println( "# Inject and red (missed-inject) windows per hatchery (E19-S02). Times as Scelight shows them (about 1 % behind the game clock)." );
            for ( final Path replay : replays ) {
                final IRepProcessor repProc = engine.parseAndWrapReplay( replay );
                if ( repProc == null )
                    throw new IllegalStateException( "Cannot parse " + replay );
                dump( out, replay, analyzer.analyze( repProc, true ) );
            }
            System.exit( 0 );
        } catch ( final Exception e ) {
            System.err.println( "ERROR: " + e );
            System.exit( 1 );
        }
    }

    private static void dump( final PrintStream out, final Path replay, final LarvaAnalysisReport report ) {
        out.println( "replay " + replay.getFileName() );
        final Map< String, List< String > > linesByPlayer = new TreeMap<>();
        final Map< String, Boolean > followedByPlayer = new TreeMap<>();
        for ( final HatcheryIdleInjectTimeline timeline : report.getIdleInjectTimelineList() ) {
            final List< String > lines = linesFor( linesByPlayer, timeline.getPlayerName() );
            lines.add( "  " + timeline.getHatcheryType() + " " + timeline.getHatcheryTagText() + " injects" + injects( report, timeline.getHatcheryTag() ) );
            final StringBuilder red = new StringBuilder( "  " + timeline.getHatcheryType() + " " + timeline.getHatcheryTagText() + " red" );
            for ( final HatcheryIdleInjectWindow window : timeline.getIdleWindowList() )
                red.append( ' ' ).append( window.getStartTimeLabel() ).append( '-' ).append( window.getEndTimeLabel() ).append( " [" )
                        .append( window.getQueenSummary() ).append( ']' );
            lines.add( red.toString() );
            for ( final String line : timeline.getDiagnosticLineList() )
                if ( line.endsWith( "queen orders followed." ) )
                    followedByPlayer.put( timeline.getPlayerName(), Boolean.TRUE );
        }
        for ( final Map.Entry< String, List< String > > entry : linesByPlayer.entrySet() ) {
            out.println( "player " + entry.getKey() + ": queen orders " + ( followedByPlayer.containsKey( entry.getKey() ) ? "followed" : "not followed" )
                    + ", missed injects " + missedInjects( report, entry.getKey() ) );
            for ( final String line : entry.getValue() )
                out.println( line );
        }
    }

    private static List< String > linesFor( final Map< String, List< String > > linesByPlayer, final String player ) {
        List< String > lines = linesByPlayer.get( player );
        if ( lines == null ) {
            lines = new ArrayList<>();
            linesByPlayer.put( player, lines );
        }
        return lines;
    }

    private static String injects( final LarvaAnalysisReport report, final int hatchTag ) {
        final StringBuilder b = new StringBuilder();
        for ( final HatcheryInjectTimeline timeline : report.getInjectTimelineList() )
            if ( timeline.getHatcheryTag() == hatchTag )
                for ( final HatcheryInjectWindow window : timeline.getInjectWindowList() )
                    b.append( ' ' ).append( window.getStartTimeLabel() ).append( '-' ).append( window.getEndTimeLabel() );
        return b.toString();
    }

    private static int missedInjects( final LarvaAnalysisReport report, final String player ) {
        final LarvaPlayerPhaseTable table = report.getPlayerPhaseTable( player );
        int missed = 0;
        if ( table != null )
            for ( final LarvaPhaseStats stats : table.getPhaseStatsMap().values() )
                missed += stats.getMissedInjectLarvaCount() / 3;
        return missed;
    }
}
