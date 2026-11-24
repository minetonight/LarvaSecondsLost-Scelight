package hu.aleks.larvasecondslostextmod;

/**
 * Forwards at most one progress update per {@link #MIN_INTERVAL_MS} (S02: the worker can finish up to 200 small files per
 * second, which would flood the EDT). The newest suppressed update is kept and delivered by {@link #flush()}, so the final
 * state is always posted. Used on the worker thread only.
 */
final class LarvaThrottledProgressSink implements LarvaProgressSink {

    /** Minimum time between two forwarded updates (epic §9). */
    static final long MIN_INTERVAL_MS = 100L;

    private final LarvaProgressSink delegate;

    private final LarvaClock clock;

    private boolean postedBefore;

    private long lastPostMs;

    private boolean pending;

    private int pendingK;

    private int pendingN;

    private String pendingFileName;

    private int pendingFailed;

    LarvaThrottledProgressSink( final LarvaProgressSink delegate, final LarvaClock clock ) {
        this.delegate = delegate;
        this.clock = clock;
    }

    @Override
    public void onProgress( final int k, final int n, final String fileName, final int failedCount ) {
        final long now = clock.nowMillis();
        if ( !postedBefore || now < lastPostMs || now - lastPostMs >= MIN_INTERVAL_MS ) {
            post( now, k, n, fileName, failedCount );
            return;
        }
        pending = true;
        pendingK = k;
        pendingN = n;
        pendingFileName = fileName;
        pendingFailed = failedCount;
    }

    /** Delivers the newest suppressed update, if any. Call once when the run ends. */
    void flush() {
        if ( pending )
            post( clock.nowMillis(), pendingK, pendingN, pendingFileName, pendingFailed );
    }

    private void post( final long now, final int k, final int n, final String fileName, final int failedCount ) {
        postedBefore = true;
        lastPostMs = now;
        pending = false;
        delegate.onProgress( k, n, fileName, failedCount );
    }
}
