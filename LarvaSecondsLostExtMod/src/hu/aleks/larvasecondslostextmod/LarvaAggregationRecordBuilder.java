package hu.aleks.larvasecondslostextmod;

import hu.scelightapi.sc2.rep.model.IReplay;
import hu.scelightapi.sc2.rep.model.attributesevents.IGameMode;
import hu.scelightapi.sc2.rep.model.details.IDetails;
import hu.scelightapi.sc2.rep.model.details.IPlayer;
import hu.scelightapi.sc2.rep.model.details.IRace;
import hu.scelightapi.sc2.rep.model.details.IToon;
import hu.scelightapi.sc2.rep.repproc.IRepProcessor;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Builds {@link LarvaAggregationGameRecord}s (E17-S09). Kept out of {@code ReplaySummaryService} on purpose.
 *
 * <p>The core ({@link #buildPlayerRecord}, {@link #buildIdentityKey}) works on plain values so unit tests need no Scelight
 * runtime. The replay-facing entry points ({@link #buildFull}, {@link #buildMetadataOnly}) adapt Scelight types to it.
 * Nothing built here retains the analysis report, the replay processor or the timeline model.</p>
 */
final class LarvaAggregationRecordBuilder {

    /** Flag set on records built without a full analysis. */
    static final String FLAG_METADATA_ONLY = "metadata-only";

    private static final String NAME_KEY_PREFIX = "name:";

    private LarvaAggregationRecordBuilder() {
    }

    /** Converts a replay loop to display milliseconds (Scelight real-time setting, game speed). */
    interface LoopTimeConverter {
        long loopToTimeMs( int loop );
    }

    /** Plain per-player input for {@link #buildPlayerRecord}. */
    static final class PlayerInput {

        String identityKey;

        String toonString;

        String name;

        IRace race;

        IRace opponentRace;

        /** <code>null</code> if the analysis produced no phase table for the player. */
        LarvaPlayerPhaseTable phaseTable;

        /** Resource snapshots; only meaningful for Zerg players (non-Zerg worker counts are SCV/probe counts). */
        List< LarvaPlayerResourceSnapshot > snapshots;

        Integer exactDrone66Loop;

        Integer exactDrone80Loop;
    }

    /**
     * Derives the identity key from toon fields (D-5, C-10): {@code region-program-realm-id}. Never uses
     * {@code IToon.toString()}, which appends the player name. A null or zero toon falls back to {@code "name:" + name}.
     */
    static String buildIdentityKey( final IToon toon, final String name ) {
        if ( toon == null )
            return buildIdentityKey( null, null, null, null, true, name );
        return buildIdentityKey( toon.getRegionId(), toon.getProgramId(), toon.getRealmId(), toon.getId(), toon.isZero(), name );
    }

    /** Plain-value variant of {@link #buildIdentityKey(IToon, String)}; any missing field also selects the name fallback. */
    static String buildIdentityKey( final Integer regionId, final String programId, final Integer realmId, final Integer id, final boolean zero,
            final String name ) {
        if ( zero || regionId == null || programId == null || realmId == null || id == null )
            return NAME_KEY_PREFIX + name;
        return regionId + "-" + programId + "-" + realmId + "-" + id;
    }

    /**
     * Builds one player record. Phases with duration 0 or a missing interval count as "not reached" (D-9); reached phases carry their end time (CR-1.1). Milestones use the
     * exact drone loops where available (S08) and are converted to display ms now (C-9, D-11).
     */
    static LarvaAggregationPlayerRecord buildPlayerRecord( final PlayerInput input, final LoopTimeConverter converter ) {
        final LarvaAggregationPlayerRecord.Builder builder = new LarvaAggregationPlayerRecord.Builder().identityKey( input.identityKey )
                .toonString( input.toonString ).name( input.name ).race( input.race ).opponentRace( input.opponentRace )
                .matchup( LarvaGameKinds.matchupOf( input.opponentRace ) );
        if ( input.phaseTable == null )
            builder.missingPhaseTable( true );
        else
            addPhaseData( builder, input.phaseTable, converter );
        builder.milestoneDisplayMs( buildMilestoneDisplayMs( input, converter ) );
        return builder.build();
    }

    private static void addPhaseData( final LarvaAggregationPlayerRecord.Builder builder, final LarvaPlayerPhaseTable table,
            final LoopTimeConverter converter ) {
        final Map< LarvaGamePhase, Integer > endLoops = new EnumMap<>( LarvaGamePhase.class );
        final Map< LarvaGamePhase, Long > endMs = new EnumMap<>( LarvaGamePhase.class );
        for ( final LarvaGamePhase phase : LarvaGamePhase.values() ) {
            final LarvaPhaseInterval interval = table.getPhaseInterval( phase );
            // A phase with duration 0 (or no interval) was not reached: it has no end time (D-9, CR-1.1).
            if ( interval == null || interval.getDurationLoops() <= 0 )
                continue;
            endLoops.put( phase, Integer.valueOf( interval.getEndLoop() ) );
            endMs.put( phase, Long.valueOf( converter.loopToTimeMs( interval.getEndLoop() ) ) );
        }
        builder.phaseStats( table.getPhaseStatsMap() ).phaseEndLoops( endLoops ).phaseEndDisplayMs( endMs );
    }

    private static Map< LarvaMilestone, Long > buildMilestoneDisplayMs( final PlayerInput input, final LoopTimeConverter converter ) {
        final Map< LarvaMilestone, Long > result = new EnumMap<>( LarvaMilestone.class );
        final Map< LarvaMilestone, Integer > loops = LarvaMilestoneCalculator.compute( input.snapshots, input.exactDrone66Loop,
                input.exactDrone80Loop );
        for ( final Map.Entry< LarvaMilestone, Integer > entry : loops.entrySet() )
            result.put( entry.getKey(), Long.valueOf( converter.loopToTimeMs( entry.getValue().intValue() ) ) );
        return result;
    }

    /**
     * Builds the record for a fully analyzed replay. Only Zerg players (race from {@code IDetails}, never from map
     * membership) are included. The report is only read here and not retained.
     */
    static LarvaAggregationGameRecord buildFull( final Path replayFile, final IDetails details, final IGameMode gameMode,
            final hu.scelightapi.sc2.rep.repproc.IFormat format, final LarvaAnalysisReport report ) {
        final LoopTimeConverter converter = new LoopTimeConverter() {
            @Override
            public long loopToTimeMs( final int loop ) {
                return report.loopToTimeMs( loop );
            }
        };
        final LarvaAggregationGameRecord.Builder game = new LarvaAggregationGameRecord.Builder().replayFile( replayFile )
                .replayDate( details == null ? null : details.getTime() ).gameMode( gameMode ).format( format )
                .oneVsOne( LarvaGameKinds.isOneVsOne( format ) ).ladder( LarvaGameKinds.isLadder( gameMode ) )
                .lengthLoops( report.getReplayLengthLoops() ).fullParse( report.isFullReplayParseUsed() )
                .diagnosticFlags( ReplaySummaryService.buildReplayDiagnosticFlagList( report, details ) );
        for ( final IPlayer player : zergPlayersOf( details ) ) {
            final PlayerInput input = newPlayerInput( details, player );
            final String name = player.getName();
            input.phaseTable = report.getPlayerPhaseTable( name );
            input.snapshots = report.getResourceSnapshotsByPlayerName().get( name );
            input.exactDrone66Loop = report.getFirstDroneCountLoop( name, LarvaMilestone.DRONES_66.getThreshold() );
            input.exactDrone80Loop = report.getFirstDroneCountLoop( name, LarvaMilestone.DRONES_80.getThreshold() );
            game.addZergPlayer( buildPlayerRecord( input, converter ) );
        }
        return game.build();
    }

    /**
     * Builds a record from replay metadata only, for the S02 pre-filter. Works on a cache-only processor (no events): format,
     * mode, date, header length and player races/toons are all available there and identical to a full parse. Zerg players
     * carry identity and races but no phase or milestone data, and the record is flagged {@value #FLAG_METADATA_ONLY}.
     */
    static LarvaAggregationGameRecord buildMetadataOnly( final IRepProcessor repProc ) {
        if ( repProc == null || repProc.getFile() == null )
            throw new IllegalArgumentException( "Replay processor with a file is required." );
        final IReplay replay = repProc.getReplay();
        final IDetails details = replay == null ? null : replay.getDetails();
        final Integer loops = replay == null || replay.getHeader() == null ? null : replay.getHeader().getElapsedGameLoops();
        final LarvaAggregationGameRecord.Builder game = new LarvaAggregationGameRecord.Builder().replayFile( repProc.getFile() )
                .replayDate( details == null ? null : details.getTime() )
                .gameMode( replay == null || replay.getAttributesEvents() == null ? null : replay.getAttributesEvents().getGameMode() )
                .format( repProc.getFormat() )
                .oneVsOne( LarvaGameKinds.isOneVsOne( repProc.getFormat() ) )
                .ladder( LarvaGameKinds.isLadder( replay == null || replay.getAttributesEvents() == null ? null : replay.getAttributesEvents().getGameMode() ) )
                .lengthLoops( loops == null ? 0 : loops.intValue() ).fullParse( false )
                .diagnosticFlags( Collections.singletonList( FLAG_METADATA_ONLY ) );
        for ( final IPlayer player : zergPlayersOf( details ) ) {
            final PlayerInput input = newPlayerInput( details, player );
            game.addZergPlayer( new LarvaAggregationPlayerRecord.Builder().identityKey( input.identityKey ).toonString( input.toonString )
                    .name( input.name ).race( input.race ).opponentRace( input.opponentRace )
                .matchup( LarvaGameKinds.matchupOf( input.opponentRace ) ).build() );
        }
        return game.build();
    }

    private static List< IPlayer > zergPlayersOf( final IDetails details ) {
        final List< IPlayer > result = new ArrayList<>();
        if ( details == null || details.getPlayerList() == null )
            return result;
        for ( final IPlayer player : details.getPlayerList() )
            if ( player != null && player.getName() != null && player.getName().length() > 0 && IRace.ZERG.equals( player.getRace() ) )
                result.add( player );
        return result;
    }

    private static PlayerInput newPlayerInput( final IDetails details, final IPlayer player ) {
        final PlayerInput input = new PlayerInput();
        input.name = player.getName();
        input.race = player.getRace();
        input.identityKey = buildIdentityKey( player.getToon(), player.getName() );
        input.toonString = player.getToon() == null ? null : player.getToon().toString();
        input.opponentRace = findOneVsOneOpponentRace( details, player );
        return input;
    }

    /** D-7: race of the other player in a two-player replay, otherwise <code>null</code>. */
    private static IRace findOneVsOneOpponentRace( final IDetails details, final IPlayer player ) {
        final IPlayer[] players = details.getPlayerList();
        if ( players.length != 2 )
            return null;
        for ( final IPlayer other : players )
            if ( other != null && other != player )
                return other.getRace();
        return null;
    }
}
