package hu.aleks.larvasecondslostextmod;

import java.nio.file.Paths;
import java.util.Date;
import java.util.EnumMap;
import java.util.Map;

/** Synthetic aggregation records for unit tests (no Scelight runtime needed). Not a test class itself. */
final class LarvaTestFixtures {

    static final long DAY_MS = 86400000L;

    private LarvaTestFixtures() {
    }

    /** Raw phase counts without interval. */
    static LarvaPhaseStats stats( final LarvaGamePhase phase, final int missed, final int missedInject, final int spawned,
            final long hatchLoops, final long injectEligibleLoops, final long injectActiveLoops ) {
        return new LarvaPhaseStats( phase, null, missed, missedInject, spawned, hatchLoops, injectEligibleLoops, injectActiveLoops );
    }

    /** Player-game builder with identity key = name. */
    static LarvaAggregationPlayerRecord.Builder player( final String name, final LarvaMatchupBucket matchup ) {
        return new LarvaAggregationPlayerRecord.Builder().identityKey( name ).name( name ).matchup( matchup );
    }

    /** Adds stats for one phase to the builder (replaces earlier stats of that phase). */
    static LarvaAggregationPlayerRecord.Builder withStats( final LarvaAggregationPlayerRecord.Builder b, final LarvaPhaseStats stats ) {
        final Map< LarvaGamePhase, LarvaPhaseStats > map = new EnumMap<>( LarvaGamePhase.class );
        map.put( stats.getPhase(), stats );
        return b.phaseStats( mergeStats( b, map ) );
    }

    private static Map< LarvaGamePhase, LarvaPhaseStats > mergeStats( final LarvaAggregationPlayerRecord.Builder b,
            final Map< LarvaGamePhase, LarvaPhaseStats > extra ) {
        final Map< LarvaGamePhase, LarvaPhaseStats > all = new EnumMap<>( LarvaGamePhase.class );
        all.putAll( b.build().getPhaseStats() );
        all.putAll( extra );
        return all;
    }

    /** Sets the game time at which one phase ends, in ms (loops derived with the game clock so both maps agree). */
    static LarvaAggregationPlayerRecord.Builder withEnd( final LarvaAggregationPlayerRecord.Builder b, final LarvaGamePhase phase,
            final long ms ) {
        final LarvaAggregationPlayerRecord current = b.build();
        final Map< LarvaGamePhase, Long > ms1 = new EnumMap<>( LarvaGamePhase.class );
        ms1.putAll( current.getPhaseEndDisplayMs() );
        ms1.put( phase, Long.valueOf( ms ) );
        final Map< LarvaGamePhase, Integer > loops = new EnumMap<>( LarvaGamePhase.class );
        loops.putAll( current.getPhaseEndLoops() );
        loops.put( phase, Integer.valueOf( LarvaGameClock.secondsToLoops( ms / 1000.0d ) ) );
        return b.phaseEndDisplayMs( ms1 ).phaseEndLoops( loops );
    }

    /** Sets one milestone time in ms. */
    static LarvaAggregationPlayerRecord.Builder withMilestone( final LarvaAggregationPlayerRecord.Builder b, final LarvaMilestone milestone,
            final long ms ) {
        final Map< LarvaMilestone, Long > map = new EnumMap<>( LarvaMilestone.class );
        map.putAll( b.build().getMilestoneDisplayMs() );
        map.put( milestone, Long.valueOf( ms ) );
        return b.milestoneDisplayMs( map );
    }

    /** Eligible-looking 1v1 ladder game on the given day (days since epoch) with the given Zerg players. */
    static LarvaAggregationGameRecord game( final long day, final LarvaAggregationPlayerRecord... players ) {
        final LarvaAggregationGameRecord.Builder b = new LarvaAggregationGameRecord.Builder()
                .replayFile( Paths.get( "g" + day + ".SC2Replay" ) ).replayDate( new Date( day * DAY_MS ) ).oneVsOne( true ).ladder( true )
                .lengthLoops( 10000 ).fullParse( true );
        for ( final LarvaAggregationPlayerRecord p : players )
            b.addZergPlayer( p );
        return b.build();
    }

    static LarvaTrackedPlayer tracked( final String name, final int games ) {
        return new LarvaTrackedPlayer( name, name, games, new Date( 0L ) );
    }

    static LarvaFilterSummary summaryOf( final int eligible ) {
        final LarvaFilterSummary.Builder b = new LarvaFilterSummary.Builder();
        for ( int i = 0; i < eligible; i++ )
            b.add( LarvaEligibility.ELIGIBLE );
        return b.build();
    }
}
