package hu.aleks.larvasecondslostextmod;

import java.util.List;

/**
 * Deterministic JSON serializer for Phase 4.5 benchmark extraction.
 */
public class LarvaBenchmarkJsonSerializer {

    /**
     * Serializes a replay export record to deterministic JSON.
     *
     * @param replayRecord replay export record to serialize
     * @return JSON document
     */
    public String toJson( final LarvaBenchmarkReplayRecord replayRecord ) {
        final StringBuilder builder = new StringBuilder( 8192 );
        appendReplayRecord( builder, replayRecord );
        return builder.toString();
    }

    /**
     * Appends a replay-level JSON object.
     */
    private void appendReplayRecord( final StringBuilder builder, final LarvaBenchmarkReplayRecord replayRecord ) {
        builder.append( '{' );
        appendStringField( builder, "replayFilePath", replayRecord.getReplayFilePath(), true );
        appendStringField( builder, "replaySha256", replayRecord.getReplaySha256(), false );
        appendStringField( builder, "sourceDescription", replayRecord.getSourceDescription(), false );
        appendStringField( builder, "mapTitle", replayRecord.getMapTitle(), false );
        appendStringField( builder, "players", replayRecord.getPlayers(), false );
        appendStringField( builder, "winners", replayRecord.getWinners(), false );
        appendLongField( builder, "replayLengthMs", replayRecord.getReplayLengthMs(), false );
        appendIntField( builder, "replayLengthLoops", replayRecord.getReplayLengthLoops(), false );
        appendStringField( builder, "replayEndTime", replayRecord.getReplayEndTime(), false );
        appendStringField( builder, "replayVersion", replayRecord.getReplayVersion(), false );
        appendStringField( builder, "baseBuild", replayRecord.getBaseBuild(), false );
        appendBooleanField( builder, "fullReplayParseUsed", replayRecord.isFullReplayParseUsed(), false );
        appendIntField( builder, "trackedHatcheryCount", replayRecord.getTrackedHatcheryCount(), false );
        appendIntField( builder, "assignedLarvaCount", replayRecord.getAssignedLarvaCount(), false );
        appendIntField( builder, "unassignedLarvaCount", replayRecord.getUnassignedLarvaCount(), false );
        appendIntField( builder, "injectWindowCount", replayRecord.getInjectWindowCount(), false );
        appendIntField( builder, "idleInjectWindowCount", replayRecord.getIdleInjectWindowCount(), false );
        appendStringListField( builder, "diagnosticFlagList", replayRecord.getDiagnosticFlagList(), false );
        appendPlayerListField( builder, "playerRecordList", replayRecord.getPlayerRecordList(), false );
        builder.append( '}' );
    }

    /**
     * Appends player records.
     */
    private void appendPlayerListField( final StringBuilder builder, final String fieldName, final List< LarvaBenchmarkPlayerRecord > playerRecordList,
            final boolean firstField ) {
        appendFieldName( builder, fieldName, firstField );
        builder.append( '[' );
        for ( int i = 0; i < playerRecordList.size(); i++ ) {
            if ( i > 0 )
                builder.append( ',' );
            appendPlayerRecord( builder, playerRecordList.get( i ) );
        }
        builder.append( ']' );
    }

    /**
     * Appends one player record.
     */
    private void appendPlayerRecord( final StringBuilder builder, final LarvaBenchmarkPlayerRecord playerRecord ) {
        builder.append( '{' );
        appendStringField( builder, "playerName", playerRecord.getPlayerName(), true );
        appendStringField( builder, "playerRace", playerRecord.getPlayerRace(), false );
        appendStringField( builder, "opponentRace", playerRecord.getOpponentRace(), false );
        appendStringField( builder, "opponentPlayerName", playerRecord.getOpponentPlayerName(), false );
        appendStringField( builder, "matchup", playerRecord.getMatchup(), false );
        appendStringField( builder, "result", playerRecord.getResult(), false );
        appendStringListField( builder, "diagnosticFlagList", playerRecord.getDiagnosticFlagList(), false );
        appendPhaseListField( builder, "phaseRecordList", playerRecord.getPhaseRecordList(), false );
        builder.append( '}' );
    }

    /**
     * Appends phase records.
     */
    private void appendPhaseListField( final StringBuilder builder, final String fieldName, final List< LarvaBenchmarkPhaseRecord > phaseRecordList,
            final boolean firstField ) {
        appendFieldName( builder, fieldName, firstField );
        builder.append( '[' );
        for ( int i = 0; i < phaseRecordList.size(); i++ ) {
            if ( i > 0 )
                builder.append( ',' );
            appendPhaseRecord( builder, phaseRecordList.get( i ) );
        }
        builder.append( ']' );
    }

