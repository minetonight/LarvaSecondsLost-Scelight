package hu.aleks.larvasecondslostextmod;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Turns a {@link LarvaAggregationResult} into the display model of the multi page (E17-S15, FR-8, FR-12 to FR-14). Pure
 * and stateless; no cell is ever empty: a missing value reads {@code n/a} (user decision 2026-11-24).
 */
final class LarvaMultiAnalysisViewModelBuilder {

    /** Row labels of a matchup table, in display order (FR-12). */
    static final String[] PHASE_ROW_LABELS = new String[] { "Avg phase end [m:ss]", "Inject uptime [%]", "Larva gen [lrv / htch / min]",
        "larva gen ranking", "Larva missed [lrv / htch / min]", "larva missed ranking", "Inject-missed larva [lrv / htch / min]" };

    static final String HELP_LINE = "Ranking rows compare averages with per-game benchmark bands; averages sit closer to the middle.";

    static final String MILESTONE_TITLE = "Milestones (mean time, reached / games)";

    static final String MILESTONE_CORNER_LABEL = "Milestone";

    private static final String EM_DASH = " — ";

    private LarvaMultiAnalysisViewModelBuilder() {
    }

    /**
     * @param result the aggregation result
     * @param locale locale of the header dates
     * @param timeZone time zone of the header dates
     * @return the immutable view model
     */
    static LarvaMultiAnalysisViewModel build( final LarvaAggregationResult result, final Locale locale, final TimeZone timeZone ) {
        final String summary = result.getFilterSummary().toText();
        final List< String > skipped = skippedLines( result );
        if ( result.getPlayers().isEmpty() || result.getDistinctGameCount() == 0 )
            return new LarvaMultiAnalysisViewModel( null, summary, null, LarvaMultiAnalysisViewModel.EMPTY_MESSAGE,
                    new ArrayList< LarvaMultiAnalysisViewModel.PlayerSection >(), skipped );
        final List< LarvaMultiAnalysisViewModel.PlayerSection > sections = new ArrayList<>();
        for ( final LarvaAggregationResult.PlayerResult player : result.getPlayers() )
            sections.add( new LarvaMultiAnalysisViewModel.PlayerSection( playerName( player ), playerTables( player ) ) );
        return new LarvaMultiAnalysisViewModel( header( result, locale, timeZone ), summary, HELP_LINE, null, sections, skipped );
    }

    /** @return {@code Averaged larva stats for A, B for 37 replays from <start> to <end>} (AC-8.1) */
    static String header( final LarvaAggregationResult result, final Locale locale, final TimeZone timeZone ) {
        final StringBuilder names = new StringBuilder();
        for ( final LarvaAggregationResult.PlayerResult player : result.getPlayers() ) {
            if ( names.length() > 0 )
                names.append( ", " );
            names.append( playerName( player ) );
        }
        final int n = result.getDistinctGameCount();
        final StringBuilder sb = new StringBuilder( "Averaged larva stats for " ).append( names ).append( " for " ).append( n )
                .append( n == 1 ? " replay" : " replays" );
        if ( result.getStartDate() != null && result.getEndDate() != null )
            sb.append( ' ' ).append( LarvaShortDateFormatter.formatRange( result.getStartDate(), result.getEndDate(), locale, timeZone ) );
        return sb.toString();
    }

    private static String playerName( final LarvaAggregationResult.PlayerResult player ) {
        final String name = player.getPlayer().getDisplayName();
        if ( name == null || name.trim().length() == 0 )
            return player.getPlayer().getIdentityKey();
        return LarvaRegionNames.withRegionSuffix( name, player.getPlayer().getIdentityKey() );
    }

    private static List< LarvaTableViewModel > playerTables( final LarvaAggregationResult.PlayerResult player ) {
        final List< LarvaTableViewModel > tables = new ArrayList<>();
        // CR-1.3: the small milestone table comes first, then All / ZvP / ZvT / ZvZ.
        tables.add( milestoneTable( player ) );
        for ( final LarvaMatchupBucket bucket : LarvaMatchupBucket.values() )
            tables.add( phaseTable( player, bucket ) );
        return tables;
    }

