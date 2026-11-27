package hu.aleks.larvasecondslostextmod;

import static hu.aleks.larvasecondslostextmod.LarvaTestFixtures.DAY_MS;
import static hu.aleks.larvasecondslostextmod.LarvaTestFixtures.game;
import static hu.aleks.larvasecondslostextmod.LarvaTestFixtures.player;
import static hu.aleks.larvasecondslostextmod.LarvaTestFixtures.stats;
import static hu.aleks.larvasecondslostextmod.LarvaTestFixtures.summaryOf;
import static hu.aleks.larvasecondslostextmod.LarvaTestFixtures.tracked;
import static hu.aleks.larvasecondslostextmod.LarvaTestFixtures.withEnd;
import static hu.aleks.larvasecondslostextmod.LarvaTestFixtures.withMilestone;
import static hu.aleks.larvasecondslostextmod.LarvaTestFixtures.withStats;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.junit.Test;

/**
 * Aggregation engine (E17-S11). Synthetic records only.
 *
 * <h3>Worked example for sign-off (checkpoint "aggregation numbers signed off")</h3>
 * <p>Tracked player {@code A}, EARLY phase, three eligible games. Rate = count * 1344 / hatchery-eligible loops
 * (1344 = 60 s * 22.4 loops/s, a game clock minute), uptime = active * 100 / eligible loops. Counts are summed first, then divided
 * (D-1, D-2). Until 2026-11-26 a minute was 960 loops (Normal-speed seconds); the hatchery loops below are 1.4 times the signed-off
 * ones, so every rate is unchanged.</p>
 * <pre>
 * game  matchup  missed  missedInject  spawned  hatchLoops  injElig  injActive  EARLY length  100 supply
 * g1    ZvP      10      4             48      13440        8000     6000       300 000 ms    400 000 ms
 * g2    ZvT       5      1             20       2688        1000     1000       120 000 ms    (not reached)
 * g3    ZvZ       3      1              8        672         200      100        61 000 ms    500 001 ms
 *
 * ZvP : missed 10*1344/13440 = 1.00   spawned 48*1344/13440 = 4.80   missedInject 0.40   uptime 6000*100/8000 = 75.00 %
 * ZvT : missed  5*1344/2688  = 2.50   spawned 20*1344/2688  = 10.00  missedInject 0.50   uptime 100.00 %
 * ZvZ : missed  3*1344/672   = 6.00   spawned  8*1344/672   = 16.00  missedInject 2.00   uptime 50.00 %
 *
 * ALL : missed 18, missedInject 6, spawned 76, loops 16800, injElig 9200, injActive 7100
 *       missed 18*1344/16800 = 1.44   spawned 76*1344/16800 = 6.08   missedInject 6*1344/16800 = 0.48
 *       uptime 7100*100/9200 = 77.1739 %
 *       (the mean of the three bucket missed rates would be (1.00+2.50+6.00)/3 = 3.1667, which is wrong on purpose)
 *       EARLY length: (300000+120000+61000)/3 = 160 333.33 -> 160 333 ms, 3/3
 *       100 supply: (400000+500001)/2 = 450 000.5 -> 450 001 ms (half-up), 2/3
 * </pre>
 */
public class LarvaAggregatorTest {

    private static final double EPS = 0.005d;

    private static final LarvaGamePhase EARLY = LarvaGamePhase.EARLY;

    private final LarvaAggregator aggregator = new LarvaAggregator();

    private static LarvaAggregationPlayerRecord early( final LarvaMatchupBucket matchup, final int missed, final int missedInject,
            final int spawned, final long loops, final long injElig, final long injActive ) {
        return withStats( player( "A", matchup ), stats( EARLY, missed, missedInject, spawned, loops, injElig, injActive ) ).build();
    }

    private LarvaAggregationResult.PlayerResult run( final List< LarvaAggregationGameRecord > games, final String... trackedNames ) {
        final List< LarvaTrackedPlayer > tracked = new ArrayList<>();
        for ( final String name : trackedNames )
            tracked.add( tracked( name, games.size() ) );
        return aggregator.aggregate( games, tracked, summaryOf( games.size() ) ).getPlayers().get( 0 );
    }

