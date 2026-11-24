package hu.aleks.larvasecondslostextmod;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/** Remembered chooser folders (CR-1.5): store, load across instances, fallback order, cancel, independence, write errors. */
public class LarvaChooserFolderMemoryTest {

    private Path root;
    private Path folderA;
    private Path folderB;
    private Path file;

    @Before
    public void setUp() throws IOException {
        root = Files.createTempDirectory( "larva-memory-test" );
        folderA = Files.createDirectory( root.resolve( "a" ) );
        folderB = Files.createDirectory( root.resolve( "b" ) );
        file = root.resolve( "cache" ).resolve( LarvaChooserFolderMemory.FILE_NAME );
    }

    @After
    public void tearDown() throws IOException {
        deleteTree( root );
    }

    private static void deleteTree( final Path path ) throws IOException {
        if ( Files.isDirectory( path ) )
            try ( final java.nio.file.DirectoryStream< Path > children = Files.newDirectoryStream( path ) ) {
                for ( final Path child : children )
                    deleteTree( child );
            }
        Files.deleteIfExists( path );
    }

    private LarvaChooserFolderMemory memory() {
        return new LarvaChooserFolderMemory( file, new StdErrLogger( false, false ) );
    }

    @Test
    public void nothingRememberedGivesNullWithoutLastDetected() {
        assertNull( memory().resolveStartFolder( LarvaChooserFolderMemory.Kind.OPEN_REPLAY, null ) );
    }

    @Test
    public void rememberStoresTheFolderOfTheSelectedFileAndSurvivesARestart() {
        memory().remember( LarvaChooserFolderMemory.Kind.OPEN_REPLAY, folderA.resolve( "x.SC2Replay" ) );
        assertTrue( Files.isRegularFile( file ) );
        final LarvaChooserFolderMemory restarted = memory();
        assertEquals( folderA, restarted.getRemembered( LarvaChooserFolderMemory.Kind.OPEN_REPLAY ) );
        assertEquals( folderA, restarted.resolveStartFolder( LarvaChooserFolderMemory.Kind.OPEN_REPLAY, null ) );
    }

    @Test
    public void openAndMultiMemoriesAreIndependent() {
        final LarvaChooserFolderMemory m = memory();
        m.remember( LarvaChooserFolderMemory.Kind.OPEN_REPLAY, folderA.resolve( "x.SC2Replay" ) );
        assertNull( m.getRemembered( LarvaChooserFolderMemory.Kind.MULTI_ANALYSIS ) );
        m.remember( LarvaChooserFolderMemory.Kind.MULTI_ANALYSIS, folderB.resolve( "y.SC2Replay" ) );
        final LarvaChooserFolderMemory restarted = memory();
        assertEquals( folderA, restarted.getRemembered( LarvaChooserFolderMemory.Kind.OPEN_REPLAY ) );
        assertEquals( folderB, restarted.getRemembered( LarvaChooserFolderMemory.Kind.MULTI_ANALYSIS ) );
    }

    @Test
    public void rememberedFolderBeatsLastDetectedReplayFolder() {
        final LarvaChooserFolderMemory m = memory();
        m.remember( LarvaChooserFolderMemory.Kind.MULTI_ANALYSIS, folderA.resolve( "x.SC2Replay" ) );
        assertEquals( folderA, m.resolveStartFolder( LarvaChooserFolderMemory.Kind.MULTI_ANALYSIS, folderB.resolve( "last.SC2Replay" ) ) );
    }

    @Test
    public void deletedRememberedFolderFallsBackToLastDetectedFolder() throws IOException {
        final LarvaChooserFolderMemory m = memory();
        m.remember( LarvaChooserFolderMemory.Kind.OPEN_REPLAY, folderA.resolve( "x.SC2Replay" ) );
        Files.delete( folderA );
        assertEquals( folderB, m.resolveStartFolder( LarvaChooserFolderMemory.Kind.OPEN_REPLAY, folderB.resolve( "last.SC2Replay" ) ) );
        assertNull( m.resolveStartFolder( LarvaChooserFolderMemory.Kind.OPEN_REPLAY, null ) );
    }

    /** Cancel never calls remember, so the memory keeps its value; null and parentless inputs are ignored too. */
    @Test
    public void cancelAndEmptySelectionsKeepTheMemory() {
        final LarvaChooserFolderMemory m = memory();
        m.remember( LarvaChooserFolderMemory.Kind.OPEN_REPLAY, folderA.resolve( "x.SC2Replay" ) );
        m.remember( LarvaChooserFolderMemory.Kind.OPEN_REPLAY, null );
        m.remember( LarvaChooserFolderMemory.Kind.OPEN_REPLAY, Paths.get( "/" ) );
        assertEquals( folderA, m.getRemembered( LarvaChooserFolderMemory.Kind.OPEN_REPLAY ) );
    }

    @Test
    public void paths_with_spaces_and_non_ascii_round_trip() throws IOException {
        final Path odd = Files.createDirectory( root.resolve( "Replays (2) äж" ) );
        memory().remember( LarvaChooserFolderMemory.Kind.OPEN_REPLAY, odd.resolve( "x.SC2Replay" ) );
        assertEquals( odd, memory().getRemembered( LarvaChooserFolderMemory.Kind.OPEN_REPLAY ) );
    }

    @Test
    public void corruptFileMeansNothingRememberedAndDoesNotThrow() throws IOException {
        Files.createDirectories( file.getParent() );
        Files.write( file, new byte[] { (byte) 0x5c, 0x75, 0x7a } ); // backslash, u, z: a malformed unicode escape
        final LarvaChooserFolderMemory m = memory();
        assertNull( m.getRemembered( LarvaChooserFolderMemory.Kind.OPEN_REPLAY ) );
        m.remember( LarvaChooserFolderMemory.Kind.OPEN_REPLAY, folderA.resolve( "x.SC2Replay" ) );
        assertEquals( folderA, memory().getRemembered( LarvaChooserFolderMemory.Kind.OPEN_REPLAY ) );
    }

    /** A write error is logged and swallowed; the value stays remembered for the session. */
    @Test
    public void failedWriteKeepsTheValueInRamAndDoesNotThrow() throws IOException {
        final Path blocker = Files.createFile( root.resolve( "blocker" ) );
        final LarvaChooserFolderMemory m = new LarvaChooserFolderMemory( blocker.resolve( LarvaChooserFolderMemory.FILE_NAME ),
                new StdErrLogger( false, false ) );
        m.remember( LarvaChooserFolderMemory.Kind.OPEN_REPLAY, folderA.resolve( "x.SC2Replay" ) );
        assertEquals( folderA, m.getRemembered( LarvaChooserFolderMemory.Kind.OPEN_REPLAY ) );
        assertFalse( Files.exists( blocker.resolve( LarvaChooserFolderMemory.FILE_NAME ) ) );
    }

    @Test
    public void withoutAFileTheMemoryLivesInRam() {
        final LarvaChooserFolderMemory m = new LarvaChooserFolderMemory( null, new StdErrLogger( false, false ) );
        m.remember( LarvaChooserFolderMemory.Kind.MULTI_ANALYSIS, folderB.resolve( "y.SC2Replay" ) );
        assertEquals( folderB, m.getRemembered( LarvaChooserFolderMemory.Kind.MULTI_ANALYSIS ) );
    }
}
