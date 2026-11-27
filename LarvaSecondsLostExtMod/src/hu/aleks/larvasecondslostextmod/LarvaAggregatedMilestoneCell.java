package hu.aleks.larvasecondslostextmod;

/**
 * Immutable milestone result of one bucket of one tracked player (AC-13.1, E20-S04, E20-S06): the median loop over the games that
 * reached the milestone, {@code k/n}, and the league ranking of that median.
 */
public final class LarvaAggregatedMilestoneCell {

    private final LarvaMilestone milestone;

    private final Integer medianLoop;

    private final int reachedCount;

    private final int gameCount;

    private final LarvaRanking ranking;

    LarvaAggregatedMilestoneCell( final LarvaMilestone milestone, final Integer medianLoop, final int reachedCount, final int gameCount,
            final LarvaRanking ranking ) {
        if ( ranking == null )
            throw new IllegalArgumentException( "ranking is required (n/a is LarvaRanking.NOT_AVAILABLE)" );
        this.milestone = milestone;
        this.medianLoop = medianLoop;
        this.reachedCount = reachedCount;
        this.gameCount = gameCount;
        this.ranking = ranking;
    }

    public LarvaMilestone getMilestone() {
        return milestone;
    }

    /**
     * @return median loop over the reached games (even count: mean of the two middle loops, half up), or null if no game reached
     *         the milestone (no censoring, D-10)
     */
    public Integer getMedianLoop() {
        return medianLoop;
    }

    /** @return k: player-games that reached the milestone */
    public int getReachedCount() {
        return reachedCount;
    }

    /** @return n: player-games in the bucket */
    public int getGameCount() {
        return gameCount;
    }

    /** @return {@code top p% <league>}, {@code <league> pace m:ss}, {@code aim for m:ss} or {@code n/a}; never null */
    public String getRanking() {
        return ranking.getText();
    }

    /** @return the ranking with its league label marked inside the bands (E20-S12); never null */
    LarvaRanking getRankingValue() {
        return ranking;
    }
}