    private static List< LarvaAggregationGameRecord > workedExampleGames() {
        final LarvaAggregationPlayerRecord.Builder g1 = withStats( player( "A", LarvaMatchupBucket.ZVP ),
                stats( EARLY, 10, 4, 48, 13440, 8000, 6000 ) );
        withEnd( g1, EARLY, 300000 );
        withMilestone( g1, LarvaMilestone.SUPPLY_100, 9000 );
        final LarvaAggregationPlayerRecord.Builder g2 = withStats( player( "A", LarvaMatchupBucket.ZVT ),
                stats( EARLY, 5, 1, 20, 2688, 1000, 1000 ) );
        withEnd( g2, EARLY, 120000 );
        final LarvaAggregationPlayerRecord.Builder g3 = withStats( player( "A", LarvaMatchupBucket.ZVZ ),
                stats( EARLY, 3, 1, 8, 672, 200, 100 ) );
        withEnd( g3, EARLY, 61000 );
        withMilestone( g3, LarvaMilestone.SUPPLY_100, 10001 );
        return Arrays.asList( game( 1, g1.build() ), game( 2, g2.build() ), game( 3, g3.build() ) );
    }

    private static void assertCell( final LarvaAggregatedPhaseCell cell, final double missed, final double missedInject,
            final double spawned, final double uptime ) {
        assertEquals( missed, cell.getMissedPerHatchPerMinute().doubleValue(), EPS );
        assertEquals( missedInject, cell.getMissedInjectPerHatchPerMinute().doubleValue(), EPS );
        assertEquals( spawned, cell.getSpawnedPerHatchPerMinute().doubleValue(), EPS );
        assertEquals( uptime, cell.getInjectUptimePercent().doubleValue(), EPS );
    }

    @Test
    public void workedExample_matchesHandCalculation() {
        final LarvaAggregationResult.PlayerResult r = run( workedExampleGames(), "A" );
        assertCell( r.getPhaseCell( LarvaMatchupBucket.ZVP, EARLY ), 1.00, 0.40, 4.80, 75.0 );
        assertCell( r.getPhaseCell( LarvaMatchupBucket.ZVT, EARLY ), 2.50, 0.50, 10.0, 100.0 );
        assertCell( r.getPhaseCell( LarvaMatchupBucket.ZVZ, EARLY ), 6.00, 2.00, 16.0, 50.0 );
        final LarvaAggregatedPhaseCell all = r.getPhaseCell( LarvaMatchupBucket.ALL, EARLY );
        assertCell( all, 1.44, 0.48, 6.08, 77.1739 );
        assertEquals( 160333L, all.getMeanEndMs().longValue() );
        assertEquals( 3, all.getReachedCount() );
        assertEquals( 3, all.getGameCount() );
        final LarvaAggregatedMilestoneCell supply = r.getMilestoneCell( LarvaMatchupBucket.ALL, LarvaMilestone.SUPPLY_100 );
        assertEquals( Integer.valueOf( 9501 ), supply.getMedianLoop() ); // (9000 + 10001) / 2, half up
        assertEquals( 2, supply.getReachedCount() );
        assertEquals( 3, supply.getGameCount() );
        assertEquals( "top 50% masters", supply.getRanking() ); // fixture SUPPLY_100.ALL: masters 9760, GM 9280
        // ZvZ has its own band (masters from 10600): the one ZvZ game, 10001 loops, is rated against it
        assertEquals( "top 20% masters", r.getMilestoneCell( LarvaMatchupBucket.ZVZ, LarvaMilestone.SUPPLY_100 ).getRanking() );
        // ZvP has no band in the fixture
        assertEquals( "n/a", r.getMilestoneCell( LarvaMatchupBucket.ZVP, LarvaMilestone.SUPPLY_100 ).getRanking() );
    }

    /** AC 1 (AC-12.1): every rate equals sum(count) * 1344 / sum(loops), computed independently here. */
    @Test
    public void ac1_pooledRatesEqualSumOverSum() {
        final List< LarvaAggregationGameRecord > games = Arrays.asList(
                game( 1, early( LarvaMatchupBucket.ZVT, 7, 3, 31, 7000, 5000, 2500 ) ),
                game( 2, early( LarvaMatchupBucket.ZVT, 2, 0, 13, 3100, 2000, 1900 ) ),
                game( 3, early( LarvaMatchupBucket.ZVT, 11, 5, 40, 9000, 6000, 1 ) ) );
        final LarvaAggregatedPhaseCell cell = run( games, "A" ).getPhaseCell( LarvaMatchupBucket.ZVT, EARLY );
        assertCell( cell, 20 * 1344d / 19100, 8 * 1344d / 19100, 84 * 1344d / 19100, 4401 * 100d / 13000 );
    }

