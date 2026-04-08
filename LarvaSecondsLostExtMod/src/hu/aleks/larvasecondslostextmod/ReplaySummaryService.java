package hu.aleks.larvasecondslostextmod;

import hu.scelightapi.sc2.balancedata.model.IAbility;
import hu.scelightapi.sc2.rep.model.IEvent;
import hu.scelightapi.sc2.rep.model.IReplay;
import hu.scelightapi.sc2.rep.model.details.IDetails;
import hu.scelightapi.sc2.rep.model.details.IPlayer;
import hu.scelightapi.sc2.rep.model.details.IRace;
import hu.scelightapi.sc2.rep.model.details.IResult;
import hu.scelightapi.sc2.rep.model.gameevents.IGameEvents;
import hu.scelightapi.sc2.rep.model.gameevents.cmd.ICmdEvent;
import hu.scelightapi.sc2.rep.factory.IRepParserEngine;
import hu.scelightapi.sc2.rep.repproc.IRepProcessor;
import hu.scelightapi.sc2.rep.repproc.IUser;
import hu.scelightapi.service.IFactory;
import hu.scelightapibase.service.log.ILogger;

import java.awt.Color;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Service that loads a replay processor and extracts lightweight replay diagnostics.
 */
public class ReplaySummaryService {

    /** Fallback mode description for Epic 2. */
    private static final String FALLBACK_INTEGRATION_MODE = "Module-owned replay diagnostics page (fallback replay-view surface)";

    /** Date formatter for replay end time. */
    private final SimpleDateFormat dateFormat = new SimpleDateFormat( "yyyy-MM-dd HH:mm:ss" );

    /** Shared Scelight factory surface. */
    private final IFactory factory;

    /** Replay parser engine used to load replay processors. */
    private final IRepParserEngine repParserEngine;

    /** Logger used for diagnostics. */
    private final ILogger logger;

    /** Epic 6 replay analyzer. */
    private final LarvaReplayAnalyzer larvaReplayAnalyzer;

    /** Builds the normalized Epic 03 timeline presentation model. */
    private final LarvaTimelineModelBuilder timelineModelBuilder;

    /**
     * Creates a new replay summary service.
     *
     * @param module owning external module
     */
    public ReplaySummaryService( final LarvaSecondsLostModule module ) {
        this( module.factory, module.repParserEngine, module.logger );
    }

    /**
     * Creates a new replay summary service from explicit standalone dependencies.
     *
     * @param factory Scelight factory used by the analyzer
     * @param repParserEngine replay parser engine used to load replay processors
     * @param logger logger used for diagnostics
     */
    public ReplaySummaryService( final IFactory factory, final IRepParserEngine repParserEngine, final ILogger logger ) {
        if ( factory == null )
            throw new IllegalArgumentException( "Factory is required." );
        if ( repParserEngine == null )
            throw new IllegalArgumentException( "Replay parser engine is required." );
        if ( logger == null )
            throw new IllegalArgumentException( "Logger is required." );
        this.factory = factory;
        this.repParserEngine = repParserEngine;
        this.logger = logger;
        larvaReplayAnalyzer = new LarvaReplayAnalyzer( factory );
        timelineModelBuilder = new LarvaTimelineModelBuilder();
    }

    /**
     * Loads a replay summary for the specified replay file.
     *
     * @param replayFile replay file to analyze
     * @param sourceDescription short description of how the replay was selected
    * @return replay summary to display on the module page
     */
    public LarvaReplayPageSummary analyze( final Path replayFile, final String sourceDescription ) {
        final ReplayAnalysisContext context = analyzeReplayContext( replayFile, sourceDescription );
        final long lengthMs = context.repProc.getLengthMs();
        final long previewWindowStartMs = lengthMs <= 0L ? 0L : lengthMs / 4L;
        final long previewWindowDurationMs = lengthMs <= 0L ? 0L : Math.max( 10000L, lengthMs / 5L );
        final long previewWindowEndMs = lengthMs <= 0L ? 0L : Math.min( lengthMs, previewWindowStartMs + previewWindowDurationMs );
        final LarvaTimelineModel timelineModel = timelineModelBuilder.build( context.replaySummary, FALLBACK_INTEGRATION_MODE, context.larvaAnalysisReport,
                previewWindowStartMs, previewWindowEndMs );
        return new LarvaReplayPageSummary( context.replaySummary, FALLBACK_INTEGRATION_MODE, timelineModel, previewWindowStartMs, previewWindowEndMs,
                context.larvaAnalysisReport );
    }

