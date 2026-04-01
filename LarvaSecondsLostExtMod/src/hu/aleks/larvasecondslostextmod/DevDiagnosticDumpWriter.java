package hu.aleks.larvasecondslostextmod;

import hu.scelightapibase.bean.IExtModManifestBean;
import hu.scelightapibase.service.log.ILogger;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Writes a predictable development-only diagnostic dump file for zero-click verification.
 */
public class DevDiagnosticDumpWriter {

    /** System property that enables the dump file. */
    public static final String PROP_ENABLED = "larva.dev.dump.enabled";

    /** System property that overrides the dump file path. */
    public static final String PROP_FILE = "larva.dev.dump.file";

    /** Default dump file name under the user's home folder. */
    private static final String DEFAULT_DUMP_FILE_NAME = "LarvaSecondsLost-dev-dump.txt";

    /** UTF-8 charset. */
    private static final Charset UTF8 = Charset.forName( "UTF-8" );

    /** Logger used for non-fatal dump write errors. */
    private final ILogger logger;

    /** Tells if dump writing is enabled. */
    private final boolean enabled;

    /** Resolved dump file path. */
    private final Path dumpFile;

    /** Timestamp formatter. */
    private final SimpleDateFormat timestampFormat = new SimpleDateFormat( "yyyy-MM-dd HH:mm:ss" );

    /** Current module lifecycle state. */
    private String lifecycleState = "not-started";

    /** Current lifecycle details. */
    private String lifecycleDetails = "No lifecycle events recorded yet.";

    /** Current replay analysis state. */
    private String analysisState = "idle";

    /** Current replay analysis details. */
    private String analysisDetails = "No replay analysis has been recorded yet.";

    /** Last write timestamp. */
    private String lastUpdated = formatNow();

    /**
     * Creates a new development diagnostic dump writer.
     *
     * @param logger logger used for non-fatal write errors
     */
    public DevDiagnosticDumpWriter( final ILogger logger ) {
        this.logger = logger;
        enabled = Boolean.parseBoolean( System.getProperty( PROP_ENABLED, "false" ) ) || hasText( System.getProperty( PROP_FILE ) );
        dumpFile = resolveDumpFile();
    }

    /**
     * Tells if dump writing is enabled.
     *
     * @return true if dump writing is enabled; false otherwise
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Returns the resolved dump file path.
     *
     * @return resolved dump file path
     */
    public Path getDumpFile() {
        return dumpFile;
    }

    /**
     * Records module startup.
     *
     * @param manifest module manifest
     */
    public synchronized void recordStartup( final IExtModManifestBean manifest ) {
        if ( !enabled )
            return;

        lifecycleState = "started";
        lifecycleDetails = "Module initialized successfully: name=" + manifest.getName() + ", version=" + manifest.getVersion();
        lastUpdated = formatNow();
        writeDump();
    }

    /**
     * Records replay analysis start.
     *
     * @param replayFile replay file being analyzed
     * @param sourceDescription request source description
     */
    public synchronized void recordAnalysisStart( final Path replayFile, final String sourceDescription ) {
        if ( !enabled )
            return;

        analysisState = "running";
        analysisDetails = "Replay analysis started from " + sourceDescription + ":\n" + replayFile;
        lastUpdated = formatNow();
        writeDump();
    }

    /**
     * Records replay analysis success.
     *
     * @param summary replay summary to dump
     */
    public synchronized void recordAnalysisSuccess( final ReplaySummary summary ) {
        if ( !enabled )
            return;

        analysisState = "succeeded";
        analysisDetails = buildSuccessDetails( summary );
        lastUpdated = formatNow();
        writeDump();
    }

    /**
     * Records replay analysis failure.
     *
     * @param replayFile replay file that failed
     * @param sourceDescription request source description
     * @param message error message
     */
    public synchronized void recordAnalysisFailure( final Path replayFile, final String sourceDescription, final String message ) {
        if ( !enabled )
            return;

        analysisState = "failed";
        analysisDetails = "Replay analysis failed from " + sourceDescription + ":\n"
                + replayFile
                + "\n\nReason:\n"
                + ( hasText( message ) ? message : "Unknown error" );
        lastUpdated = formatNow();
        writeDump();
    }

