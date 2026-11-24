package hu.aleks.larvasecondslostextmod;

/** Size guard of the multi analysis (E17-S14, OQ-14): above {@link #CONFIRM_THRESHOLD} files the user must confirm. */
final class LarvaMultiAnalysisEstimate {

    /** More selected files than this need a confirmation (OQ-14). */
    static final int CONFIRM_THRESHOLD = 500;

    /** Measured full-analysis cost per replay, S02 (about 17 ms mean, rounded up generously). */
    static final long ESTIMATED_MS_PER_REPLAY = 50L;

    /** Worker threads of the batch runner (S02: one is enough). */
    static final int WORKER_COUNT = 1;

    private static final long MS_PER_MINUTE = 60000L;

    private LarvaMultiAnalysisEstimate() {
    }

    /** @return <code>true</code> if this many files need the confirm dialog */
    static boolean needsConfirmation( final int fileCount ) {
        return fileCount > CONFIRM_THRESHOLD;
    }

    /** @return the estimated run time in whole minutes, rounded up, at least 1 */
    static long estimatedMinutes( final int fileCount ) {
        final long totalMs = fileCount * ESTIMATED_MS_PER_REPLAY / WORKER_COUNT;
        return Math.max( 1L, ( totalMs + MS_PER_MINUTE - 1L ) / MS_PER_MINUTE );
    }

    /** @return the text of the confirm dialog, e.g. {@code Analyze 501 replays? Estimated time: ~1 min.} */
    static String confirmMessage( final int fileCount ) {
        return "Analyze " + fileCount + " replays? Estimated time: ~" + estimatedMinutes( fileCount ) + " min.";
    }
}
