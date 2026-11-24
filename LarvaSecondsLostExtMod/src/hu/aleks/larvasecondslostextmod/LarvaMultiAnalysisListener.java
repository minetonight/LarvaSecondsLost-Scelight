package hu.aleks.larvasecondslostextmod;

/** Callbacks of a {@link LarvaMultiAnalysisRunner}. All methods are called on the EDT (E17-S12). */
interface LarvaMultiAnalysisListener {

    /**
     * @param k 1-based index of the file being analyzed
     * @param n number of selected files
     * @param fileName name of that file
     * @param failedCount files that failed to parse so far
     */
    void onProgress( int k, int n, String fileName, int failedCount );

    /**
     * @param result the aggregated result
     * @param viewModel the display model, built on the worker thread so the EDT only paints it (E17-S15)
     */
    void onCompleted( LarvaAggregationResult result, LarvaMultiAnalysisViewModel viewModel );

    /** The run was cancelled; no result exists. */
    void onCancelled();

    /** @param error unexpected failure of the whole run (a bad single replay never ends here) */
    void onFailed( Throwable error );
}