    /** AC 2 (AC-12.2): All pools the union; with unbalanced loops it differs from the mean of the bucket rates. */
    @Test
    public void ac2_allIsPooledUnionNotMeanOfRates() {
        final LarvaAggregationResult.PlayerResult r = run( workedExampleGames(), "A" );
        final double meanOfBuckets = ( r.getPhaseCell( LarvaMatchupBucket.ZVP, EARLY ).getMissedPerHatchPerMinute()
                + r.getPhaseCell( LarvaMatchupBucket.ZVT, EARLY ).getMissedPerHatchPerMinute()
                + r.getPhaseCell( LarvaMatchupBucket.ZVZ, EARLY ).getMissedPerHatchPerMinute() ) / 3;
        final double all = r.getPhaseCell( LarvaMatchupBucket.ALL, EARLY ).getMissedPerHatchPerMinute();
        assertEquals( 18 * 1344d / 16800, all, 1e-9 );
        assertTrue( Math.abs( all - meanOfBuckets ) > 1.0 );
        assertEquals( 3, r.getGameCount( LarvaMatchupBucket.ALL ) );
    }

    /** AC 3 (AC-12.3, OQ-4): a matchup without games exists with 0 games and only null cells. */
    @Test
    public void ac3_emptyBucketHasNullCells() {
        final List< LarvaAggregationGameRecord > games = Arrays.asList( game( 1, early( LarvaMatchupBucket.ZVT, 1, 1, 5, 1344, 500, 100 ) ) );
        final LarvaAggregationResult.PlayerResult r = run( games, "A" );
        assertEquals( 0, r.getGameCount( LarvaMatchupBucket.ZVP ) );
        for ( final LarvaGamePhase phase : LarvaGamePhase.values() ) {
            final LarvaAggregatedPhaseCell c = r.getPhaseCell( LarvaMatchupBucket.ZVP, phase );
            assertNotNull( c );
            assertNull( c.getMissedPerHatchPerMinute() );
            assertNull( c.getMissedInjectPerHatchPerMinute() );
            assertNull( c.getSpawnedPerHatchPerMinute() );
            assertNull( c.getInjectUptimePercent() );
            assertNull( c.getSpawnedRanking() );
            assertNull( c.getMissedRanking() );
            assertNull( c.getMeanEndMs() );
            assertEquals( 0, c.getReachedCount() );
            assertEquals( 0, c.getGameCount() );
        }
        for ( final LarvaMilestone m : LarvaMilestone.values() ) {
            assertNull( r.getMilestoneCell( LarvaMatchupBucket.ZVP, m ).getMedianLoop() );
            assertEquals( 0, r.getMilestoneCell( LarvaMatchupBucket.ZVP, m ).getGameCount() );
            assertEquals( "n/a", r.getMilestoneCell( LarvaMatchupBucket.ZVP, m ).getRanking() );
        }
    }

    /** AC 1 (CR-1.1, D-9, OQ-13): END ends at 10:00 / 12:00 / 14:00 in 3 of 5 games gives 12:00 with 3/5; LATE never reached gives null with 0/5. */
    @Test
    public void ac1_phaseEndMeanOverReachedGames() {
        final List< LarvaAggregationGameRecord > games = new ArrayList<>();
        final long[] endMs = { 600000, 720000, 840000, 0, 0 };
        for ( int i = 0; i < 5; i++ ) {
            final LarvaAggregationPlayerRecord.Builder b = withStats( player( "A", LarvaMatchupBucket.ZVP ),
                    stats( LarvaGamePhase.END, 1, 0, 2, 1344, 0, 0 ) );
            if ( endMs[ i ] > 0 )
                withEnd( b, LarvaGamePhase.END, endMs[ i ] );
            games.add( game( i + 1, b.build() ) );
        }
        final LarvaAggregationResult.PlayerResult r = run( games, "A" );
        final LarvaAggregatedPhaseCell end = r.getPhaseCell( LarvaMatchupBucket.ALL, LarvaGamePhase.END );
        assertEquals( 720000L, end.getMeanEndMs().longValue() ); // 10:00 / 12:00 / 14:00 -> 12:00
        assertEquals( 3, end.getReachedCount() );
        assertEquals( 5, end.getGameCount() );
        final LarvaAggregatedPhaseCell late = r.getPhaseCell( LarvaMatchupBucket.ALL, LarvaGamePhase.LATE );
        assertNull( late.getMeanEndMs() );
        assertEquals( 0, late.getReachedCount() );
        assertEquals( 5, late.getGameCount() );
    }

