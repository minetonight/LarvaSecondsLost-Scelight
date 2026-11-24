package hu.aleks.larvasecondslostextmod;

import hu.scelightapi.service.IGuiFactory;
import hu.scelightapibase.gui.comp.IFileChooser;

import java.awt.Component;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * File choosers of the hub and the replay pages (single and multi selection). Each remembers the folder of its last confirmed selection (CR-1.5). EDT only.
 */
final class LarvaReplayFileChooser {

    private static final String REPLAY_SUFFIX = ".sc2replay";

    private final IGuiFactory guiFactory;
    private final LarvaChooserFolderMemory folderMemory;

    LarvaReplayFileChooser( final IGuiFactory guiFactory, final LarvaChooserFolderMemory folderMemory ) {
        this.guiFactory = guiFactory;
        this.folderMemory = folderMemory;
    }

    /**
     * Creates the single-replay chooser, not shown yet (so a dev command can inspect it). Start folder: the remembered folder
     * of {@code Open Replay...} if it still exists, else the folder of the last detected replay, else Scelight's default.
     *
     * @param lastDetectedReplayPath last replay reported by the monitor; may be <code>null</code>
     */
    IFileChooser newReplayChooser( final Path lastDetectedReplayPath ) {
        final IFileChooser fileChooser = guiFactory.newFileChooser();
        fileChooser.asFileChooser().setFileSelectionMode( JFileChooser.FILES_ONLY );
        fileChooser.asFileChooser().setFileFilter( replayFilter() );
        applyStartFolder( fileChooser, LarvaChooserFolderMemory.Kind.OPEN_REPLAY, lastDetectedReplayPath );
        return fileChooser;
    }

    /**
     * Shows the single-replay chooser and remembers the folder of a confirmed selection (CR-1.5).
     *
     * @param parent dialog parent
     * @param lastDetectedReplayPath last replay reported by the monitor; may be <code>null</code>
     * @return the chosen replay, or <code>null</code> if the user cancelled (the memory is untouched)
     */
    Path chooseReplay( final Component parent, final Path lastDetectedReplayPath ) {
        final IFileChooser fileChooser = newReplayChooser( lastDetectedReplayPath );
        if ( JFileChooser.APPROVE_OPTION != fileChooser.asFileChooser().showOpenDialog( parent ) )
            return null;
        final Path selected = fileChooser.getSelectedPath();
        rememberSelection( LarvaChooserFolderMemory.Kind.OPEN_REPLAY, selected );
        return selected;
    }

    /**
     * Creates the multi-selection chooser (E17-S14, FR-5, OQ-9): multi-select, files only, filter {@code *.SC2Replay}.
     * Start folder as in {@link #newReplayChooser(Path)} but with its own memory. Not shown yet, so it can be inspected headlessly.
     *
     * @param lastDetectedReplayPath last replay reported by the monitor; may be <code>null</code>
     */
    IFileChooser newMultiChooser( final Path lastDetectedReplayPath ) {
        final IFileChooser fileChooser = guiFactory.newFileChooser();
        configureMultiChooser( fileChooser.asFileChooser() );
        applyStartFolder( fileChooser, LarvaChooserFolderMemory.Kind.MULTI_ANALYSIS, lastDetectedReplayPath );
        return fileChooser;
    }

    private void applyStartFolder( final IFileChooser fileChooser, final LarvaChooserFolderMemory.Kind kind, final Path lastDetectedReplayPath ) {
        final Path start = folderMemory.resolveStartFolder( kind, lastDetectedReplayPath );
        if ( start != null )
            fileChooser.setCurrentFolder( start );
    }

    /**
     * The one place a confirmed selection reaches the memory; the dev command {@code chooser-folder-confirm} calls it too.
     *
     * @param kind which chooser was confirmed
     * @param selectedFile a selected file (its folder is remembered); null is ignored
     */
    void rememberSelection( final LarvaChooserFolderMemory.Kind kind, final Path selectedFile ) {
        folderMemory.remember( kind, selectedFile );
    }

    /** @return the folder memory (dev command {@code chooser-folder-state}) */
    LarvaChooserFolderMemory getFolderMemory() {
        return folderMemory;
    }

    /** Applies the multi-selection settings to a chooser. */
    static void configureMultiChooser( final JFileChooser chooser ) {
        chooser.setMultiSelectionEnabled( true );
        chooser.setFileSelectionMode( JFileChooser.FILES_ONLY );
        chooser.setFileFilter( replayFilter() );
    }

    /**
     * Shows the multi-selection chooser.
     *
     * @param parent dialog parent
     * @param lastDetectedReplayPath last replay reported by the monitor; may be <code>null</code>
     * @return the chosen replay files in chooser order; empty if the user cancelled or chose no replay (AC-5.2)
     */
    List< Path > chooseReplays( final Component parent, final Path lastDetectedReplayPath ) {
        final JFileChooser chooser = newMultiChooser( lastDetectedReplayPath ).asFileChooser();
        if ( JFileChooser.APPROVE_OPTION != chooser.showOpenDialog( parent ) )
            return new ArrayList< Path >();
        final List< Path > paths = replayPaths( chooser.getSelectedFiles() );
        if ( !paths.isEmpty() )
            rememberSelection( LarvaChooserFolderMemory.Kind.MULTI_ANALYSIS, paths.get( 0 ) );
        return paths;
    }

    /** @return the selected files that really are replays (a typed name can bypass the filter), as absolute paths, in order */
    static List< Path > replayPaths( final File[] selected ) {
        final List< Path > paths = new ArrayList< Path >();
        if ( selected == null )
            return paths;
        for ( final File file : selected )
            if ( file != null && file.getName().toLowerCase( Locale.ROOT ).endsWith( REPLAY_SUFFIX ) )
                paths.add( file.toPath().toAbsolutePath() );
        return paths;
    }

    private static FileFilter replayFilter() {
        return new FileNameExtensionFilter( "StarCraft II Replay (*.SC2Replay)", "SC2Replay", "sc2replay" );
    }

}
