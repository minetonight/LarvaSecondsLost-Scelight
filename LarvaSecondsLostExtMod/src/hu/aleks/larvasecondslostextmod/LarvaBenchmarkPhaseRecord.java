package hu.aleks.larvasecondslostextmod;

/**
 * Immutable headless benchmark-export row for one gameplay phase.
 */
public class LarvaBenchmarkPhaseRecord {

    /** Gameplay phase represented by this record. */
    private final LarvaGamePhase phase;

    /** Inclusive phase start loop. */
    private final int startLoop;

    /** Exclusive phase end loop. */
    private final int endLoop;

    /** Phase duration in loops. */
    private final int durationLoops;

    /** Missed-larva count inside the phase. */
    private final int missedLarvaCount;

    /** Potential injected larva missed inside the phase. */
    private final int missedInjectLarvaCount;

    /** Spawned larva count inside the phase. */
    private final int totalSpawnedLarvaCount;

    /** Hatchery-eligible loops used for normalization. */
    private final long hatchEligibleLoops;

    /** Inject-eligible loops used for uptime normalization. */
    private final long injectEligibleLoops;

    /** Inject-active loops inside the phase. */
    private final long injectActiveLoops;

    /** Missed-larva rate per hatch per minute. */
    private final Double missedLarvaPerHatchPerMinute;

    /** Inject-missed larva rate per hatch per minute. */
    private final Double missedInjectLarvaPerHatchPerMinute;

    /** Spawned-larva rate per hatch per minute. */
    private final Double spawnedLarvaPerHatchPerMinute;

    /** Inject uptime percentage. */
    private final Double injectUptimePercentage;

    /**
     * Creates a new phase export record.
     */
    public LarvaBenchmarkPhaseRecord( final LarvaGamePhase phase, final int startLoop, final int endLoop, final int durationLoops,
            final int missedLarvaCount, final int missedInjectLarvaCount, final int totalSpawnedLarvaCount, final long hatchEligibleLoops,
            final long injectEligibleLoops, final long injectActiveLoops, final Double missedLarvaPerHatchPerMinute,
            final Double missedInjectLarvaPerHatchPerMinute, final Double spawnedLarvaPerHatchPerMinute, final Double injectUptimePercentage ) {
        this.phase = phase;
        this.startLoop = startLoop;
        this.endLoop = endLoop;
        this.durationLoops = durationLoops;
        this.missedLarvaCount = missedLarvaCount;
        this.missedInjectLarvaCount = missedInjectLarvaCount;
        this.totalSpawnedLarvaCount = totalSpawnedLarvaCount;
        this.hatchEligibleLoops = hatchEligibleLoops;
        this.injectEligibleLoops = injectEligibleLoops;
        this.injectActiveLoops = injectActiveLoops;
        this.missedLarvaPerHatchPerMinute = missedLarvaPerHatchPerMinute;
        this.missedInjectLarvaPerHatchPerMinute = missedInjectLarvaPerHatchPerMinute;
        this.spawnedLarvaPerHatchPerMinute = spawnedLarvaPerHatchPerMinute;
        this.injectUptimePercentage = injectUptimePercentage;
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
        return durationLoops;
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

    public Double getMissedLarvaPerHatchPerMinute() {
        return missedLarvaPerHatchPerMinute;
    }

    public Double getMissedInjectLarvaPerHatchPerMinute() {
        return missedInjectLarvaPerHatchPerMinute;
    }

    public Double getSpawnedLarvaPerHatchPerMinute() {
        return spawnedLarvaPerHatchPerMinute;
    }

    public Double getInjectUptimePercentage() {
        return injectUptimePercentage;
    }

}