    private LarvaAggregatedMilestoneCell supply200( final int... loops ) {
        final List< LarvaAggregationGameRecord > games = new ArrayList<>();
        for ( int i = 0; i < loops.length; i++ ) {
            final LarvaAggregationPlayerRecord.Builder b = withStats( player( "A", LarvaMatchupBucket.ZVZ ),
                    stats( EARLY, 1, 0, 1, 1344, 0, 0 ) );
            if ( loops[ i ] > 0 )
                withMilestone( b, LarvaMilestone.SUPPLY_200, loops[ i ] );
            games.add( game( i + 1, b.build() ) );
        }
        return run( games, "A" ).getMilestoneCell( LarvaMatchupBucket.ALL, LarvaMilestone.SUPPLY_200 );
    }

    /** AC 5 (AC-13.1, E20-S04): 2 of 4 games reach 200 supply; even count: the mean of the two middle loops, with 2/4. */
    @Test
    public void ac5_milestoneMedianOverReachedGames_evenCount() {
        final LarvaAggregatedMilestoneCell cell = supply200( 13440, 0, 20160, 0 );
        assertEquals( Integer.valueOf( 16800 ), cell.getMedianLoop() );
        assertEquals( 2, cell.getReachedCount() );
        assertEquals( 4, cell.getGameCount() );
        assertEquals( "aim for 12:12", cell.getRanking() ); // fixture SUPPLY_200.ALL starts at masters 16400 (OQ-5, E20-S12)
    }

    /** E20-S04: odd count, the middle loop; a late outlier does not move it (the reason for the median, F-4). */
    @Test
    public void milestoneMedianOverReachedGames_oddCountIgnoresALateOutlier() {
        final LarvaAggregatedMilestoneCell cell = supply200( 16000, 40000, 15600, 0 );
        assertEquals( Integer.valueOf( 16000 ), cell.getMedianLoop() );
        assertEquals( 3, cell.getReachedCount() );
        assertEquals( 4, cell.getGameCount() );
        assertEquals( "top 75% GM", cell.getRanking() ); // GM 16240 .. pro 15280
    }

    /** E20-S04: one reached game is its own median; none reached gives null, n/a and 0/n. */
    @Test
    public void milestoneMedianOfOneGameAndOfNone() {
        assertEquals( Integer.valueOf( 15000 ), supply200( 0, 15000 ).getMedianLoop() );
        assertEquals( "top 70% pro", supply200( 0, 15000 ).getRanking() ); // best tier: width of the GM tier
        final LarvaAggregatedMilestoneCell none = supply200( 0, 0, 0 );
        assertNull( none.getMedianLoop() );
        assertEquals( 0, none.getReachedCount() );
        assertEquals( 3, none.getGameCount() );
        assertEquals( "n/a", none.getRanking() );
    }

    @Test
    public void medianHalfUpRoundsTheMiddlePairUpAndLeavesTheInputAlone() {
        final List< Integer > loops = new ArrayList<>( Arrays.asList( 7, 1, 4, 2 ) );
        assertEquals( Integer.valueOf( 3 ), LarvaAggregator.medianHalfUp( loops ) ); // (2 + 4) / 2
        assertEquals( Integer.valueOf( 4 ), LarvaAggregator.medianHalfUp( Arrays.asList( 3, 4 ) ) ); // 3.5 -> 4
        assertEquals( Arrays.asList( 7, 1, 4, 2 ), loops );
        assertNull( LarvaAggregator.medianHalfUp( new ArrayList< Integer >() ) );
    }