    /**
     * Loads a replay into a benchmark-friendly immutable export model without building page UI state.
     *
     * @param replayFile replay file to analyze
     * @param sourceDescription short description of how the replay was selected
     * @return headless benchmark export model
     */
    public LarvaBenchmarkReplayRecord analyzeBenchmarkReplay( final Path replayFile, final String sourceDescription ) {
        final ReplayAnalysisContext context = analyzeReplayContext( replayFile, sourceDescription );
        return buildBenchmarkReplayRecord( context );
    }

    /**
     * Builds the shared replay-analysis context used by both page rendering and headless benchmark export.
     *
     * @param replayFile replay file to analyze
     * @param sourceDescription short description of how the replay was selected
     * @return analysis context
     */
    private ReplayAnalysisContext analyzeReplayContext( final Path replayFile, final String sourceDescription ) {
        if ( replayFile == null )
            throw new IllegalArgumentException( "Replay file is required." );

        if ( !Files.exists( replayFile ) )
            throw new IllegalArgumentException( "Replay file does not exist: " + replayFile );

        IRepProcessor repProc = repParserEngine.getRepProc( replayFile );
        if ( repProc == null )
            throw new IllegalStateException( "Scelight could not parse the replay file." );

        boolean fullReplayParseUsed = false;
        if ( repProc.getReplay().getTrackerEvents() == null || repProc.getReplay().getGameEvents() == null ) {
            logger.debug( "Larva analysis requires full replay events; reparsing replay instead of using cache-only processor: " + replayFile );
            final IRepProcessor reparsedRepProc = repParserEngine.parseAndWrapReplay( replayFile );
            if ( reparsedRepProc != null ) {
                repProc = reparsedRepProc;
                fullReplayParseUsed = true;
            }
        }

        final IReplay replay = repProc.getReplay();
        final IDetails details = replay.getDetails();
        final Date replayEndTime = details == null ? null : details.getTime();
        final long lengthMs = repProc.getLengthMs();
        final LarvaAnalysisReport larvaAnalysisReport = larvaReplayAnalyzer.analyze( repProc, fullReplayParseUsed );
        logger.debug( "Larva analysis summary for " + replayFile.getFileName() + ": trackerEvents=" + larvaAnalysisReport.getTrackerEventCount()
            + ", gameEvents=" + larvaAnalysisReport.getGameEventCount() + ", hatcheries=" + larvaAnalysisReport.getTrackedHatcheryCount() + ", larvaBirths="
            + larvaAnalysisReport.getLarvaBirthCount() + ", assigned=" + larvaAnalysisReport.getAssignedLarvaCount() + ", unassigned="
            + larvaAnalysisReport.getUnassignedLarvaCount() + ", ambiguous=" + larvaAnalysisReport.getAmbiguousLarvaCount() + ", noEligible="
            + larvaAnalysisReport.getNoEligibleHatcheryLarvaCount() + ", injectEvidence=" + larvaAnalysisReport.getInjectEvidenceCount()
            + ", injectWindows=" + larvaAnalysisReport.getInjectWindowCount() + ", injectOverlapDiscarded="
            + larvaAnalysisReport.getInjectOverlapDiscardCount() + ", fullReplayParseUsed=" + larvaAnalysisReport.isFullReplayParseUsed() );
        logSpawnLarvaCommands( repProc );

        final ReplaySummary replaySummary = new ReplaySummary( replayFile, safe( sourceDescription, "Unknown" ),
            safe( details == null ? null : details.getTitle(), "Unknown map" ), safe( repProc.getPlayersGrouped(), "Unknown players" ),
            safe( repProc.getWinnersString(), "Unknown / undecided" ), formatDuration( lengthMs ), lengthMs, formatDate( replayEndTime ),
            replay.getHeader().versionString( true ), String.valueOf( replay.getHeader().getBaseBuild() ), buildPlayerColorMap( details ) );
        return new ReplayAnalysisContext( repProc, details, replaySummary, larvaAnalysisReport );
    }

