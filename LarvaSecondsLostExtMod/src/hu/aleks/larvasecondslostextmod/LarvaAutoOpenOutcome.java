package hu.aleks.larvasecondslostextmod;

/** Result of the worker-side part of auto-open: Zerg pre-check plus (if it passed) the full analysis, in one parse. */
final class LarvaAutoOpenOutcome {

    enum Kind {
        /** No Zerg player: no page (D17-a). */
        NOT_ZERG,
        /** The replay could not be loaded for the pre-check: no page (D17-j). */
        PRECHECK_FAILED,
        /** Zerg replay analyzed, summary available. */
        ANALYZED,
        /** Zerg replay, but the analysis failed: the page shows the error state (D17-j, AC-3.3). */
        ANALYSIS_FAILED
    }

    private final Kind kind;
    private final LarvaReplayPageSummary summary;
    private final String message;

    private LarvaAutoOpenOutcome( final Kind kind, final LarvaReplayPageSummary summary, final String message ) {
        this.kind = kind;
        this.summary = summary;
        this.message = message;
    }

    static LarvaAutoOpenOutcome notZerg() {
        return new LarvaAutoOpenOutcome( Kind.NOT_ZERG, null, null );
    }

    static LarvaAutoOpenOutcome precheckFailed( final String message ) {
        return new LarvaAutoOpenOutcome( Kind.PRECHECK_FAILED, null, message );
    }

    static LarvaAutoOpenOutcome analyzed( final LarvaReplayPageSummary summary ) {
        return new LarvaAutoOpenOutcome( Kind.ANALYZED, summary, null );
    }

    static LarvaAutoOpenOutcome analysisFailed( final String message ) {
        return new LarvaAutoOpenOutcome( Kind.ANALYSIS_FAILED, null, message );
    }

    Kind getKind() {
        return kind;
    }

    /** @return the summary for {@link Kind#ANALYZED}, else <code>null</code> */
    LarvaReplayPageSummary getSummary() {
        return summary;
    }

    /** @return the error text for the failure kinds, else <code>null</code> */
    String getMessage() {
        return message;
    }

}
