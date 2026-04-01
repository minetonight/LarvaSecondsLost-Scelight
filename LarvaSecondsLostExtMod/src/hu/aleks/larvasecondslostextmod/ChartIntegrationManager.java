package hu.aleks.larvasecondslostextmod;

import hu.scelightapibase.service.log.ILogger;

/**
 * Owns Epic 04 native chart-integration capability evaluation.
 *
 * <p>This isolates the public-API feasibility check from the rest of the module so the
 * module-owned Larva page remains the supported path regardless of whether native chart
 * registration ever becomes available in a future external module API.</p>
 */
public class ChartIntegrationManager {

    /** Detector that evaluates native chart registration support. */
    private final ChartIntegrationCapabilityDetector capabilityDetector;

    /** Last evaluated capability report. */
    private ChartIntegrationCapability capability;

    /**
     * Creates a new chart integration manager.
     *
     * @param capabilityDetector detector used to evaluate the runtime capability
     */
    public ChartIntegrationManager( final ChartIntegrationCapabilityDetector capabilityDetector ) {
        if ( capabilityDetector == null )
            throw new IllegalArgumentException( "Chart integration capability detector is required." );

        this.capabilityDetector = capabilityDetector;
    }

    /**
     * Evaluates chart integration capability and records the outcome in the log.
     *
     * @param logger logger used for lifecycle diagnostics
     * @return evaluated capability report
     */
    public ChartIntegrationCapability initialize( final ILogger logger ) {
        capability = capabilityDetector.detect();
        logCapability( logger, capability );
        return capability;
    }

    /**
     * Returns the last evaluated capability report.
     *
     * @return evaluated capability report; may be <code>null</code> before initialization
     */
    public ChartIntegrationCapability getCapability() {
        return capability;
    }

    /**
     * Logs the capability review result.
     *
     * @param logger logger used for diagnostics
     * @param capability capability report to log
     */
    private void logCapability( final ILogger logger, final ChartIntegrationCapability capability ) {
        if ( logger == null || capability == null )
            return;

        logger.debug( "Epic 4 native chart dropdown integration: "
                + ( capability.isNativeDropdownSupported() ? "supported" : "unsupported" ) );
        logger.debug( "Epic 4 public API review: " + capability.getPublicApiEvidence() );
        logger.debug( "Epic 4 internal chart wiring review: " + capability.getTechnicalEvidence() );
        logger.debug( "Epic 4 registration status: " + capability.getRegistrationStatus() );
    }

}