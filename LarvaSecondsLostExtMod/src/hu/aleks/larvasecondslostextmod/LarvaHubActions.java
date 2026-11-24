package hu.aleks.larvasecondslostextmod;

import java.awt.Component;

/**
 * The actions behind the three hub buttons. The hub component depends only on this interface; the page navigator
 * provides the implementation (placeholder until E17-S06 and E17-S14 land).
 */
interface LarvaHubActions {

    /** "Open Replay...": choose one replay and open it on its own sub-page. */
    void openReplay( Component parent );

    /** "Analyze Latest Replay": open the most recent replay on its own sub-page. */
    void analyzeLatestReplay();

    /** "Multi Analysis...": choose several replays and aggregate them. */
    void startMultiAnalysis( Component parent, boolean ladderOnly );

}
