package hu.aleks.larvasecondslostextmod;

/**
 * Immutable page-level replay diagnostics model for the module-owned Larva page.
 *
 * <p>This wraps core replay metadata together with Larva-page-specific diagnostics such as the
 * fallback integration mode, normalized timeline presentation model, preview interval, and
 * larva-analysis report.</p>
 */
public class LarvaReplayPageSummary {

    /** Core replay metadata. */
    private final ReplaySummary replaySummary;

    /** Diagnostic note describing the fallback integration. */
    private final String integrationMode;

    /** Normalized timeline model consumed by the module-owned preview component. */
    private final LarvaTimelineModel timelineModel;

    /** Preview window start in milliseconds. */
    private final long previewWindowStartMs;

    /** Preview window end in milliseconds. */
    private final long previewWindowEndMs;

    /** Epic 6 larva analysis foundation report. */
    private final LarvaAnalysisReport larvaAnalysisReport;

    /**
     * Creates a new Larva page summary.
     *
     * @param replaySummary core replay metadata summary
     * @param integrationMode fallback integration mode description
     * @param timelineModel normalized timeline model for the preview component
     * @param previewWindowStartMs preview window start
     * @param previewWindowEndMs preview window end
     * @param larvaAnalysisReport larva analysis diagnostics; may be <code>null</code>
     */
    public LarvaReplayPageSummary( final ReplaySummary replaySummary, final String integrationMode, final LarvaTimelineModel timelineModel,
            final long previewWindowStartMs, final long previewWindowEndMs, final LarvaAnalysisReport larvaAnalysisReport ) {
        this.replaySummary = replaySummary;
        this.integrationMode = integrationMode;
        this.timelineModel = timelineModel;
        this.previewWindowStartMs = previewWindowStartMs;
        this.previewWindowEndMs = previewWindowEndMs;
        this.larvaAnalysisReport = larvaAnalysisReport;
    }

    public ReplaySummary getReplaySummary() {
        return replaySummary;
    }

    public String getIntegrationMode() {
        return integrationMode;
    }

    public LarvaTimelineModel getTimelineModel() {
        return timelineModel;
    }

    public long getPreviewWindowStartMs() {
        return previewWindowStartMs;
    }

    public long getPreviewWindowEndMs() {
        return previewWindowEndMs;
    }

    public LarvaAnalysisReport getLarvaAnalysisReport() {
        return larvaAnalysisReport;
    }

}