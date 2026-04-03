package hu.aleks.larvasecondslostextmod;

/**
 * One normalized replay-derived idle-inject opportunity window for a hatchery.
 */
public class HatcheryIdleInjectWindow {

    /** Effective window start loop. */
    private final int startLoop;

    /** Effective window end loop. */
    private final int endLoop;

    /** Effective window start in milliseconds. */
    private final long startMs;

    /** Effective window end in milliseconds. */
    private final long endMs;

    /** Formatted effective window start. */
    private final String startTimeLabel;

    /** Formatted effective window end. */
    private final String endTimeLabel;

    /** Number of queens contributing trustworthy eligibility to this window. */
    private final int qualifyingQueenCount;

    /** Minimum estimated queen energy at window start. */
    private final double minEstimatedStartEnergy;

    /** Maximum estimated queen energy at window start. */
    private final double maxEstimatedStartEnergy;

    /** Minimum estimated queen energy at window end. */
    private final double minEstimatedEndEnergy;

    /** Maximum estimated queen energy at window end. */
    private final double maxEstimatedEndEnergy;

    /** Human-readable explanation of why the window exists and how it was qualified. */
    private final String diagnosticNote;

    /** Compact queen summary for tooltips and diagnostics. */
    private final String queenSummary;

    /**
     * Creates a new idle-inject opportunity window.
     *
     * @param startLoop effective window start loop
     * @param endLoop effective window end loop
     * @param startMs effective window start in milliseconds
     * @param endMs effective window end in milliseconds
     * @param startTimeLabel formatted effective window start
     * @param endTimeLabel formatted effective window end
     * @param qualifyingQueenCount number of queens contributing trustworthy eligibility
     * @param minEstimatedStartEnergy minimum estimated queen energy at window start
     * @param maxEstimatedStartEnergy maximum estimated queen energy at window start
     * @param minEstimatedEndEnergy minimum estimated queen energy at window end
     * @param maxEstimatedEndEnergy maximum estimated queen energy at window end
     * @param queenSummary compact queen summary
     * @param diagnosticNote explanation of why the window exists and how it was qualified
     */
    public HatcheryIdleInjectWindow( final int startLoop, final int endLoop, final long startMs, final long endMs,
            final String startTimeLabel, final String endTimeLabel, final int qualifyingQueenCount,
            final double minEstimatedStartEnergy, final double maxEstimatedStartEnergy,
            final double minEstimatedEndEnergy, final double maxEstimatedEndEnergy,
            final String queenSummary, final String diagnosticNote ) {
        this.startLoop = startLoop;
        this.endLoop = endLoop;
        this.startMs = startMs;
        this.endMs = endMs;
        this.startTimeLabel = startTimeLabel;
        this.endTimeLabel = endTimeLabel;
        this.qualifyingQueenCount = qualifyingQueenCount;
        this.minEstimatedStartEnergy = minEstimatedStartEnergy;
        this.maxEstimatedStartEnergy = maxEstimatedStartEnergy;
        this.minEstimatedEndEnergy = minEstimatedEndEnergy;
        this.maxEstimatedEndEnergy = maxEstimatedEndEnergy;
        this.queenSummary = queenSummary;
        this.diagnosticNote = diagnosticNote;
    }

    public int getStartLoop() {
        return startLoop;
    }

    public int getEndLoop() {
        return endLoop;
    }

    public long getStartMs() {
        return startMs;
    }

    public long getEndMs() {
        return endMs;
    }

    public String getStartTimeLabel() {
        return startTimeLabel;
    }

    public String getEndTimeLabel() {
        return endTimeLabel;
    }

    public int getQualifyingQueenCount() {
        return qualifyingQueenCount;
    }

    public double getMinEstimatedStartEnergy() {
        return minEstimatedStartEnergy;
    }

    public double getMaxEstimatedStartEnergy() {
        return maxEstimatedStartEnergy;
    }

    public double getMinEstimatedEndEnergy() {
        return minEstimatedEndEnergy;
    }

    public double getMaxEstimatedEndEnergy() {
        return maxEstimatedEndEnergy;
    }

    public String getQueenSummary() {
        return queenSummary;
    }

    public String getDiagnosticNote() {
        return diagnosticNote;
    }

}