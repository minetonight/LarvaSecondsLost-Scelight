package hu.aleks.larvasecondslostextmod;

/**
 * One resolved per-player gameplay phase interval in replay loops.
 */
public class LarvaPhaseInterval {

    /** Phase represented by the interval. */
    private final LarvaGamePhase phase;

    /** Inclusive interval start loop. */
    private final int startLoop;

    /** Exclusive interval end loop. */
    private final int endLoop;

    /**
     * Creates a new phase interval.
     *
     * @param phase phase represented by the interval
     * @param startLoop inclusive start loop
     * @param endLoop exclusive end loop
     */
    public LarvaPhaseInterval( final LarvaGamePhase phase, final int startLoop, final int endLoop ) {
        this.phase = phase;
        this.startLoop = startLoop;
        this.endLoop = endLoop;
    }

    public LarvaGamePhase getPhase() {
        return phase;
    }

    public int getStartLoop() {
        return startLoop;
    }

    public int getEndLoop() {
        return endLoop;
    }

    public int getDurationLoops() {
        return Math.max( 0, endLoop - startLoop );
    }

}