    /**
     * Builds a headless benchmark export model from a shared replay-analysis context.
     *
     * @param context shared replay-analysis context
     * @return benchmark replay record
     */
    private LarvaBenchmarkReplayRecord buildBenchmarkReplayRecord( final ReplayAnalysisContext context ) {
        final List< String > replayDiagnosticFlagList = buildReplayDiagnosticFlagList( context );
        final List< LarvaBenchmarkPlayerRecord > playerRecordList = new ArrayList<>();
        final Map< String, IPlayer > playerByName = indexPlayersByName( context.details );
        for ( final Map.Entry< String, LarvaPlayerPhaseTable > entry : context.larvaAnalysisReport.getPlayerPhaseTableByPlayerName().entrySet() ) {
            final String playerName = entry.getKey();
            final IPlayer player = playerByName.get( playerName );
            if ( player != null && !IRace.ZERG.equals( player.getRace() ) )
                continue;

            final IPlayer opponent = findOpponent( context.details, playerName );
            final List< String > playerDiagnosticFlagList = buildPlayerDiagnosticFlagList( playerName, context.larvaAnalysisReport );
            playerRecordList.add( new LarvaBenchmarkPlayerRecord( playerName, safeRaceName( player == null ? null : player.getRace() ),
                safeRaceName( opponent == null ? null : opponent.getRace() ), safe( opponent == null ? null : opponent.getName(), "Unknown" ),
                buildMatchupLabel( player == null ? null : player.getRace(), opponent == null ? null : opponent.getRace() ),
                safeResult( player ), playerDiagnosticFlagList, buildPhaseRecordList( entry.getValue() ) ) );
        }

        return new LarvaBenchmarkReplayRecord( context.replaySummary.getReplayFile() == null ? null : context.replaySummary.getReplayFile().toString(),
            computeSha256Hex( context.replaySummary.getReplayFile() ), context.replaySummary.getSourceDescription(), context.replaySummary.getMapTitle(),
            context.replaySummary.getPlayers(), context.replaySummary.getWinners(), context.replaySummary.getLengthMs(),
            context.larvaAnalysisReport.getReplayLengthLoops(), context.replaySummary.getReplayEndTime(), context.replaySummary.getReplayVersion(),
            context.replaySummary.getBaseBuild(), context.larvaAnalysisReport.isFullReplayParseUsed(), context.larvaAnalysisReport.getTrackedHatcheryCount(),
            context.larvaAnalysisReport.getAssignedLarvaCount(), context.larvaAnalysisReport.getUnassignedLarvaCount(),
            context.larvaAnalysisReport.getInjectWindowCount(), context.larvaAnalysisReport.getIdleInjectWindowCount(), replayDiagnosticFlagList,
            playerRecordList );
    }

    /**
     * Builds phase-level benchmark export rows in stable gameplay-phase order.
     *
     * @param playerPhaseTable phase table to export
     * @return immutable-ready phase record list
     */
    private List< LarvaBenchmarkPhaseRecord > buildPhaseRecordList( final LarvaPlayerPhaseTable playerPhaseTable ) {
        final List< LarvaBenchmarkPhaseRecord > phaseRecordList = new ArrayList<>();
        if ( playerPhaseTable == null )
            return phaseRecordList;

        for ( final LarvaGamePhase phase : LarvaGamePhase.values() ) {
            final LarvaPhaseInterval interval = playerPhaseTable.getPhaseInterval( phase );
            final LarvaPhaseStats phaseStats = playerPhaseTable.getPhaseStats( phase );
            phaseRecordList.add( new LarvaBenchmarkPhaseRecord( phase, interval == null ? 0 : interval.getStartLoop(), interval == null ? 0 : interval.getEndLoop(),
                interval == null ? 0 : interval.getDurationLoops(), phaseStats == null ? 0 : phaseStats.getMissedLarvaCount(),
                phaseStats == null ? 0 : phaseStats.getMissedInjectLarvaCount(), phaseStats == null ? 0 : phaseStats.getTotalSpawnedLarvaCount(),
                phaseStats == null ? 0L : phaseStats.getHatchEligibleLoops(), phaseStats == null ? 0L : phaseStats.getInjectEligibleLoops(),
                phaseStats == null ? 0L : phaseStats.getInjectActiveLoops(), phaseStats == null ? null : phaseStats.getMissedLarvaPerHatchPerMinute(),
                phaseStats == null ? null : phaseStats.getMissedInjectLarvaPerHatchPerMinute(),
                phaseStats == null ? null : phaseStats.getSpawnedLarvaPerHatchPerMinute(),
                phaseStats == null ? null : phaseStats.getInjectUptimePercentage() ) );
        }

        return phaseRecordList;
    }

