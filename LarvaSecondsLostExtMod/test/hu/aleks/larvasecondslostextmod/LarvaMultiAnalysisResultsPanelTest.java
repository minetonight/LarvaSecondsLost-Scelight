package hu.aleks.larvasecondslostextmod;

import static hu.aleks.larvasecondslostextmod.LarvaTestFixtures.game;
import static hu.aleks.larvasecondslostextmod.LarvaTestFixtures.player;
import static hu.aleks.larvasecondslostextmod.LarvaTestFixtures.stats;
import static hu.aleks.larvasecondslostextmod.LarvaTestFixtures.summaryOf;
import static hu.aleks.larvasecondslostextmod.LarvaTestFixtures.tracked;
import static hu.aleks.larvasecondslostextmod.LarvaTestFixtures.withStats;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.awt.Rectangle;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import org.junit.Test;

/** Layout rules of the results panel (E17-S15, OQ-1, AC-14.1), laid out headless at a fixed size. */
public class LarvaMultiAnalysisResultsPanelTest {

    private static final int WIDE = 2000;

    private static LarvaAggregationPlayerRecord record( final String name ) {
        return withStats( player( name, LarvaMatchupBucket.ZVT ), stats( LarvaGamePhase.EARLY, 7, 6, 66, 12391, 8267, 4528 ) ).build();
    }

    private static LarvaMultiAnalysisViewModel model( final List< LarvaTrackedPlayer > tracked, final List< LarvaSkippedReplay > skipped ) {
        final LarvaAggregationResult result = new LarvaAggregator().aggregate(
                Arrays.asList( game( 1, record( "A" ), record( "B" ) ), game( 2, record( "A" ) ) ), tracked, summaryOf( 2 ), skipped );
        return LarvaMultiAnalysisViewModelBuilder.build( result, Locale.US, TimeZone.getTimeZone( "UTC" ) );
    }

    private static LarvaMultiAnalysisResultsPanel layout( final LarvaMultiAnalysisViewModel model, final int width ) {
        final LarvaMultiAnalysisResultsPanel panel = new LarvaMultiAnalysisResultsPanel( model );
        panel.setSize( width, panel.getPreferredSize().height );
        layoutTree( panel );
        return panel;
    }

    /** A component without a peer is never validated by {@code validate()}, so lay the tree out by hand, top down. */
    private static void layoutTree( final java.awt.Container container ) {
        container.doLayout();
        for ( final java.awt.Component child : container.getComponents() )
            if ( child instanceof java.awt.Container )
                layoutTree( (java.awt.Container) child );
    }

    /** AC 2/5: columns have equal width, player 1 left, tables stacked in the same order and not clipped when there is room. */
    @Test
    public void twoPlayersGetTwoEqualColumnsWithAlignedTables() {
        final LarvaMultiAnalysisResultsPanel panel = layout( model( Arrays.asList( tracked( "A", 2 ), tracked( "B", 1 ) ),
                Arrays.< LarvaSkippedReplay >asList() ), WIDE );
        final Rectangle left = panel.getColumnBounds( 0 );
        final Rectangle right = panel.getColumnBounds( 1 );
        assertEquals( left.width, right.width );
        assertTrue( right.x > left.x );
        assertEquals( 5, panel.getTables( 0 ).size() );
        assertEquals( 5, panel.getTables( 1 ).size() );
        for ( int i = 0; i < 5; i++ ) {
            assertEquals( "row alignment of table " + i, panel.boundsInPanel( panel.getTables( 0 ).get( i ) ).y,
                    panel.boundsInPanel( panel.getTables( 1 ).get( i ) ).y );
            assertFalse( LarvaMultiAnalysisResultsPanel.isClipped( panel.getTables( 0 ).get( i ) ) );
            assertFalse( LarvaMultiAnalysisResultsPanel.isClipped( panel.getTables( 1 ).get( i ) ) );
        }
    }

    /** AC 4 (OQ-1): one player fills the left column, the right one is an empty placeholder of equal width. */
    @Test
    public void onePlayerLeavesTheRightColumnEmptyButEquallyWide() {
        final LarvaMultiAnalysisResultsPanel panel = layout( model( Arrays.asList( tracked( "A", 2 ) ), Arrays.< LarvaSkippedReplay >asList() ), WIDE );
        assertEquals( 5, panel.getTables( 0 ).size() );
        assertEquals( 0, panel.getTables( 1 ).size() );
        assertEquals( panel.getColumnBounds( 0 ).width, panel.getColumnBounds( 1 ).width );
    }

