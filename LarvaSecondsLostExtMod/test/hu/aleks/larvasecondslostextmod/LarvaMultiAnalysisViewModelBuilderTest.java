package hu.aleks.larvasecondslostextmod;

import static hu.aleks.larvasecondslostextmod.LarvaTestFixtures.game;
import static hu.aleks.larvasecondslostextmod.LarvaTestFixtures.player;
import static hu.aleks.larvasecondslostextmod.LarvaTestFixtures.stats;
import static hu.aleks.larvasecondslostextmod.LarvaTestFixtures.summaryOf;
import static hu.aleks.larvasecondslostextmod.LarvaTestFixtures.tracked;
import static hu.aleks.larvasecondslostextmod.LarvaTestFixtures.withEnd;
import static hu.aleks.larvasecondslostextmod.LarvaTestFixtures.withMilestone;
import static hu.aleks.larvasecondslostextmod.LarvaTestFixtures.withStats;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import org.junit.Test;

/** View-model builder of the multi page (E17-S15): header, tables, k/n strings, n/a rule, empty state, skipped list. */
public class LarvaMultiAnalysisViewModelBuilderTest {

    private static final Locale LOCALE = Locale.US;
    private static final TimeZone UTC = TimeZone.getTimeZone( "UTC" );
    private static final LarvaAggregator AGGREGATOR = new LarvaAggregator();

    private static LarvaAggregationPlayerRecord record( final String name, final LarvaMatchupBucket matchup ) {
        final LarvaAggregationPlayerRecord.Builder b = withStats( player( name, matchup ), stats( LarvaGamePhase.EARLY, 7, 6, 66, 12391, 8267, 4528 ) );
        withEnd( b, LarvaGamePhase.EARLY, 411666 );
        withMilestone( b, LarvaMilestone.SUPPLY_100, 678888 );
        return b.build();
    }

    private static LarvaMultiAnalysisViewModel build( final List< LarvaAggregationGameRecord > games, final LarvaTrackedPlayer... players ) {
        return LarvaMultiAnalysisViewModelBuilder.build( AGGREGATOR.aggregate( games, Arrays.asList( players ), summaryOf( games.size() ) ), LOCALE, UTC );
    }

    private static List< LarvaAggregationGameRecord > games( final int count, final int withB ) {
        final List< LarvaAggregationGameRecord > games = new ArrayList<>();
        for ( int i = 0; i < count; i++ ) {
            final LarvaMatchupBucket m = i % 2 == 0 ? LarvaMatchupBucket.ZVT : LarvaMatchupBucket.ZVZ;
            games.add( i < withB ? game( i, record( "A", m ), record( "B", m ) ) : game( i, record( "A", m ) ) );
        }
        return games;
    }

    /** AC 1 (AC-8.1): two players, 37 distinct games. */
    @Test
    public void headerNamesBothPlayersCountsDistinctGamesAndShowsDates() {
        final LarvaMultiAnalysisViewModel model = build( games( 37, 5 ), tracked( "A", 37 ), tracked( "B", 5 ) );
        assertEquals( "Averaged larva stats for A, B for 37 replays from 1/1/70 to 2/6/70", model.getHeader() );
        assertEquals( "selected 37 · eligible 37 · excluded: not 1v1 0, non-ladder 0, < 4 min 0, no Zerg 0, failed to parse 0", model.getFilterSummary() );
        assertEquals( LarvaMultiAnalysisViewModelBuilder.HELP_LINE, model.getHelpLine() );
        assertEquals( 2, model.getPlayers().size() );
    }

    @Test
    public void headerWithOnePlayerAndOneReplayIsSingular() {
        final LarvaMultiAnalysisViewModel model = build( games( 1, 0 ), tracked( "A", 1 ) );
        assertEquals( "Averaged larva stats for A for 1 replay from 1/1/70 to 1/1/70", model.getHeader() );
    }

    /** AC 3 (CR-1.3): the milestone table first, then All, ZvP, ZvT, ZvZ; shapes are identical per player. */
    @Test
    public void everyPlayerHasMilestonesFirstThenAllZvpZvtZvz() {
        final LarvaMultiAnalysisViewModel model = build( games( 4, 2 ), tracked( "A", 4 ), tracked( "B", 2 ) );
        for ( final LarvaMultiAnalysisViewModel.PlayerSection section : model.getPlayers() ) {
            final List< LarvaTableViewModel > tables = section.getTables();
            assertEquals( 5, tables.size() );
            for ( int i = 1; i < 5; i++ ) {
                assertEquals( 7, tables.get( i ).getRowCount() );
                assertEquals( 4, tables.get( i ).getColumnCount() );
                assertEquals( "Metric", tables.get( i ).getCornerLabel() );
            }
            assertEquals( "Early game [<37]", tables.get( 1 ).getColumnHeader( 0 ) );
            assertEquals( "End game [90+]", tables.get( 1 ).getColumnHeader( 3 ) );
            assertEquals( LarvaMultiAnalysisViewModelBuilder.MILESTONE_TITLE, tables.get( 0 ).getTitle() );
            assertEquals( 4, tables.get( 0 ).getRowCount() );
            assertEquals( 4, tables.get( 0 ).getColumnCount() );
        }
        final List< LarvaTableViewModel > a = model.getPlayers().get( 0 ).getTables();
        assertEquals( "All games — 4 games", a.get( 1 ).getTitle() );
        assertEquals( "ZvP — 0 games", a.get( 2 ).getTitle() );
        assertEquals( "ZvT — 2 games", a.get( 3 ).getTitle() );
        assertEquals( "ZvZ — 2 games", a.get( 4 ).getTitle() );
        // CR-1.1: row 1 is the phase end, not the phase length
        assertEquals( "Avg phase end [m:ss]", a.get( 1 ).getRowLabel( 0 ) );
        assertEquals( "Inject-missed larva [lrv / htch / min]", a.get( 1 ).getRowLabel( 6 ) );
    }

