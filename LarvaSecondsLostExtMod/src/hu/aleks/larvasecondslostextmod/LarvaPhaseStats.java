package hu.aleks.larvasecondslostextmod;

/**
 * Aggregated Epic 12 metrics for one player and one gameplay phase.
 */
public class LarvaPhaseStats {

    /** Replay loops per second in SC2 timelines. */
    private static final double REPLAY_LOOPS_PER_SECOND = 16.0d;

    /** Phase represented by this stat block. */
    private final LarvaGamePhase phase;

    /** Resolved interval for the phase. */
    private final LarvaPhaseInterval interval;

    /** Missed-larva count attributed to the phase. */
    private final int missedLarvaCount;

    /** Potential injected larva missed inside the phase. */
    private final int missedInjectLarvaCount;

    /** Assigned larva births inside the phase. */
    private final int totalSpawnedLarvaCount;

    /** Total hatchery-eligible loops used for per-hatch-per-minute normalization. */
    private final long hatchEligibleLoops;

    /** Total inject-eligible loops inside the phase. */
    private final long injectEligibleLoops;

    /** Total inject-active loops inside the inject-eligible phase intersection. */
    private final long injectActiveLoops;

    /**
     * Creates a new phase stat block.
     *
     * @param phase phase represented by the stat block
     * @param interval resolved phase interval
     * @param missedLarvaCount missed-larva count
     * @param missedInjectLarvaCount potential injected larva missed
     * @param totalSpawnedLarvaCount assigned larva births
     * @param hatchEligibleLoops hatchery-eligible loops for normalization
     * @param injectEligibleLoops inject-eligible loops
     * @param injectActiveLoops inject-active loops
     */
    public LarvaPhaseStats( final LarvaGamePhase phase, final LarvaPhaseInterval interval, final int missedLarvaCount,
            final int missedInjectLarvaCount, final int totalSpawnedLarvaCount, final long hatchEligibleLoops,
            final long injectEligibleLoops, final long injectActiveLoops ) {
        this.phase = phase;
        this.interval = interval;
        this.missedLarvaCount = missedLarvaCount;
        this.missedInjectLarvaCount = missedInjectLarvaCount;
        this.totalSpawnedLarvaCount = totalSpawnedLarvaCount;
        this.hatchEligibleLoops = hatchEligibleLoops;
        this.injectEligibleLoops = injectEligibleLoops;
        this.injectActiveLoops = injectActiveLoops;
    }

    public LarvaGamePhase getPhase() {
        return phase;
    }

    public LarvaPhaseInterval getInterval() {
        return interval;
    }

    public int getMissedLarvaCount() {
        return missedLarvaCount;
    }

    public int getMissedInjectLarvaCount() {
        return missedInjectLarvaCount;
    }

    public int getTotalSpawnedLarvaCount() {
        return totalSpawnedLarvaCount;
    }

    public long getHatchEligibleLoops() {
        return hatchEligibleLoops;
    }

    public long getInjectEligibleLoops() {
        return injectEligibleLoops;
    }

    public long getInjectActiveLoops() {
        return injectActiveLoops;
    }

    /**
     * Returns larva missed per hatch per minute, or <code>null</code> if no denominator exists.
     */
    public Double getMissedLarvaPerHatchPerMinute() {
        return hatchEligibleLoops <= 0L ? null : Double.valueOf( missedLarvaCount * 60.0d * REPLAY_LOOPS_PER_SECOND / hatchEligibleLoops );
    }

    /**
     * Returns potential injected larva missed per hatch per minute, or <code>null</code> if no denominator exists.
     */
    public Double getMissedInjectLarvaPerHatchPerMinute() {
        return hatchEligibleLoops <= 0L ? null : Double.valueOf( missedInjectLarvaCount * 60.0d * REPLAY_LOOPS_PER_SECOND / hatchEligibleLoops );
    }

    /**
     * Returns spawned larva per hatch per minute, or <code>null</code> if no denominator exists.
     */
    public Double getSpawnedLarvaPerHatchPerMinute() {
        return hatchEligibleLoops <= 0L ? null : Double.valueOf( totalSpawnedLarvaCount * 60.0d * REPLAY_LOOPS_PER_SECOND / hatchEligibleLoops );
    }

    /**
     * Returns inject uptime as percentage, or <code>null</code> if no inject-eligible denominator exists.
     */
    public Double getInjectUptimePercentage() {
        return injectEligibleLoops <= 0L ? null : Double.valueOf( injectActiveLoops * 100.0d / injectEligibleLoops );
    }

}