    /** AC 6 (D-7): tracked A vs tracked B in one ZvZ game counts for both players and once in the header n. */
    @Test
    public void ac6_mirrorGameCountsForBothPlayersAndOnceInHeader() {
        final LarvaAggregationPlayerRecord a = withStats( player( "A", LarvaMatchupBucket.ZVZ ), stats( EARLY, 2, 0, 10, 1344, 0, 0 ) ).build();
        final LarvaAggregationPlayerRecord b = withStats( player( "B", LarvaMatchupBucket.ZVZ ), stats( EARLY, 4, 0, 20, 1344, 0, 0 ) ).build();
        final List< LarvaAggregationGameRecord > games = Arrays.asList( game( 1, a, b ) );
        final LarvaAggregationResult result = aggregator.aggregate( games, Arrays.asList( tracked( "A", 1 ), tracked( "B", 1 ) ), summaryOf( 1 ) );
        assertEquals( 1, result.getDistinctGameCount() );
        for ( final LarvaAggregationResult.PlayerResult p : result.getPlayers() ) {
            assertEquals( 1, p.getGameCount( LarvaMatchupBucket.ALL ) );
            assertEquals( 1, p.getGameCount( LarvaMatchupBucket.ZVZ ) );
        }
        assertEquals( 2.0, result.getPlayers().get( 0 ).getPhaseCell( LarvaMatchupBucket.ZVZ, EARLY ).getMissedPerHatchPerMinute(), EPS );
        assertEquals( 4.0, result.getPlayers().get( 1 ).getPhaseCell( LarvaMatchupBucket.ALL, EARLY ).getMissedPerHatchPerMinute(), EPS );
    }

    /** AC 7 (D-3): END ranking is n/a; EARLY/MID/LATE rankings equal the formatter output for the pooled value. */
    @Test
    public void ac7_rankingsComeFromTheFormatter() {
        final LarvaAggregationPlayerRecord.Builder b = player( "A", LarvaMatchupBucket.ZVP );
        for ( final LarvaGamePhase phase : LarvaGamePhase.values() )
            withStats( b, stats( phase, 6, 0, 60, 1344, 0, 0 ) );
        final LarvaAggregationResult.PlayerResult r = run( Arrays.asList( game( 1, b.build() ), game( 2, b.build() ) ), "A" );
        final LarvaPhaseBenchmarkRatingFormatter formatter = new LarvaPhaseBenchmarkRatingFormatter();
        for ( final LarvaGamePhase phase : LarvaGamePhase.values() ) {
            final LarvaAggregatedPhaseCell c = r.getPhaseCell( LarvaMatchupBucket.ALL, phase );
            if ( phase == LarvaGamePhase.END ) {
                assertEquals( "n/a", c.getSpawnedRanking() );
                assertEquals( "n/a", c.getMissedRanking() );
                continue;
            }
            assertEquals( formatter.formatSpawnedLarvaRanking( phase, Double.valueOf( 60.0 ) ), c.getSpawnedRanking() );
            assertEquals( formatter.formatMissedLarvaRanking( phase, Double.valueOf( 6.0 ) ), c.getMissedRanking() );
            // the fixture bands (test/resources) have no early larva gen band
            assertTrue( c.getSpawnedRanking(), phase == LarvaGamePhase.EARLY ? "n/a".equals( c.getSpawnedRanking() ) : c.getSpawnedRanking().startsWith( "top " ) );
        }
    }

    /** AC 8 (AC-7.2): the date range covers the eligible games only. */
    @Test
    public void ac8_dateRangeIgnoresExcludedReplays() {
        final LarvaAggregationPlayerRecord p = early( LarvaMatchupBucket.ZVP, 1, 0, 1, 1344, 0, 0 );
        final LarvaAggregationGameRecord tooShort = new LarvaAggregationGameRecord.Builder().replayFile( java.nio.file.Paths.get( "x.SC2Replay" ) ).replayDate( new Date( 1 * DAY_MS ) )
                .oneVsOne( true ).ladder( true ).lengthLoops( 100 ).addZergPlayer( p ).build();
        final LarvaAggregationGameRecord nonLadder = new LarvaAggregationGameRecord.Builder().replayFile( java.nio.file.Paths.get( "x.SC2Replay" ) ).replayDate( new Date( 99 * DAY_MS ) )
                .oneVsOne( true ).ladder( false ).lengthLoops( 10000 ).addZergPlayer( p ).build();
        final List< LarvaAggregationGameRecord > all = Arrays.asList( tooShort, game( 10, p ), game( 30, p ), game( 20, p ), nonLadder );
        final LarvaEligibilityFilter filter = new LarvaEligibilityFilter();
        final LarvaFilterSummary.Builder summary = new LarvaFilterSummary.Builder();
        final List< LarvaAggregationGameRecord > eligible = new ArrayList<>();
        for ( final LarvaAggregationGameRecord g : all ) {
            final LarvaEligibility e = filter.evaluate( g, true );
            summary.add( e );
            if ( e == LarvaEligibility.ELIGIBLE )
                eligible.add( g );
        }
        final LarvaAggregationResult result = aggregator.aggregate( eligible, Arrays.asList( tracked( "A", 3 ) ), summary.build() );
        assertEquals( 10 * DAY_MS, result.getStartDate().getTime() );
        assertEquals( 30 * DAY_MS, result.getEndDate().getTime() );
        assertEquals( 3, result.getDistinctGameCount() );
        assertEquals( 5, result.getFilterSummary().getSelected() );
    }

