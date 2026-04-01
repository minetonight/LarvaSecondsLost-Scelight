package hu.aleks.larvasecondslostextmod;

/**
 * Determines whether the public Scelight external module API exposes native chart dropdown integration.
 */
public class ChartIntegrationCapabilityDetector {

    /** Title used when native integration is not available. */
    private static final String FALLBACK_TITLE = "Fallback timeline remains the supported path";

    /** Public API review result. */
    private static final String PUBLIC_API_EVIDENCE = "The public external module SDK exposes module lifecycle, page registration, replay parsing, replay processing, replay-folder monitoring, settings, and general utilities, but it exposes no chart registry, chart provider, or replay-chart contribution interface.";

    /** Runtime registration result when native chart integration is unavailable. */
    private static final String UNSUPPORTED_REGISTRATION_STATUS = "No native `larva` chart entry was registered because the public external module API exposes no supported hook for Scelight's built-in chart dropdown.";

    /**
     * Evaluates chart integration capability for the current runtime.
     *
     * @return capability report
     */
    public ChartIntegrationCapability detect() {
        return new ChartIntegrationCapability( false, FALLBACK_TITLE,
                "The public external module API exposes page-level UI hooks, but it does not expose a chart registration API for Scelight's native replay chart dropdown.",
                PUBLIC_API_EVIDENCE,
                "Scelight's native chart selector is built from the internal enum hu.scelight.gui.page.repanalyzer.charts.ChartType and internal ChartsComp factory wiring. Those internals are not part of the external module API.",
                UNSUPPORTED_REGISTRATION_STATUS,
                "Keep using the module-owned Larva page and timeline preview as the supported integration path until real larva windows are ready." );
    }

}