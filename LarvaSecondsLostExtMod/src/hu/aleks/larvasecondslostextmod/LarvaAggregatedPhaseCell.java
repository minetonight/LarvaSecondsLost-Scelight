package hu.aleks.larvasecondslostextmod;

/**
 * Immutable pooled result of one phase in one bucket of one tracked player (E17-S11, D-1, D-2, D-3, D-9).
 *
 * <p>Every value is <code>null</code> when its denominator is 0 (D-10, OQ-13); the view model turns <code>null</code> into
 * {@code n/a}. Rates come from {@link LarvaPhaseStats} built over the summed counts, so there is no second formula.</p>
 */
public final class LarvaAggregatedPhaseCell {

    private final LarvaGamePhase phase;

    private final LarvaPhaseStats pooledStats;

    private final Double missedPerHatchPerMinute;

    private final Double missedInjectPerHatchPerMinute;

    private final Double spawnedPerHatchPerMinute;

    private final Double injectUptimePercent;

    private final LarvaRanking spawnedRanking;

    private final LarvaRanking missedRanking;

    private final Long meanEndMs;

    private final int reachedCount;

    private final int gameCount;

    LarvaAggregatedPhaseCell( final LarvaGamePhase phase, final LarvaPhaseStats pooled, final LarvaRanking spawnedRanking,
            final LarvaRanking missedRanking, final Long meanEndMs, final int reachedCount, final int gameCount ) {
        this.phase = phase;
        this.pooledStats = pooled;
        this.missedPerHatchPerMinute = pooled == null ? null : pooled.getMissedLarvaPerHatchPerMinute();
        this.missedInjectPerHatchPerMinute = pooled == null ? null : pooled.getMissedInjectLarvaPerHatchPerMinute();
        this.spawnedPerHatchPerMinute = pooled == null ? null : pooled.getSpawnedLarvaPerHatchPerMinute();
        this.injectUptimePercent = pooled == null ? null : pooled.getInjectUptimePercentage();
        this.spawnedRanking = spawnedRanking;
        this.missedRanking = missedRanking;
        this.meanEndMs = meanEndMs;
        this.reachedCount = reachedCount;
        this.gameCount = gameCount;
    }

    public LarvaGamePhase getPhase() {
        return phase;
    }

    /** @return the pooled stats over the summed counts and loops (immutable), or null if no player-game had data; for dumps and tests */
    public LarvaPhaseStats getPooledStats() {
        return pooledStats;
    }

    /** @return pooled missed larva per hatchery per minute, or null */
    public Double getMissedPerHatchPerMinute() {
        return missedPerHatchPerMinute;
    }

    /** @return pooled missed inject larva per hatchery per minute, or null */
    public Double getMissedInjectPerHatchPerMinute() {
        return missedInjectPerHatchPerMinute;
    }

    /** @return pooled spawned larva per hatchery per minute, or null */
    public Double getSpawnedPerHatchPerMinute() {
        return spawnedPerHatchPerMinute;
    }

    /** @return pooled inject uptime in percent, or null */
    public Double getInjectUptimePercent() {
        return injectUptimePercent;
    }

    /** @return ranking text of the pooled spawned rate; {@code n/a} for END (D-3); null if there is no data */
    public String getSpawnedRanking() {
        return spawnedRanking == null ? null : spawnedRanking.getText();
    }

    /** @return the spawned ranking with its league label marked inside the bands (E20-S12), or null without a rate */
    LarvaRanking getSpawnedRankingValue() {
        return spawnedRanking;
    }

    /** @return ranking text of the pooled missed rate; {@code n/a} for END (D-3); null if there is no data */
    public String getMissedRanking() {
        return missedRanking == null ? null : missedRanking.getText();
    }

    /** @return the missed ranking with its league label marked inside the bands (E20-S12), or null without a rate */
    LarvaRanking getMissedRankingValue() {
        return missedRanking;
    }

    /** @return mean game time in display ms at which the phase ended (CR-1.1), over the player-games that reached the phase (D-9), or null if none did */
    public Long getMeanEndMs() {
        return meanEndMs;
    }

    /** @return k: player-games that reached the phase (duration &gt; 0) */
    public int getReachedCount() {
        return reachedCount;
    }

    /** @return n: player-games in the bucket */
    public int getGameCount() {
        return gameCount;
    }
}