    /**
     * Appends one phase record.
     */
    private void appendPhaseRecord( final StringBuilder builder, final LarvaBenchmarkPhaseRecord phaseRecord ) {
        builder.append( '{' );
        appendStringField( builder, "phase", phaseRecord.getPhase() == null ? null : phaseRecord.getPhase().name(), true );
        appendIntField( builder, "startLoop", phaseRecord.getStartLoop(), false );
        appendIntField( builder, "endLoop", phaseRecord.getEndLoop(), false );
        appendIntField( builder, "durationLoops", phaseRecord.getDurationLoops(), false );
        appendIntField( builder, "missedLarvaCount", phaseRecord.getMissedLarvaCount(), false );
        appendIntField( builder, "missedInjectLarvaCount", phaseRecord.getMissedInjectLarvaCount(), false );
        appendIntField( builder, "totalSpawnedLarvaCount", phaseRecord.getTotalSpawnedLarvaCount(), false );
        appendLongField( builder, "hatchEligibleLoops", phaseRecord.getHatchEligibleLoops(), false );
        appendLongField( builder, "injectEligibleLoops", phaseRecord.getInjectEligibleLoops(), false );
        appendLongField( builder, "injectActiveLoops", phaseRecord.getInjectActiveLoops(), false );
        appendDoubleField( builder, "missedLarvaPerHatchPerMinute", phaseRecord.getMissedLarvaPerHatchPerMinute(), false );
        appendDoubleField( builder, "missedInjectLarvaPerHatchPerMinute", phaseRecord.getMissedInjectLarvaPerHatchPerMinute(), false );
        appendDoubleField( builder, "spawnedLarvaPerHatchPerMinute", phaseRecord.getSpawnedLarvaPerHatchPerMinute(), false );
        appendDoubleField( builder, "injectUptimePercentage", phaseRecord.getInjectUptimePercentage(), false );
        builder.append( '}' );
    }

    /**
     * Appends a string array field.
     */
    private void appendStringListField( final StringBuilder builder, final String fieldName, final List< String > valueList, final boolean firstField ) {
        appendFieldName( builder, fieldName, firstField );
        builder.append( '[' );
        for ( int i = 0; i < valueList.size(); i++ ) {
            if ( i > 0 )
                builder.append( ',' );
            appendQuoted( builder, valueList.get( i ) );
        }
        builder.append( ']' );
    }

    /**
     * Appends a string field.
     */
    private void appendStringField( final StringBuilder builder, final String fieldName, final String value, final boolean firstField ) {
        appendFieldName( builder, fieldName, firstField );
        appendQuoted( builder, value );
    }

    /**
     * Appends an integer field.
     */
    private void appendIntField( final StringBuilder builder, final String fieldName, final int value, final boolean firstField ) {
        appendFieldName( builder, fieldName, firstField );
        builder.append( value );
    }

    /**
     * Appends a long field.
     */
    private void appendLongField( final StringBuilder builder, final String fieldName, final long value, final boolean firstField ) {
        appendFieldName( builder, fieldName, firstField );
        builder.append( value );
    }

    /**
     * Appends a boolean field.
     */
    private void appendBooleanField( final StringBuilder builder, final String fieldName, final boolean value, final boolean firstField ) {
        appendFieldName( builder, fieldName, firstField );
        builder.append( value );
    }

    /**
     * Appends a nullable double field.
     */
    private void appendDoubleField( final StringBuilder builder, final String fieldName, final Double value, final boolean firstField ) {
        appendFieldName( builder, fieldName, firstField );
        if ( value == null )
            builder.append( "null" );
        else
            builder.append( safeDouble( value.doubleValue() ) );
    }

    /**
     * Appends a field name.
     */
    private void appendFieldName( final StringBuilder builder, final String fieldName, final boolean firstField ) {
        if ( !firstField )
            builder.append( ',' );
        appendQuoted( builder, fieldName );
        builder.append( ':' );
    }

    /**
     * Appends a JSON string literal.
     */
    private void appendQuoted( final StringBuilder builder, final String value ) {
        if ( value == null ) {
            builder.append( "null" );
            return;
        }

        builder.append( '"' );
        for ( int i = 0; i < value.length(); i++ ) {
            final char c = value.charAt( i );
            switch ( c ) {
            case '\\':
                builder.append( "\\\\" );
                break;
            case '"':
                builder.append( "\\\"" );
                break;
            case '\b':
                builder.append( "\\b" );
                break;
            case '\f':
                builder.append( "\\f" );
                break;
            case '\n':
                builder.append( "\\n" );
                break;
            case '\r':
                builder.append( "\\r" );
                break;
            case '\t':
                builder.append( "\\t" );
                break;
            default:
                if ( c < 0x20 )
                    builder.append( String.format( "\\u%04x", Integer.valueOf( c ) ) );
                else
                    builder.append( c );
            }
        }
        builder.append( '"' );
    }

    /**
     * Formats doubles deterministically without scientific notation for normal replay values.
     */
    private String safeDouble( final double value ) {
        final String text = String.format( java.util.Locale.US, "%.10f", Double.valueOf( value ) );
        int end = text.length();
        while ( end > 0 && text.charAt( end - 1 ) == '0' )
            end--;
        if ( end > 0 && text.charAt( end - 1 ) == '.' )
            end--;
        return end <= 0 ? "0" : text.substring( 0, end );
    }

}