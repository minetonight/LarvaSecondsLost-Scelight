package hu.aleks.larvasecondslostextmod.update;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import hu.aleks.larvasecondslostextmod.update.UpdateTestFixtures.FakeDownloader;
import hu.aleks.larvasecondslostextmod.update.UpdateTestFixtures.ReleaseBuilder;

/** One-click install (E18-S10): happy path, every verification, zip-slip, and that nothing is left behind on failure. */
public class UpdateInstallerTest {

    private Path temp;
    private Path modRoot;
    private Path running;
    private Path downloads;
    private FakeDownloader downloader;
    private UpdateInstaller installer;
    private final UpdateInstaller.Progress progress = new UpdateInstaller.Progress() {
        @Override
        public void report( final String text ) {
            // not checked here
        }
    };

    @Before
    public void setUp() throws IOException {
        temp = Files.createTempDirectory( "larva-update-test" );
        modRoot = Files.createDirectories( temp.resolve( "Scelight/mod-x/larva-seconds-lost" ) );
        running = UpdateTestFixtures.versionFolder( modRoot, "3.1" );
        downloads = temp.resolve( "cache/update" );
        downloader = new FakeDownloader();
        installer = new UpdateInstaller( downloader, modRoot, downloads, running, UpdateTestFixtures.LOGGER );
    }

    @After
    public void tearDown() throws IOException {
        UpdateTestFixtures.deleteTree( temp );
    }

    private void assertInstallFails( final RemoteRelease release, final String messagePart ) throws IOException {
        try {
            installer.install( release, progress );
            fail( "expected failure: " + messagePart );
        } catch ( final UpdateException e ) {
            assertTrue( "message was: " + e.getMessage(), e.getMessage().contains( messagePart ) );
        }
        assertNothingLeftBehind();
    }

    private void assertNothingLeftBehind() throws IOException {
        assertEquals( "only the running version stays", 1, UpdateTestFixtures.countChildren( modRoot ) );
        assertTrue( Files.isRegularFile( running.resolve( "marker.txt" ) ) );
        assertTrue( !Files.exists( downloads ) || UpdateTestFixtures.countChildren( downloads ) == 0 );
    }

    @Test
    public void installsNewVersionNextToTheRunningOne() throws Exception {
        final ReleaseBuilder builder = new ReleaseBuilder( "3.2" );
        final RemoteRelease release = builder.publish( downloader );

        final Path installed = installer.install( release, progress );

        assertEquals( modRoot.resolve( "3.2" ), installed );
        for ( final Map.Entry< String, byte[] > file : builder.files.entrySet() )
            assertArrayEquals( file.getValue(), Files.readAllBytes( installed.resolve( file.getKey() ) ) );
        assertTrue( "running version untouched", Files.isRegularFile( running.resolve( "marker.txt" ) ) );
        assertFalse( "no staging folder left", Files.exists( modRoot.resolve( UpdateInstaller.STAGING_PREFIX + "3.2" ) ) );
        assertEquals( "download removed", 0, UpdateTestFixtures.countChildren( downloads ) );
        assertTrue( installer.isInstalled( release ) );
    }

    @Test
    public void secondInstallOfACompleteFolderDownloadsNothing() throws Exception {
        final RemoteRelease release = new ReleaseBuilder( "3.2" ).publish( downloader );
        installer.install( release, progress );
        final int opens = downloader.openCount;
        installer.install( release, progress );
        assertEquals( opens, downloader.openCount );
    }

    @Test
    public void incompleteTargetFolderIsReplaced() throws Exception {
        final RemoteRelease release = new ReleaseBuilder( "3.2" ).publish( downloader );
        UpdateTestFixtures.versionFolder( modRoot, "3.2" ); // junk only
        assertFalse( installer.isInstalled( release ) );
        installer.install( release, progress );
        assertTrue( installer.isInstalled( release ) );
        assertFalse( Files.exists( modRoot.resolve( "3.2/marker.txt" ) ) );
    }

    @Test
    public void refusesToReplaceTheRunningVersion() throws Exception {
        assertInstallFails( new ReleaseBuilder( "3.1" ).publish( downloader ), "running version" );
    }

    @Test
    public void archiveChecksumMismatchFails() throws Exception {
        final ReleaseBuilder builder = new ReleaseBuilder( "3.2" );
        final RemoteRelease release = builder.publish( downloader );
        final byte[] zip = downloader.content.get( UpdateTestFixtures.ZIP_URL_BASE + builder.zipFileName() );
        zip[ zip.length - 1 ] ^= 1; // same size, other content
        assertInstallFails( release, "download failed (archive checksum" );
    }

    @Test
    public void archiveSizeMismatchFails() throws Exception {
        final ReleaseBuilder builder = new ReleaseBuilder( "3.2" );
        final RemoteRelease release = builder.publish( downloader );
        downloader.content.put( UpdateTestFixtures.ZIP_URL_BASE + builder.zipFileName(), new byte[] { 1, 2, 3 } );
        assertInstallFails( release, "archive size is 3 bytes" );
    }

