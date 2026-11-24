package hu.aleks.larvasecondslostextmod;

/** What a {@link LarvaMultiAnalysisPageController} needs from the (lazily created) page component. EDT only. */
interface LarvaMultiAnalysisView {

    /** Shows the running view; <code>k</code> is 0 before the first file. */
    void showProgress( int k, int n, String fileName, int failedCount );

    /** Shows the finished analysis. */
    void showResult( LarvaMultiAnalysisViewModel viewModel );

    /** Shows why there is no result (cancelled or failed). */
    void showStopped( String message );
}
