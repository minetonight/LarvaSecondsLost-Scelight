package hu.aleks.larvasecondslostextmod;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

public class LarvaReplayPageControllerTest {

    private static final Path FILE = Paths.get( "/replays/x.SC2Replay" ).toAbsolutePath();
    private static final LarvaReplayPageSummary SUMMARY = new LarvaReplayPageSummary( null, "mode", null, 0L, 0L, null );

    /** Records submissions; the test delivers results by hand. */
    private static final class FakeQueue implements LarvaReplayAnalysisQueue {
        final List< String > sources = new ArrayList< String >();
        final List< Long > ids = new ArrayList< Long >();

        @Override
        public void submit( final LarvaReplayPageController controller, final String sourceDescription, final long requestId ) {
            sources.add( sourceDescription );
            ids.add( Long.valueOf( requestId ) );
        }

        long lastId() {
            return ids.get( ids.size() - 1 ).longValue();
        }
    }

    private static final class FakeView implements LarvaReplayPageView {
        final List< String > calls = new ArrayList< String >();

        @Override
        public void showBusy( final Path replayFile, final String sourceDescription ) {
            calls.add( "busy:" + sourceDescription );
        }

        @Override
        public void showSummary( final LarvaReplayPageSummary summary ) {
            calls.add( "summary" );
        }

        @Override
        public void showError( final Path replayFile, final String sourceDescription, final String errorMessage ) {
            calls.add( "error:" + errorMessage );
        }
    }

    private FakeQueue queue;
    private LarvaReplayPageController controller;

    @Before
    public void setUp() {
        queue = new FakeQueue();
        controller = new LarvaReplayPageController( FILE, queue, new StdErrLogger( false, false ) );
    }

    @Test
    public void startsBusyAndBoundToItsReplay() {
        assertEquals( LarvaReplayPageController.State.BUSY, controller.getState() );
        assertEquals( FILE, controller.getReplayFile() );
    }

    @Test
    public void requestSubmitsOnceWithTheSourceDescription() {
        controller.requestAnalysis( "Manual replay selection" );
        assertEquals( 1, queue.sources.size() );
        assertEquals( "Manual replay selection", queue.sources.get( 0 ) );
    }

    @Test
    public void busyToReady() {
        controller.requestAnalysis( "s" );
        controller.onAnalysisSucceeded( queue.lastId(), SUMMARY );
        assertEquals( LarvaReplayPageController.State.READY, controller.getState() );
        assertSame( SUMMARY, controller.getSummary() );
    }

    @Test
    public void busyToError() {
        controller.requestAnalysis( "s" );
        controller.onAnalysisFailed( queue.lastId(), "corrupt" );
        assertEquals( LarvaReplayPageController.State.ERROR, controller.getState() );
        assertEquals( "corrupt", controller.getErrorMessage() );
        assertNull( controller.getSummary() );
    }

    @Test
    public void resultAfterDisposeIsIgnored() {
        controller.requestAnalysis( "s" );
        final FakeView view = new FakeView();
        controller.attach( view );
        view.calls.clear();
        controller.dispose();
        controller.onAnalysisSucceeded( queue.lastId(), SUMMARY );
        controller.onAnalysisFailed( queue.lastId(), "late" );
        assertTrue( controller.isDisposed() );
        assertEquals( LarvaReplayPageController.State.BUSY, controller.getState() );
        assertTrue( "no component update after dispose", view.calls.isEmpty() );
        assertNull( controller.getView() );
    }

    @Test
    public void requestAfterDisposeSubmitsNothing() {
        controller.dispose();
        controller.requestAnalysis( "s" );
        assertTrue( queue.sources.isEmpty() );
    }

    @Test
    public void disposeTwiceIsHarmless() {
        controller.dispose();
        controller.dispose();
        assertTrue( controller.isDisposed() );
    }

    @Test
    public void refreshInPlaceShowsBusyAgainAndOnlyTheNewResultCounts() {
        final FakeView view = new FakeView();
        controller.attach( view );
        controller.requestAnalysis( "first" );
        final long firstId = queue.lastId();
        controller.onAnalysisSucceeded( firstId, SUMMARY );
        controller.requestAnalysis( "Larva replay page refresh" );
        assertEquals( LarvaReplayPageController.State.BUSY, controller.getState() );
        assertNull( controller.getSummary() );
        assertEquals( FILE, controller.getReplayFile() );
        assertEquals( "busy:Larva replay page refresh", view.calls.get( view.calls.size() - 1 ) );
        controller.onAnalysisSucceeded( queue.lastId(), SUMMARY );
        assertEquals( LarvaReplayPageController.State.READY, controller.getState() );
    }

    @Test
    public void supersededResultIsIgnored() {
        controller.requestAnalysis( "first" );
        final long firstId = queue.lastId();
        controller.requestAnalysis( "second" );
        controller.onAnalysisSucceeded( firstId, SUMMARY );
        assertEquals( LarvaReplayPageController.State.BUSY, controller.getState() );
        controller.onAnalysisFailed( queue.lastId(), "boom" );
        assertEquals( LarvaReplayPageController.State.ERROR, controller.getState() );
    }

    @Test
    public void componentCreatedAfterTheResultRendersTheFinishedState() {
        controller.requestAnalysis( "s" );
        controller.onAnalysisSucceeded( queue.lastId(), SUMMARY );
        final FakeView late = new FakeView();
        controller.attach( late );
        assertEquals( 1, late.calls.size() );
        assertEquals( "summary", late.calls.get( 0 ) );
    }

    @Test
    public void componentCreatedAfterAFailureRendersTheError() {
        controller.requestAnalysis( "s" );
        controller.onAnalysisFailed( queue.lastId(), "corrupt" );
        final FakeView late = new FakeView();
        controller.attach( late );
        assertEquals( "error:corrupt", late.calls.get( 0 ) );
    }

    @Test
    public void componentCreatedWhileBusyRendersBusyThenGetsTheResult() {
        controller.requestAnalysis( "src" );
        final FakeView view = new FakeView();
        controller.attach( view );
        assertEquals( "busy:src", view.calls.get( 0 ) );
        controller.onAnalysisSucceeded( queue.lastId(), SUMMARY );
        assertEquals( "summary", view.calls.get( 1 ) );
    }

    @Test
    public void attachAfterDisposeDoesNothing() {
        controller.dispose();
        final FakeView view = new FakeView();
        controller.attach( view );
        assertTrue( view.calls.isEmpty() );
        assertFalse( view == controller.getView() );
    }

}
