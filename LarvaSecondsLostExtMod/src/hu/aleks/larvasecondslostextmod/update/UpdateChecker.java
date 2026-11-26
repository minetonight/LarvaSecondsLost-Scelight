package hu.aleks.larvasecondslostextmod.update;

import java.io.IOException;
import java.io.InputStream;

/**
 * Downloads the hosted module.xml and compares it with the running module (E18-S08).
 *
 * <p>The name and folder must match the running module; Scelight's own updater has the same rule, and it stops a wrong
 * or hijacked module.xml from installing some other module.</p>
 */
public final class UpdateChecker {

    private final ReleaseDownloader downloader;
    private final ModuleXmlParser parser = new ModuleXmlParser();
    private final String moduleXmlUrl;
    private final String expectedName;
    private final String expectedFolder;
    private final ModuleVersion runningVersion;

    public UpdateChecker( final ReleaseDownloader downloader, final String moduleXmlUrl, final String expectedName, final String expectedFolder,
            final ModuleVersion runningVersion ) {
        this.downloader = downloader;
        this.moduleXmlUrl = moduleXmlUrl;
        this.expectedName = expectedName;
        this.expectedFolder = expectedFolder;
        this.runningVersion = runningVersion;
    }

    /**
     * Runs the check. Blocking (network), so never call it on the EDT. Never throws.
     *
     * @return the outcome
     */
    public UpdateCheckResult check() {
        final RemoteRelease release;
        try ( final InputStream in = new LimitedInputStream( downloader.open( moduleXmlUrl ), UpdateConfig.MAX_MODULE_XML_BYTES ) ) {
            release = parser.parse( in );
        } catch ( final IOException e ) {
            return UpdateCheckResult.failed( "cannot reach the update page (" + ConnectionProblems.describe( e, System.getProperty( "java.version" ) ) + ")" );
        } catch ( final UpdateException e ) {
            return UpdateCheckResult.failed( e.getMessage() );
        }
        if ( !expectedName.equals( release.getName() ) )
            return UpdateCheckResult.failed( "module.xml is for '" + release.getName() + "', expected '" + expectedName + "'" );
        if ( !expectedFolder.equals( release.getFolder() ) )
            return UpdateCheckResult.failed( "module.xml is for folder '" + release.getFolder() + "', expected '" + expectedFolder + "'" );
        return release.getVersion().isNewerThan( runningVersion ) ? UpdateCheckResult.available( release ) : UpdateCheckResult.upToDate( release );
    }

    /** @return the URL that is checked */
    public String getModuleXmlUrl() {
        return moduleXmlUrl;
    }

}
