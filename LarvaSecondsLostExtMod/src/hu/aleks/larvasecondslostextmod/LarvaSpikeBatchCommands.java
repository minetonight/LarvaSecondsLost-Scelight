package hu.aleks.larvasecondslostextmod;

import hu.scelightapi.sc2.rep.model.details.IPlayer;
import hu.scelightapi.sc2.rep.repproc.IRepProcessor;

import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * E17-S02 spike commands ({@code spike-batch-*}) for the dev command channel. Throwaway: measures the cache-only
 * {@code getRepProc} metadata path and the full analysis inside the real Scelight JVM. Delete after S02 (S12 and S07
 * replace the findings with production code). These commands run on the channel thread, not on the EDT.
 */
final class LarvaSpikeBatchCommands {

    private static final int MIN_LOOPS = 5376;

    private final LarvaSecondsLostModule module;

    private LarvaSpikeBatchCommands( final LarvaSecondsLostModule module ) {
        this.module = module;
    }

    static void registerAll( final LarvaDevCommandRegistry registry, final LarvaSecondsLostModule module ) {
        final LarvaSpikeBatchCommands c = new LarvaSpikeBatchCommands( module );
        registry.register( c.meta() );
        registry.register( c.full() );
    }

    /** {@code spike-batch-meta <listFile> <outTsv>}: cache-only getRepProc versus parseAndWrapReplay, same fields. */
    private LarvaDevCommand meta() {
        return new BatchCmd( "spike-batch-meta" ) {
            @Override
            LarvaDevCommandResult run( final List< String > a ) throws Exception {
                if ( a.size() < 2 )
                    return LarvaDevCommandResult.fail( "usage: <listFile> <outTsv>" );
                final List< String > files = Files.readAllLines( Paths.get( a.get( 0 ) ), StandardCharsets.UTF_8 );
                final PrintWriter out = new PrintWriter( a.get( 1 ), "UTF-8" );
                out.println( "idx\tcheapUs\tcacheHit\tfullUs\tidentical\tpassFilter\tcheap\tfull\tfile" );
                int hits = 0, identical = 0, nulls = 0, pass = 0;
                long cheapNs = 0, fullNs = 0;
                for ( int i = 0; i < files.size(); i++ ) {
                    final Path p = Paths.get( files.get( i ) );
                    long s = System.nanoTime();
                    IRepProcessor cheap;
                    try {
                        cheap = module.repParserEngine.getRepProc( p );
                    } catch ( final Throwable t ) {
                        cheap = null;
                    }
                    final long cheapUs = ( System.nanoTime() - s ) / 1000L;
                    if ( cheap == null ) {
                        nulls++;
                        out.println( i + "\t" + cheapUs + "\tNULL\t\t\t\t\t\t" + p );
                        continue;
                    }
                    final boolean hit = cheap.getReplay().getTrackerEvents() == null || cheap.getReplay().getGameEvents() == null;
                    s = System.nanoTime();
                    IRepProcessor full = null;
                    try {
                        full = module.repParserEngine.parseAndWrapReplay( p );
                    } catch ( final Throwable t ) {
                        // reported below as a blank full column
                    }
                    final long fullUs = ( System.nanoTime() - s ) / 1000L;
                    final String c = describe( cheap );
                    final String f = full == null ? "FULLFAIL" : describe( full );
                    final boolean same = c.equals( f );
                    final boolean passes = passesFilter( cheap );
                    hits += hit ? 1 : 0;
                    identical += same ? 1 : 0;
                    pass += passes ? 1 : 0;
                    if ( hit ) {
                        cheapNs += cheapUs;
                        fullNs += fullUs;
                    }
                    out.println( i + "\t" + cheapUs + "\t" + hit + "\t" + fullUs + "\t" + same + "\t" + passes + "\t" + c + "\t" + f + "\t" + p );
                }
                out.close();
                return LarvaDevCommandResult.ok( "meta files=" + files.size() + " nullProc=" + nulls + " cacheHit=" + hits + " identicalToFull=" + identical
                        + " passFilter=" + pass + " hitMeanCheapUs=" + ( hits == 0 ? 0 : cheapNs / hits ) + " hitMeanFullParseUs="
                        + ( hits == 0 ? 0 : fullNs / hits ) );
            }
        };
    }

