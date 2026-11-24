package hu.aleks.larvasecondslostextmod;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Records, per player, the first loop where the alive drone count reached each drone milestone threshold (E17-S08, D-4).
 * Later dips below and re-reaching never overwrite. Used by {@link LarvaReplayAnalyzer}; one instance per analysis.
 */
final class LarvaFirstDroneLoopTracker {

    /** Player name to (threshold to first loop). */
    private final Map< String, Map< Integer, Integer > > firstLoopByPlayerName = new LinkedHashMap<>();

    /**
     * Observes the current alive drone counts after one tracker event.
     *
     * @param aliveDroneCountByPlayerName current alive drone count per player
     * @param loop loop of the event just processed
     */
    void observe( final Map< String, Integer > aliveDroneCountByPlayerName, final int loop ) {
        for ( final Map.Entry< String, Integer > entry : aliveDroneCountByPlayerName.entrySet() ) {
            final Integer count = entry.getValue();
            if ( count == null || count.intValue() < LarvaMilestone.DRONES_66.getThreshold() )
                continue;
            observePlayer( entry.getKey(), count.intValue(), loop );
        }
    }

    private void observePlayer( final String playerName, final int count, final int loop ) {
        Map< Integer, Integer > loops = firstLoopByPlayerName.get( playerName );
        if ( loops == null ) {
            loops = new LinkedHashMap<>();
            firstLoopByPlayerName.put( playerName, loops );
        }
        for ( final LarvaMilestone milestone : LarvaMilestone.values() ) {
            if ( milestone.getKind() != LarvaMilestone.Kind.DRONES || count < milestone.getThreshold() )
                continue;
            final Integer threshold = Integer.valueOf( milestone.getThreshold() );
            if ( !loops.containsKey( threshold ) )
                loops.put( threshold, Integer.valueOf( loop ) );
        }
    }

    /**
     * Returns the collected loops.
     *
     * @return player name to (threshold to first loop); players that never reached a threshold are absent
     */
    Map< String, Map< Integer, Integer > > getFirstLoopByPlayerName() {
        return firstLoopByPlayerName;
    }
}
