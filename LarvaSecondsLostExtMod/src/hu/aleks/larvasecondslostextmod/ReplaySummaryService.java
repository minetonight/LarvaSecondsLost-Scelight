package hu.aleks.larvasecondslostextmod;

import hu.scelightapi.sc2.rep.model.IReplay;
import hu.scelightapi.sc2.rep.model.details.IDetails;
import hu.scelightapi.sc2.rep.model.details.IPlayer;
import hu.scelightapi.sc2.rep.repproc.IRepProcessor;

import java.awt.Color;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;
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

    /** Owning external module. */
    private final LarvaSecondsLostModule module;

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
        this.module = module;
        larvaReplayAnalyzer = new LarvaReplayAnalyzer();
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
        if ( replayFile == null )
            throw new IllegalArgumentException( "Replay file is required." );

        if ( !Files.exists( replayFile ) )
            throw new IllegalArgumentException( "Replay file does not exist: " + replayFile );

        IRepProcessor repProc = module.repParserEngine.getRepProc( replayFile );
        if ( repProc == null )
            throw new IllegalStateException( "Scelight could not parse the replay file." );

        boolean fullReplayParseUsed = false;
        if ( repProc.getReplay().getTrackerEvents() == null || repProc.getReplay().getGameEvents() == null ) {
            module.logger.debug( "Larva analysis requires full replay events; reparsing replay instead of using cache-only processor: " + replayFile );
            final IRepProcessor reparsedRepProc = module.repParserEngine.parseAndWrapReplay( replayFile );
            if ( reparsedRepProc != null ) {
                repProc = reparsedRepProc;
                fullReplayParseUsed = true;
            }
        }

        final IReplay replay = repProc.getReplay();
        final IDetails details = replay.getDetails();
        final Date replayEndTime = details == null ? null : details.getTime();

        final long lengthMs = repProc.getLengthMs();
        final long previewWindowStartMs = lengthMs <= 0L ? 0L : lengthMs / 4L;
        final long previewWindowDurationMs = lengthMs <= 0L ? 0L : Math.max( 10000L, lengthMs / 5L );
        final long previewWindowEndMs = lengthMs <= 0L ? 0L : Math.min( lengthMs, previewWindowStartMs + previewWindowDurationMs );
        final LarvaAnalysisReport larvaAnalysisReport = larvaReplayAnalyzer.analyze( repProc, fullReplayParseUsed );
        module.logger.debug( "Larva analysis summary for " + replayFile.getFileName() + ": trackerEvents=" + larvaAnalysisReport.getTrackerEventCount()
            + ", gameEvents=" + larvaAnalysisReport.getGameEventCount() + ", hatcheries=" + larvaAnalysisReport.getTrackedHatcheryCount() + ", larvaBirths="
            + larvaAnalysisReport.getLarvaBirthCount() + ", assigned=" + larvaAnalysisReport.getAssignedLarvaCount() + ", unassigned="
            + larvaAnalysisReport.getUnassignedLarvaCount() + ", ambiguous=" + larvaAnalysisReport.getAmbiguousLarvaCount() + ", noEligible="
            + larvaAnalysisReport.getNoEligibleHatcheryLarvaCount() + ", fullReplayParseUsed=" + larvaAnalysisReport.isFullReplayParseUsed() );

        final ReplaySummary replaySummary = new ReplaySummary( replayFile, safe( sourceDescription, "Unknown" ),
            safe( details == null ? null : details.getTitle(), "Unknown map" ),
                safe( repProc.getPlayersGrouped(), "Unknown players" ), safe( repProc.getWinnersString(), "Unknown / undecided" ),
            formatDuration( lengthMs ), lengthMs, formatDate( replayEndTime ), replay.getHeader().versionString( true ),
            String.valueOf( replay.getHeader().getBaseBuild() ), buildPlayerColorMap( details ) );
        final LarvaTimelineModel timelineModel = timelineModelBuilder.build( replaySummary, FALLBACK_INTEGRATION_MODE, larvaAnalysisReport,
                previewWindowStartMs, previewWindowEndMs );
        return new LarvaReplayPageSummary( replaySummary, FALLBACK_INTEGRATION_MODE, timelineModel, previewWindowStartMs, previewWindowEndMs,
                larvaAnalysisReport );
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

}