package hu.aleks.larvasecondslostextmod;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Test;

public class LarvaReplayPathsTest {

    @Test
    public void nodeNameStripsReplayExtensionCaseInsensitive() {
        assertEquals( "X", LarvaReplayPaths.nodeName( "X.SC2Replay" ) );
        assertEquals( "x", LarvaReplayPaths.nodeName( "x.sc2replay" ) );
        assertEquals( "x", LarvaReplayPaths.nodeName( "x.Sc2RePlAy" ) );
    }

    @Test
    public void nodeNameKeepsInnerDotsAndStripsOnlyTheTrailingExtension() {
        assertEquals( "a.b", LarvaReplayPaths.nodeName( "a.b.SC2Replay" ) );
        assertEquals( "2026-07-03- - (Z)Scorpion VS (T)fificent",
                LarvaReplayPaths.nodeName( "2026-07-03- - (Z)Scorpion VS (T)fificent.SC2Replay" ) );
    }

    @Test
    public void nodeNameLeavesOtherNamesAlone() {
        assertEquals( "game", LarvaReplayPaths.nodeName( "game" ) );
        assertEquals( "game.zip", LarvaReplayPaths.nodeName( "game.zip" ) );
        assertEquals( "game.SC2Replay.bak", LarvaReplayPaths.nodeName( "game.SC2Replay.bak" ) );
    }

    @Test
    public void nodeNameNeverReturnsAnEmptyLabel() {
        assertEquals( ".SC2Replay", LarvaReplayPaths.nodeName( ".SC2Replay" ) );
        assertEquals( "", LarvaReplayPaths.nodeName( "" ) );
    }

    @Test
    public void nodeNameOfPathUsesTheFileName() {
        assertEquals( "game", LarvaReplayPaths.nodeName( Paths.get( "/some/dir/game.SC2Replay" ) ) );
    }

    @Test
    public void normalizeMakesEquivalentPathsEqual() {
        final Path direct = LarvaReplayPaths.normalize( Paths.get( "/a/b/game.SC2Replay" ) );
        final Path indirect = LarvaReplayPaths.normalize( Paths.get( "/a/b/../b/./game.SC2Replay" ) );
        assertEquals( direct, indirect );
    }

    @Test
    public void normalizeMakesRelativePathsAbsolute() {
        assertTrue( LarvaReplayPaths.normalize( Paths.get( "game.SC2Replay" ) ).isAbsolute() );
    }

}