    /**
     * Indexes replay players by player name.
     *
     * @param details replay details
     * @return player map by name
     */
    private Map< String, IPlayer > indexPlayersByName( final IDetails details ) {
        final Map< String, IPlayer > playerByName = new LinkedHashMap<>();
        if ( details == null || details.getPlayerList() == null )
            return playerByName;

        for ( final IPlayer player : details.getPlayerList() )
            if ( player != null && player.getName() != null && player.getName().length() > 0 )
                playerByName.put( player.getName(), player );

        return playerByName;
    }

    /**
     * Finds a simple opponent candidate for a player from replay details.
     *
     * @param details replay details
     * @param playerName analyzed player name
     * @return opponent candidate, or <code>null</code>
     */
    private IPlayer findOpponent( final IDetails details, final String playerName ) {
        if ( details == null || details.getPlayerList() == null )
            return null;

        for ( final IPlayer player : details.getPlayerList() ) {
            if ( player == null || player.getName() == null || player.getName().length() == 0 || player.getName().equals( playerName ) )
                continue;
            return player;
        }

        return null;
    }

    /**
     * Builds stable replay-level diagnostic flags for later batch filtering.
     *
     * @param context shared replay-analysis context
     * @return diagnostic flag list
     */
    private List< String > buildReplayDiagnosticFlagList( final ReplayAnalysisContext context ) {
        final List< String > diagnosticFlagList = new ArrayList<>();
        if ( context.larvaAnalysisReport.isFullReplayParseUsed() )
            diagnosticFlagList.add( "full-replay-reparse" );
        if ( context.larvaAnalysisReport.getUnassignedLarvaCount() > 0 )
            diagnosticFlagList.add( "unassigned-larva-present" );
        if ( context.larvaAnalysisReport.getAmbiguousLarvaCount() > 0 )
            diagnosticFlagList.add( "ambiguous-larva-present" );
        if ( context.larvaAnalysisReport.getNoEligibleHatcheryLarvaCount() > 0 )
            diagnosticFlagList.add( "no-eligible-hatchery-larva-present" );
        if ( context.details == null || context.details.getPlayerList() == null )
            diagnosticFlagList.add( "missing-player-details" );
        else if ( context.details.getPlayerList().length > 2 )
            diagnosticFlagList.add( "multi-player-replay" );
        return diagnosticFlagList;
    }

    /**
     * Builds stable player-level diagnostic flags for later batch filtering.
     *
     * @param playerName analyzed player name
     * @param larvaAnalysisReport analysis report
     * @return diagnostic flag list
     */
    private List< String > buildPlayerDiagnosticFlagList( final String playerName, final LarvaAnalysisReport larvaAnalysisReport ) {
        final List< String > diagnosticFlagList = new ArrayList<>();
        final List< LarvaPlayerResourceSnapshot > snapshotList = larvaAnalysisReport.getResourceSnapshotsByPlayerName().get( playerName );
        if ( snapshotList == null || snapshotList.isEmpty() )
            diagnosticFlagList.add( "missing-resource-snapshots" );
        if ( larvaAnalysisReport.getPlayerPhaseTable( playerName ) == null )
            diagnosticFlagList.add( "missing-phase-table" );
        return diagnosticFlagList;
    }

    /**
     * Builds a matchup label from player and opponent race.
     *
     * @param playerRace analyzed player race
     * @param opponentRace opponent race
     * @return matchup label
     */
    private String buildMatchupLabel( final IRace playerRace, final IRace opponentRace ) {
        if ( playerRace == null || opponentRace == null )
            return "Unknown";
        return String.valueOf( raceLetter( playerRace ) ) + 'v' + raceLetter( opponentRace );
    }

    /**
     * Resolves a stable race letter.
     *
     * @param race race to convert
     * @return uppercase race letter, or <code>'?'</code>
     */
    private char raceLetter( final IRace race ) {
        if ( race == null )
            return '?';

        final char letter = race.getLetter();
        return letter == 0 ? '?' : Character.toUpperCase( letter );
    }