    @Test
    public void noGamesGivesNullDatesAndZeroCounts() {
        final LarvaAggregationResult result = aggregator.aggregate( Collections.< LarvaAggregationGameRecord > emptyList(),
                Collections.< LarvaTrackedPlayer > emptyList(), summaryOf( 0 ) );
        assertNull( result.getStartDate() );
        assertNull( result.getEndDate() );
        assertEquals( 0, result.getDistinctGameCount() );
        assertTrue( result.getPlayers().isEmpty() );
    }

    @Test
    public void missingPhaseTableIsSkippedAndListedAndNotCounted() {
        final LarvaAggregationPlayerRecord missing = player( "A", LarvaMatchupBucket.ZVP ).missingPhaseTable( true ).build();
        final List< LarvaAggregationGameRecord > games = Arrays.asList( game( 1, missing ),
                game( 2, early( LarvaMatchupBucket.ZVP, 1, 0, 1, 1344, 0, 0 ) ) );
        final LarvaAggregationResult result = aggregator.aggregate( games, Arrays.asList( tracked( "A", 1 ) ), summaryOf( 2 ) );
        assertEquals( 1, result.getPlayers().get( 0 ).getGameCount( LarvaMatchupBucket.ALL ) );
        assertEquals( 1, result.getDistinctGameCount() );
        assertEquals( 1, result.getSkipped().size() );
        assertEquals( LarvaSkippedReplay.REASON_MISSING_PHASE_TABLE, result.getSkipped().get( 0 ).getReason() );
        assertTrue( result.getSkipped().get( 0 ).getFile().startsWith( "g1.SC2Replay" ) );
    }

    @Test
    public void priorSkippedEntriesComeFirst() {
        final LarvaAggregationResult result = aggregator.aggregate( Collections.< LarvaAggregationGameRecord > emptyList(),
                Collections.< LarvaTrackedPlayer > emptyList(), summaryOf( 0 ),
                Arrays.asList( new LarvaSkippedReplay( "bad.SC2Replay", "failed-to-parse" ) ) );
        assertEquals( "bad.SC2Replay", result.getSkipped().get( 0 ).getFile() );
    }

    /** User decision 2026-11-24: a player-game with unknown matchup is omitted entirely and listed. */
    @Test
    public void unknownMatchupIsOmittedEverywhereAndListed() {
        final LarvaAggregationResult result = aggregator.aggregate(
                Arrays.asList( game( 1, early( LarvaMatchupBucket.ZVP, 1, 0, 1, 1344, 0, 0 ) ), game( 5, early( null, 1, 0, 1, 1344, 0, 0 ) ) ),
                Arrays.asList( tracked( "A", 2 ) ), summaryOf( 2 ) );
        final LarvaAggregationResult.PlayerResult r = result.getPlayers().get( 0 );
        assertEquals( 1, r.getGameCount( LarvaMatchupBucket.ALL ) );
        assertEquals( 1, r.getGameCount( LarvaMatchupBucket.ZVP ) );
        assertEquals( 1, r.getPhaseCell( LarvaMatchupBucket.ALL, EARLY ).getGameCount() );
        assertEquals( 1, result.getDistinctGameCount() );
        assertEquals( new Date( DAY_MS ), result.getStartDate() );
        assertEquals( new Date( DAY_MS ), result.getEndDate() );
        assertEquals( 1, result.getSkipped().size() );
        assertEquals( "g5.SC2Replay (A)", result.getSkipped().get( 0 ).getFile() );
        assertEquals( LarvaSkippedReplay.REASON_UNKNOWN_OPPONENT_RACE, result.getSkipped().get( 0 ).getReason() );
    }

