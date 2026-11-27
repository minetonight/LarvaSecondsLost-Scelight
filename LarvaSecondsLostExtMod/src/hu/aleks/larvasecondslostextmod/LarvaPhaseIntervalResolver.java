package hu.aleks.larvasecondslostextmod;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Splits one player's game into Early, Mid, Late and End phase intervals (E12, E19-S03). A phase starts at the first
 * {@code PlayerStats} sample whose workers count is above the phase's threshold ({@link LarvaGamePhase#getPromotionWorkerThreshold()}).
 * The workers count is {@code workersActiveCount}, the value of Scelight's Workers Active chart. Phases never go back, and a phase
 * that is never reached starts (and ends) at the replay end. Pure and stateless.
 */
final class LarvaPhaseIntervalResolver {

    private LarvaPhaseIntervalResolver() {
    }

    /**
     * @param snapshots worker samples of one player in any order, may be null or empty
     * @param replayEndLoop replay end loop
     * @return the four phase intervals in phase order, never null
     */
    static Map< LarvaGamePhase, LarvaPhaseInterval > resolve( final List< LarvaPlayerResourceSnapshot > snapshots, final int replayEndLoop ) {
        int midStart = firstLoopAbove( snapshots, LarvaGamePhase.MID.getPromotionWorkerThreshold(), replayEndLoop );
        int lateStart = firstLoopAbove( snapshots, LarvaGamePhase.LATE.getPromotionWorkerThreshold(), replayEndLoop );
        int endStart = firstLoopAbove( snapshots, LarvaGamePhase.END.getPromotionWorkerThreshold(), replayEndLoop );
        // A later phase reached implies the earlier ones (a sample above 66 is also above 36): start them no later.
        if ( lateStart < 0 )
            lateStart = endStart;
        if ( midStart < 0 )
            midStart = lateStart;
        if ( lateStart >= 0 )
            lateStart = Math.max( lateStart, midStart );
        if ( endStart >= 0 )
            endStart = Math.max( endStart, lateStart );

        final int midStartLoop = midStart < 0 ? replayEndLoop : midStart;
        final int lateStartLoop = lateStart < 0 ? replayEndLoop : lateStart;
        final int endStartLoop = endStart < 0 ? replayEndLoop : endStart;
        final Map< LarvaGamePhase, LarvaPhaseInterval > intervals = new LinkedHashMap<>();
        intervals.put( LarvaGamePhase.EARLY, new LarvaPhaseInterval( LarvaGamePhase.EARLY, 0, midStartLoop ) );
        intervals.put( LarvaGamePhase.MID, new LarvaPhaseInterval( LarvaGamePhase.MID, midStartLoop, lateStartLoop ) );
        intervals.put( LarvaGamePhase.LATE, new LarvaPhaseInterval( LarvaGamePhase.LATE, lateStartLoop, endStartLoop ) );
        intervals.put( LarvaGamePhase.END, new LarvaPhaseInterval( LarvaGamePhase.END, endStartLoop, replayEndLoop ) );
        return intervals;
    }

    /**
     * @return the earliest sample loop (clamped to {@code [0, replayEndLoop]}) whose workers count is above the threshold, or -1
     */
    static int firstLoopAbove( final List< LarvaPlayerResourceSnapshot > snapshots, final int threshold, final int replayEndLoop ) {
        if ( snapshots == null || replayEndLoop <= 0 )
            return -1;
        int first = -1;
        for ( final LarvaPlayerResourceSnapshot snapshot : snapshots ) {
            final Integer workers = snapshot == null ? null : snapshot.getWorkersActiveCount();
            if ( workers == null || workers.intValue() <= threshold )
                continue;
            if ( first < 0 || snapshot.getLoop() < first )
                first = snapshot.getLoop();
        }
        return first < 0 ? -1 : Math.max( 0, Math.min( first, replayEndLoop ) );
    }
}