    /** AC 6 (AC-15.3): the empty state has the summary and the message, no grid and no table. */
    @Test
    public void emptyStateHasNoTables() {
        final LarvaMultiAnalysisViewModel empty = LarvaMultiAnalysisViewModelBuilder.build( new LarvaAggregator().aggregate(
                Arrays.< LarvaAggregationGameRecord >asList(), Arrays.< LarvaTrackedPlayer >asList(), summaryOf( 0 ) ), Locale.US, TimeZone.getDefault() );
        final LarvaMultiAnalysisResultsPanel panel = layout( empty, WIDE );
        assertEquals( 0, panel.getColumnCount() );
        assertEquals( "No eligible Zerg games in the selection", panel.getEmptyText() );
        assertEquals( "", panel.getHeaderText() );
        assertTrue( panel.getSummaryText().startsWith( "selected 0" ) );
    }

    /** AC 7: the toggle shows the count, starts collapsed and expands to the entries. */
    @Test
    public void skippedSectionExpandsToTheEntries() {
        final List< LarvaSkippedReplay > skipped = Arrays.asList( new LarvaSkippedReplay( "x.SC2Replay", LarvaSkippedReplay.REASON_FAILED_TO_PARSE ),
                new LarvaSkippedReplay( "y.SC2Replay", LarvaSkippedReplay.REASON_FAILED_TO_PARSE ) );
        final LarvaMultiAnalysisResultsPanel panel = layout( model( Arrays.asList( tracked( "A", 2 ) ), skipped ), WIDE );
        assertTrue( panel.getSkippedToggleText().contains( "Skipped replays (2)" ) );
        assertFalse( panel.isSkippedExpanded() );
        panel.toggleSkipped();
        assertTrue( panel.isSkippedExpanded() );
        assertTrue( panel.getSkippedListText().contains( "x.SC2Replay — replay could not be parsed" ) );
        panel.toggleSkipped();
        assertFalse( panel.isSkippedExpanded() );
    }

    /**
     * CR-1.2: no compact look and no squeezing. Two single-look tables are wider than Scelight's page area at 1280 px (about
     * 983 px), so the panel reports its full preferred width (the page scroll pane then scrolls horizontally) and, laid out at
     * that width, no table is clipped and every table keeps its own preferred width.
     */
    @Test
    public void twoColumnsKeepTheirFullWidthAndTheNarrowPageScrolls() {
        final int scelightPageWidthAt1280 = 983;
        final String[] headers = new String[] { "Early game [<37]", "Mid game [<67]", "Late game [<90]", "End game [90+]" };
        final String[][] cells = new String[ LarvaMultiAnalysisViewModelBuilder.PHASE_ROW_LABELS.length ][ 4 ];
        for ( final String[] row : cells )
            Arrays.fill( row, "top 100% gold" );
        Arrays.fill( cells[ 0 ], "12:38 (1234/1234)" );
        final LarvaTableViewModel table = new LarvaTableViewModel( "All games \u2014 1234 games", "Metric", headers,
                LarvaMultiAnalysisViewModelBuilder.PHASE_ROW_LABELS, cells );
        final java.util.List< LarvaTableViewModel > tables = Arrays.asList( table, table );
        final LarvaMultiAnalysisViewModel.PlayerSection section = new LarvaMultiAnalysisViewModel.PlayerSection( "Player", tables );
        final LarvaMultiAnalysisViewModel big = new LarvaMultiAnalysisViewModel( "h", "s", "help", null, Arrays.asList( section, section ),
                Arrays.< String >asList() );
        final LarvaMultiAnalysisResultsPanel panel = new LarvaMultiAnalysisResultsPanel( big );
        final int tableWidth = panel.getTables( 0 ).get( 0 ).getPreferredSize().width;
        assertTrue( "preferred width " + panel.getPreferredSize().width, panel.getPreferredSize().width >= 2 * tableWidth );
        assertTrue( panel.getPreferredSize().width > scelightPageWidthAt1280 );
        final LarvaMultiAnalysisResultsPanel laidOut = layout( big, panel.getPreferredSize().width );
        for ( int column = 0; column < 2; column++ )
            for ( final LarvaTableComp comp : laidOut.getTables( column ) ) {
                assertFalse( LarvaMultiAnalysisResultsPanel.isClipped( comp ) );
                assertEquals( tableWidth, comp.getWidth() );
            }
    }
}