    /**
     * Returns a readable race name.
     *
     * @param race race to render
     * @return readable race name
     */
    private String safeRaceName( final IRace race ) {
        return race == null ? "Unknown" : safe( race.toString(), "Unknown" );
    }

    /**
     * Returns a readable result string.
     *
     * @param player replay player
     * @return readable result string
     */
    private String safeResult( final IPlayer player ) {
        final IResult result = player == null ? null : player.getResult();
        return result == null ? "Unknown" : safe( result.toString(), "Unknown" );
    }

    /**
     * Computes a SHA-256 replay hash for stable offline-corpus identity.
     *
     * @param replayFile replay file to hash
     * @return lowercase SHA-256 digest text
     */
    private String computeSha256Hex( final Path replayFile ) {
        if ( replayFile == null )
            return "";

        final MessageDigest digest;
        try {
            digest = MessageDigest.getInstance( "SHA-256" );
        } catch ( final NoSuchAlgorithmException e ) {
            throw new IllegalStateException( "SHA-256 digest algorithm is unavailable.", e );
        }

        try ( InputStream input = Files.newInputStream( replayFile ) ) {
            final byte[] buffer = new byte[ 8192 ];
            int readCount;
            while ( ( readCount = input.read( buffer ) ) >= 0 ) {
                if ( readCount > 0 )
                    digest.update( buffer, 0, readCount );
            }
        } catch ( final IOException e ) {
            throw new IllegalStateException( "Failed to hash replay file: " + replayFile, e );
        }

        final byte[] hash = digest.digest();
        final StringBuilder builder = new StringBuilder( hash.length * 2 );
        for ( final byte value : hash ) {
            final int unsignedValue = value & 0xff;
            if ( unsignedValue < 0x10 )
                builder.append( '0' );
            builder.append( Integer.toHexString( unsignedValue ) );
        }
        return builder.toString();
    }

    /**
     * Formats a replay duration.
     *
     * @param lengthMs replay duration in milliseconds
     * @return formatted duration string
     */
    private String formatDuration( final long lengthMs ) {
        long totalSeconds = lengthMs / 1000L;
        final long hours = totalSeconds / 3600L;
        totalSeconds %= 3600L;
        final long minutes = totalSeconds / 60L;
        final long seconds = totalSeconds % 60L;

        final StringBuilder builder = new StringBuilder();
        if ( hours > 0L ) {
            builder.append( hours );
            builder.append( 'h' );
            builder.append( ' ' );
        }

        if ( minutes < 10L && hours > 0L )
            builder.append( '0' );
        builder.append( minutes );
        builder.append( 'm' );
        builder.append( ' ' );
        if ( seconds < 10L )
            builder.append( '0' );
        builder.append( seconds );
        builder.append( 's' );

        return builder.toString();
    }

    /**
     * Formats a replay end time.
     *
     * @param replayEndTime replay end time
     * @return formatted replay end time string
     */
    private String formatDate( final Date replayEndTime ) {
        return replayEndTime == null ? "Unknown" : dateFormat.format( replayEndTime );
    }

    /**
     * Returns a non-empty string.
     *
     * @param value value to sanitize
     * @param fallback fallback value
     * @return sanitized value
     */
    private String safe( final String value, final String fallback ) {
        return value == null || value.length() == 0 ? fallback : value;
    }

    /**
     * Logs all replay-exposed SpawnLarva commands in a simple player / hatch / timestamp form.
     *
     * @param repProc replay processor
     */
    private void logSpawnLarvaCommands( final IRepProcessor repProc ) {
        final IReplay replay = repProc.getReplay();
        final IEvent[] gameEventArray = replay == null || replay.getGameEvents() == null ? null : replay.getGameEvents().getEvents();

        if ( gameEventArray == null ) {
            logger.debug( "Larva inject command dump unavailable because the replay has no game events." );
            return;
        }

        int injectCommandCount = 0;
        for ( final IEvent event : gameEventArray ) {
            if ( event == null || event.getId() != IGameEvents.ID_CMD || !( event instanceof ICmdEvent ) )
                continue;

            final ICmdEvent cmdEvent = (ICmdEvent) event;
            if ( cmdEvent.getCommand() == null || !IAbility.ID_SPAWN_LARVA.equals( cmdEvent.getCommand().getAbilId() ) || cmdEvent.getTargetUnit() == null
                    || cmdEvent.getTargetUnit().getTag() == null )
                continue;

            injectCommandCount++;

            final Integer hatcheryTag = cmdEvent.getTargetUnit().getTag();
            final String hatcheryTagText = repProc.getTagTransformation() == null ? String.valueOf( hatcheryTag )
                    : repProc.getTagTransformation().tagToString( hatcheryTag.intValue() );
            final String playerName = resolvePlayerName( repProc, event.getPlayerId(), event.getUserId() );

                logger.debug( "Larva inject command: " + playerName + " injected hatch " + hatcheryTagText + " at "
                    + repProc.formatLoopTime( event.getLoop() ) + " (loop " + event.getLoop() + ")." );
        }

        if ( injectCommandCount == 0 )
                logger.debug( "Larva inject command dump: no SpawnLarva target commands were exposed for this replay." );
    }

