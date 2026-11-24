package hu.aleks.larvasecondslostextmod;

import hu.scelightapi.service.IGuiFactory;
import hu.scelightapibase.gui.comp.IFileChooser;

import java.awt.Component;
import java.nio.file.Path;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * Single-replay file chooser shared by the hub and the replay pages (and, later, the multi analysis). EDT only.
 */
final class LarvaReplayFileChooser {

    private final IGuiFactory guiFactory;

    LarvaReplayFileChooser( final IGuiFactory guiFactory ) {
        this.guiFactory = guiFactory;
    }

    /**
     * Shows the chooser. Start folder: the folder of the replay of the current page; without one, the folder of the
     * last replay detected by the replay folder monitor; without that, Scelight's default.
     *
     * @param parent dialog parent
     * @param currentReplayFile replay of the currently selected single-replay page; may be <code>null</code>
     * @param lastDetectedReplayPath last replay reported by the monitor; may be <code>null</code>
     * @return the chosen replay, or <code>null</code> if the user cancelled
     */
    Path chooseReplay( final Component parent, final Path currentReplayFile, final Path lastDetectedReplayPath ) {
        final IFileChooser fileChooser = currentReplayFile == null ? guiFactory.newFileChooser() : guiFactory.newFileChooser( currentReplayFile.getParent() );
        fileChooser.asFileChooser().setFileSelectionMode( JFileChooser.FILES_ONLY );
        fileChooser.asFileChooser().setFileFilter( new FileNameExtensionFilter( "StarCraft II Replay (*.SC2Replay)", "SC2Replay", "sc2replay" ) );

        if ( currentReplayFile == null && lastDetectedReplayPath != null && lastDetectedReplayPath.getParent() != null )
            fileChooser.setCurrentFolder( lastDetectedReplayPath.getParent() );

        if ( JFileChooser.APPROVE_OPTION != fileChooser.asFileChooser().showOpenDialog( parent ) )
            return null;
        return fileChooser.getSelectedPath();
    }

}
