package hu.aleks.larvasecondslostextmod;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Deterministic text dump of a {@link LarvaAggregationResult} for integration diffs (E17-S12, epic §8). Machine
 * independent by construction: file names only (no folders), dates in UTC with a fixed pattern, numbers through
 * {@link LarvaStatFormat} and {@link Locale#ROOT}, fixed line separator {@code \n}, no run timestamps or durations.
 */
final class LarvaMultiAnalysisDumpFormatter {

    private static final String NL = "\n";

    private static final String DATE_PATTERN = "yyyy-MM-dd HH:mm:ss";

    private static final String NO_DATE = "none";

    /** @return the dump text, ending with a newline */
    String format( final LarvaAggregationResult result ) {
        final StringBuilder sb = new StringBuilder();
        sb.append( "== Multi analysis dump ==" ).append( NL );
        sb.append( "filter: " ).append( result.getFilterSummary().toText() ).append( NL );
        sb.append( "distinct eligible games: " ).append( result.getDistinctGameCount() ).append( NL );
        sb.append( "date range (UTC): " ).append( formatDate( result.getStartDate() ) ).append( " .. " ).append( formatDate( result.getEndDate() ) )
                .append( NL );
        appendSkipped( sb, result );
        sb.append( "tracked players: " ).append( result.getPlayers().size() ).append( NL );
        int rank = 0;
        for ( final LarvaAggregationResult.PlayerResult player : result.getPlayers() )
            appendPlayer( sb, ++rank, player );
        return sb.toString();
    }

    private static void appendSkipped( final StringBuilder sb, final LarvaAggregationResult result ) {
        sb.append( "skipped: " ).append( result.getSkipped().size() ).append( NL );
        for ( final LarvaSkippedReplay skipped : result.getSkipped() )
            sb.append( "  " ).append( skipped.getFile() ).append( " | " ).append( skipped.getReason() ).append( NL );
    }

    private static void appendPlayer( final StringBuilder sb, final int rank, final LarvaAggregationResult.PlayerResult player ) {
        final LarvaTrackedPlayer tracked = player.getPlayer();
        sb.append( "player " ).append( rank ).append( ": name=" ).append( tracked.getDisplayName() ).append( " key=" ).append( tracked.getIdentityKey() )
                .append( " games=" ).append( tracked.getGameCount() ).append( " last=" ).append( formatDate( tracked.getLastGameDate() ) ).append( NL );
        for ( final LarvaMatchupBucket bucket : LarvaMatchupBucket.values() ) {
            sb.append( "  bucket " ).append( bucket ).append( " games=" ).append( player.getGameCount( bucket ) ).append( NL );
            for ( final LarvaGamePhase phase : LarvaGamePhase.values() )
                appendPhase( sb, player.getPhaseCell( bucket, phase ) );
            for ( final LarvaMilestone milestone : LarvaMilestone.values() )
                appendMilestone( sb, player.getMilestoneCell( bucket, milestone ) );
        }
    }

    private static void appendPhase( final StringBuilder sb, final LarvaAggregatedPhaseCell cell ) {
        sb.append( "    phase " ).append( cell.getPhase() ).append( ": " );
        final LarvaPhaseStats stats = cell.getPooledStats();
        if ( stats == null )
            sb.append( "no stats" );
        else
            sb.append( "missed=" ).append( stats.getMissedLarvaCount() ).append( " missedInject=" ).append( stats.getMissedInjectLarvaCount() )
                    .append( " spawned=" ).append( stats.getTotalSpawnedLarvaCount() ).append( " hatchEligibleLoops=" )
                    .append( stats.getHatchEligibleLoops() ).append( " injectEligibleLoops=" ).append( stats.getInjectEligibleLoops() )
                    .append( " injectActiveLoops=" ).append( stats.getInjectActiveLoops() );
        sb.append( " | missed/h/min=" ).append( LarvaStatFormat.formatRate( cell.getMissedPerHatchPerMinute() ) )
                .append( " missedInject/h/min=" ).append( LarvaStatFormat.formatRate( cell.getMissedInjectPerHatchPerMinute() ) )
                .append( " spawned/h/min=" ).append( LarvaStatFormat.formatRate( cell.getSpawnedPerHatchPerMinute() ) )
                .append( " uptime=" ).append( LarvaStatFormat.formatPercent( cell.getInjectUptimePercent() ) )
                .append( " spawnedRank=" ).append( text( cell.getSpawnedRanking() ) ).append( " missedRank=" ).append( text( cell.getMissedRanking() ) )
                .append( " end=" ).append( cell.getMeanEndMs() == null ? LarvaStatFormat.NOT_AVAILABLE : LarvaStatFormat.formatMinSec( cell.getMeanEndMs().longValue() ) )
                .append( " (" ).append( cell.getMeanEndMs() == null ? "n/a" : cell.getMeanEndMs() + " ms" ).append( ") " )
                .append( cell.getReachedCount() ).append( '/' ).append( cell.getGameCount() ).append( NL );
    }

    private static void appendMilestone( final StringBuilder sb, final LarvaAggregatedMilestoneCell cell ) {
        sb.append( "    milestone " ).append( cell.getMilestone().getLabel() ).append( ": " );
        if ( cell.getMeanMs() == null )
            sb.append( LarvaStatFormat.NOT_AVAILABLE );
        else
            sb.append( LarvaStatFormat.formatMinSec( cell.getMeanMs().longValue() ) ).append( " (" ).append( cell.getMeanMs() ).append( " ms)" );
        sb.append( " (" ).append( cell.getReachedCount() ).append( '/' ).append( cell.getGameCount() ).append( ')' ).append( NL );
    }

    private static String text( final String value ) {
        // Never a blank: a missing or empty ranking reads n/a (user decision 2026-11-24).
        return value == null || value.trim().length() == 0 ? LarvaStatFormat.NOT_AVAILABLE : value;
    }

    private static String formatDate( final Date date ) {
        if ( date == null )
            return NO_DATE;
        final SimpleDateFormat format = new SimpleDateFormat( DATE_PATTERN, Locale.ROOT );
        format.setTimeZone( TimeZone.getTimeZone( "UTC" ) );
        return format.format( date );
    }
}
