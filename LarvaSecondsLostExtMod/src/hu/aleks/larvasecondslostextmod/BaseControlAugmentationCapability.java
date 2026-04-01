package hu.aleks.larvasecondslostextmod;

/**
 * Immutable capability report describing whether the native Base Control chart can be augmented.
 */
public class BaseControlAugmentationCapability {

    /** Tells if Base Control augmentation is supported. */
    private final boolean augmentationSupported;

    /** Short title of the active integration mode. */
    private final String integrationModeTitle;

    /** Main explanation of the capability result. */
    private final String explanation;

    /** Evidence collected from the public external module API surface review. */
    private final String publicApiEvidence;

    /** Technical evidence note. */
    private final String technicalEvidence;

    /** Runtime augmentation status derived from the capability result. */
    private final String augmentationStatus;

    /** Recommended next path. */
    private final String recommendedPath;

    /**
     * Creates a new Base Control augmentation capability report.
     *
     * @param augmentationSupported tells if Base Control augmentation is supported
     * @param integrationModeTitle short title of the capability result
     * @param explanation main explanation
     * @param publicApiEvidence evidence from the public API surface review
     * @param technicalEvidence technical evidence note
     * @param augmentationStatus runtime augmentation status
     * @param recommendedPath recommended path to keep using
     */
    public BaseControlAugmentationCapability( final boolean augmentationSupported, final String integrationModeTitle, final String explanation,
            final String publicApiEvidence, final String technicalEvidence, final String augmentationStatus, final String recommendedPath ) {
        this.augmentationSupported = augmentationSupported;
        this.integrationModeTitle = integrationModeTitle;
        this.explanation = explanation;
        this.publicApiEvidence = publicApiEvidence;
        this.technicalEvidence = technicalEvidence;
        this.augmentationStatus = augmentationStatus;
        this.recommendedPath = recommendedPath;
    }

    public boolean isAugmentationSupported() {
        return augmentationSupported;
    }

    public String getIntegrationModeTitle() {
        return integrationModeTitle;
    }

    public String getExplanation() {
        return explanation;
    }

    public String getPublicApiEvidence() {
        return publicApiEvidence;
    }

    public String getTechnicalEvidence() {
        return technicalEvidence;
    }

    public String getAugmentationStatus() {
        return augmentationStatus;
    }

    public String getRecommendedPath() {
        return recommendedPath;
    }

}