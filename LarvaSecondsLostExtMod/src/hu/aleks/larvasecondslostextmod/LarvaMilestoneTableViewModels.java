package hu.aleks.larvasecondslostextmod;

/**
 * Builds the milestone table of the single-replay page (E20-S09): one column per milestone, a time row and a ranking row against
 * the bands of the game's matchup. One game is exactly what the bands describe (league medians of single games).
 */
final class LarvaMilestoneTableViewModels {

    /** Corner label of the table. */
    static final String CORNER_LABEL = "Milestone";

    /** Label of the time row. */
    static final String TIME_ROW_LABEL = "Reached at";

    /** Number of body rows: time and ranking. */
    static final int BODY_ROW_COUNT = 2;

    private static final LarvaMilestoneBenchmarkRatingFormatter RATINGS = new LarvaMilestoneBenchmarkRatingFormatter();

    private LarvaMilestoneTableViewModels() {
    }

    /**
     * @param milestones milestones of one player in one replay
     * @return the view model (no title): {@code m:ss} or {@code n/a} when not reached, and the ranking text
     */
    static LarvaTableViewModel fromPlayerMilestones( final LarvaPlayerMilestones milestones ) {
        final LarvaMilestone[] all = LarvaMilestone.values();
        final String[] headers = new String[ all.length ];
        final String[][] cells = new String[ BODY_ROW_COUNT ][ all.length ];
        final LarvaTableViewModel.Bold[][] bold = new LarvaTableViewModel.Bold[ BODY_ROW_COUNT ][ all.length ];
        final LarvaMatchupBucket bucket = milestones.getRatingBucket();
        for ( int column = 0; column < all.length; column++ ) {
            final Integer loop = milestones.getLoop( all[ column ] );
            headers[ column ] = all[ column ].getLabel();
            cells[ 0 ][ column ] = loop == null ? LarvaStatFormat.NOT_AVAILABLE : LarvaGameClock.format( loop.intValue() );
            final LarvaRanking ranking = RATINGS.rank( all[ column ], bucket, loop );
            cells[ 1 ][ column ] = ranking.getText();
            bold[ 1 ][ column ] = LarvaTableViewModel.boldLabel( ranking ); // E20-S12: the league inside the bands is bold
        }
        return new LarvaTableViewModel( null, CORNER_LABEL, headers, new String[] { TIME_ROW_LABEL, rankingRowLabel( bucket ) }, cells,
                bold );
    }

    /** @return {@code ranking (ZvP)}, or {@code ranking (All)} when the matchup is unknown */
    static String rankingRowLabel( final LarvaMatchupBucket bucket ) {
        return "ranking (" + bucket.getLabel() + ")";
    }
}
