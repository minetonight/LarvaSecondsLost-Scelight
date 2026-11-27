package hu.aleks.larvasecondslostextmod;

/** Builds {@link LarvaTableViewModel}s for the phase tables. */
final class LarvaPhaseTableViewModels {

    /** Row labels of the single-replay phase table, in display order. */
    static final String[] SINGLE_REPLAY_ROW_LABELS = new String[] {
        "Phase span",
        "Inject uptime [%]",
        "Larva gen [lrv / htch / min]",
        "larva gen ranking",
        "Larva missed [lrv / htch / min]",
        "larva missed ranking",
        "Inject-missed larva [lrv / htch / min]"
    };

    private static final LarvaPhaseBenchmarkRatingFormatter RATING_FORMATTER = new LarvaPhaseBenchmarkRatingFormatter();

    private LarvaPhaseTableViewModels() {
    }

    /**
     * Builds today's 7-row, 4-phase table of the single-replay page.
     *
     * @param table player phase table
     * @param report analysis report providing the replay time basis; <code>null</code> uses a plain 16 loops per second fallback
     * @return the view model (no title, corner label <code>Metric</code>)
     */
    static LarvaTableViewModel fromPlayerPhaseTable( final LarvaPlayerPhaseTable table, final LarvaAnalysisReport report ) {
        final LarvaGamePhase[] phases = LarvaGamePhase.values();
        final String[] headers = new String[ phases.length ];
        final String[][] cells = new String[ SINGLE_REPLAY_ROW_LABELS.length ][ phases.length ];
        final LarvaTableViewModel.Bold[][] bold = new LarvaTableViewModel.Bold[ SINGLE_REPLAY_ROW_LABELS.length ][ phases.length ];
        for ( int column = 0; column < phases.length; column++ ) {
            final LarvaGamePhase phase = phases[ column ];
            headers[ column ] = LarvaPhaseHeaderLabels.resolve( phase );
            final LarvaPhaseStats stats = table.getPhaseStats( phase );
            final Double spawned = stats == null ? null : stats.getSpawnedLarvaPerHatchPerMinute();
            final Double missed = stats == null ? null : stats.getMissedLarvaPerHatchPerMinute();
            cells[ 0 ][ column ] = formatInterval( table.getPhaseInterval( phase ), report );
            cells[ 1 ][ column ] = LarvaStatFormat.formatPercent( stats == null ? null : stats.getInjectUptimePercentage() );
            cells[ 2 ][ column ] = LarvaStatFormat.formatRate( spawned );
            final LarvaRanking spawnedRanking = RATING_FORMATTER.rankSpawnedLarva( phase, spawned );
            final LarvaRanking missedRanking = RATING_FORMATTER.rankMissedLarva( phase, missed );
            cells[ 3 ][ column ] = spawnedRanking.getText();
            bold[ 3 ][ column ] = LarvaTableViewModel.boldLabel( spawnedRanking ); // E20-S12: the league inside the bands is bold
            cells[ 4 ][ column ] = LarvaStatFormat.formatRate( missed );
            cells[ 5 ][ column ] = missedRanking.getText();
            bold[ 5 ][ column ] = LarvaTableViewModel.boldLabel( missedRanking );
            cells[ 6 ][ column ] = LarvaStatFormat.formatRate( stats == null ? null : stats.getMissedInjectLarvaPerHatchPerMinute() );
        }
        return new LarvaTableViewModel( null, LarvaTableViewModel.DEFAULT_CORNER_LABEL, headers, SINGLE_REPLAY_ROW_LABELS, cells, bold );
    }

    private static String formatInterval( final LarvaPhaseInterval interval, final LarvaAnalysisReport report ) {
        if ( interval == null || interval.getEndLoop() <= interval.getStartLoop() )
            return LarvaStatFormat.NOT_AVAILABLE;
        return LarvaGameClock.format( interval.getStartLoop() ) + "-" + LarvaGameClock.format( interval.getEndLoop() );
    }

}
