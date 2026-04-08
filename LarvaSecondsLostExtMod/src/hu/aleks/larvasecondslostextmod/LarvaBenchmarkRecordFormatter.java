package hu.aleks.larvasecondslostextmod;

import java.util.List;

/**
 * Renders a deterministic, diff-friendly benchmark export snapshot for validation.
 */
public class LarvaBenchmarkRecordFormatter {

    /** Benchmark export format version. */
    private static final String FORMAT_VERSION = "1";

    /**
     * Formats a benchmark replay record into a stable text representation.
     *
     * @param replayRecord replay record to format
     * @return deterministic text output
     */
    public String format( final LarvaBenchmarkReplayRecord replayRecord ) {
        final StringBuilder builder = new StringBuilder();
        builder.append( "formatVersion=" ).append( FORMAT_VERSION ).append( '\n' );
        if ( replayRecord == null ) {
            builder.append( "benchmarkReplay=missing" );
            return builder.toString();
        }

        builder.append( "replay.file=" ).append( safe( replayRecord.getReplayFilePath() ) ).append( '\n' );
        builder.append( "replay.sha256=" ).append( safe( replayRecord.getReplaySha256() ) ).append( '\n' );
        builder.append( "replay.source=" ).append( safe( replayRecord.getSourceDescription() ) ).append( '\n' );
        builder.append( "replay.map=" ).append( safe( replayRecord.getMapTitle() ) ).append( '\n' );
        builder.append( "replay.players=" ).append( safe( replayRecord.getPlayers() ) ).append( '\n' );
        builder.append( "replay.winners=" ).append( safe( replayRecord.getWinners() ) ).append( '\n' );
        builder.append( "replay.lengthMs=" ).append( replayRecord.getReplayLengthMs() ).append( '\n' );
        builder.append( "replay.lengthLoops=" ).append( replayRecord.getReplayLengthLoops() ).append( '\n' );
        builder.append( "replay.endTime=" ).append( safe( replayRecord.getReplayEndTime() ) ).append( '\n' );
        builder.append( "replay.version=" ).append( safe( replayRecord.getReplayVersion() ) ).append( '\n' );
        builder.append( "replay.baseBuild=" ).append( safe( replayRecord.getBaseBuild() ) ).append( '\n' );
        builder.append( "replay.fullReplayParseUsed=" ).append( replayRecord.isFullReplayParseUsed() ).append( '\n' );
        builder.append( "replay.trackedHatcheries=" ).append( replayRecord.getTrackedHatcheryCount() ).append( '\n' );
        builder.append( "replay.assignedLarva=" ).append( replayRecord.getAssignedLarvaCount() ).append( '\n' );
        builder.append( "replay.unassignedLarva=" ).append( replayRecord.getUnassignedLarvaCount() ).append( '\n' );
        builder.append( "replay.injectWindows=" ).append( replayRecord.getInjectWindowCount() ).append( '\n' );
        builder.append( "replay.idleInjectWindows=" ).append( replayRecord.getIdleInjectWindowCount() ).append( '\n' );
        appendFlags( builder, "replay.flag", replayRecord.getDiagnosticFlagList() );
        builder.append( "replay.playerCount=" ).append( replayRecord.getPlayerRecordList().size() ).append( '\n' );
        for ( int playerIndex = 0; playerIndex < replayRecord.getPlayerRecordList().size(); playerIndex++ )
            appendPlayerRecord( builder, playerIndex, replayRecord.getPlayerRecordList().get( playerIndex ) );
        return builder.toString();
    }

