package hu.aleks.larvasecondslostextmod;

import hu.scelightapi.sc2.rep.model.IEvent;
import hu.scelightapi.sc2.rep.model.IReplay;
import hu.scelightapi.sc2.rep.model.trackerevents.IPlayerStatsEvent;
import hu.scelightapi.sc2.rep.model.trackerevents.ITrackerEvents;
import hu.scelightapi.sc2.rep.repproc.IRepProcessor;
import hu.scelightapi.sc2.rep.repproc.IUser;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Dev-only corpus regression check (E19-S00). Analyzes every replay of a list file with the module and prints one CSV line per
 * Zerg player to stdout, and a summary to stderr:
 * <ul>
 * <li>drones: the module's worker count, phase starts and drone milestones against Scelight's {@code workersActiveCount}, the value
 * behind Scelight's Workers Active chart;</li>
 * <li>missed injects: red seconds, red seconds that overlap a queen tumor, and the energy upper bound (total queen energy minus
 * injects and queen tumors, in 25-energy units) against the module's missed injects.</li>
 * </ul>
 *
 * <pre>
 * java ... hu.aleks.larvasecondslostextmod.LarvaAnalysisCheckCli --list &lt;file&gt; [--scelight-app-dir &lt;path&gt;]
 * </pre>
 *
 * <p>List file: one replay path per line, {@code #} starts a comment, and {@code ${NAME}} is replaced by the environment variable
 * {@code NAME} (for example {@code ${REPO}}), so committed lists hold no machine paths. Never run it while Scelight runs
 * (single-instance socket).</p>
 */
public final class LarvaAnalysisCheckCli {

    /** Queen energy regeneration per game loop ({@link hu.aleks.larvasecondslostextmod.queens.QueenEnergy#PER_LOOP}). */
    static final double QUEEN_ENERGY_PER_LOOP = hu.aleks.larvasecondslostextmod.queens.QueenEnergy.PER_LOOP;

    /** A tumor at least this far from the hatchery means its queen was away from the hatchery. */
    static final double FAR_TUMOR_CELLS = 12.0d;

    /** Tumors placed up to this many loops before a red window also count as overlapping (30 s on the game clock). */
    static final int TUMOR_LOOKBACK_LOOPS = LarvaGameClock.secondsToLoops( 30 );

    private static final String CSV_HEADER = "file,matchup,player,lengthSec,waMax,modMax,maxAbsDiff,diffSamples,samples,"
            + "midWa,lateWa,endWa,midMod,lateMod,endMod,m66Wa,m80Wa,m66Mod,m80Mod,"
            + "queens,tumors,injectWindows,redWindows,redSec,redSecTumor,redSecFarTumor,energyBound,missedInjects,ordersFollowed";

    private static final Pattern PLACEHOLDER = Pattern.compile( "\\$\\{([A-Za-z0-9_]+)\\}" );

    private LarvaAnalysisCheckCli() {
    }

    /** CLI main entry point. */
    public static void main( final String[] args ) {
        try {
            final Path listFile = requirePath( args, "--list" );
            final Path appDir = optionalPath( args, "--scelight-app-dir" );
            final List< Path > replays = readList( listFile );
            final PrintStream csv = new PrintStream( new FileOutputStream( FileDescriptor.out ), true, "UTF-8" );
            System.setOut( System.err ); // keep Scelight's bootstrap chatter out of the CSV
            new StandaloneScelightBootstrap().ensureInitialized( appDir );
            final ReflectiveScelightRepParserEngine engine = new ReflectiveScelightRepParserEngine();
            final LarvaReplayAnalyzer analyzer = new LarvaReplayAnalyzer( new ReflectiveScelightFactory().createFactory() );
            final LarvaAnalysisCheckSummary summary = new LarvaAnalysisCheckSummary();
            csv.println( CSV_HEADER );
            for ( final Path replay : replays )
                checkOne( replay, engine, analyzer, csv, summary );
            System.err.println( summary.format() );
            System.exit( 0 );
        } catch ( final IllegalArgumentException e ) {
            System.err.println( "ERROR: " + e.getMessage() );
            System.err.println( "Usage: java ... " + LarvaAnalysisCheckCli.class.getName() + " --list <file> [--scelight-app-dir <path>]" );
            System.exit( 2 );
        } catch ( final Exception e ) {
            System.err.println( "ERROR: check failed: " + e );
            e.printStackTrace( System.err );
            System.exit( 1 );
        }
    }

    private static void checkOne( final Path replay, final ReflectiveScelightRepParserEngine engine, final LarvaReplayAnalyzer analyzer,
            final PrintStream csv, final LarvaAnalysisCheckSummary summary ) {
        try {
            final IRepProcessor repProc = engine.parseAndWrapReplay( replay );
            if ( repProc == null )
                throw new IllegalStateException( "could not parse" );
            final LarvaAnalysisReport report = analyzer.analyze( repProc, true );
            for ( final LarvaAnalysisCheckRow row : buildRows( replay, repProc, report ) ) {
                csv.println( row.toCsv() );
                summary.add( row );
            }
        } catch ( final RuntimeException e ) {
            csv.println( "# ERROR " + replay.getFileName() + ": " + e );
            summary.addError();
        }
    }

    /** One row per Zerg player of the replay. */
    static List< LarvaAnalysisCheckRow > buildRows( final Path replay, final IRepProcessor repProc, final LarvaAnalysisReport report ) {
        final IReplay rep = repProc.getReplay();
        final IUser[] usersByPlayerId = repProc.getUsersByPlayerId();
        final Facts facts = collectFacts( rep.getTrackerEvents() == null ? new IEvent[ 0 ] : rep.getTrackerEvents().getEvents() );
        final LarvaAggregationGameRecord record = LarvaAggregationRecordBuilder.buildFull( replay, rep.getDetails(),
                rep.getAttributesEvents() == null ? null : rep.getAttributesEvents().getGameMode(), repProc.getFormat(), report );
        final List< LarvaAnalysisCheckRow > rows = new ArrayList<>();
        for ( int playerId = 0; usersByPlayerId != null && playerId < usersByPlayerId.length; playerId++ ) {
            final IUser user = usersByPlayerId[ playerId ];
            if ( user == null || user.getPlayer() == null || user.getPlayer().getRace() == null
                    || !"ZERG".equals( user.getPlayer().getRace().name() ) )
                continue;
            final LarvaAnalysisCheckRow row = new LarvaAnalysisCheckRow( replay.getFileName().toString(), matchupOf( usersByPlayerId, playerId ),
                    user.getName(), report );
            row.fillDrones( facts.workerSamples( playerId ), report.getResourceSnapshotsByPlayerName().get( user.getName() ),
                    record == null ? null : findPlayer( record, user.getName() ) );
            row.fillQueens( facts, playerId );
            rows.add( row );
        }
        return rows;
    }

    private static LarvaAggregationPlayerRecord findPlayer( final LarvaAggregationGameRecord record, final String name ) {
        for ( final LarvaAggregationPlayerRecord player : record.getZergPlayers() )
            if ( name.equals( player.getName() ) )
                return player;
        return null;
    }

    private static String matchupOf( final IUser[] usersByPlayerId, final int playerId ) {
        final StringBuilder opponents = new StringBuilder();
        int players = 0;
        for ( int i = 0; i < usersByPlayerId.length; i++ ) {
            final IUser user = usersByPlayerId[ i ];
            if ( user == null || user.getPlayer() == null || user.getPlayer().getRace() == null )
                continue;
            players++;
            if ( i != playerId )
                opponents.append( user.getPlayer().getRace().name().charAt( 0 ) );
        }
        return players == 2 ? "Zv" + opponents : "other";
    }

    /** Raw tracker facts the check compares the module with. */
    static final class Facts {

        /** Player id to (loop, workersActiveCount) samples in event order. */
        final Map< Integer, List< int[] > > workersByPlayerId = new HashMap<>();

        /** Player id to queen lifetimes (born loop, died loop or -1). */
        final Map< Integer, List< int[] > > queensByPlayerId = new HashMap<>();

        /** Player id to queen tumors (loop, x, y). */
        final Map< Integer, List< int[] > > tumorsByPlayerId = new HashMap<>();

        /** Hatchery tag to (x, y). */
        final Map< Integer, int[] > hatcheryXyByTag = new HashMap<>();

        /** Last loop of the replay's tracker events. */
        int lastLoop;

        List< int[] > workerSamples( final int playerId ) {
            final List< int[] > samples = workersByPlayerId.get( Integer.valueOf( playerId ) );
            return samples == null ? new ArrayList< int[] >() : samples;
        }

        List< int[] > of( final Map< Integer, List< int[] > > map, final int playerId ) {
            final List< int[] > list = map.get( Integer.valueOf( playerId ) );
            return list == null ? new ArrayList< int[] >() : list;
        }
    }

    private static Facts collectFacts( final IEvent[] trackerEvents ) {
        final Facts facts = new Facts();
        final Map< Integer, int[] > queenByTag = new HashMap<>();
        for ( final IEvent event : trackerEvents ) {
            if ( event == null )
                continue;
            facts.lastLoop = Math.max( facts.lastLoop, event.getLoop() );
            final int id = event.getId();
            if ( id == ITrackerEvents.ID_PLAYER_STATS ) {
                final IPlayerStatsEvent stats = (IPlayerStatsEvent) event;
                final Integer workers = stats.getWorkersActiveCount();
                listOf( facts.workersByPlayerId, stats.getPlayerId() ).add( new int[] { event.getLoop(), workers == null ? 0 : workers.intValue() } );
            } else if ( id == ITrackerEvents.ID_UNIT_BORN || id == ITrackerEvents.ID_UNIT_INIT )
                collectUnitStart( facts, queenByTag, event );
            else if ( id == ITrackerEvents.ID_UNIT_DIED ) {
                final int[] queen = queenByTag.remove( tagOf( event ) );
                if ( queen != null )
                    queen[ 1 ] = event.getLoop();
            }
        }
        return facts;
    }

    private static void collectUnitStart( final Facts facts, final Map< Integer, int[] > queenByTag, final IEvent event ) {
        final Object typeValue = event.get( "unitTypeName" );
        final String type = typeValue == null ? "" : typeValue.toString();
        final Integer playerId = (Integer) event.get( "controlPlayerId" );
        final Integer x = (Integer) event.get( "x" );
        final Integer y = (Integer) event.get( "y" );
        if ( playerId == null || x == null || y == null )
            return;
        if ( "Queen".equals( type ) && event.getId() == ITrackerEvents.ID_UNIT_BORN ) {
            final int[] queen = new int[] { event.getLoop(), -1 };
            queenByTag.put( tagOf( event ), queen );
            listOf( facts.queensByPlayerId, playerId ).add( queen );
        } else if ( "CreepTumorQueen".equals( type ) )
            listOf( facts.tumorsByPlayerId, playerId ).add( new int[] { event.getLoop(), x.intValue(), y.intValue() } );
        else if ( "Hatchery".equals( type ) )
            facts.hatcheryXyByTag.put( tagOf( event ), new int[] { x.intValue(), y.intValue() } );
    }

    private static Integer tagOf( final IEvent event ) {
        final Object index = event.get( "unitTagIndex" );
        final Object recycle = event.get( "unitTagRecycle" );
        return index instanceof Integer && recycle instanceof Integer ? Integer.valueOf( (Integer) index << 18 | (Integer) recycle ) : null;
    }

    private static List< int[] > listOf( final Map< Integer, List< int[] > > map, final Integer key ) {
        List< int[] > list = map.get( key );
        if ( list == null ) {
            list = new ArrayList<>();
            map.put( key, list );
        }
        return list;
    }

    /** Reads a list file: comments and blank lines skipped, {@code ${NAME}} expanded from the environment. */
    static List< Path > readList( final Path listFile ) throws IOException {
        final List< Path > replays = new ArrayList<>();
        for ( final String rawLine : Files.readAllLines( listFile, StandardCharsets.UTF_8 ) ) {
            final String line = rawLine.trim();
            if ( line.length() == 0 || line.startsWith( "#" ) )
                continue;
            replays.add( Paths.get( expand( line, System.getenv() ) ).toAbsolutePath() );
        }
        return replays;
    }

    /** Replaces each {@code ${NAME}} by its value; fails on an unknown name so a list never silently points nowhere. */
    static String expand( final String line, final Map< String, String > environment ) {
        final Matcher matcher = PLACEHOLDER.matcher( line );
        final StringBuffer expanded = new StringBuffer();
        while ( matcher.find() ) {
            final String value = environment.get( matcher.group( 1 ) );
            if ( value == null )
                throw new IllegalArgumentException( "Environment variable " + matcher.group( 1 ) + " is not set (used in: " + line + ")" );
            matcher.appendReplacement( expanded, Matcher.quoteReplacement( value ) );
        }
        matcher.appendTail( expanded );
        return expanded.toString();
    }

    private static Path requirePath( final String[] args, final String flag ) {
        final Path path = optionalPath( args, flag );
        if ( path == null )
            throw new IllegalArgumentException( flag + " is required." );
        return path.toAbsolutePath(); // the bootstrap changes user.dir later
    }

    private static Path optionalPath( final String[] args, final String flag ) {
        for ( int i = 0; i < args.length; i++ ) {
            if ( !flag.equals( args[ i ] ) )
                continue;
            if ( i + 1 >= args.length )
                throw new IllegalArgumentException( flag + " requires a value." );
            return Paths.get( args[ i + 1 ] );
        }
        return null;
    }

    /** Formats a double with a dot, for CSV. */
    static String fmt( final double value ) {
        return String.format( Locale.ROOT, "%.1f", Double.valueOf( value ) );
    }
}
