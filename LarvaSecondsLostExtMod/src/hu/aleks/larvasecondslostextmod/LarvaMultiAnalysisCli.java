package hu.aleks.larvasecondslostextmod;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Headless multi-replay dump (E17-S12, epic §8): runs the batch pipeline over the {@code *.SC2Replay} files of one folder
 * (non-recursive, sorted by file name) and prints {@link LarvaMultiAnalysisDumpFormatter} output to stdout (UTF-8). A sibling of
 * {@link LarvaBenchmarkCliExtractor}, which stays untouched.
 *
 * <pre>
 * java ... hu.aleks.larvasecondslostextmod.LarvaMultiAnalysisCli --multi &lt;dir&gt; [--all-modes] [--no-prefilter]
 *          [--view-model] [--scelight-app-dir &lt;path&gt;] [--debug] [--trace]
 * </pre>
 *
 * <p>Default is "ladder games only"; {@code --all-modes} lifts it. {@code --no-prefilter} runs the full analysis of every file
 * (the dump must not change, only the time). {@code --view-model} prints the multi page's display model (E17-S15) instead.</p>
 */
public final class LarvaMultiAnalysisCli {

    private static final String HELP = "--help";
    private static final String MULTI = "--multi";
    private static final String ALL_MODES = "--all-modes";
    private static final String NO_PREFILTER = "--no-prefilter";
    private static final String SCELIGHT_APP_DIR = "--scelight-app-dir";
    private static final String VIEW_MODEL = "--view-model";
    private static final String DEBUG = "--debug";
    private static final String TRACE = "--trace";
    private static final String REPLAY_EXTENSION = ".sc2replay";

    private LarvaMultiAnalysisCli() {
    }

    /** CLI main entry point. */
    public static void main( final String[] args ) {
        try {
            final CliArgs cliArgs = parseArgs( args );
            if ( cliArgs.help ) {
                printUsage();
                return;
            }
            final List< Path > files = listReplays( cliArgs.folder );
            // Scelight's bootstrap prints warnings to stdout (e.g. missing boot-settings.xml, with an absolute path). Keep stdout
            // for the dump alone so it is byte-identical on every machine.
            final PrintStream dumpOut = new PrintStream( new FileOutputStream( FileDescriptor.out ), false, "UTF-8" );
            System.setOut( System.err );
            new StandaloneScelightBootstrap().ensureInitialized( cliArgs.scelightAppDir );
            final StdErrLogger logger = new StdErrLogger( cliArgs.debug, cliArgs.trace );
            final ReplaySummaryService service = new ReplaySummaryService( new ReflectiveScelightFactory().createFactory(),
                    new ReflectiveScelightRepParserEngine(), logger );
            final LarvaAggregationResult result = new LarvaMultiAnalysisPipeline( logger, new LarvaServiceAggregationSource( service ),
                    !cliArgs.noPreFilter ).run( files, !cliArgs.allModes, new LarvaProgressSink() {
                        @Override
                        public void onProgress( final int k, final int n, final String fileName, final int failedCount ) {
                            // headless: the dump is the output, progress is not needed
                        }
                    }, new LarvaCancellationToken() );
            dumpOut.print( cliArgs.viewModel ? viewModelDump( result ) : new LarvaMultiAnalysisDumpFormatter().format( result ) );
            dumpOut.flush();
            System.exit( 0 );
        } catch ( final IllegalArgumentException e ) {
            System.err.println( "ERROR: " + e.getMessage() );
            printUsage();
            System.exit( 2 );
        } catch ( final Exception e ) {
            System.err.println( "ERROR: Multi analysis failed: " + e.getMessage() );
            e.printStackTrace( System.err );
            System.exit( 1 );
        }
    }

    /** The multi page's display model as text (E17-S15 golden), with a fixed locale and time zone so it is machine independent. */
    private static String viewModelDump( final LarvaAggregationResult result ) {
        return LarvaMultiAnalysisViewModelBuilder.build( result, Locale.US, TimeZone.getTimeZone( "UTC" ) ).toText();
    }

    /**
     * @param folder folder to scan
     * @return the {@code *.SC2Replay} files directly inside, sorted by file name (case-sensitive, locale independent)
     */
    static List< Path > listReplays( final Path folderArg ) throws java.io.IOException {
        final Path folder = folderArg.toAbsolutePath(); // the bootstrap changes user.dir later
        if ( !Files.isDirectory( folder ) )
            throw new IllegalArgumentException( "Not a folder: " + folderArg );
        final List< Path > files = new ArrayList<>();
        try ( final DirectoryStream< Path > stream = Files.newDirectoryStream( folder ) ) {
            for ( final Path path : stream )
                if ( Files.isRegularFile( path ) && path.getFileName().toString().toLowerCase( Locale.ROOT ).endsWith( REPLAY_EXTENSION ) )
                    files.add( path );
        }
        Collections.sort( files, new Comparator< Path >() {
            @Override
            public int compare( final Path a, final Path b ) {
                return a.getFileName().toString().compareTo( b.getFileName().toString() );
            }
        } );
        return files;
    }

    private static CliArgs parseArgs( final String[] args ) {
        final CliArgs cliArgs = new CliArgs();
        for ( int i = 0; i < args.length; i++ ) {
            final String arg = args[ i ];
            if ( HELP.equals( arg ) ) {
                cliArgs.help = true;
                return cliArgs;
            } else if ( MULTI.equals( arg ) )
                cliArgs.folder = Paths.get( requireValue( args, ++i, MULTI ) );
            else if ( ALL_MODES.equals( arg ) )
                cliArgs.allModes = true;
            else if ( VIEW_MODEL.equals( arg ) )
                cliArgs.viewModel = true;
            else if ( NO_PREFILTER.equals( arg ) )
                cliArgs.noPreFilter = true;
            else if ( SCELIGHT_APP_DIR.equals( arg ) )
                cliArgs.scelightAppDir = Paths.get( requireValue( args, ++i, SCELIGHT_APP_DIR ) );
            else if ( DEBUG.equals( arg ) )
                cliArgs.debug = true;
            else if ( TRACE.equals( arg ) ) {
                cliArgs.debug = true;
                cliArgs.trace = true;
            } else
                throw new IllegalArgumentException( "Unknown argument: " + arg );
        }
        if ( cliArgs.folder == null )
            throw new IllegalArgumentException( MULTI + " is required." );
        return cliArgs;
    }

    private static String requireValue( final String[] args, final int valueIndex, final String flagName ) {
        if ( valueIndex >= args.length )
            throw new IllegalArgumentException( flagName + " requires a value." );
        return args[ valueIndex ];
    }

    private static void printUsage() {
        System.err.println( "Usage: java ... " + LarvaMultiAnalysisCli.class.getName()
                + " --multi <dir> [--all-modes] [--no-prefilter] [--view-model] [--scelight-app-dir <path>] [--debug] [--trace]" );
    }

    private static final class CliArgs {
        private boolean help;
        private boolean debug;
        private boolean trace;
        private boolean allModes;
        private boolean noPreFilter;
        private boolean viewModel;
        private Path folder;
        private Path scelightAppDir;
    }
}
