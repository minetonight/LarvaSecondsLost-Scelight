package hu.aleks.larvasecondslostextmod;

/**
 * Immutable capability report describing whether native chart dropdown integration is available.
 */
public class ChartIntegrationCapability {

    /** Tells if native chart dropdown integration is supported. */
    private final boolean nativeDropdownSupported;

    /** Short title of the active integration mode. */
    private final String integrationModeTitle;

    /** Main explanation of the capability result. */
    private final String explanation;

    /** Technical evidence note. */
    private final String technicalEvidence;

    /** Recommended next path. */
    private final String recommendedPath;

    /**
     * Creates a new chart integration capability report.
     *
     * @param nativeDropdownSupported tells if native chart dropdown integration is supported
     * @param integrationModeTitle short title of the capability result
     * @param explanation main explanation
     * @param technicalEvidence technical evidence note
     * @param recommendedPath recommended path to keep using
     */
    public ChartIntegrationCapability( final boolean nativeDropdownSupported, final String integrationModeTitle, final String explanation,
            final String technicalEvidence, final String recommendedPath ) {
        this.nativeDropdownSupported = nativeDropdownSupported;
        this.integrationModeTitle = integrationModeTitle;
        this.explanation = explanation;
        this.technicalEvidence = technicalEvidence;
        this.recommendedPath = recommendedPath;
    }

    public boolean isNativeDropdownSupported() {
        return nativeDropdownSupported;
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