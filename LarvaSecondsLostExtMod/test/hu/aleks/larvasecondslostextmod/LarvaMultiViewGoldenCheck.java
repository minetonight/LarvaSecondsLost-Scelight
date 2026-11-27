package hu.aleks.larvasecondslostextmod;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Golden check of the multi page's view model (E17-S15, criterion 8): every cell string of the view-model text dump must
 * equal the formatted value in the S12 {@code --multi} golden dump. Not a JUnit class (no {@code Test} suffix): the Ant target
 * {@code TEST_GOLDEN_MULTI} runs it with {@code main(goldenDump, viewModelDump)}, and
 * {@code LarvaMultiViewGoldenCheckTest} covers the comparison logic on synthetic data.
 *
 * <p>Mapping, per player and bucket: the golden phase line gives the seven rows (phase end with {@code (k/n)}, uptime, spawned,
 * spawned ranking, missed, missed ranking, missed inject), the golden milestone lines give the milestone table cells: per
 * milestone the time row ({@code m:ss (k/n)}) and the ranking row (E20-S06).</p>
 */
public final class LarvaMultiViewGoldenCheck {

    private static final Pattern PHASE_LINE = Pattern.compile( "^\\s+phase \\w+: .*\\| missed/h/min=(\\S+) missedInject/h/min=(\\S+) "
            + "spawned/h/min=(\\S+) uptime=(\\S+) spawnedRank=(.*?) missedRank=(.*?) end=(\\S+) \\(.*?\\) (\\d+)/(\\d+)$" );
    private static final Pattern MILESTONE_LINE = Pattern.compile( "^\\s+milestone .+?: (\\S+)(?: \\(\\d+ loops\\))? \\((\\d+)/(\\d+)\\) rank=(.*)$" );
    private static final Pattern PLAYER_LINE = Pattern.compile( "^player \\d+: name=.*" );
    private static final Pattern BUCKET_LINE = Pattern.compile( "^\\s+bucket (\\w+) games=(\\d+)$" );
    private static final int BUCKETS = 4;
    private static final int PHASES = 4;
    private static final int PHASE_ROWS = 7;
    private static final int MILESTONES = LarvaMilestone.values().length;

    private LarvaMultiViewGoldenCheck() {
    }

    /** Usage: {@code <golden dump> <view-model dump>}; exits 1 and lists the differences if any cell differs. */
    public static void main( final String[] args ) throws IOException {
        if ( args.length != 2 ) {
            System.err.println( "usage: LarvaMultiViewGoldenCheck <golden dump> <view-model dump>" );
            System.exit( 2 );
        }
        final Charset utf8 = Charset.forName( "UTF-8" );
        final List< String > problems = compare( new String( Files.readAllBytes( Paths.get( args[ 0 ] ) ), utf8 ),
                new String( Files.readAllBytes( Paths.get( args[ 1 ] ) ), utf8 ) );
        for ( final String problem : problems )
            System.err.println( problem );
        if ( !problems.isEmpty() ) {
            System.err.println( problems.size() + " view-model cell(s) differ from the golden dump." );
            System.exit( 1 );
        }
        System.out.println( "View-model cells match the golden dump." );
    }

    /**
     * @param goldenDump text of {@link LarvaMultiAnalysisDumpFormatter}
     * @param viewModelDump text of {@link LarvaMultiAnalysisViewModel#toText()}
     * @return one line per difference; empty if every cell matches
     */
    public static List< String > compare( final String goldenDump, final String viewModelDump ) {
        final List< String > problems = new ArrayList<>();
        final List< String[][][] > expected = parseGolden( goldenDump, problems );
        final List< List< List< String > > > actual = parseView( viewModelDump );
        if ( expected.size() != actual.size() )
            problems.add( "player count: golden " + expected.size() + ", view model " + actual.size() );
        for ( int p = 0; p < Math.min( expected.size(), actual.size() ); p++ ) {
            final String[][][] player = expected.get( p );
            final List< List< String > > tables = actual.get( p );
            if ( tables.size() != BUCKETS + 1 ) {
                problems.add( "player " + ( p + 1 ) + ": " + tables.size() + " tables, expected " + ( BUCKETS + 1 ) );
                continue;
            }
            // CR-1.3: the milestone table is the first table, then All / ZvP / ZvT / ZvZ
            compareMilestoneTable( problems, p, player, tables.get( 0 ) );
            for ( int b = 0; b < BUCKETS; b++ )
                comparePhaseTable( problems, p, b, player[ b ], tables.get( b + 1 ) );
        }
        return problems;
    }

    private static void comparePhaseTable( final List< String > problems, final int p, final int b, final String[][] golden,
            final List< String > rows ) {
        if ( rows.size() != PHASE_ROWS )
            problems.add( "player " + ( p + 1 ) + " table " + b + ": " + rows.size() + " rows" );
        for ( int row = 0; row < Math.min( PHASE_ROWS, rows.size() ); row++ ) {
            final String[] cells = cells( rows.get( row ) );
            for ( int phase = 0; phase < PHASES; phase++ ) {
                final String expectedCell = phase < golden.length ? goldenPhaseCell( golden[ phase ], row ) : null;
                final String actualCell = phase < cells.length ? cells[ phase ] : null;
                if ( expectedCell == null || !expectedCell.equals( actualCell ) )
                    problems.add( "player " + ( p + 1 ) + " bucket " + b + " row " + row + " phase " + phase + ": golden '" + expectedCell
                            + "', view model '" + actualCell + "'" );
            }
        }
    }

