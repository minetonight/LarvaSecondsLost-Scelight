package hu.aleks.larvasecondslostextmod;

/**
 * One replay-derived hatchery interval where larva count stays at 3 or above.
 */
public class LarvaSaturationWindow {

    /** Window start loop. */
    private final int startLoop;

    /** Window end loop. */
    private final int endLoop;

    /** Window start in milliseconds. */
    private final long startMs;

    /** Window end in milliseconds. */
    private final long endMs;

    /** Formatted start time label. */
    private final String startTimeLabel;

    /** Formatted end time label. */
    private final String endTimeLabel;

    /**
     * Creates a new saturation window.
     *
     * @param startLoop window start loop
     * @param endLoop window end loop
     * @param startMs window start in milliseconds
     * @param endMs window end in milliseconds
     * @param startTimeLabel formatted start time label
     * @param endTimeLabel formatted end time label
     */
    public LarvaSaturationWindow( final int startLoop, final int endLoop, final long startMs, final long endMs,
            final String startTimeLabel, final String endTimeLabel ) {
        this.startLoop = startLoop;
        this.endLoop = endLoop;
        this.startMs = startMs;
        this.endMs = endMs;
        this.startTimeLabel = startTimeLabel;
        this.endTimeLabel = endTimeLabel;
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

}