    /**
     * Resolves a player name from either player id or user id.
     *
     * @param repProc replay processor
     * @param playerId player id if available
     * @param userId user id if available
     * @return resolved player name
     */
    private String resolvePlayerName( final IRepProcessor repProc, final Integer playerId, final int userId ) {
        if ( playerId != null ) {
            final IUser[] usersByPlayerId = repProc.getUsersByPlayerId();
            if ( usersByPlayerId != null && playerId.intValue() > 0 && playerId.intValue() < usersByPlayerId.length
                    && usersByPlayerId[ playerId.intValue() ] != null && usersByPlayerId[ playerId.intValue() ].getName() != null
                    && usersByPlayerId[ playerId.intValue() ].getName().length() > 0 )
                return usersByPlayerId[ playerId.intValue() ].getName();
        }

        final IUser user = repProc.getUser( userId );
        if ( user != null && user.getName() != null && user.getName().length() > 0 )
            return user.getName();

        return playerId == null ? "Unknown player" : "Player " + playerId;
    }

    /**
     * Builds a player-name to player-color map from replay details.
     *
     * @param details replay details
     * @return immutable-ready player color map
     */
    private Map< String, Color > buildPlayerColorMap( final IDetails details ) {
        final Map< String, Color > playerColorMap = new LinkedHashMap<>();
        if ( details == null || details.getPlayerList() == null )
            return playerColorMap;

        for ( final IPlayer player : details.getPlayerList() ) {
            if ( player == null || player.getName() == null || player.getName().length() == 0 )
                continue;

            final Color color = toPlayerColor( player.getArgb() );
            if ( color != null )
                playerColorMap.put( player.getName(), color );
        }

        return playerColorMap;
    }

    /**
     * Converts replay ARGB values to an AWT color.
     *
     * @param argb replay ARGB components
     * @return converted color; may be <code>null</code>
     */
    private Color toPlayerColor( final int[] argb ) {
        if ( argb == null || argb.length < 4 )
            return null;

        final int alpha = clampColorComponent( argb[ 0 ] );
        final int red = clampColorComponent( argb[ 1 ] );
        final int green = clampColorComponent( argb[ 2 ] );
        final int blue = clampColorComponent( argb[ 3 ] );
        if ( alpha == 0 )
            return null;

        return new Color( red, green, blue );
    }

    /**
     * Clamps a replay color component into the AWT range.
     *
     * @param value raw component value
     * @return clamped component
     */
    private int clampColorComponent( final int value ) {
        if ( value < 0 )
            return 0;
        if ( value > 255 )
            return 255;
        return value;
    }

    /**
     * Shared immutable replay-analysis context used by page and benchmark export flows.
     */
    private static class ReplayAnalysisContext {

        /** Replay processor used for the analysis. */
        private final IRepProcessor repProc;

        /** Replay details used for player metadata lookups. */
        private final IDetails details;

        /** Lightweight replay summary. */
        private final ReplaySummary replaySummary;

        /** Core larva-analysis output. */
        private final LarvaAnalysisReport larvaAnalysisReport;

        /**
         * Creates a new analysis context.
         */
        private ReplayAnalysisContext( final IRepProcessor repProc, final IDetails details, final ReplaySummary replaySummary,
                final LarvaAnalysisReport larvaAnalysisReport ) {
            this.repProc = repProc;
            this.details = details;
            this.replaySummary = replaySummary;
            this.larvaAnalysisReport = larvaAnalysisReport;
        }

    }

}