    /** AC 3 (AC-12.3, OQ-4): a matchup without games shows n/a everywhere, phase length n/a (0/0). */
    @Test
    public void zeroGameMatchupIsAllNotAvailable() {
        final LarvaTableViewModel zvp = build( games( 4, 0 ), tracked( "A", 4 ) ).getPlayers().get( 0 ).getTables().get( 2 );
        assertEquals( "ZvP — 0 games", zvp.getTitle() );
        for ( int column = 0; column < 4; column++ ) {
            assertEquals( "n/a (0/0)", zvp.getCell( 0, column ) );
            for ( int row = 1; row < zvp.getRowCount(); row++ )
                assertEquals( "n/a", zvp.getCell( row, column ) );
        }
    }

    /** k/n strings, formatted values and the milestone rows in order (AC-13.1, D-9). */
    @Test
    public void cellsCarryFormattedValuesAndReachedOverGames() {
        final List< LarvaTableViewModel > tables = build( games( 2, 0 ), tracked( "A", 2 ) ).getPlayers().get( 0 ).getTables();
        final LarvaTableViewModel all = tables.get( 1 );
        assertEquals( "6:51 (2/2)", all.getCell( 0, 0 ) );
        assertEquals( "n/a (0/2)", all.getCell( 0, 1 ) );
        assertEquals( "54.77%", all.getCell( 1, 0 ) );
        assertEquals( "7.16", all.getCell( 2, 0 ) ); // 66 larva * 1344 / 12391 loops
        assertEquals( "n/a", all.getCell( 3, 0 ) ); // fixture bands: no early larva gen band
        assertEquals( "0.76", all.getCell( 4, 0 ) );
        assertEquals( "0.65", all.getCell( 6, 0 ) );
        final LarvaTableViewModel milestones = tables.get( 0 );
        assertEquals( "Milestone", milestones.getCornerLabel() );
        assertEquals( "100 supply used", milestones.getRowLabel( 0 ) );
        assertEquals( "66 drones alive", milestones.getRowLabel( 1 ) );
        assertEquals( "80 drones alive", milestones.getRowLabel( 2 ) );
        assertEquals( "200 supply used", milestones.getRowLabel( 3 ) );
        assertEquals( "All", milestones.getColumnHeader( 0 ) );
        assertEquals( "ZvZ", milestones.getColumnHeader( 3 ) );
        assertEquals( "11:18 (2/2)", milestones.getCell( 0, 0 ) );
        assertEquals( "11:18 (1/1)", milestones.getCell( 0, 2 ) );
        assertEquals( "n/a (0/1)", milestones.getCell( 1, 3 ) );
        assertEquals( "n/a (0/0)", milestones.getCell( 0, 1 ) );
    }

    /** User decision 2026-11-24: no blank cell anywhere, also not for a ranking without rate (END, no benchmark). */
    @Test
    public void noCellIsEverEmpty() {
        final LarvaMultiAnalysisViewModel model = build( games( 3, 1 ), tracked( "A", 3 ), tracked( "B", 1 ) );
        for ( final LarvaMultiAnalysisViewModel.PlayerSection section : model.getPlayers() )
            for ( final LarvaTableViewModel table : section.getTables() )
                for ( int row = 0; row < table.getRowCount(); row++ )
                    for ( int column = 0; column < table.getColumnCount(); column++ ) {
                        final String cell = table.getCell( row, column );
                        assertTrue( table.getTitle() + " r" + row + " c" + column, cell != null && cell.trim().length() > 0 );
                    }
        assertEquals( "n/a", model.getPlayers().get( 0 ).getTables().get( 1 ).getCell( 3, 3 ) );
        assertFalse( model.toText().contains( "|  |" ) );
        assertFalse( model.toText().contains( "| \n" ) );
    }

    /** AC 6 (AC-15.3): no eligible games or no tracked player gives the empty state. */
    @Test
    public void noGamesOrNoTrackedPlayerGivesEmptyState() {
        final LarvaMultiAnalysisViewModel none = build( new ArrayList< LarvaAggregationGameRecord >() );
        assertTrue( none.isEmpty() );
        assertNull( none.getHeader() );
        assertNull( none.getHelpLine() );
        assertEquals( "No eligible Zerg games in the selection", none.getEmptyMessage() );
        assertTrue( none.getPlayers().isEmpty() );
        assertTrue( none.getFilterSummary().startsWith( "selected 0" ) );
        // a tracked player whose only games were omitted (unknown matchup) must not give an empty-looking table page
        final LarvaMultiAnalysisViewModel unknown = build( Arrays.asList( game( 1, record( "A", null ) ) ), tracked( "A", 1 ) );
        assertTrue( unknown.isEmpty() );
        assertEquals( 1, unknown.getSkippedLines().size() );
    }