    /**
     * Appends one player export block.
     */
    private void appendPlayerRecord( final StringBuilder builder, final int playerIndex, final LarvaBenchmarkPlayerRecord playerRecord ) {
        builder.append( "player[" ).append( playerIndex ).append( "].name=" ).append( safe( playerRecord.getPlayerName() ) ).append( '\n' );
        builder.append( "player[" ).append( playerIndex ).append( "].race=" ).append( safe( playerRecord.getPlayerRace() ) ).append( '\n' );
        builder.append( "player[" ).append( playerIndex ).append( "].opponent=" ).append( safe( playerRecord.getOpponentPlayerName() ) ).append( '\n' );
        builder.append( "player[" ).append( playerIndex ).append( "].opponentRace=" ).append( safe( playerRecord.getOpponentRace() ) ).append( '\n' );
        builder.append( "player[" ).append( playerIndex ).append( "].matchup=" ).append( safe( playerRecord.getMatchup() ) ).append( '\n' );
        builder.append( "player[" ).append( playerIndex ).append( "].result=" ).append( safe( playerRecord.getResult() ) ).append( '\n' );
        appendFlags( builder, "player[" + playerIndex + "].flag", playerRecord.getDiagnosticFlagList() );
        builder.append( "player[" ).append( playerIndex ).append( "].phaseCount=" ).append( playerRecord.getPhaseRecordList().size() ).append( '\n' );
        for ( int phaseIndex = 0; phaseIndex < playerRecord.getPhaseRecordList().size(); phaseIndex++ )
            appendPhaseRecord( builder, playerIndex, phaseIndex, playerRecord.getPhaseRecordList().get( phaseIndex ) );
    }

    /**
     * Appends one phase export block.
     */
    private void appendPhaseRecord( final StringBuilder builder, final int playerIndex, final int phaseIndex,
            final LarvaBenchmarkPhaseRecord phaseRecord ) {
        final String prefix = "player[" + playerIndex + "].phase[" + phaseIndex + "].";
        builder.append( prefix ).append( "name=" ).append( phaseRecord.getPhase() == null ? "Unknown" : phaseRecord.getPhase().name() ).append( '\n' );
        builder.append( prefix ).append( "range=" ).append( phaseRecord.getStartLoop() ).append( '-' ).append( phaseRecord.getEndLoop() ).append( '\n' );
        builder.append( prefix ).append( "durationLoops=" ).append( phaseRecord.getDurationLoops() ).append( '\n' );
        builder.append( prefix ).append( "missedLarvaCount=" ).append( phaseRecord.getMissedLarvaCount() ).append( '\n' );
        builder.append( prefix ).append( "missedInjectLarvaCount=" ).append( phaseRecord.getMissedInjectLarvaCount() ).append( '\n' );
        builder.append( prefix ).append( "spawnedLarvaCount=" ).append( phaseRecord.getTotalSpawnedLarvaCount() ).append( '\n' );
        builder.append( prefix ).append( "hatchEligibleLoops=" ).append( phaseRecord.getHatchEligibleLoops() ).append( '\n' );
        builder.append( prefix ).append( "injectEligibleLoops=" ).append( phaseRecord.getInjectEligibleLoops() ).append( '\n' );
        builder.append( prefix ).append( "injectActiveLoops=" ).append( phaseRecord.getInjectActiveLoops() ).append( '\n' );
        builder.append( prefix ).append( "missedLarvaRate=" ).append( safeDouble( phaseRecord.getMissedLarvaPerHatchPerMinute() ) ).append( '\n' );
        builder.append( prefix ).append( "missedInjectRate=" ).append( safeDouble( phaseRecord.getMissedInjectLarvaPerHatchPerMinute() ) ).append( '\n' );
        builder.append( prefix ).append( "spawnedRate=" ).append( safeDouble( phaseRecord.getSpawnedLarvaPerHatchPerMinute() ) ).append( '\n' );
        builder.append( prefix ).append( "injectUptime=" ).append( safeDouble( phaseRecord.getInjectUptimePercentage() ) ).append( '\n' );
    }

    /**
     * Appends a stable list of flags.
     */
    private void appendFlags( final StringBuilder builder, final String prefix, final List< String > flagList ) {
        builder.append( prefix ).append( "Count=" ).append( flagList == null ? 0 : flagList.size() ).append( '\n' );
        if ( flagList == null )
            return;

        for ( int index = 0; index < flagList.size(); index++ )
            builder.append( prefix ).append( '[' ).append( index ).append( "]=" ).append( safe( flagList.get( index ) ) ).append( '\n' );
    }

    /**
     * Formats a nullable decimal deterministically.
     */
    private String safeDouble( final Double value ) {
        if ( value == null )
            return "n/a";

        final long scaled = Math.round( value.doubleValue() * 100.0d );
        final long whole = scaled / 100L;
        final long fraction = Math.abs( scaled % 100L );
        return whole + "." + ( fraction < 10L ? "0" : "" ) + fraction;
    }

    /**
     * Returns a non-empty string.
     */
    private String safe( final String value ) {
        return value == null || value.length() == 0 ? "n/a" : value;
    }

}
