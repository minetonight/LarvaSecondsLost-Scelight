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

    /** Technical evidence note. */
    private final String technicalEvidence;

    /** Recommended next path. */
    private final String recommendedPath;

    /**
     * Creates a new Base Control augmentation capability report.
     *
     * @param augmentationSupported tells if Base Control augmentation is supported
     * @param integrationModeTitle short title of the capability result
     * @param explanation main explanation
     * @param technicalEvidence technical evidence note
     * @param recommendedPath recommended path to keep using
     */
    public BaseControlAugmentationCapability( final boolean augmentationSupported, final String integrationModeTitle, final String explanation,
            final String technicalEvidence, final String recommendedPath ) {
        this.augmentationSupported = augmentationSupported;
        this.integrationModeTitle = integrationModeTitle;
        this.explanation = explanation;
        this.technicalEvidence = technicalEvidence;
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

    public String getTechnicalEvidence() {
        return technicalEvidence;
    }

    public String getRecommendedPath() {
        return recommendedPath;
    }

}