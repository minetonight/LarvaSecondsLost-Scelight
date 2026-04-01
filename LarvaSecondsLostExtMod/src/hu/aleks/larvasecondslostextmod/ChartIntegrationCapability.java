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

    /** Evidence collected from the public external module API surface review. */
    private final String publicApiEvidence;

    /** Technical evidence note. */
    private final String technicalEvidence;

    /** Runtime registration status derived from the capability result. */
    private final String registrationStatus;

    /** Recommended next path. */
    private final String recommendedPath;

    /**
     * Creates a new chart integration capability report.
     *
     * @param nativeDropdownSupported tells if native chart dropdown integration is supported
     * @param integrationModeTitle short title of the capability result
     * @param explanation main explanation
     * @param publicApiEvidence evidence from the public API surface review
     * @param technicalEvidence technical evidence note
     * @param registrationStatus runtime registration status
     * @param recommendedPath recommended path to keep using
     */
    public ChartIntegrationCapability( final boolean nativeDropdownSupported, final String integrationModeTitle, final String explanation,
            final String publicApiEvidence, final String technicalEvidence, final String registrationStatus, final String recommendedPath ) {
        this.nativeDropdownSupported = nativeDropdownSupported;
        this.integrationModeTitle = integrationModeTitle;
        this.explanation = explanation;
        this.publicApiEvidence = publicApiEvidence;
        this.technicalEvidence = technicalEvidence;
        this.registrationStatus = registrationStatus;
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

    public String getPublicApiEvidence() {
        return publicApiEvidence;
    }

    public String getTechnicalEvidence() {
        return technicalEvidence;
    }

    public String getRegistrationStatus() {
        return registrationStatus;
    }

    public String getRecommendedPath() {
        return recommendedPath;
    }

}