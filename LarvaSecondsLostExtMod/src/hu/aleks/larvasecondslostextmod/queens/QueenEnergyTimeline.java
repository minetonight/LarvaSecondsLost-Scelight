package hu.aleks.larvasecondslostextmod.queens;

import java.util.ArrayList;
import java.util.List;

/**
 * Energy of one queen over time (E19-S09): {@link QueenEnergy#START} at birth, regeneration up to {@link QueenEnergy#CAP}, minus
 * her attributed spends. Spends are added in loop order while they are attributed.
 *
 * <p>{@link #spareIntervals(int)} answers the missed-inject question: when did she have 25 energy that she could have put on
 * hatchery H without giving up anything she really did later? Energy she later spent on a tumor, a transfusion or another
 * hatchery is not spare: a queen that spreads creep is not missing injects. Her next inject on H does not count against it,
 * because injecting earlier would have replaced that late inject. Once she is at the cap, any extra energy would have been lost,
 * so nothing after that point counts against it either.</p>
 *
 * <p>With a horizon, only spends within the horizon count against it: a queen that hoards energy at an uninjected hatchery for
 * minutes before a creep burst was idle until shortly before the burst (E19-S02 scenario: the third base, skipped on purpose).</p>
 */
public final class QueenEnergyTimeline {

    private final int bornLoop;
    private final int endLoop;
    private final List< QueenSpend > spends = new ArrayList<>();
    private final List< Double > energyAfter = new ArrayList<>();
    private int conflictCount;

    /**
     * @param bornLoop birth loop
     * @param endLoop death loop, or the replay end
     */
    public QueenEnergyTimeline( final int bornLoop, final int endLoop ) {
        this.bornLoop = bornLoop;
        this.endLoop = Math.max( bornLoop, endLoop );
    }

    public int getBornLoop() {
        return bornLoop;
    }

    public int getEndLoop() {
        return endLoop;
    }

    /** @return true if the queen is alive at the loop */
    public boolean isAliveAt( final int loop ) {
        return loop >= bornLoop && loop < endLoop;
    }

    /** @return spends that she could not afford (an attribution was probably wrong); each set her energy to 0 */
    public int getConflictCount() {
        return conflictCount;
    }

    public int getSpendCount() {
        return spends.size();
    }

    /**
     * Energy at the loop after all spends added so far; the loop must not be before the last added spend.
     *
     * @param loop loop at or after the last added spend
     * @return energy
     */
    public double energyNow( final int loop ) {
        if ( spends.isEmpty() )
            return regenerate( QueenEnergy.START, bornLoop, loop );
        final int last = spends.size() - 1;
        return regenerate( energyAfter.get( last ).doubleValue(), spends.get( last ).getLoop(), loop );
    }

    /** @return true if she has the energy for the cost at the loop (at or after the last added spend) */
    public boolean canAfford( final int loop, final double cost ) {
        return energyNow( loop ) + QueenEnergy.EPSILON >= cost;
    }

    /**
     * Energy at any loop, after the spends at that loop.
     *
     * @param loop loop
     * @return energy, 0 outside her life
     */
    public double energyAt( final int loop ) {
        if ( loop < bornLoop )
            return 0.0d;
        int index = -1;
        while ( index + 1 < spends.size() && spends.get( index + 1 ).getLoop() <= loop )
            index++;
        return index < 0 ? regenerate( QueenEnergy.START, bornLoop, loop )
                : regenerate( energyAfter.get( index ).doubleValue(), spends.get( index ).getLoop(), loop );
    }

    /**
     * Adds a spend at or after the last one. A spend she cannot afford sets her energy to 0 and counts as a conflict.
     *
     * @param spend spend to add
     */
    public void addSpend( final QueenSpend spend ) {
        if ( !spends.isEmpty() && spend.getLoop() < spends.get( spends.size() - 1 ).getLoop() )
            throw new IllegalArgumentException( "Spends must be added in loop order." );
        final double after = energyNow( spend.getLoop() ) - spend.getCost();
        if ( after < -QueenEnergy.EPSILON )
            conflictCount++;
        spends.add( spend );
        energyAfter.add( Double.valueOf( Math.max( 0.0d, after ) ) );
    }

    /**
     * @param hatchTag hatchery the inject would go to
     * @return loops where she had 25 spare energy for this hatchery (no horizon), sorted and merged
     */
    public List< int[] > spareIntervals( final int hatchTag ) {
        return spareIntervals( hatchTag, Integer.MAX_VALUE );
    }

    /**
     * @param hatchTag hatchery the inject would go to
     * @param horizonLoops only spends at most this many loops ahead count against the spare energy
     * @return loops where she had 25 spare energy for this hatchery, sorted and merged
     */
    public List< int[] > spareIntervals( final int hatchTag, final int horizonLoops ) {
        final List< int[] > result = new ArrayList<>();
        for ( int segment = 0; segment <= spends.size(); segment++ ) {
            final int segmentStart = segmentStart( segment );
            final int segmentEnd = segment == spends.size() ? endLoop : spends.get( segment ).getLoop();
            final double startEnergy = segmentStartEnergy( segment );
            final int readyLoop = startEnergy + QueenEnergy.EPSILON >= QueenEnergy.INJECT_COST ? segmentStart
                    : segmentStart + QueenEnergy.loopsToRegenerate( QueenEnergy.INJECT_COST - startEnergy );
            final int blocking = firstBlockingSpend( segment, hatchTag );
            final int end = blocking < 0 ? segmentEnd
                    : (int) Math.min( segmentEnd, (long) spends.get( blocking ).getLoop() - horizonLoops );
            if ( readyLoop < end )
                result.add( new int[] { readyLoop, end } );
        }
        return LoopIntervals.normalize( result );
    }

    /**
     * @return the first later spend she could not afford with 25 less (before her next inject on H and before the cap), or -1:
     *         an inject during the segment would have cost that spend
     */
    private int firstBlockingSpend( final int segment, final int hatchTag ) {
        for ( int j = segment; j < spends.size(); j++ ) {
            if ( reachesCapBeforeSpend( j ) )
                return -1;
            final QueenSpend spend = spends.get( j );
            if ( spend.isInjectOn( hatchTag ) )
                return -1;
            if ( energyAfter.get( j ).doubleValue() + QueenEnergy.EPSILON < QueenEnergy.INJECT_COST )
                return j;
        }
        return -1;
    }

    private boolean reachesCapBeforeSpend( final int spendIndex ) {
        final int start = segmentStart( spendIndex );
        return segmentStartEnergy( spendIndex ) + QueenEnergy.PER_LOOP * ( spends.get( spendIndex ).getLoop() - start )
                >= QueenEnergy.CAP - QueenEnergy.EPSILON;
    }

    private int segmentStart( final int segment ) {
        return segment == 0 ? bornLoop : spends.get( segment - 1 ).getLoop();
    }

    private double segmentStartEnergy( final int segment ) {
        return segment == 0 ? QueenEnergy.START : energyAfter.get( segment - 1 ).doubleValue();
    }

    private static double regenerate( final double energy, final int fromLoop, final int toLoop ) {
        return toLoop <= fromLoop ? energy : Math.min( QueenEnergy.CAP, energy + QueenEnergy.PER_LOOP * ( toLoop - fromLoop ) );
    }
}
