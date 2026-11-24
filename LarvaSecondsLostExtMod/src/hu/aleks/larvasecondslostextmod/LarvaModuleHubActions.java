package hu.aleks.larvasecondslostextmod;

import java.awt.Component;
import java.nio.file.Path;

/**
 * The behavior behind the hub buttons and the identical buttons on single-replay pages: Open Replay and Analyze Latest
 * create or focus a single-replay sub-page (E17-S06); Multi Analysis stays a placeholder until E17-S14.
 *
 * <p>All methods run on the EDT. The dev commands call the same methods as the buttons.</p>
 */
final class LarvaModuleHubActions implements LarvaHubActions {

    static final String SOURCE_MANUAL = "Manual replay selection";
    static final String SOURCE_LATEST = "Latest replay resolver";
    static final String SOURCE_REFRESH = "Larva replay page refresh";
    static final String NO_LATEST_MESSAGE = "No replay could be resolved from the replay monitor or monitored replay folders yet.";

    private final LarvaSecondsLostModule module;
    private final LarvaPageNavigator navigator;
    private final LarvaReplayFileChooser fileChooser;

    LarvaModuleHubActions( final LarvaSecondsLostModule module, final LarvaPageNavigator navigator, final LarvaReplayFileChooser fileChooser ) {
        this.module = module;
        this.navigator = navigator;
        this.fileChooser = fileChooser;
    }

    @Override
    public void openReplay( final Component parent ) {
        final Path replayFile = fileChooser.chooseReplay( parent, navigator.getSelectedReplayFile(), module.getLastDetectedReplayPath() );
        if ( replayFile != null )
            openChosenReplay( replayFile );
    }

    /** Opens a replay picked by the user (or by the dev command, which bypasses only the chooser). */
    LarvaReplayPageController openChosenReplay( final Path replayFile ) {
        return navigator.openSingleReplay( replayFile, SOURCE_MANUAL, true );
    }

    @Override
    public void analyzeLatestReplay() {
        final Path replayFile = module.resolveLatestReplayPath();
        if ( replayFile == null ) {
            module.guiUtils.showInfoMsg( NO_LATEST_MESSAGE );
            return;
        }
        openLatestReplay( replayFile );
    }

    /** Opens the resolved latest replay (AC-3.2 is handled by the caller: no replay, no page). */
    LarvaReplayPageController openLatestReplay( final Path replayFile ) {
        return navigator.openSingleReplay( replayFile, SOURCE_LATEST, true );
    }

    @Override
    public void startMultiAnalysis( final Component parent, final boolean ladderOnly ) {
        module.logger.info( "[larva-hub] action startMultiAnalysis ladderOnly=" + ladderOnly + " (not available yet)" );
        module.guiUtils.showInfoMsg( "Not available yet" );
    }

}