    /** AC 7 (OQ-15): parse failures and missing-phase-table player-games are listed as {@code file — reason}. */
    @Test
    public void skippedListHasEntriesWithReasons() {
        final List< LarvaAggregationGameRecord > games = Arrays.asList( game( 1, record( "A", LarvaMatchupBucket.ZVP ) ),
                game( 2, player( "A", LarvaMatchupBucket.ZVT ).missingPhaseTable( true ).build() ) );
        final List< LarvaSkippedReplay > prior = Arrays.asList( new LarvaSkippedReplay( "bad1.SC2Replay", LarvaSkippedReplay.REASON_FAILED_TO_PARSE ),
                new LarvaSkippedReplay( "bad2.SC2Replay", "replay could not be parsed: boom" ) );
        final LarvaAggregationResult result = AGGREGATOR.aggregate( games, Arrays.asList( tracked( "A", 1 ) ), summaryOf( 2 ), prior );
        final List< String > lines = LarvaMultiAnalysisViewModelBuilder.build( result, LOCALE, UTC ).getSkippedLines();
        assertEquals( Arrays.asList( "bad1.SC2Replay — replay could not be parsed", "bad2.SC2Replay — replay could not be parsed: boom",
                "g2.SC2Replay (A) — missing-phase-table" ), lines );
    }

    @Test
    public void textDumpListsPlayersTablesAndSkipped() {
        final String text = build( games( 2, 0 ), tracked( "A", 2 ) ).toText();
        assertTrue( text.startsWith( "header: Averaged larva stats for A for 2 replays" ) );
        assertTrue( text.contains( "player 1: A\ntable: " + LarvaMultiAnalysisViewModelBuilder.MILESTONE_TITLE + "\n" ) );
        assertTrue( text.contains( "table: All games — 2 games\n  Metric | Early game [<37] | Mid game [<67]" ) );
        assertTrue( text.contains( "  Avg phase end [m:ss] | 6:51 (2/2) | n/a (0/2) | n/a (0/2) | n/a (0/2)\n" ) );
        assertTrue( text.endsWith( "skipped (0):\n" ) );
    }

    /** AC 4 (CR-1.4): the heading and the header carry the region of the toon; a name: key gets no suffix. */
    @Test
    public void headingAndHeaderCarryTheRegionSuffix() {
        final LarvaAggregationPlayerRecord us = LarvaTestFixtures.player( "Scorpion", LarvaMatchupBucket.ZVT ).identityKey( "1-S2-1-111" ).build();
        final LarvaAggregationPlayerRecord eu = LarvaTestFixtures.player( "Scorpion", LarvaMatchupBucket.ZVT ).identityKey( "2-S2-1-222" ).build();
        final LarvaAggregationPlayerRecord bot = LarvaTestFixtures.player( "Bot", LarvaMatchupBucket.ZVT ).identityKey( "name:Bot" ).build();
        final List< LarvaAggregationGameRecord > games = Arrays.asList( game( 1, us, eu ), game( 2, us, eu ), game( 3, bot ) );
        final LarvaMultiAnalysisViewModel model = build( games, new LarvaTrackedPlayer( "1-S2-1-111", "Scorpion", 2, new java.util.Date( 0L ) ),
                new LarvaTrackedPlayer( "2-S2-1-222", "Scorpion", 2, new java.util.Date( 0L ) ),
                new LarvaTrackedPlayer( "name:Bot", "Bot", 1, new java.util.Date( 0L ) ) );
        assertEquals( "Scorpion (US)", model.getPlayers().get( 0 ).getHeading() );
        assertEquals( "Scorpion (EU)", model.getPlayers().get( 1 ).getHeading() );
        assertEquals( "Bot", model.getPlayers().get( 2 ).getHeading() );
        assertTrue( model.getHeader(), model.getHeader().startsWith( "Averaged larva stats for Scorpion (US), Scorpion (EU), Bot for 3 replays" ) );
    }

    /** AC 1 (CR-1.1): the cell reads {@code m:ss (k/n)}; the user's example "mid game ends at 9:10", and none reached. */
    @Test
    public void phaseEndCellReadsMinSecWithCount() {
        assertEquals( "9:10 (5/5)", LarvaMultiAnalysisViewModelBuilder.timeWithCount( Long.valueOf( 550000L ), 5, 5 ) );
        assertEquals( "12:00 (3/5)", LarvaMultiAnalysisViewModelBuilder.timeWithCount( Long.valueOf( 720000L ), 3, 5 ) );
        assertEquals( "n/a (0/5)", LarvaMultiAnalysisViewModelBuilder.timeWithCount( null, 0, 5 ) );
    }
}