    /** {@code spike-batch-full <listFile> <threads> <outJsonl> [prefilter]}: real-engine full analysis, one service per worker. */
    private LarvaDevCommand full() {
        return new BatchCmd( "spike-batch-full" ) {
            @Override
            LarvaDevCommandResult run( final List< String > a ) throws Exception {
                if ( a.size() < 3 )
                    return LarvaDevCommandResult.fail( "usage: <listFile> <threads> <outJsonl> [prefilter]" );
                final List< String > files = Files.readAllLines( Paths.get( a.get( 0 ) ), StandardCharsets.UTF_8 );
                final int threads = Integer.parseInt( a.get( 1 ) );
                final boolean prefilter = a.size() > 3 && "prefilter".equals( a.get( 3 ) );
                final int n = files.size();
                final long[] us = new long[ n ];
                final String[] json = new String[ n ];
                final AtomicInteger next = new AtomicInteger();
                final AtomicInteger skipped = new AtomicInteger();
                final ExecutorService pool = Executors.newFixedThreadPool( threads );
                final long t0 = System.nanoTime();
                for ( int w = 0; w < threads; w++ )
                    pool.submit( new Runnable() {
                        @Override
                        public void run() {
                            final ReplaySummaryService svc = new ReplaySummaryService( module );
                            final LarvaBenchmarkJsonSerializer ser = new LarvaBenchmarkJsonSerializer();
                            int i;
                            while ( ( i = next.getAndIncrement() ) < n ) {
                                final long s = System.nanoTime();
                                try {
                                    final Path p = Paths.get( files.get( i ) );
                                    if ( prefilter && !passesFilter( module.repParserEngine.getRepProc( p ) ) ) {
                                        skipped.incrementAndGet();
                                        json[ i ] = "SKIPPED";
                                    } else
                                        json[ i ] = ser.toJson( svc.analyzeBenchmarkReplay( p, "spike" ) );
                                } catch ( final Throwable t ) {
                                    json[ i ] = "ERROR " + t;
                                }
                                us[ i ] = ( System.nanoTime() - s ) / 1000L;
                            }
                        }
                    } );
                pool.shutdown();
                pool.awaitTermination( 30, TimeUnit.MINUTES );
                final long totalMs = ( System.nanoTime() - t0 ) / 1000000L;
                final PrintWriter out = new PrintWriter( a.get( 2 ), "UTF-8" );
                int errors = 0;
                for ( int i = 0; i < n; i++ ) {
                    out.println( json[ i ] == null ? "ERROR null" : json[ i ].replace( "\n", " " ) );
                    errors += json[ i ] != null && json[ i ].startsWith( "ERROR" ) ? 1 : 0;
                }
                out.close();
                final long[] sorted = us.clone();
                Arrays.sort( sorted );
                long sum = 0;
                for ( final long v : us )
                    sum += v;
                final Runtime rt = Runtime.getRuntime();
                return LarvaDevCommandResult.ok( "full threads=" + threads + " prefilter=" + prefilter + " files=" + n + " skipped=" + skipped.get()
                        + " errors=" + errors + " totalMs=" + totalMs + " meanUs=" + sum / Math.max( 1, n ) + " medianUs=" + sorted[ n / 2 ] + " p90Us="
                        + sorted[ (int) Math.ceil( n * 0.9 ) - 1 ] + " maxUs=" + sorted[ n - 1 ] + " usedHeapMb=" + ( rt.totalMemory() - rt.freeMemory() )
                        / 1048576L + " maxHeapMb=" + rt.maxMemory() / 1048576L );
            }
        };
    }

    /** The S12/S07 pre-filter candidate: AutoMM, 1v1, length >= 5376 loops, at least one Zerg player. */
    private static boolean passesFilter( final IRepProcessor rp ) {
        if ( rp == null || rp.getReplay() == null )
            return false;
        final Integer loops = rp.getReplay().getHeader().getElapsedGameLoops();
        if ( loops == null || loops.intValue() < MIN_LOOPS || rp.getFormat() != hu.scelightapi.sc2.rep.repproc.IFormat.ONE_VS_ONE
                || rp.getReplay().getAttributesEvents().getGameMode() != hu.scelightapi.sc2.rep.model.attributesevents.IGameMode.AMM )
            return false;
        for ( final IPlayer pl : rp.getReplay().getDetails().getPlayerList() )
            if ( pl.getRace() == hu.scelightapi.sc2.rep.model.details.IRace.ZERG )
                return true;
        return false;
    }

    private static String describe( final IRepProcessor rp ) {
        final StringBuilder sb = new StringBuilder();
        sb.append( rp.getFormat() ).append( '|' ).append( rp.getReplay().getAttributesEvents().getGameMode() ).append( '|' );
        sb.append( rp.getReplay().getDetails().getTime() == null ? "" : String.valueOf( rp.getReplay().getDetails().getTime().getTime() ) ).append( '|' );
        sb.append( rp.getReplay().getHeader().getElapsedGameLoops() ).append( '|' ).append( rp.getLengthMs() ).append( '|' );
        final List< String > players = new ArrayList< String >();
        for ( final IPlayer pl : rp.getReplay().getDetails().getPlayerList() )
            players.add( pl.getRace() + "/" + ( pl.getToon() == null ? "null" : pl.getToon().getRegionId() + "-" + pl.getToon().getProgramId() + "-"
                    + pl.getToon().getRealmId() + "-" + pl.getToon().getId() ) );
        return sb.append( players ).toString();
    }

    /** Batch commands block the channel thread while they run (spike only). */
    private abstract static class BatchCmd implements LarvaDevCommand {
        private final String name;

        BatchCmd( final String name ) {
            this.name = name;
        }

        abstract LarvaDevCommandResult run( List< String > args ) throws Exception;

        @Override
        public String getName() {
            return name;
        }

        @Override
        public boolean runsOnEdt() {
            return false;
        }

        @Override
        public LarvaDevCommandResult execute( final List< String > args ) throws Exception {
            return run( args );
        }
    }

}
