package hu.aleks.larvasecondslostextmod;

/** Receives the progress of a batch run on the worker thread (E17-S12). */
interface LarvaProgressSink {

    /**
     * Called before each file is analyzed.
     *
     * @param k 1-based index of the file being analyzed
     * @param n number of selected files
     * @param fileName name of that file (no folder)
     * @param failedCount files that failed to parse so far
     */
    void onProgress( int k, int n, String fileName, int failedCount );
}
