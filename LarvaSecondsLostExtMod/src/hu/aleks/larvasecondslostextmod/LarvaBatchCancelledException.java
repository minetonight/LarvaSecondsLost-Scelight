package hu.aleks.larvasecondslostextmod;

/** Thrown by {@link LarvaMultiAnalysisPipeline#run} when the token was cancelled; no result exists then. */
final class LarvaBatchCancelledException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int processed;

    private final int total;

    LarvaBatchCancelledException( final int processed, final int total ) {
        super( "cancelled after " + processed + "/" + total );
        this.processed = processed;
        this.total = total;
    }

    /** @return files fully processed before the cancellation */
    int getProcessed() {
        return processed;
    }

    /** @return files selected */
    int getTotal() {
        return total;
    }
}
