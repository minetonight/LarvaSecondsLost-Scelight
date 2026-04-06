package hu.aleks.larvasecondslostextmod;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * Immutable Epic 12 phase table for one player.
 */
public class LarvaPlayerPhaseTable {

    /** Player name owning the table. */
    private final String playerName;

    /** Phase intervals keyed by phase. */
    private final Map< LarvaGamePhase, LarvaPhaseInterval > phaseIntervalMap;

    /** Aggregated phase stats keyed by phase. */
    private final Map< LarvaGamePhase, LarvaPhaseStats > phaseStatsMap;

    /**
     * Creates a new player phase table.
     *
     * @param playerName player name
     * @param phaseIntervalMap phase intervals keyed by phase
     * @param phaseStatsMap phase stats keyed by phase
     */
    public LarvaPlayerPhaseTable( final String playerName, final Map< LarvaGamePhase, LarvaPhaseInterval > phaseIntervalMap,
            final Map< LarvaGamePhase, LarvaPhaseStats > phaseStatsMap ) {
        this.playerName = playerName;
        this.phaseIntervalMap = Collections.unmodifiableMap( new EnumMap< LarvaGamePhase, LarvaPhaseInterval >( phaseIntervalMap ) );
        this.phaseStatsMap = Collections.unmodifiableMap( new EnumMap< LarvaGamePhase, LarvaPhaseStats >( phaseStatsMap ) );
    }

    public String getPlayerName() {
        return playerName;
    }

    public Map< LarvaGamePhase, LarvaPhaseInterval > getPhaseIntervalMap() {
        return phaseIntervalMap;
    }

    public Map< LarvaGamePhase, LarvaPhaseStats > getPhaseStatsMap() {
        return phaseStatsMap;
    }

    public LarvaPhaseInterval getPhaseInterval( final LarvaGamePhase phase ) {
        return phase == null ? null : phaseIntervalMap.get( phase );
    }

    public LarvaPhaseStats getPhaseStats( final LarvaGamePhase phase ) {
        return phase == null ? null : phaseStatsMap.get( phase );
    }

}
