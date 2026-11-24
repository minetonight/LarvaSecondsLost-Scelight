package hu.aleks.larvasecondslostextmod;

import java.nio.file.Path;

/** Runs the replay analysis; {@link ReplaySummaryService#analyze} in production. Called on the queue worker only. */
interface LarvaReplayAnalysisRunner {

    LarvaReplayPageSummary analyze( Path replayFile, String sourceDescription );

    /**
     * Auto-open path (E17-S07): loads the replay once, checks for a Zerg player and, if there is one, analyzes the
     * already loaded replay (no second parse). Never throws for bad replays; reports them in the outcome.
     */
    LarvaAutoOpenOutcome analyzeForAutoOpen( Path replayFile, String sourceDescription );

}