    @Test
    public void missingArchiveFails() throws Exception {
        final ReleaseBuilder builder = new ReleaseBuilder( "3.2" );
        final RemoteRelease release = builder.publish( downloader );
        downloader.content.remove( UpdateTestFixtures.ZIP_URL_BASE + builder.zipFileName() );
        assertInstallFails( release, "HTTP 404" );
    }

    @Test
    public void fileChecksumMismatchFails() throws Exception {
        final ReleaseBuilder builder = new ReleaseBuilder( "3.2" );
        final byte[] zip = builder.zip();
        builder.files.put( "lib/helper.jar", "other content".getBytes( UpdateTestFixtures.UTF8 ) ); // module.xml lists another hash
        final String xml = builder.moduleXml( zip );
        downloader.content.put( UpdateTestFixtures.ZIP_URL_BASE + builder.zipFileName(), zip );
        assertInstallFails( builder.parse( xml ), "checksum of Scelight/mod-x/larva-seconds-lost/3.2/lib/helper.jar" );
    }

    @Test
    public void unlistedFileInArchiveFails() throws Exception {
        final ReleaseBuilder builder = new ReleaseBuilder( "3.2" );
        final String xml = builder.moduleXml( builder.zip() ); // lists 3 files
        builder.files.put( "extra.txt", "x".getBytes( UpdateTestFixtures.UTF8 ) );
        final byte[] zip = builder.zip(); // has 4
        downloader.content.put( UpdateTestFixtures.ZIP_URL_BASE + builder.zipFileName(), zip );
        assertInstallFails( withArchive( builder, xml, zip ), "unexpected file in archive" );
    }

    @Test
    public void missingFileInArchiveFails() throws Exception {
        final ReleaseBuilder builder = new ReleaseBuilder( "3.2" );
        final byte[] full = builder.zip();
        final String xml = builder.moduleXml( full );
        builder.files.remove( "lib/helper.jar" );
        final byte[] zip = builder.zip();
        downloader.content.put( UpdateTestFixtures.ZIP_URL_BASE + builder.zipFileName(), zip );
        assertInstallFails( withArchive( builder, xml, zip ), "archive has 2 of the 3 files" );
    }

    @Test
    public void zipSlipEntryIsRejected() throws Exception {
        final ReleaseBuilder builder = new ReleaseBuilder( "3.2" );
        final Map< String, byte[] > entries = new LinkedHashMap<>();
        entries.put( "Scelight/mod-x/larva-seconds-lost/3.2/../../../../evil.txt", "evil".getBytes( UpdateTestFixtures.UTF8 ) );
        final byte[] zip = UpdateTestFixtures.zipOf( entries );
        downloader.content.put( UpdateTestFixtures.ZIP_URL_BASE + builder.zipFileName(), zip );
        assertInstallFails( withArchive( builder, builder.moduleXml( builder.zip() ), zip ), "unexpected file in archive" );
        assertFalse( Files.exists( temp.resolve( "evil.txt" ) ) );
    }

    @Test
    public void unsafeFileListPathIsRejectedBeforeDownloading() throws Exception {
        final ReleaseBuilder builder = new ReleaseBuilder( "3.2" );
        builder.files.put( "../../escape.jar", "x".getBytes( UpdateTestFixtures.UTF8 ) );
        final RemoteRelease release = builder.publish( downloader );
        assertInstallFails( release, "unsafe file path" );
        assertEquals( 0, downloader.openCount );
    }

    @Test
    public void versionFolderMustMatchTheVersion() throws Exception {
        final ReleaseBuilder builder = new ReleaseBuilder( "3.2" );
        final String xml = builder.moduleXml( builder.zip() ).replace( "minor=\"2\"", "minor=\"3\"" );
        assertInstallFails( builder.parse( xml ), "does not match version 3.3" );
    }

    @Test
    public void filesOutsideTheModuleFolderAreRejected() throws Exception {
        final ReleaseBuilder builder = new ReleaseBuilder( "3.2" );
        final String xml = builder.moduleXml( builder.zip() ).replace( "Scelight/mod-x/larva-seconds-lost/3.2/lib/", "Scelight/mod/app/" );
        assertInstallFails( builder.parse( xml ), "outside the module folder" );
    }

    @Test
    public void leftoverStagingFolderIsReplaced() throws Exception {
        final RemoteRelease release = new ReleaseBuilder( "3.2" ).publish( downloader );
        UpdateTestFixtures.versionFolder( modRoot, UpdateInstaller.STAGING_PREFIX + "3.2" );
        installer.install( release, progress );
        assertTrue( installer.isInstalled( release ) );
        assertFalse( Files.exists( modRoot.resolve( UpdateInstaller.STAGING_PREFIX + "3.2" ) ) );
    }

    /** Release parsed from <code>xml</code>, but with archive size and hash of <code>zip</code>, so only the content check can fail. */
    private static RemoteRelease withArchive( final ReleaseBuilder builder, final String xml, final byte[] zip ) throws UpdateException {
        final String fixed = xml.replaceFirst( "(<archiveFile path=\"[^\"]*\" sha256=\")[0-9a-f]{64}", "$1" + UpdateTestFixtures.sha256( zip ) )
                .replaceAll( "<archiveSize>\\d+</archiveSize>", "<archiveSize>" + zip.length + "</archiveSize>" );
        return builder.parse( fixed );
    }

}
