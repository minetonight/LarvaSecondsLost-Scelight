package hu.aleks.larvasecondslostextmod;

/** Cooperative cancellation flag of one batch run (E17-S12). Set from any thread, checked by the worker before each file. */
final class LarvaCancellationToken {

    private volatile boolean cancelled;

    void cancel() {
        cancelled = true;
    }

    boolean isCancelled() {
        return cancelled;
    }
}
