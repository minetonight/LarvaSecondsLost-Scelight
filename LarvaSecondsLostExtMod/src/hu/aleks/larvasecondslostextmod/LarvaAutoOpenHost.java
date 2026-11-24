package hu.aleks.larvasecondslostextmod;

import java.nio.file.Path;

/** What the analysis queue needs from the module to auto-open pages. Both methods run on the EDT. */
interface LarvaAutoOpenHost {

    /** Ready, dedupe and cap checks for a replay (see {@link LarvaAutoOpenPolicy#admit}). */
    LarvaAutoOpenPolicy.Admission admit( Path replayFile );

    /**
     * Re-checks admission (the user may have opened the replay meanwhile) and, if still {@link LarvaAutoOpenPolicy.Admission#OPEN},
     * adds the page with the outcome's result, selected only by the focus rule.
     *
     * @return the new page's controller, or <code>null</code> if no page was created
     */
    LarvaReplayPageController open( Path replayFile, LarvaAutoOpenOutcome outcome );

}
