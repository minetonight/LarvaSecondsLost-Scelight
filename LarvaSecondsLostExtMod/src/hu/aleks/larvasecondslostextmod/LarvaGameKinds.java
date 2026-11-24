package hu.aleks.larvasecondslostextmod;

import hu.scelightapi.sc2.rep.model.attributesevents.IGameMode;
import hu.scelightapi.sc2.rep.model.details.IRace;
import hu.scelightapi.sc2.rep.repproc.IFormat;

/**
 * The only place where the aggregation code compares Scelight SDK enum constants (format, game mode).
 *
 * <p>The SDK constants cannot be loaded without the Scelight runtime, so unit tests never call this class. The extraction
 * code calls it once per replay and stores plain booleans in the {@link LarvaAggregationGameRecord}; the eligibility filter
 * only reads those booleans (E17-S10).</p>
 */
final class LarvaGameKinds {

    private LarvaGameKinds() {
    }

    /** @return <code>true</code> if the format is 1v1 */
    static boolean isOneVsOne( final IFormat format ) {
        return format == IFormat.ONE_VS_ONE;
    }

    /** @return <code>true</code> for a ladder (AMM) game; <code>false</code> for null or any other mode */
    static boolean isLadder( final IGameMode gameMode ) {
        return gameMode == IGameMode.AMM;
    }

    /**
     * @param opponentRace
     *            race of the opponent, may be null
     * @return {@code ZVP}, {@code ZVT} or {@code ZVZ}; null for a null or other race (E17-S11, D-7)
     */
    static LarvaMatchupBucket matchupOf( final IRace opponentRace ) {
        if ( opponentRace == null )
            return null;
        if ( IRace.PROTOSS.equals( opponentRace ) )
            return LarvaMatchupBucket.ZVP;
        if ( IRace.TERRAN.equals( opponentRace ) )
            return LarvaMatchupBucket.ZVT;
        if ( IRace.ZERG.equals( opponentRace ) )
            return LarvaMatchupBucket.ZVZ;
        return null;
    }
}
