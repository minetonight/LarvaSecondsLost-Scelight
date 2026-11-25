package hu.aleks.larvasecondslostextmod.update;

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
import java.nio.file.StandardCopyOption;
import java.util.Properties;

import hu.scelightapibase.service.log.ILogger;

/**
 * Update settings and state, kept in a small properties file in the module cache folder (E18-S09).
 *
 * <p>Same approach as {@code LarvaChooserFolderMemory}: not an SDK settings bean, so nothing appears in Scelight's Settings
 * dialog; the one user choice (automatic checks) is a checkbox on the Larva hub. Read and write problems are logged and never
 * reach the caller.</p>
 */
public final class UpdatePreferences {

    /** File name inside the module cache folder. */
    public static final String FILE_NAME = "update.properties";

    private static final String KEY_AUTO_CHECK = "autoCheck";
    private static final String KEY_LAST_CHECK = "lastCheckMillis";
    private static final String KEY_HIDDEN_VERSION = "hiddenVersion";
    private static final Charset UTF8 = Charset.forName( "UTF-8" );

    private final Path file;
    private final ILogger logger;
    private final Properties values = new Properties();

    /**
     * @param file the properties file (may not exist yet); <code>null</code> keeps everything in RAM only
     */
    public UpdatePreferences( final Path file, final ILogger logger ) {
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
            logger.error( "[larva-update] could not read " + file + ", using defaults", e );
            values.clear();
        }
    }

    /** @return whether automatic checks are on; default <code>true</code> */
    public synchronized boolean isAutoCheck() {
        return !"false".equals( values.getProperty( KEY_AUTO_CHECK ) );
    }

    public synchronized void setAutoCheck( final boolean autoCheck ) {
        values.setProperty( KEY_AUTO_CHECK, Boolean.toString( autoCheck ) );
        save();
    }

    /** @return time of the last successful check, or 0 if never */
    public synchronized long getLastCheckMillis() {
        try {
            return Long.parseLong( values.getProperty( KEY_LAST_CHECK, "0" ) );
        } catch ( final NumberFormatException e ) {
            return 0;
        }
    }

    public synchronized void setLastCheckMillis( final long millis ) {
        values.setProperty( KEY_LAST_CHECK, Long.toString( millis ) );
        save();
    }

    /** @return the version the user chose to hide, or <code>null</code> */
    public synchronized ModuleVersion getHiddenVersion() {
        return ModuleVersion.parseOrNull( values.getProperty( KEY_HIDDEN_VERSION ) );
    }

    public synchronized void setHiddenVersion( final ModuleVersion version ) {
        if ( version == null )
            values.remove( KEY_HIDDEN_VERSION );
        else
            values.setProperty( KEY_HIDDEN_VERSION, version.toString() );
        save();
    }

    /** Writes through a temp file and a move, so a crash never leaves a half-written file. */
    private void save() {
        if ( file == null )
            return;
        final Path temp = file.resolveSibling( FILE_NAME + ".tmp" );
        try {
            if ( file.getParent() != null )
                Files.createDirectories( file.getParent() );
            try ( final OutputStream out = Files.newOutputStream( temp ); final Writer writer = new OutputStreamWriter( out, UTF8 ) ) {
                values.store( writer, "Larva Seconds Lost: online update settings" );
            }
            Files.move( temp, file, StandardCopyOption.REPLACE_EXISTING );
        } catch ( final IOException | RuntimeException e ) {
            logger.error( "[larva-update] could not save " + file + " (kept for this session)", e );
        }
    }

}
