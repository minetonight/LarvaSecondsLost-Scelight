package hu.aleks.larvasecondslostextmod;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Standalone CLI entry point that emits benchmark replay analysis as JSON.
 */
public class LarvaBenchmarkCliExtractor {

    /** Help flag. */
    private static final String HELP = "--help";

    /** Replay file flag. */
    private static final String REPLAY_FILE = "--replay-file";

    /** Source description flag. */
    private static final String SOURCE_DESCRIPTION = "--source-description";

    /** Debug flag. */
    private static final String DEBUG = "--debug";

    /** Trace flag. */
    private static final String TRACE = "--trace";

    /** Scelight app dir flag. */
    private static final String SCELIGHT_APP_DIR = "--scelight-app-dir";

    /**
     * CLI main entry point.
     */
    public static void main( final String[] args ) {
        try {
            final CliArgs cliArgs = parseArgs( args );
            if ( cliArgs.help ) {
                printUsage();
                return;
            }

            new StandaloneScelightBootstrap().ensureInitialized( cliArgs.scelightAppDir );

            final ReplaySummaryService replaySummaryService = new ReplaySummaryService( new ReflectiveScelightFactory().createFactory(),
                    new ReflectiveScelightRepParserEngine(), new StdErrLogger( cliArgs.debug, cliArgs.trace ) );
            final LarvaBenchmarkReplayRecord replayRecord = new LarvaBenchmarkExtractionService( replaySummaryService ).extract( cliArgs.replayFile,
                    cliArgs.sourceDescription );
            System.out.println( new LarvaBenchmarkJsonSerializer().toJson( replayRecord ) );
            System.exit( 0 );
        } catch ( final IllegalArgumentException e ) {
            System.err.println( "ERROR: " + e.getMessage() );
            printUsage();
            System.exit( 2 );
        } catch ( final Exception e ) {
            System.err.println( "ERROR: Benchmark extraction failed: " + e.getMessage() );
            e.printStackTrace( System.err );
            System.exit( 1 );
        }
    }

    /**
     * Parses CLI arguments.
     */
    private static CliArgs parseArgs( final String[] args ) {
        final CliArgs cliArgs = new CliArgs();
        for ( int i = 0; i < args.length; i++ ) {
            final String arg = args[ i ];
            if ( HELP.equals( arg ) ) {
                cliArgs.help = true;
                return cliArgs;
            }
            if ( DEBUG.equals( arg ) ) {
                cliArgs.debug = true;
                continue;
            }
            if ( TRACE.equals( arg ) ) {
                cliArgs.debug = true;
                cliArgs.trace = true;
                continue;
            }
            if ( REPLAY_FILE.equals( arg ) ) {
                cliArgs.replayFile = Paths.get( requireValue( args, ++i, REPLAY_FILE ) );
                continue;
            }
            if ( SOURCE_DESCRIPTION.equals( arg ) ) {
                cliArgs.sourceDescription = requireValue( args, ++i, SOURCE_DESCRIPTION );
                continue;
            }
            if ( SCELIGHT_APP_DIR.equals( arg ) ) {
                cliArgs.scelightAppDir = Paths.get( requireValue( args, ++i, SCELIGHT_APP_DIR ) );
                continue;
            }
            throw new IllegalArgumentException( "Unknown argument: " + arg );
        }

        if ( cliArgs.replayFile == null )
            throw new IllegalArgumentException( REPLAY_FILE + " is required." );
        if ( cliArgs.sourceDescription == null || cliArgs.sourceDescription.length() == 0 )
            throw new IllegalArgumentException( SOURCE_DESCRIPTION + " is required." );
        return cliArgs;
    }

    /**
     * Reads the value paired with a flag.
     */
    private static String requireValue( final String[] args, final int valueIndex, final String flagName ) {
        if ( valueIndex >= args.length )
            throw new IllegalArgumentException( flagName + " requires a value." );
        return args[ valueIndex ];
    }

    /**
     * Prints CLI usage.
     */
    private static void printUsage() {
        System.err.println( "Usage: java ... " + LarvaBenchmarkCliExtractor.class.getName()
            + " --replay-file <path> --source-description <text> [--scelight-app-dir <path>] [--debug] [--trace]" );
    }

    /**
     * Parsed CLI arguments.
     */
    private static class CliArgs {
        private boolean help;
        private boolean debug;
        private boolean trace;
        private Path replayFile;
        private String sourceDescription;
        private Path scelightAppDir;
    }

}