    private static LarvaTableViewModel phaseTable( final LarvaAggregationResult.PlayerResult player, final LarvaMatchupBucket bucket ) {
        final LarvaGamePhase[] phases = LarvaGamePhase.values();
        final String[] headers = new String[ phases.length ];
        final String[][] cells = new String[ PHASE_ROW_LABELS.length ][ phases.length ];
        for ( int column = 0; column < phases.length; column++ ) {
            final LarvaAggregatedPhaseCell cell = player.getPhaseCell( bucket, phases[ column ] );
            headers[ column ] = LarvaPhaseHeaderLabels.resolve( phases[ column ] );
            cells[ 0 ][ column ] = timeWithCount( cell.getMeanEndMs(), cell.getReachedCount(), cell.getGameCount() );
            cells[ 1 ][ column ] = LarvaStatFormat.formatPercent( cell.getInjectUptimePercent() );
            cells[ 2 ][ column ] = LarvaStatFormat.formatRate( cell.getSpawnedPerHatchPerMinute() );
            cells[ 3 ][ column ] = textOrNotAvailable( cell.getSpawnedRanking() );
            cells[ 4 ][ column ] = LarvaStatFormat.formatRate( cell.getMissedPerHatchPerMinute() );
            cells[ 5 ][ column ] = textOrNotAvailable( cell.getMissedRanking() );
            cells[ 6 ][ column ] = LarvaStatFormat.formatRate( cell.getMissedInjectPerHatchPerMinute() );
        }
        final int games = player.getGameCount( bucket );
        return new LarvaTableViewModel( bucketTitle( bucket, games ), LarvaTableViewModel.DEFAULT_CORNER_LABEL, headers, PHASE_ROW_LABELS, cells );
    }

    private static LarvaTableViewModel milestoneTable( final LarvaAggregationResult.PlayerResult player ) {
        final LarvaMatchupBucket[] buckets = LarvaMatchupBucket.values();
        final LarvaMilestone[] milestones = LarvaMilestone.values();
        final String[] headers = new String[ buckets.length ];
        final String[] rowLabels = new String[ milestones.length ];
        final String[][] cells = new String[ milestones.length ][ buckets.length ];
        for ( int column = 0; column < buckets.length; column++ )
            headers[ column ] = buckets[ column ].getLabel();
        for ( int row = 0; row < milestones.length; row++ ) {
            rowLabels[ row ] = milestoneRowLabel( milestones[ row ] );
            for ( int column = 0; column < buckets.length; column++ ) {
                final LarvaAggregatedMilestoneCell cell = player.getMilestoneCell( buckets[ column ], milestones[ row ] );
                cells[ row ][ column ] = timeWithCount( cell.getMeanMs(), cell.getReachedCount(), cell.getGameCount() );
            }
        }
        return new LarvaTableViewModel( MILESTONE_TITLE, MILESTONE_CORNER_LABEL, headers, rowLabels, cells );
    }

    /** @return {@code 100 supply used} or {@code 66 drones alive} */
    static String milestoneRowLabel( final LarvaMilestone milestone ) {
        return milestone.getThreshold() + ( milestone.getKind() == LarvaMilestone.Kind.SUPPLY ? " supply used" : " drones alive" );
    }

    /** @return {@code All games — 12 games}, {@code ZvP — 0 games} (OQ-4) */
    static String bucketTitle( final LarvaMatchupBucket bucket, final int games ) {
        final String label = bucket == LarvaMatchupBucket.ALL ? "All games" : bucket.getLabel();
        return label + EM_DASH + games + ( games == 1 ? " game" : " games" );
    }

    /** @return {@code m:ss (k/n)}, or {@code n/a (k/n)} when no game reached it (D-9, OQ-13) */
    static String timeWithCount( final Long ms, final int reached, final int games ) {
        final String time = ms == null ? LarvaStatFormat.NOT_AVAILABLE : LarvaStatFormat.formatMinSec( ms.longValue() );
        return time + " (" + reached + "/" + games + ")";
    }

    private static String textOrNotAvailable( final String text ) {
        return text == null || text.trim().length() == 0 ? LarvaStatFormat.NOT_AVAILABLE : text;
    }

    private static List< String > skippedLines( final LarvaAggregationResult result ) {
        final List< String > lines = new ArrayList<>();
        for ( final LarvaSkippedReplay skipped : result.getSkipped() )
            lines.add( skipped.getFile() + EM_DASH + skipped.getReason() );
        return lines;
    }
}
