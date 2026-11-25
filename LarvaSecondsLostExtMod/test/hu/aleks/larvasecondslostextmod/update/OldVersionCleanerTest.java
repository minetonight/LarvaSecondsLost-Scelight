package hu.aleks.larvasecondslostextmod.update;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/** Cleanup at start (E18-S10): only lower versions and staging leftovers go; the running and newer versions stay. */
public class OldVersionCleanerTest {

    private Path modRoot;
    private final OldVersionCleaner cleaner = new OldVersionCleaner( UpdateTestFixtures.LOGGER );

    @Before
    public void setUp() throws IOException {
        modRoot = Files.createTempDirectory( "larva-cleaner-test" ).resolve( "larva-seconds-lost" );
        Files.createDirectories( modRoot );
    }

    @After
    public void tearDown() throws IOException {
        UpdateTestFixtures.deleteTree( modRoot.getParent() );
    }

    @Test
    public void removesLowerVersionsAndStagingButKeepsRunningAndNewer() throws IOException {
        UpdateTestFixtures.versionFolder( modRoot, "2.9" );
        UpdateTestFixtures.versionFolder( modRoot, "3.0" );
        UpdateTestFixtures.versionFolder( modRoot, "3.0.5" );
        final Path running = UpdateTestFixtures.versionFolder( modRoot, "3.1" );
        UpdateTestFixtures.versionFolder( modRoot, "3.1.1" ); // installed, waiting for a restart
        UpdateTestFixtures.versionFolder( modRoot, UpdateInstaller.STAGING_PREFIX + "3.2" );
        UpdateTestFixtures.versionFolder( modRoot, "notes" ); // not a version: not ours to judge
        Files.write( modRoot.resolve( "2.0" + ".txt" ), new byte[ 0 ] ); // a file, not a folder

        assertEquals( 4, cleaner.clean( modRoot, running ).size() );

        assertFalse( Files.exists( modRoot.resolve( "2.9" ) ) );
        assertFalse( Files.exists( modRoot.resolve( "3.0" ) ) );
        assertFalse( Files.exists( modRoot.resolve( "3.0.5" ) ) );
        assertFalse( Files.exists( modRoot.resolve( UpdateInstaller.STAGING_PREFIX + "3.2" ) ) );
        assertTrue( Files.isRegularFile( running.resolve( "marker.txt" ) ) );
        assertTrue( Files.exists( modRoot.resolve( "3.1.1" ) ) );
        assertTrue( Files.exists( modRoot.resolve( "notes" ) ) );
        assertTrue( Files.exists( modRoot.resolve( "2.0.txt" ) ) );
    }

    @Test
    public void runningFolderWithTheSameVersionInOtherSpellingIsKept() throws IOException {
        final Path running = UpdateTestFixtures.versionFolder( modRoot, "3.1" );
        UpdateTestFixtures.versionFolder( modRoot, "3.1.0" );
        cleaner.clean( modRoot, running );
        assertTrue( Files.exists( modRoot.resolve( "3.1.0" ) ) );
        assertTrue( Files.exists( running ) );
    }

    @Test
    public void doesNothingIfRunningFolderIsNotAVersionFolderOfTheRoot() throws IOException {
        UpdateTestFixtures.versionFolder( modRoot, "1.0" );
        final Path elsewhere = UpdateTestFixtures.versionFolder( modRoot.getParent(), "3.1" );
        assertTrue( cleaner.clean( modRoot, elsewhere ).isEmpty() );
        final Path notVersion = UpdateTestFixtures.versionFolder( modRoot, "dev" );
        assertTrue( cleaner.clean( modRoot, notVersion ).isEmpty() );
        assertTrue( Files.exists( modRoot.resolve( "1.0" ) ) );
    }

}
