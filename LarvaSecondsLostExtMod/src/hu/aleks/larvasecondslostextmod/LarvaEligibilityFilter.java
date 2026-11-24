package hu.aleks.larvasecondslostextmod;

/**
 * Decides whether a replay record takes part in the aggregation (FR-10, OQ-5). Pure and stateless.
 *
 * <p>Checks run in a fixed order and the first failing check wins: failed to parse, not 1v1, non-ladder (only if requested),
 * too short, no Zerg.</p>
 */
public final class LarvaEligibilityFilter {

    /**
     * @param record
     *            the replay record, <code>null</code> if the replay failed to parse
     * @param ladderOnly
     *            <code>true</code> to accept only ladder (AMM) games
     * @return {@link LarvaEligibility#ELIGIBLE} or the first exclusion reason
     */
    public LarvaEligibility evaluate( final LarvaAggregationGameRecord record, final boolean ladderOnly ) {
        if ( record == null )
            return LarvaEligibility.FAILED_TO_PARSE;
        if ( !record.isOneVsOne() )
            return LarvaEligibility.NOT_1V1;
        if ( ladderOnly && !record.isLadder() )
            return LarvaEligibility.NON_LADDER;
        if ( record.getLengthLoops() < LarvaAggregationConstants.MIN_GAME_LENGTH_LOOPS )
            return LarvaEligibility.TOO_SHORT;
        if ( record.getZergPlayers().isEmpty() )
            return LarvaEligibility.NO_ZERG;
        return LarvaEligibility.ELIGIBLE;
    }
}
