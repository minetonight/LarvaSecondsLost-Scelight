package hu.aleks.larvasecondslostextmod;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.awt.GraphicsEnvironment;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFileChooser;
import javax.swing.JProgressBar;

import org.junit.Assume;
import org.junit.Test;

/** Running view text (AC-6.2) and the chooser configuration (AC-5.1, OQ-9) without a display server where possible. */
public class LarvaMultiAnalysisPageCompTest {

    @Test
    public void runningViewShowsKOfNFileProgressBarAndFailedCount() {
        Assume.assumeFalse( GraphicsEnvironment.isHeadless() );
        final List< Path > files = new ArrayList< Path >();
        files.add( Paths.get( "a.SC2Replay" ) );
        files.add( Paths.get( "b.SC2Replay" ) );
        final LarvaMultiAnalysisPageController controller = new LarvaMultiAnalysisPageController( files, true, new LarvaMultiPageLabels( true, 1 ),
                null, new LarvaMultiAnalysisPageController.LabelSink() {
                    @Override
                    public void setLabel( final String label ) {
                    }
                }, new StdErrLogger( false, false ) );
        final LarvaMultiAnalysisPageComp comp = new LarvaMultiAnalysisPageComp( controller, new JProgressBar() );
        assertEquals( "Preparing 2 replays...", comp.getStatusText() );
        comp.showProgress( 2, 2, "b.SC2Replay", 1 );
        assertEquals( "Analyzing 2 / 2: b.SC2Replay", comp.getStatusText() );
        assertEquals( "Failed: 1", comp.getFailedText() );
        assertEquals( 2, comp.getProgressValue() );
        assertEquals( 2, comp.getProgressMaximum() );
        comp.pageDisposed();
        assertTrue( controller.isDisposed() );
    }

    @Test
    public void multiChooserIsMultiSelectFilesOnlyAndFiltersReplays() {
        Assume.assumeFalse( GraphicsEnvironment.isHeadless() );
        final JFileChooser chooser = new JFileChooser();
        LarvaReplayFileChooser.configureMultiChooser( chooser );
        assertTrue( chooser.isMultiSelectionEnabled() );
        assertEquals( JFileChooser.FILES_ONLY, chooser.getFileSelectionMode() );
        assertTrue( chooser.getFileFilter().accept( new File( "game.SC2Replay" ) ) );
        assertTrue( chooser.getFileFilter().accept( new File( "game.sc2replay" ) ) );
        assertEquals( false, chooser.getFileFilter().accept( new File( "notes.txt" ) ) );
        assertEquals( "StarCraft II Replay (*.SC2Replay)", chooser.getFileFilter().getDescription() );
    }

    @Test
    public void chosenFilesAreKeptInOrderAndNonReplaysDropped() {
        final List< Path > paths = LarvaReplayFileChooser.replayPaths( new File[] { new File( "/r/b.SC2Replay" ), new File( "/r/x.txt" ),
                new File( "/r/a.sc2replay" ) } );
        assertEquals( 2, paths.size() );
        assertEquals( "b.SC2Replay", paths.get( 0 ).getFileName().toString() );
        assertEquals( "a.sc2replay", paths.get( 1 ).getFileName().toString() );
        assertTrue( LarvaReplayFileChooser.replayPaths( null ).isEmpty() );
    }
}
