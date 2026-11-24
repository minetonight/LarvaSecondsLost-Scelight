package hu.aleks.larvasecondslostextmod;

import hu.scelightapibase.service.log.ILogger;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Properties;

/**
 * Remembers the folder of the last confirmed selection per file chooser, also across Scelight restarts (CR-1.5).
 *
 * <p>Persistence is a tiny UTF-8 properties file in the module's own cache folder ({@code IModEnv.getCacheFolder()}, kept by
 * Scelight across restarts). The SDK's settings beans were considered and not used: they appear automatically in Scelight's
 * Settings dialog (node, group, skill level, icon needed) and the dialog cannot be verified by an agent, while these two values
 * are internal state, not user settings.</p>
 *
 * <p>Failures never reach the caller: an unreadable file means "nothing remembered", a failed write is logged and the value
 * stays remembered for this session. Start folder order (FR-3, FR-5): remembered folder if it still exists, then the parent
 * of the last detected replay, then <code>null</code> (the chooser default). EDT only in production; methods are synchronized
 * anyway.</p>
 */
final class LarvaChooserFolderMemory {

    /** The two choosers that remember a folder each, independently of each other. */
    enum Kind {
        /** {@code Open Replay...} on the hub and on single-replay pages (one shared memory). */
        OPEN_REPLAY( "openReplay.folder" ),
        /** {@code Multi Analysis...} on the hub. */
        MULTI_ANALYSIS( "multiAnalysis.folder" );

        private final String key;

        private Kind( final String key ) {
            this.key = key;
        }
    }

    /** File name inside the module cache folder. */
    static final String FILE_NAME = "chooser-folders.properties";

    private static final Charset UTF8 = Charset.forName( "UTF-8" );

    private final Path file;
    private final ILogger logger;
    private final Properties values = new Properties();

    /**
     * @param file
     *            the properties file (may not exist yet); <code>null</code> keeps the memory in RAM only
     * @param logger
     *            module logger for read and write problems
     */
    LarvaChooserFolderMemory( final Path file, final ILogger logger ) {
        this.file = file;
        this.logger = logger;
        load();
    }

    private void load() {
        if ( file == null || !Files.isRegularFile( file ) )
            return;
        try ( final InputStream in = Files.newInputStream( file ); final Reader reader = new InputStreamReader( in, UTF8 ) ) {
            values.load( reader );
        } catch ( final IOException | RuntimeException e ) {
            logger.error( "[larva-hub] could not read remembered chooser folders from " + file + ", starting empty", e );
            values.clear();
        }
    }

    /** @return the properties file, for logs and dev commands */
    Path getFile() {
        return file;
    }

    /** @return the remembered folder of the chooser (it may no longer exist), or <code>null</code> if none */
    synchronized Path getRemembered( final Kind kind ) {
        final String value = values.getProperty( kind.key );
        if ( value == null || value.length() == 0 )
            return null;
        try {
            return Paths.get( value );
        } catch ( final RuntimeException e ) {
            logger.debug( "[larva-hub] ignoring invalid remembered folder '" + value + "'" );
            return null;
        }
    }

    /**
     * Start folder of a chooser: the remembered folder if it still exists, else the parent of the last detected replay, else
     * <code>null</code> (the chooser default).
     *
     * @param kind
     *            which chooser
     * @param lastDetectedReplayPath
     *            last replay reported by the replay monitor; may be <code>null</code>
     * @return the folder to start in, or <code>null</code>
     */
    synchronized Path resolveStartFolder( final Kind kind, final Path lastDetectedReplayPath ) {
        final Path remembered = getRemembered( kind );
        if ( remembered != null && Files.isDirectory( remembered ) )
            return remembered;
        if ( lastDetectedReplayPath != null && lastDetectedReplayPath.getParent() != null )
            return lastDetectedReplayPath.getParent();
        return null;
    }

    /**
     * Remembers the folder of a confirmed selection and writes it to disk. Not called on cancel, so cancel keeps the memory.
     *
     * @param kind
     *            which chooser
     * @param selectedFile
     *            a file the user confirmed; its parent folder is remembered; <code>null</code> or without parent is ignored
     */
    synchronized void remember( final Kind kind, final Path selectedFile ) {
        final Path folder = selectedFile == null ? null : selectedFile.toAbsolutePath().getParent();
        if ( folder == null )
            return;
        values.setProperty( kind.key, folder.toString() );
        save();
    }

    /** Writes the file through a temp file and a move, so a crash never leaves a half-written file. */
    private void save() {
        if ( file == null )
            return;
        final Path temp = file.resolveSibling( FILE_NAME + ".tmp" );
        try {
            if ( file.getParent() != null )
                Files.createDirectories( file.getParent() );
            try ( final OutputStream out = Files.newOutputStream( temp ); final Writer writer = new OutputStreamWriter( out, UTF8 ) ) {
                values.store( writer, "Larva Seconds Lost: last folder of the file choosers" );
            }
            Files.move( temp, file, StandardCopyOption.REPLACE_EXISTING );
        } catch ( final IOException | RuntimeException e ) {
            logger.error( "[larva-hub] could not save remembered chooser folders to " + file + " (kept for this session)", e );
        }
    }
}
