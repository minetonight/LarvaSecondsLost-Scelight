package hu.aleks.larvasecondslostextmod;

import java.awt.Component;
import java.nio.file.Path;
import java.util.List;

/**
 * The behavior behind the hub buttons and the identical buttons on single-replay pages: Open Replay and Analyze Latest
 * create or focus a single-replay sub-page (E17-S06); Multi Analysis chooses files, confirms big selections and opens a new multi page (E17-S14).
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
        final Path replayFile = fileChooser.chooseReplay( parent, module.getLastDetectedReplayPath() );
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

    /** Yes/no question to the user; the seam that lets the dev channel answer the confirm dialog (E17-S14). */
    interface Confirmer {
        boolean confirm( String message );
    }

    /** The real confirm dialog. */
    private final Confirmer dialogConfirmer = new Confirmer() {
        @Override
        public boolean confirm( final String message ) {
            return module.guiUtils.confirm( message );
        }
    };

    @Override
    public void startMultiAnalysis( final Component parent, final boolean ladderOnly ) {
        final List< Path > files = fileChooser.chooseReplays( parent, module.getLastDetectedReplayPath() );
        if ( files.isEmpty() ) {
            module.logger.debug( "[larva-hub] multi analysis: nothing chosen, no page" );
            return;
        }
        openMultiAnalysisPage( files, ladderOnly, dialogConfirmer );
    }

    /**
     * Everything the Multi Analysis button does after the chooser: asks for confirmation above
     * {@link LarvaMultiAnalysisEstimate#CONFIRM_THRESHOLD} files (OQ-14), then always opens a new page (FR-6).
     *
     * @param files the chosen replays
     * @param ladderOnly the checkbox value at click time
     * @param confirmer answers the confirm question
     * @return the new page's controller; <code>null</code> if declined, empty or the hub is not ready
     */
    LarvaMultiAnalysisPageController openMultiAnalysisPage( final List< Path > files, final boolean ladderOnly, final Confirmer confirmer ) {
        if ( files.isEmpty() )
            return null;
        if ( LarvaMultiAnalysisEstimate.needsConfirmation( files.size() )
                && !confirmer.confirm( LarvaMultiAnalysisEstimate.confirmMessage( files.size() ) ) ) {
            module.logger.info( "[larva-hub] multi analysis of " + files.size() + " replays declined, no page" );
            return null;
        }
        module.logger.info( "[larva-hub] multi analysis requested: " + files.size() + " replays, ladderOnly=" + ladderOnly );
        return navigator.openMultiAnalysisPage( files, ladderOnly );
    }

    /** @return the chooser factory (dev command {@code multi-chooser-config}) */
    LarvaReplayFileChooser getFileChooser() {
        return fileChooser;
    }

}
