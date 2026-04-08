package hu.aleks.larvasecondslostextmod;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Immutable headless benchmark-export block for one analyzed player.
 */
public class LarvaBenchmarkPlayerRecord {

    /** Analyzed player name. */
    private final String playerName;

    /** Stable readable player race name. */
    private final String playerRace;

    /** Stable readable opponent race name. */
    private final String opponentRace;

    /** Opponent player name if known. */
    private final String opponentPlayerName;

    /** Matchup label from the analyzed player's perspective. */
    private final String matchup;

    /** Match result text if known. */
    private final String result;

    /** Diagnostic flags attached to the player export. */
    private final List< String > diagnosticFlagList;

    /** Phase-level metric rows in stable gameplay-phase order. */
    private final List< LarvaBenchmarkPhaseRecord > phaseRecordList;

    /**
     * Creates a new player export block.
     */
    public LarvaBenchmarkPlayerRecord( final String playerName, final String playerRace, final String opponentRace, final String opponentPlayerName,
            final String matchup, final String result, final List< String > diagnosticFlagList,
            final List< LarvaBenchmarkPhaseRecord > phaseRecordList ) {
        this.playerName = playerName;
        this.playerRace = playerRace;
        this.opponentRace = opponentRace;
        this.opponentPlayerName = opponentPlayerName;
        this.matchup = matchup;
        this.result = result;
        this.diagnosticFlagList = diagnosticFlagList == null ? Collections.< String >emptyList()
                : Collections.unmodifiableList( new ArrayList<>( diagnosticFlagList ) );
        this.phaseRecordList = phaseRecordList == null ? Collections.< LarvaBenchmarkPhaseRecord >emptyList()
                : Collections.unmodifiableList( new ArrayList<>( phaseRecordList ) );
    }

    public String getPlayerName() {
        return playerName;
    }

    public String getPlayerRace() {
        return playerRace;
    }

    public String getOpponentRace() {
        return opponentRace;
    }

    public String getOpponentPlayerName() {
        return opponentPlayerName;
    }

    public String getMatchup() {
        return matchup;
    }

    public String getResult() {
        return result;
    }

    public List< String > getDiagnosticFlagList() {
        return diagnosticFlagList;
    }

    public List< LarvaBenchmarkPhaseRecord > getPhaseRecordList() {
        return phaseRecordList;
    }

    public LarvaBenchmarkPhaseRecord getPhaseRecord( final LarvaGamePhase phase ) {
        if ( phase == null )
            return null;

        for ( final LarvaBenchmarkPhaseRecord phaseRecord : phaseRecordList )
            if ( phase == phaseRecord.getPhase() )
                return phaseRecord;

        return null;
    }

}
