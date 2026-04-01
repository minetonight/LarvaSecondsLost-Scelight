package hu.aleks.larvasecondslostextmod;

/**
 * Describes the calibrated hatchery-to-larva offset used by the larva assignment heuristic.
 */
public class LarvaHeuristicCalibration {

    /** Tells if the calibration was derived from the replay opening. */
    private final boolean calibratedFromReplay;

    /** Number of larva samples used to derive the offset. */
    private final int sampleCount;

    /** Average x offset from hatchery to larva. */
    private final double averageDx;

    /** Average y offset from hatchery to larva. */
    private final double averageDy;

    /** Maximum observed hatchery-to-larva distance in calibration samples. */
    private final double maxObservedDistance;

    /** Recommended assignment distance for the heuristic. */
    private final double recommendedAssignmentDistance;

    /** Human-readable note about the calibration source. */
    private final String note;

    /**
     * Creates a new heuristic calibration.
     *
     * @param calibratedFromReplay tells if the calibration was derived from the replay
     * @param sampleCount number of larva samples used
     * @param averageDx average x offset
     * @param averageDy average y offset
     * @param maxObservedDistance max observed distance
     * @param recommendedAssignmentDistance recommended assignment distance
     * @param note human-readable note
     */
    public LarvaHeuristicCalibration( final boolean calibratedFromReplay, final int sampleCount, final double averageDx, final double averageDy,
            final double maxObservedDistance, final double recommendedAssignmentDistance, final String note ) {
        this.calibratedFromReplay = calibratedFromReplay;
        this.sampleCount = sampleCount;
        this.averageDx = averageDx;
        this.averageDy = averageDy;
        this.maxObservedDistance = maxObservedDistance;
        this.recommendedAssignmentDistance = recommendedAssignmentDistance;
        this.note = note;
    }

    public boolean isCalibratedFromReplay() {
        return calibratedFromReplay;
    }

    public int getSampleCount() {
        return sampleCount;
    }

    public double getAverageDx() {
        return averageDx;
    }

    public double getAverageDy() {
        return averageDy;
    }

    public double getMaxObservedDistance() {
        return maxObservedDistance;
    }

    public double getRecommendedAssignmentDistance() {
        return recommendedAssignmentDistance;
    }

    public String getNote() {
        return note;
    }

}