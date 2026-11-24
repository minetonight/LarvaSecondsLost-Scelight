package hu.aleks.larvasecondslostextmod;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * E17-S12 dev commands for the batch runner; they replace the S02 spike commands and are also used by S14.
 *
 * <ul>
 * <li>{@code multi-run <dir|listFile|replay> [ladderOnly=true|false] [noprefilter]}: starts a background run (a folder is
 * scanned non-recursively for {@code *.SC2Replay}, sorted by name; a list file has one path per line). Returns at once.</li>
 * <li>{@code multi-cancel}: cancels the current run (latency is reported by {@code multi-state}).</li>
 * <li>{@code multi-state [full]}: state, k/N, failed count, elapsed, result summary; {@code full} adds the result dump.</li>
 * <li>{@code multi-heap [gc]}: JVM heap numbers; with {@code gc} after a forced GC.</li>
 * </ul>
 */
final class LarvaDevMultiCommands {

    private static final Charset UTF8 = Charset.forName( "UTF-8" );
    private static final long BYTES_PER_MB = 1048576L;

    private final LarvaSecondsLostModule module;

    private LarvaMultiAnalysisRunner runner;
    private LarvaDevRunMonitor monitor;
    private volatile int progressPosts;
    private volatile String lastProgress = "none";
    private volatile long cancelRequestedMs;
    private volatile long cancelObservedMs;
    private volatile String edtOutcome = "none";

    private LarvaDevMultiCommands( final LarvaSecondsLostModule module ) {
        this.module = module;
    }

    static void registerAll( final LarvaDevCommandRegistry registry, final LarvaSecondsLostModule module ) {
        final LarvaDevMultiCommands c = new LarvaDevMultiCommands( module );
        registry.register( c.run() );
        registry.register( c.cancel() );
        registry.register( c.state() );
        registry.register( c.heap() );
    }

    private LarvaDevCommand run() {
        return new Cmd( "multi-run" ) {
            @Override
            LarvaDevCommandResult run( final List< String > args ) throws IOException {
                if ( args.isEmpty() )
                    return LarvaDevCommandResult.fail( "usage: multi-run <dir|listFile|replay> [ladderOnly=true|false] [noprefilter]" );
                final List< Path > files = resolveFiles( Paths.get( args.get( 0 ) ) );
                if ( files.isEmpty() )
                    return LarvaDevCommandResult.fail( "no replay files found for " + args.get( 0 ) );
                final boolean ladderOnly = args.size() < 2 || !"false".equalsIgnoreCase( args.get( 1 ) );
                final boolean preFilter = !args.contains( "noprefilter" );
                return start( files, ladderOnly, preFilter );
            }
        };
    }

    private synchronized LarvaDevCommandResult start( final List< Path > files, final boolean ladderOnly, final boolean preFilter ) {
        if ( runner != null && runner.getState() == LarvaMultiAnalysisRunner.State.RUNNING )
            return LarvaDevCommandResult.fail( "a run is still in progress (multi-cancel first)" );
        progressPosts = 0;
        lastProgress = "none";
        cancelRequestedMs = 0L;
        cancelObservedMs = 0L;
        edtOutcome = "running";
        final LarvaMultiAnalysisRunner created = LarvaMultiAnalysisRunnerFactory.create( module, files, ladderOnly, preFilter, new DevListener() );
        runner = created;
        monitor = new LarvaDevRunMonitor( created );
        created.start();
        return LarvaDevCommandResult.ok( "started " + files.size() + " files ladderOnly=" + ladderOnly + " preFilter=" + preFilter );
    }

    private LarvaDevCommand cancel() {
        return new Cmd( "multi-cancel" ) {
            @Override
            LarvaDevCommandResult run( final List< String > args ) {
                final LarvaMultiAnalysisRunner current = runner;
                if ( current == null || current.getState() != LarvaMultiAnalysisRunner.State.RUNNING )
                    return LarvaDevCommandResult.fail( "no run in progress" );
                cancelRequestedMs = System.nanoTime() / 1000000L;
                current.cancel();
                return LarvaDevCommandResult.ok( "cancel requested at " + current.getCurrentK() + "/" + current.getTotal() );
            }
        };
    }

