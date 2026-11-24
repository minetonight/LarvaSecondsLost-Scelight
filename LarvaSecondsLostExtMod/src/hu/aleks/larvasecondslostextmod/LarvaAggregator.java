package hu.aleks.larvasecondslostextmod;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Pools the eligible player-games of the tracked players into per-matchup tables (E17-S11, FR-12, FR-13).
 *
 * <p>Pure and stateless. Rules:</p>
 * <ul>
 * <li>One player-game is one record entry whose identity key equals a tracked player. It counts in {@code ALL} and, if the
 * opponent matchup is known, in that matchup bucket. A game between two tracked players therefore counts in both players'
 * tables (D-7), but once in {@link LarvaAggregationResult#getDistinctGameCount()}.</li>
 * <li>Pooled larva rates come from a {@link LarvaPhaseStats} over the summed counts and loops (D-1, D-2); this class holds no
 * rate formula.</li>
 * <li>Phase length is the mean over the player-games that reached the phase, reported with {@code k} of {@code n} (D-9).
 * Milestones are the mean over the games that reached them, again {@code k/n}, without censoring (D-10).</li>
 * <li>Player-games without phase table are left out and listed as {@code missing-phase-table} (OQ-15). They are not part of
 * {@code n}.</li>
 * <li>Any value without data (zero denominator, empty bucket) is <code>null</code> (OQ-4, OQ-13).</li>
 * </ul>
 */
public final class LarvaAggregator {

    private static final LarvaPhaseBenchmarkRatingFormatter RATINGS = new LarvaPhaseBenchmarkRatingFormatter();

    /**
     * @param eligible
     *            eligible games (filter already applied)
     * @param tracked
     *            tracked players in rank order
     * @param summary
     *            filter summary to carry into the result
     * @return the immutable result
     * @throws IllegalStateException
     *             if a summed count does not fit an int
     */
    public LarvaAggregationResult aggregate( final List< LarvaAggregationGameRecord > eligible, final List< LarvaTrackedPlayer > tracked,
            final LarvaFilterSummary summary ) {
        return aggregate( eligible, tracked, summary, Collections.< LarvaSkippedReplay > emptyList() );
    }

    /**
     * Same as {@link #aggregate(List, List, LarvaFilterSummary)}, with entries skipped earlier (for example failed files) that
     * are put in front of the missing-phase-table entries found here.
     */
    public LarvaAggregationResult aggregate( final List< LarvaAggregationGameRecord > eligible, final List< LarvaTrackedPlayer > tracked,
            final LarvaFilterSummary summary, final List< LarvaSkippedReplay > priorSkipped ) {
        if ( eligible == null || tracked == null || priorSkipped == null )
            throw new IllegalArgumentException( "Eligible games, tracked players and skipped list are required." );
        final List< LarvaSkippedReplay > skipped = new ArrayList<>( priorSkipped );
        final Set< LarvaAggregationGameRecord > contributing = new HashSet<>();
        final List< LarvaAggregationResult.PlayerResult > players = new ArrayList<>();
        for ( final LarvaTrackedPlayer player : tracked )
            players.add( aggregatePlayer( eligible, player, contributing, skipped ) );

        Date start = null;
        Date end = null;
        for ( final LarvaAggregationGameRecord game : eligible ) {
            final Date date = game.getReplayDate();
            if ( date == null )
                continue;
            if ( start == null || date.before( start ) )
                start = date;
            if ( end == null || date.after( end ) )
                end = date;
        }
        return new LarvaAggregationResult( players, contributing.size(), start, end, summary, skipped );
    }

    private static LarvaAggregationResult.PlayerResult aggregatePlayer( final List< LarvaAggregationGameRecord > eligible,
            final LarvaTrackedPlayer player, final Set< LarvaAggregationGameRecord > contributing, final List< LarvaSkippedReplay > skipped ) {
        final Map< LarvaMatchupBucket, BucketAccumulator > buckets = new EnumMap<>( LarvaMatchupBucket.class );
        for ( final LarvaMatchupBucket bucket : LarvaMatchupBucket.values() )
            buckets.put( bucket, new BucketAccumulator( bucket ) );

        for ( final LarvaAggregationGameRecord game : eligible )
            for ( final LarvaAggregationPlayerRecord record : game.getZergPlayers() ) {
                if ( !player.getIdentityKey().equals( record.getIdentityKey() ) )
                    continue;
                if ( record.isMissingPhaseTable() ) {
                    skipped.add( new LarvaSkippedReplay( String.valueOf( game.getReplayFile() == null ? null : game.getReplayFile().getFileName() )
                            + " (" + record.getName() + ")", LarvaSkippedReplay.REASON_MISSING_PHASE_TABLE ) );
                    continue;
                }
                contributing.add( game );
                buckets.get( LarvaMatchupBucket.ALL ).add( record );
                if ( record.getMatchup() != null )
                    buckets.get( record.getMatchup() ).add( record );
            }

        final Map< LarvaMatchupBucket, Integer > counts = new EnumMap<>( LarvaMatchupBucket.class );
        final Map< LarvaMatchupBucket, Map< LarvaGamePhase, LarvaAggregatedPhaseCell > > phaseCells = new EnumMap<>( LarvaMatchupBucket.class );
        final Map< LarvaMatchupBucket, Map< LarvaMilestone, LarvaAggregatedMilestoneCell > > milestoneCells = new EnumMap<>(
                LarvaMatchupBucket.class );
        for ( final BucketAccumulator acc : buckets.values() ) {
            counts.put( acc.bucket, acc.games );
            phaseCells.put( acc.bucket, Collections.unmodifiableMap( acc.buildPhaseCells() ) );
            milestoneCells.put( acc.bucket, Collections.unmodifiableMap( acc.buildMilestoneCells() ) );
        }
        return new LarvaAggregationResult.PlayerResult( player, counts, phaseCells, milestoneCells );
    }

    /** Mean of non-negative values, rounded half-up to a whole number; null if k is 0. */
    static Long meanHalfUp( final long sum, final int k ) {
        if ( k <= 0 )
            return null;
        return Long.valueOf( ( 2L * sum + k ) / ( 2L * k ) );
    }

    private static int toInt( final long value, final String context ) {
        if ( value < Integer.MIN_VALUE || value > Integer.MAX_VALUE )
            throw new IllegalStateException( "Summed count does not fit an int: " + context + " = " + value );
        return (int) value;
    }

    /** Running sums of one bucket of one player. */
    private static final class BucketAccumulator {

        private final LarvaMatchupBucket bucket;

        private int games;

        private final Map< LarvaGamePhase, PhaseSums > phaseSums = new EnumMap<>( LarvaGamePhase.class );

        private final Map< LarvaMilestone, long[] > milestoneSums = new EnumMap<>( LarvaMilestone.class );

        BucketAccumulator( final LarvaMatchupBucket bucket ) {
            this.bucket = bucket;
            for ( final LarvaGamePhase phase : LarvaGamePhase.values() )
                phaseSums.put( phase, new PhaseSums() );
            for ( final LarvaMilestone milestone : LarvaMilestone.values() )
                milestoneSums.put( milestone, new long[ 2 ] );
        }

        void add( final LarvaAggregationPlayerRecord record ) {
            games++;
            for ( final LarvaGamePhase phase : LarvaGamePhase.values() ) {
                final PhaseSums sums = phaseSums.get( phase );
                final LarvaPhaseStats stats = record.getPhaseStats().get( phase );
                if ( stats != null )
                    sums.add( stats );
                final Long lengthMs = record.getPhaseDurationDisplayMs().get( phase );
                final Integer lengthLoops = record.getPhaseDurationLoops().get( phase );
                if ( lengthMs != null && ( lengthLoops == null || lengthLoops.intValue() > 0 ) ) {
                    sums.lengthMsSum += lengthMs.longValue();
                    sums.reached++;
                }
            }
            for ( final LarvaMilestone milestone : LarvaMilestone.values() ) {
                final Long ms = record.getMilestoneDisplayMs().get( milestone );
                if ( ms != null ) {
                    final long[] sums = milestoneSums.get( milestone );
                    sums[ 0 ] += ms.longValue();
                    sums[ 1 ]++;
                }
            }
        }

        Map< LarvaGamePhase, LarvaAggregatedPhaseCell > buildPhaseCells() {
            final Map< LarvaGamePhase, LarvaAggregatedPhaseCell > cells = new EnumMap<>( LarvaGamePhase.class );
            for ( final LarvaGamePhase phase : LarvaGamePhase.values() ) {
                final PhaseSums sums = phaseSums.get( phase );
                final LarvaPhaseStats pooled = sums.pool( phase, bucket );
                final Double spawned = pooled == null ? null : pooled.getSpawnedLarvaPerHatchPerMinute();
                final Double missed = pooled == null ? null : pooled.getMissedLarvaPerHatchPerMinute();
                // END has no benchmark anchors, so the formatter itself answers n/a (D-3).
                final String spawnedRanking = spawned == null ? null : RATINGS.formatSpawnedLarvaRanking( phase, spawned );
                final String missedRanking = missed == null ? null : RATINGS.formatMissedLarvaRanking( phase, missed );
                cells.put( phase, new LarvaAggregatedPhaseCell( phase, pooled, spawnedRanking, missedRanking,
                        meanHalfUp( sums.lengthMsSum, sums.reached ), sums.reached, games ) );
            }
            return cells;
        }

        Map< LarvaMilestone, LarvaAggregatedMilestoneCell > buildMilestoneCells() {
            final Map< LarvaMilestone, LarvaAggregatedMilestoneCell > cells = new EnumMap<>( LarvaMilestone.class );
            for ( final LarvaMilestone milestone : LarvaMilestone.values() ) {
                final long[] sums = milestoneSums.get( milestone );
                final int reached = (int) sums[ 1 ];
                cells.put( milestone, new LarvaAggregatedMilestoneCell( milestone, meanHalfUp( sums[ 0 ], reached ), reached, games ) );
            }
            return cells;
        }
    }

    /** Running sums of one phase. */
    private static final class PhaseSums {

        private long missed;

        private long missedInject;

        private long spawned;

        private long hatchEligibleLoops;

        private long injectEligibleLoops;

        private long injectActiveLoops;

        private int statsCount;

        private long lengthMsSum;

        private int reached;

        void add( final LarvaPhaseStats stats ) {
            statsCount++;
            missed += stats.getMissedLarvaCount();
            missedInject += stats.getMissedInjectLarvaCount();
            spawned += stats.getTotalSpawnedLarvaCount();
            hatchEligibleLoops += stats.getHatchEligibleLoops();
            injectEligibleLoops += stats.getInjectEligibleLoops();
            injectActiveLoops += stats.getInjectActiveLoops();
        }

        /** @return the pooled stats, or null if no player-game had stats for the phase */
        LarvaPhaseStats pool( final LarvaGamePhase phase, final LarvaMatchupBucket bucket ) {
            if ( statsCount == 0 )
                return null;
            final String where = bucket + "/" + phase;
            return new LarvaPhaseStats( phase, null, toInt( missed, where + " missed" ), toInt( missedInject, where + " missedInject" ),
                    toInt( spawned, where + " spawned" ), hatchEligibleLoops, injectEligibleLoops, injectActiveLoops );
        }
    }
}
