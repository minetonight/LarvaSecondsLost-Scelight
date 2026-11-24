package hu.aleks.larvasecondslostextmod;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import hu.scelightapibase.gui.comp.multipage.IPage;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

/** OQ-6 dedupe lookup over a synthetic child list. */
public class LarvaSingleReplayLookupTest {

    private final List< IPage< ? > > pages = new ArrayList< IPage< ? > >();
    private LarvaSubPage pageA;
    private LarvaSubPage pageB;
    private LarvaSubPage multiPage;

    private static LarvaSubPage single( final String file ) {
        final LarvaSubPage page = new LarvaSubPage( LarvaSubPageKind.SINGLE_REPLAY, LarvaReplayPaths.nodeName( file ), null, null );
        page.setReplayController( new LarvaReplayPageController( LarvaReplayPaths.normalize( Paths.get( file ) ), null, new StdErrLogger() ) );
        return page;
    }

    @Before
    public void setUp() {
        pageA = single( "/r/a.SC2Replay" );
        pageB = single( "/r/b.SC2Replay" );
        multiPage = new LarvaSubPage( LarvaSubPageKind.MULTI_ANALYSIS, "Multi", null, null );
        pages.add( pageA );
        pages.add( multiPage );
        pages.add( pageB );
    }

    private LarvaSubPage find( final String file ) {
        return LarvaHubPage.findSingleReplayPage( pages, LarvaReplayPaths.normalize( Paths.get( file ) ) );
    }

    @Test
    public void findsTheOpenPageOfAReplay() {
        assertSame( pageA, find( "/r/a.SC2Replay" ) );
        assertSame( pageB, find( "/r/b.SC2Replay" ) );
    }

    @Test
    public void equivalentPathFormsHitTheSamePage() {
        assertSame( pageA, find( "/r/x/../a.SC2Replay" ) );
        assertSame( pageB, find( "/r/./b.SC2Replay" ) );
    }

    @Test
    public void unknownReplayAndSameNameInOtherFolderMiss() {
        assertNull( find( "/r/c.SC2Replay" ) );
        assertNull( find( "/other/a.SC2Replay" ) );
    }

    @Test
    public void multiPagesAndEmptyListsNeverMatch() {
        final Path any = LarvaReplayPaths.normalize( Paths.get( "/r/a.SC2Replay" ) );
        assertNull( LarvaHubPage.findSingleReplayPage( new ArrayList< IPage< ? > >(), any ) );
        pages.remove( pageA );
        assertNull( find( "/r/a.SC2Replay" ) );
    }

}