    private LarvaDevCommand state() {
        return new Cmd( "multi-state" ) {
            @Override
            LarvaDevCommandResult run( final List< String > args ) {
                final LarvaMultiAnalysisRunner current = runner;
                if ( current == null )
                    return LarvaDevCommandResult.fail( "no run yet" );
                final StringBuilder sb = new StringBuilder();
                sb.append( "state=" ).append( current.getState() ).append( " k=" ).append( current.getCurrentK() ).append( '/' )
                        .append( current.getTotal() ).append( " failed=" ).append( current.getFailedCount() ).append( " elapsedMs=" )
                        .append( current.getElapsedMs() ).append( " progressPostsOnEdt=" ).append( progressPosts ).append( " lastProgress=" )
                        .append( lastProgress ).append( " edtOutcome=" ).append( edtOutcome );
                if ( current.getState() == LarvaMultiAnalysisRunner.State.CANCELLED )
                    sb.append( " processedWhenCancelled=" ).append( current.getProcessedWhenCancelled() ).append( " cancelLatencyMs=" )
                            .append( cancelObservedMs == 0L ? -1L : cancelObservedMs - cancelRequestedMs );
                final LarvaDevRunMonitor mon = monitor;
                if ( mon != null )
                    sb.append( ' ' ).append( mon.describe() );
                final LarvaAggregationResult result = current.getResult();
                final List< String > detail = new ArrayList<>();
                if ( result != null ) {
                    sb.append( " | " ).append( summaryOf( result ) );
                    if ( args.contains( "full" ) )
                        detail.addAll( Arrays.asList( new LarvaMultiAnalysisDumpFormatter().format( result ).split( "\n" ) ) );
                }
                return LarvaDevCommandResult.ok( sb.toString(), detail );
            }
        };
    }

    private static String summaryOf( final LarvaAggregationResult result ) {
        final StringBuilder sb = new StringBuilder( result.getFilterSummary().toText() ).append( "; games=" ).append( result.getDistinctGameCount() )
                .append( "; skipped=" ).append( result.getSkipped().size() ).append( "; tracked=" );
        for ( final LarvaAggregationResult.PlayerResult player : result.getPlayers() )
            sb.append( player.getPlayer().getDisplayName() ).append( '(' ).append( player.getPlayer().getGameCount() ).append( ") " );
        return sb.toString().trim();
    }

    private LarvaDevCommand heap() {
        return new Cmd( "multi-heap" ) {
            @Override
            LarvaDevCommandResult run( final List< String > args ) {
                if ( args.contains( "gc" ) )
                    System.gc();
                final Runtime rt = Runtime.getRuntime();
                final long retained = ( rt.totalMemory() - rt.freeMemory() ) / BYTES_PER_MB;
                final LarvaDevRunMonitor mon = monitor;
                return LarvaDevCommandResult.ok( "usedMb=" + retained + " totalMb=" + rt.totalMemory() / BYTES_PER_MB + " maxMb="
                        + rt.maxMemory() / BYTES_PER_MB + ( args.contains( "gc" ) ? " (after gc)" : "" )
                        + ( mon == null ? "" : " baselineMb=" + mon.getBaselineMb() + " retainedGrowthMb=" + ( retained - mon.getBaselineMb() ) ) );
            }
        };
    }

    static List< Path > resolveFiles( final Path argument ) throws IOException {
        if ( Files.isDirectory( argument ) )
            return LarvaMultiAnalysisCli.listReplays( argument );
        final List< Path > files = new ArrayList<>();
        if ( !Files.isRegularFile( argument ) )
            return files;
        if ( argument.getFileName().toString().toLowerCase( Locale.ROOT ).endsWith( ".sc2replay" ) ) {
            files.add( argument );
            return files;
        }
        for ( final String line : Files.readAllLines( argument, UTF8 ) )
            if ( line.trim().length() > 0 )
                files.add( Paths.get( line.trim() ) );
        return files;
    }

    /** Records what arrives on the EDT, so the dev commands can show that progress really reaches the UI thread. */
    private final class DevListener implements LarvaMultiAnalysisListener {

        @Override
        public void onProgress( final int k, final int n, final String fileName, final int failedCount ) {
            progressPosts++;
            lastProgress = k + "/" + n + " failed=" + failedCount;
        }

        @Override
        public void onCompleted( final LarvaAggregationResult result, final LarvaMultiAnalysisViewModel viewModel ) {
            edtOutcome = "completed";
            finishMonitor();
            module.logger.info( LarvaMultiAnalysisPipeline.LOG_PREFIX + "[dev] completed: " + summaryOf( result ) );
        }

        @Override
        public void onCancelled() {
            cancelObservedMs = System.nanoTime() / 1000000L;
            edtOutcome = "cancelled";
            finishMonitor();
        }

        @Override
        public void onFailed( final Throwable error ) {
            edtOutcome = "failed: " + error;
            finishMonitor();
        }

        private void finishMonitor() {
            final LarvaDevRunMonitor mon = monitor;
            if ( mon != null )
                mon.stop();
        }
    }

    private abstract static class Cmd implements LarvaDevCommand {
        private final String name;

        Cmd( final String name ) {
            this.name = name;
        }

        abstract LarvaDevCommandResult run( List< String > args ) throws IOException;

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
