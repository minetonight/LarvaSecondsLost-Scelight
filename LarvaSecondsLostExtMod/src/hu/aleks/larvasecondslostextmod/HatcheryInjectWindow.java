package hu.aleks.larvasecondslostextmod;

/**
 * One normalized replay-derived inject-active window for a hatchery.
 */
public class HatcheryInjectWindow {

     /** Replay loop of the evidence point that proves the inject completion. */
     private final int evidenceLoop;

     /** Formatted time of the evidence point that proves the inject completion. */
     private final String evidenceTimeLabel;

     /** Short label describing the evidence type used to infer the inject. */
     private final String evidenceKind;

    /** Effective window start loop after bounds normalization. */
    private final int startLoop;

    /** Effective window end loop after bounds normalization. */
    private final int endLoop;

    /** Effective window start in milliseconds. */
    private final long startMs;

    /** Effective window end in milliseconds. */
    private final long endMs;

    /** Formatted effective window start. */
    private final String startTimeLabel;

    /** Formatted effective window end. */
    private final String endTimeLabel;

    /** Tells if the window start had to be trimmed to valid hatchery lifetime bounds. */
    private final boolean trimmedAtStart;

    /** Tells if the window end had to be trimmed to valid hatchery lifetime bounds. */
    private final boolean trimmedAtEnd;

    /** Human-readable explanation of why the window exists and how it was normalized. */
    private final String diagnosticNote;

    /**
     * Creates a new inject window.
     *
    * @param evidenceLoop evidence loop that proves the inject completion
    * @param evidenceTimeLabel formatted evidence time
    * @param evidenceKind short label describing the evidence type used to infer the inject
     * @param startLoop effective window start loop
     * @param endLoop effective window end loop
     * @param startMs effective window start in milliseconds
     * @param endMs effective window end in milliseconds
     * @param startTimeLabel formatted effective window start
     * @param endTimeLabel formatted effective window end
     * @param trimmedAtStart tells if the start was trimmed
     * @param trimmedAtEnd tells if the end was trimmed
     * @param diagnosticNote explanation of why the window exists and how it was normalized
     */
    public HatcheryInjectWindow( final int evidenceLoop, final String evidenceTimeLabel, final String evidenceKind, final int startLoop, final int endLoop,
            final long startMs, final long endMs, final String startTimeLabel, final String endTimeLabel,
            final boolean trimmedAtStart, final boolean trimmedAtEnd, final String diagnosticNote ) {
        this.evidenceLoop = evidenceLoop;
        this.evidenceTimeLabel = evidenceTimeLabel;
        this.evidenceKind = evidenceKind;
        this.startLoop = startLoop;
        this.endLoop = endLoop;
        this.startMs = startMs;
        this.endMs = endMs;
        this.startTimeLabel = startTimeLabel;
        this.endTimeLabel = endTimeLabel;
        this.trimmedAtStart = trimmedAtStart;
        this.trimmedAtEnd = trimmedAtEnd;
        this.diagnosticNote = diagnosticNote;
    }

    public int getEvidenceLoop() {
        return evidenceLoop;
    }

    public String getEvidenceTimeLabel() {
        return evidenceTimeLabel;
    }

    public String getEvidenceKind() {
        return evidenceKind;
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

    public boolean isTrimmedAtStart() {
        return trimmedAtStart;
    }

    public boolean isTrimmedAtEnd() {
        return trimmedAtEnd;
    }

    public String getDiagnosticNote() {
        return diagnosticNote;
    }

    public int getCommandLoop() {
        return getEvidenceLoop();
    }

    public String getCommandTimeLabel() {
        return getEvidenceTimeLabel();
    }

}