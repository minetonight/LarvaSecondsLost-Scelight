package hu.aleks.larvasecondslostextmod;

import hu.scelightapibase.service.log.ILogger;

/**
 * Owns Epic 05 native Base Control augmentation capability evaluation.
 *
 * <p>This isolates Base Control feasibility checks from the replay-page fallback so the
 * supported Larva timeline remains stable even when native augmentation is unavailable.</p>
 */
public class BaseControlAugmentationManager {

    /** Detector that evaluates native Base Control augmentation support. */
    private final BaseControlAugmentationCapabilityDetector capabilityDetector;

    /** Last evaluated capability report. */
    private BaseControlAugmentationCapability capability;

    /**
     * Creates a new Base Control augmentation manager.
     *
     * @param capabilityDetector detector used to evaluate the runtime capability
     */
    public BaseControlAugmentationManager( final BaseControlAugmentationCapabilityDetector capabilityDetector ) {
        if ( capabilityDetector == null )
            throw new IllegalArgumentException( "Base Control augmentation capability detector is required." );

        this.capabilityDetector = capabilityDetector;
    }

    /**
     * Evaluates Base Control augmentation capability and records the outcome in the log.
     *
     * @param logger logger used for lifecycle diagnostics
     * @return evaluated capability report
     */
    public BaseControlAugmentationCapability initialize( final ILogger logger ) {
        capability = capabilityDetector.detect();
        logCapability( logger, capability );
        return capability;
    }

    /**
     * Returns the last evaluated capability report.
     *
     * @return evaluated capability report; may be <code>null</code> before initialization
     */
    public BaseControlAugmentationCapability getCapability() {
        return capability;
    }

    /**
     * Logs the capability review result.
     *
     * @param logger logger used for diagnostics
     * @param capability capability report to log
     */
    private void logCapability( final ILogger logger, final BaseControlAugmentationCapability capability ) {
        if ( logger == null || capability == null )
            return;

        logger.debug( "Epic 5 native Base Control augmentation: "
                + ( capability.isAugmentationSupported() ? "supported" : "unsupported" ) );
        logger.debug( "Epic 5 public API review: " + capability.getPublicApiEvidence() );
        logger.debug( "Epic 5 internal Base Control wiring review: " + capability.getTechnicalEvidence() );
        logger.debug( "Epic 5 augmentation status: " + capability.getAugmentationStatus() );
    }

}