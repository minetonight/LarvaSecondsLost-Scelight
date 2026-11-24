package hu.aleks.larvasecondslostextmod;

/** Immutable milestone result of one bucket of one tracked player: mean display ms over reached games plus {@code k/n} (AC-13.1). */
public final class LarvaAggregatedMilestoneCell {

    private final LarvaMilestone milestone;

    private final Long meanMs;

    private final int reachedCount;

    private final int gameCount;

    LarvaAggregatedMilestoneCell( final LarvaMilestone milestone, final Long meanMs, final int reachedCount, final int gameCount ) {
        this.milestone = milestone;
        this.meanMs = meanMs;
        this.reachedCount = reachedCount;
        this.gameCount = gameCount;
    }

    public LarvaMilestone getMilestone() {
        return milestone;
    }

    /** @return mean time in display ms (half-up), or null if no game reached the milestone (no censoring, D-10) */
    public Long getMeanMs() {
        return meanMs;
    }

    /** @return k: player-games that reached the milestone */
    public int getReachedCount() {
        return reachedCount;
    }

    /** @return n: player-games in the bucket */
    public int getGameCount() {
        return gameCount;
    }
}
