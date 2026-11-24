package hu.aleks.larvasecondslostextmod;

/**
 * Result of the eligibility check of one replay (E17-S10). The declaration order of the exclusion reasons is the order in
 * which the checks run, and the first failing check wins, so every file is counted exactly once.
 */
public enum LarvaEligibility {

    /** The replay is used in the aggregation. */
    ELIGIBLE( "eligible" ),

    /** The record is missing or the replay could not be parsed (set by the batch runner). */
    FAILED_TO_PARSE( "failed to parse" ),

    /** The replay is not a 1v1 game. */
    NOT_1V1( "not 1v1" ),

    /** Only with "Ladder games only": the game mode is not AMM (null or unknown included). */
    NON_LADDER( "non-ladder" ),

    /** The game is shorter than the minimum length. */
    TOO_SHORT( "< 4 min" ),

    /** No Zerg player in the replay. */
    NO_ZERG( "no Zerg" );

    private final String label;

    private LarvaEligibility( final String label ) {
        this.label = label;
    }

    /** @return short human readable label */
    public String getLabel() {
        return label;
    }
}