    private static String goldenPhaseCell( final String[] g, final int row ) {
        // g = missed, missedInject, spawned, uptime, spawnedRank, missedRank, end, k, n
        switch ( row ) {
            case 0 :
                return g[ 6 ] + " (" + g[ 7 ] + "/" + g[ 8 ] + ")";
            case 1 :
                return g[ 3 ];
            case 2 :
                return g[ 2 ];
            case 3 :
                return g[ 4 ];
            case 4 :
                return g[ 0 ];
            case 5 :
                return g[ 5 ];
            default :
                return g[ 1 ];
        }
    }

    private static void compareMilestoneTable( final List< String > problems, final int p, final String[][][] player, final List< String > rows ) {
        if ( rows.size() != 2 * MILESTONES )
            problems.add( "player " + ( p + 1 ) + " milestone table: " + rows.size() + " rows" );
        for ( int row = 0; row < Math.min( 2 * MILESTONES, rows.size() ); row++ ) {
            final String[] cells = cells( rows.get( row ) );
            final int milestone = row / 2;
            for ( int b = 0; b < BUCKETS; b++ ) {
                final String[] m = player[ BUCKETS + b ].length > milestone ? player[ BUCKETS + b ][ milestone ] : null;
                final String expectedCell = m == null ? null : row % 2 == 1 ? m[ 3 ] : m[ 0 ] + " (" + m[ 1 ] + "/" + m[ 2 ] + ")";
                final String actualCell = b < cells.length ? cells[ b ] : null;
                if ( expectedCell == null || !expectedCell.equals( actualCell ) )
                    problems.add( "player " + ( p + 1 ) + " milestone row " + row + " bucket " + b + ": golden '" + expectedCell + "', view model '"
                            + actualCell + "'" );
            }
        }
    }

    /** @return the cells of a table row line {@code label | c1 | c2 ...} (label dropped) */
    private static String[] cells( final String rowLine ) {
        final String[] parts = rowLine.trim().split( " \\| ", -1 );
        final String[] cells = new String[ parts.length - 1 ];
        System.arraycopy( parts, 1, cells, 0, cells.length );
        return cells;
    }

    /**
     * @return per player: index 0..3 = phase lines of bucket ALL..ZVZ (each String[] of the 9 groups), index 4..7 = milestone lines of
     *         the bucket (each String[] of time, k, n, ranking); stored as {@code [slot][line][group]}
     */
    private static List< String[][][] > parseGolden( final String golden, final List< String > problems ) {
        final List< List< List< String[] > > > all = new ArrayList<>();
        List< List< String[] > > slots = null;
        int bucket = -1;
        for ( final String line : golden.split( "\n" ) ) {
            if ( PLAYER_LINE.matcher( line ).matches() ) {
                slots = newSlots();
                all.add( slots );
                bucket = -1;
            } else if ( BUCKET_LINE.matcher( line ).matches() )
                bucket++;
            else if ( slots != null && bucket >= 0 )
                parsePhaseOrMilestone( line, slots, bucket, problems );
        }
        final List< String[][][] > players = new ArrayList<>();
        for ( final List< List< String[] > > player : all )
            players.add( toArray( player ) );
        return players;
    }

    private static void parsePhaseOrMilestone( final String line, final List< List< String[] > > slots, final int bucket,
            final List< String > problems ) {
        final Matcher phase = PHASE_LINE.matcher( line );
        final Matcher milestone = MILESTONE_LINE.matcher( line );
        if ( phase.matches() )
            slots.get( bucket ).add( groups( phase, 9 ) );
        else if ( milestone.matches() )
            slots.get( BUCKETS + bucket ).add( groups( milestone, 4 ) );
        else if ( line.trim().startsWith( "phase " ) || line.trim().startsWith( "milestone " ) )
            problems.add( "unparsable golden line: " + line );
    }

    private static List< List< String[] > > newSlots() {
        final List< List< String[] > > slots = new ArrayList<>();
        for ( int i = 0; i < 2 * BUCKETS; i++ )
            slots.add( new ArrayList< String[] >() );
        return slots;
    }

    private static String[][][] toArray( final List< List< String[] > > slots ) {
        final String[][][] result = new String[ slots.size() ][][];
        for ( int i = 0; i < result.length; i++ )
            result[ i ] = slots.get( i ).toArray( new String[ 0 ][] );
        return result;
    }

    private static String[] groups( final Matcher matcher, final int count ) {
        final String[] groups = new String[ count ];
        for ( int i = 0; i < count; i++ )
            groups[ i ] = matcher.group( i + 1 );
        return groups;
    }

    /** @return per player the row lines of each table, in order: tables of the player, then the rows of each */
    private static List< List< List< String > > > parseView( final String view ) {
        final List< List< List< String > > > players = new ArrayList<>();
        List< List< String > > tables = null;
        List< String > rows = null;
        boolean headerSeen = false;
        for ( final String line : view.split( "\n" ) ) {
            if ( line.startsWith( "player " ) ) {
                tables = new ArrayList<>();
                players.add( tables );
            } else if ( line.startsWith( "table: " ) && tables != null ) {
                rows = new ArrayList<>();
                tables.add( rows );
                headerSeen = false;
            } else if ( line.startsWith( "skipped (" ) ) {
                tables = null;
                rows = null;
            } else if ( rows != null && line.startsWith( "  " ) ) {
                if ( headerSeen )
                    rows.add( line );
                else
                    headerSeen = true; // first indented line of a table is the header row
            }
        }
        return players;
    }
}