    /** A player whose only games have no matchup gives empty tables and no distinct games, not a broken result. */
    @Test
    public void playerWithOnlyUnknownMatchupGamesHasEmptyResult() {
        final LarvaAggregationResult result = aggregator.aggregate( Arrays.asList( game( 3, early( null, 1, 0, 1, 1344, 0, 0 ) ) ),
                Arrays.asList( tracked( "A", 1 ) ), summaryOf( 1 ) );
        assertEquals( 0, result.getDistinctGameCount() );
        assertNull( result.getStartDate() );
        assertNull( result.getEndDate() );
        final LarvaAggregationResult.PlayerResult r = result.getPlayers().get( 0 );
        for ( final LarvaMatchupBucket bucket : LarvaMatchupBucket.values() )
            assertEquals( 0, r.getGameCount( bucket ) );
        assertNull( r.getPhaseCell( LarvaMatchupBucket.ALL, EARLY ).getMissedPerHatchPerMinute() );
        assertEquals( 1, result.getSkipped().size() );
    }

    @Test
    public void zeroDenominatorCellsAreNullButPhaseLengthStillCounts() {
        // Stats exist but with zero hatchery and inject loops: rates and uptime are null (D-10).
        final LarvaAggregationPlayerRecord.Builder b = withStats( player( "A", LarvaMatchupBucket.ZVP ), stats( EARLY, 0, 0, 0, 0, 0, 0 ) );
        withEnd( b, EARLY, 5000 );
        final LarvaAggregatedPhaseCell c = run( Arrays.asList( game( 1, b.build() ) ), "A" ).getPhaseCell( LarvaMatchupBucket.ALL, EARLY );
        assertNull( c.getMissedPerHatchPerMinute() );
        assertNull( c.getInjectUptimePercent() );
        assertNull( c.getSpawnedRanking() );
        assertEquals( 5000L, c.getMeanEndMs().longValue() );
    }

    @Test
    public void meanRoundsHalfUp() {
        assertEquals( 2L, LarvaAggregator.meanHalfUp( 3, 2 ).longValue() );
        assertEquals( 1L, LarvaAggregator.meanHalfUp( 2, 3 ).longValue() );
        assertEquals( 2L, LarvaAggregator.meanHalfUp( 5, 3 ).longValue() );
        assertNull( LarvaAggregator.meanHalfUp( 0, 0 ) );
    }

    @Test
    public void countsBeyondIntRangeFailWithContext() {
        final LarvaAggregationPlayerRecord big = early( LarvaMatchupBucket.ZVP, Integer.MAX_VALUE, 0, 0, 1344, 0, 0 );
        try {
            run( Arrays.asList( game( 1, big ), game( 2, big ) ), "A" );
            fail( "expected IllegalStateException" );
        } catch ( final IllegalStateException expected ) {
            assertTrue( expected.getMessage().contains( "ALL/EARLY missed" ) );
        }
    }

    @Test
    public void resultIsImmutable() {
        final LarvaAggregationResult result = aggregator.aggregate( workedExampleGames(), Arrays.asList( tracked( "A", 3 ) ), summaryOf( 3 ) );
        try {
            result.getPlayers().clear();
            fail( "players list must be unmodifiable" );
        } catch ( final UnsupportedOperationException expected ) {
            // expected
        }
        try {
            result.getSkipped().add( new LarvaSkippedReplay( "x", "y" ) );
            fail( "skipped list must be unmodifiable" );
        } catch ( final UnsupportedOperationException expected ) {
            // expected
        }
        result.getStartDate().setTime( 0L );
        assertEquals( DAY_MS, result.getStartDate().getTime() );
    }

    @Test( expected = IllegalArgumentException.class )
    public void nullInputsAreRejected() {
        aggregator.aggregate( null, Collections.< LarvaTrackedPlayer > emptyList(), summaryOf( 0 ) );
    }

    @Test( expected = IllegalArgumentException.class )
    public void allIsNotAMatchupForTheRecord() {
        player( "A", LarvaMatchupBucket.ALL );
    }
}
