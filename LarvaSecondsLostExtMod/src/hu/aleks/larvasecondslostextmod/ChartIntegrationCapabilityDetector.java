package hu.aleks.larvasecondslostextmod;

/**
 * Determines whether the public Scelight external module API exposes native chart dropdown integration.
 */
public class ChartIntegrationCapabilityDetector {

    /** Title used when native integration is not available. */
    private static final String FALLBACK_TITLE = "Fallback timeline remains the supported path";

    /**
     * Evaluates chart integration capability for the current runtime.
     *
     * @return capability report
     */
    public ChartIntegrationCapability detect() {
        return new ChartIntegrationCapability( false, FALLBACK_TITLE,
                "The public external module API exposes page-level UI hooks, but it does not expose a chart registration API for Scelight's native replay chart dropdown.",
                "Scelight's native chart selector is built from the internal enum hu.scelight.gui.page.repanalyzer.charts.ChartType and internal ChartsComp factory wiring. Those internals are not part of the external module API.",
                "Keep using the module-owned Larva page and timeline preview as the supported integration path until real larva windows are ready." );
    }

}