    /**
     * Records module shutdown.
     *
     * @param manifest module manifest
     */
    public synchronized void recordShutdown( final IExtModManifestBean manifest ) {
        if ( !enabled )
            return;

        lifecycleState = "stopped";
        lifecycleDetails = "Module stopped cleanly: name=" + manifest.getName() + ", version=" + manifest.getVersion();
        lastUpdated = formatNow();
        writeDump();
    }

    /**
     * Builds success details for the dump file.
     *
     * @param summary replay summary
     * @return formatted success details
     */
    private String buildSuccessDetails( final ReplaySummary summary ) {
        final StringBuilder builder = new StringBuilder();
        builder.append( "Replay analysis completed successfully." ).append( '\n' );
        builder.append( "Replay source: " ).append( summary.getSourceDescription() ).append( '\n' );
        builder.append( "Replay file: " ).append( summary.getReplayFile() ).append( '\n' );
        builder.append( "Map: " ).append( summary.getMapTitle() ).append( '\n' );
        builder.append( "Players: " ).append( summary.getPlayers() ).append( '\n' );
        builder.append( "Winners: " ).append( summary.getWinners() ).append( '\n' );
        builder.append( "Length: " ).append( summary.getLength() ).append( '\n' );
        builder.append( "Replay end time: " ).append( summary.getReplayEndTime() ).append( '\n' );
        builder.append( "Replay version: " ).append( summary.getReplayVersion() ).append( '\n' );
        builder.append( "Base build: " ).append( summary.getBaseBuild() ).append( '\n' );
        builder.append( "Integration mode: " ).append( summary.getIntegrationMode() ).append( '\n' );
        if ( summary.getLarvaAnalysisReport() != null )
            builder.append( '\n' ).append( summary.getLarvaAnalysisReport().toDisplayText() );
        return builder.toString();
    }

    /**
     * Writes the dump file to disk.
     */
    private void writeDump() {
        try {
            final Path parent = dumpFile.getParent();
            if ( parent != null )
                Files.createDirectories( parent );

            final List< String > lineList = new ArrayList<>();
            lineList.add( "Larva Seconds Lost dev diagnostic dump" );
            lineList.add( "lastUpdated=" + lastUpdated );
            lineList.add( "dumpFile=" + dumpFile );
            lineList.add( "lifecycleState=" + lifecycleState );
            lineList.add( "analysisState=" + analysisState );
            lineList.add( "" );
            lineList.add( "[Lifecycle]" );
            addMultiline( lineList, lifecycleDetails );
            lineList.add( "" );
            lineList.add( "[Analysis]" );
            addMultiline( lineList, analysisDetails );

            Files.write( dumpFile, lineList, UTF8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE );
        } catch ( final IOException e ) {
            logger.error( "Failed to write Larva Seconds Lost dev diagnostic dump: " + dumpFile, e );
        }
    }

    /**
     * Adds multi-line text to a line list.
     *
     * @param lineList target line list
     * @param text text to split into lines
     */
    private void addMultiline( final List< String > lineList, final String text ) {
        final String[] lines = text == null ? new String[] { "" } : text.split( "\\r?\\n" );
        for ( final String line : lines )
            lineList.add( line );
    }

    /**
     * Resolves the dump file path.
     *
     * @return resolved dump file path
     */
    private Path resolveDumpFile() {
        final String fileProperty = System.getProperty( PROP_FILE );
        if ( hasText( fileProperty ) )
            return Paths.get( fileProperty ).toAbsolutePath().normalize();

        return Paths.get( System.getProperty( "user.home" ), DEFAULT_DUMP_FILE_NAME ).toAbsolutePath().normalize();
    }

    /**
     * Tells if a string has visible content.
     *
     * @param value value to test
     * @return true if the string has visible content; false otherwise
     */
    private boolean hasText( final String value ) {
        return value != null && value.trim().length() > 0;
    }

    /**
     * Formats the current timestamp.
     *
     * @return formatted timestamp
     */
    private String formatNow() {
        return timestampFormat.format( new Date() );
    }

}