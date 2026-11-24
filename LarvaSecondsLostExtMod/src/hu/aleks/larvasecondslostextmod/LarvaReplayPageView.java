package hu.aleks.larvasecondslostextmod;

import java.nio.file.Path;

/**
 * What a {@link LarvaReplayPageController} needs from the (lazily created) page component. All methods are called
 * on the EDT only.
 */
interface LarvaReplayPageView {

    /** Shows the busy state while the replay is analyzed. */
    void showBusy( Path replayFile, String sourceDescription );

    /** Shows the finished analysis. */
    void showSummary( LarvaReplayPageSummary summary );

    /** Shows the error state of a failed analysis. */
    void showError( Path replayFile, String sourceDescription, String errorMessage );

}
