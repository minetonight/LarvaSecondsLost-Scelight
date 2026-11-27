package hu.aleks.larvasecondslostextmod;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import org.junit.Test;

/** View model content and painter geometry (headless BufferedImage graphics). */
public class LarvaTablePainterTest {

    private static final int GRID_RGB = new Color( 207, 214, 222 ).getRGB() & 0xFFFFFF;

    private static final int WHITE = 0xFFFFFF;

    private static final int MARGIN = 20;

    @Test
    public void fromPlayerPhaseTableYieldsTodaysRowsAndCells() {
        final LarvaTableViewModel model = LarvaPhaseTableViewModels.fromPlayerPhaseTable( LarvaPhaseTableRenderFixture.fullTable( "A" ), null );
        assertEquals( "Metric", model.getCornerLabel() );
        assertEquals( false, model.hasTitle() );
        assertEquals( 4, model.getColumnCount() );
        assertEquals( "Early game [<37]", model.getColumnHeader( 0 ) );
        assertEquals( "End game [90+]", model.getColumnHeader( 3 ) );
        assertArrayEquals( new String[] { "Phase span", "Inject uptime [%]", "Larva gen [lrv / htch / min]", "larva gen ranking",
            "Larva missed [lrv / htch / min]", "larva missed ranking", "Inject-missed larva [lrv / htch / min]" }, rowLabels( model ) );
        // Game clock: 4000 loops = 178.6 s; rates per game clock minute (1344 loops), 1.4 times the pre-2026-11-26 values.
        assertEquals( "0:00-2:58", model.getCell( 0, 0 ) );
        assertEquals( "66.65%", model.getCell( 1, 0 ) );
        assertEquals( "2.35", model.getCell( 2, 0 ) );
        assertEquals( "n/a", model.getCell( 3, 0 ) ); // fixture bands: no early larva gen band
        assertEquals( "0.50", model.getCell( 4, 0 ) );
        assertEquals( "top 50% masters", model.getCell( 5, 0 ) ); // fixture bands (test/resources)
        assertEquals( "0.34", model.getCell( 6, 0 ) );
        assertEquals( "0.00", model.getCell( 4, 3 ) );
    }

    @Test
    public void missingPhasesGiveNotAvailableCells() {
        final LarvaTableViewModel model = LarvaPhaseTableViewModels.fromPlayerPhaseTable( LarvaPhaseTableRenderFixture.earlyOnlyTable( "B" ), null );
        for ( int row = 0; row < model.getRowCount(); row++ )
            assertEquals( "row " + row, "n/a", model.getCell( row, 3 ) );
        assertEquals( "0:00-2:18", model.getCell( 0, 0 ) );
    }

    @Test
    public void nullCellsReadAsNotAvailableAndModelIsImmutable() {
        final String[] headers = { "A", "B" };
        final String[][] cells = { { "1", null } };
        final LarvaTableViewModel model = new LarvaTableViewModel( "t", null, headers, new String[] { "r" }, cells );
        headers[ 0 ] = "changed";
        cells[ 0 ][ 0 ] = "changed";
        assertEquals( "A", model.getColumnHeader( 0 ) );
        assertEquals( "1", model.getCell( 0, 0 ) );
        assertEquals( "n/a", model.getCell( 0, 1 ) );
        assertEquals( "", model.getCornerLabel() );
    }

    @Test( expected = IllegalArgumentException.class )
    public void rejectsRowCountMismatch() {
        new LarvaTableViewModel( null, "x", new String[] { "A" }, new String[] { "r1", "r2" }, new String[][] { { "1" } } );
    }

    /** E20-S11: a bold time is measured with the bold font and puts more ink on the page than the same text in plain. */
    @Test
    public void aBoldPrefixIsMeasuredAndPaintedBold() {
        final String cell = "10:08 10:08 10:08 10:08 10:08 10:08 (1234/1234)"; // wider than the 220 px minimum table
        final LarvaTableViewModel plain = new LarvaTableViewModel( null, "M", new String[] { "A" }, new String[] { "r" }, new String[][] { { cell } } );
        final LarvaTableViewModel bold = new LarvaTableViewModel( null, "M", new String[] { "A" }, new String[] { "r" }, new String[][] { { cell } },
                new LarvaTableViewModel.Bold[][] { { LarvaTableViewModel.bold( 0, 35 ) } } );
        assertEquals( "", plain.getBoldText( 0, 0 ) );
        assertEquals( "10:08 10:08 10:08 10:08 10:08 10:08", bold.getBoldText( 0, 0 ) );
        final FontMetrics fm = metrics();
        // Dialog has bold digits as wide as plain ones, so the bold cell is at least as wide, never narrower
        assertTrue( new LarvaTablePainter().measure( fm, bold ).width >= new LarvaTablePainter().measure( fm, plain ).width );
        assertTrue( ink( bold ) > ink( plain ) );
    }

    /** E20-S12: a bold part in the middle or at the end (the league of {@code top 50% plat}) paints bold too. */
    @Test
    public void aBoldLabelAtTheEndIsPaintedBold() {
        final String cell = "top 50% masters/GM/pro masters/GM/pro masters/GM/pro";
        final LarvaTableViewModel plain = new LarvaTableViewModel( null, "M", new String[] { "A" }, new String[] { "r" }, new String[][] { { cell } } );
        final LarvaTableViewModel bold = new LarvaTableViewModel( null, "M", new String[] { "A" }, new String[] { "r" }, new String[][] { { cell } },
                new LarvaTableViewModel.Bold[][] { { LarvaTableViewModel.bold( 8, cell.length() ) } } );
        assertEquals( "masters/GM/pro masters/GM/pro masters/GM/pro", bold.getBoldText( 0, 0 ) );
        assertTrue( ink( bold ) > ink( plain ) );
    }

    @Test
    public void aRankingWithoutLabelHasNoBoldPart() {
        assertEquals( null, LarvaTableViewModel.boldLabel( LarvaRanking.plain( "aim for 9:01" ) ) );
        assertEquals( null, LarvaTableViewModel.boldLabel( null ) );
        final LarvaTableViewModel.Bold label = LarvaTableViewModel.boldLabel( LarvaRanking.withLabel( "top 50% ", "plat", "" ) );
        assertEquals( 8, label.start );
        assertEquals( 12, label.end );
    }

    @Test( expected = IllegalArgumentException.class )
    public void rejectsABoldPartPastTheCellText() {
        new LarvaTableViewModel( null, "x", new String[] { "A" }, new String[] { "r" }, new String[][] { { "1:00" } },
                new LarvaTableViewModel.Bold[][] { { LarvaTableViewModel.bold( 0, 5 ) } } );
    }

    @Test( expected = IllegalArgumentException.class )
    public void rejectsBoldOfAnotherShape() {
        new LarvaTableViewModel( null, "x", new String[] { "A" }, new String[] { "r" }, new String[][] { { "1:00" } },
                new LarvaTableViewModel.Bold[][] { { null }, { null } } );
    }

    @Test( expected = IllegalArgumentException.class )
    public void rejectsAnEmptyBoldPart() {
        LarvaTableViewModel.bold( 3, 3 );
    }

    /** @return number of dark text pixels of the painted model */
    private static int ink( final LarvaTableViewModel model ) {
        final BufferedImage image = new BufferedImage( 600, 100, BufferedImage.TYPE_INT_RGB );
        paintOnWhite( image, model, MARGIN, MARGIN );
        int dark = 0;
        for ( int y = 0; y < image.getHeight(); y++ )
            for ( int x = 0; x < image.getWidth(); x++ )
                if ( ( image.getRGB( x, y ) & 0xFF ) < 160 )
                    dark++;
        return dark;
    }

    @Test( expected = IllegalArgumentException.class )
    public void rejectsNoColumns() {
        new LarvaTableViewModel( null, "x", new String[0], new String[0], new String[0][] );
    }

    @Test
    public void measureGrowsMonotonicallyWithRowCount() {
        final FontMetrics fm = metrics();
        int previousHeight = -1;
        for ( int rows = 0; rows <= 6; rows++ ) {
            final int height = new LarvaTablePainter().measure( fm, matrixModel( rows ) ).height;
            assertTrue( "rows=" + rows, height > previousHeight );
            previousHeight = height;
        }
        assertEquals( LarvaTablePainter.ROW_HEIGHT, new LarvaTablePainter().measure( fm, matrixModel( 5 ) ).height
                - new LarvaTablePainter().measure( fm, matrixModel( 4 ) ).height );
    }

    @Test
    public void measureReservesTitleLine() {
        final FontMetrics fm = metrics();
        final LarvaTableViewModel untitled = new LarvaTableViewModel( null, "Metric", new String[] { "A" }, new String[] { "r" }, new String[][] { { "1" } } );
        final LarvaTableViewModel titled = new LarvaTableViewModel( "ZvP - 3 games", "Metric", new String[] { "A" }, new String[] { "r" }, new String[][] { { "1" } } );
        assertEquals( LarvaTablePainter.TITLE_HEIGHT, new LarvaTablePainter().measure( fm, titled ).height - new LarvaTablePainter().measure( fm, untitled ).height );
    }

    /** AC-3: Metric + All/ZvP/ZvT/ZvZ and 4 rows with long cells: all ink lies inside the measured size and the last grid lines are inside it. */
    @Test
    public void fiveColumnFourRowTableIsNotClippedAtMeasuredSize() {
        final String[] headers = { "All", "ZvP", "ZvT", "ZvZ" };
        final String[] rowLabels = { "Early game [<37]", "Mid game [<67]", "Late game [<90]", "End game [90+]" };
        final String[][] cells = {
            { "3.12 (10/12)", "n/a", "2.50 (3/4)", "1.00 (1/1)" },
            { "12.34 (200/200)", "n/a", "n/a", "n/a" },
            { "0.00", "0.00", "0.00", "0.00" },
            { "n/a", "n/a", "n/a", "a very very wide cell text" } };
        final LarvaTableViewModel model = new LarvaTableViewModel( "Player X - 12 games", "Phase", headers, rowLabels, cells );

        final BufferedImage big = new BufferedImage( 900, 300, BufferedImage.TYPE_INT_RGB );
        final Dimension size = paintOnWhite( big, model, MARGIN, MARGIN );

        // right and bottom grid line are the last inked column / row
        assertEquals( GRID_RGB, big.getRGB( MARGIN + size.width - 1, MARGIN + size.height - 1 ) & 0xFFFFFF );
        for ( int y = 0; y < big.getHeight(); y++ ) {
            for ( int x = 0; x < big.getWidth(); x++ ) {
                final boolean inside = x >= MARGIN && x < MARGIN + size.width && y >= MARGIN && y < MARGIN + size.height;
                if ( !inside )
                    assertEquals( "ink outside measured box at " + x + "," + y, WHITE, big.getRGB( x, y ) & 0xFFFFFF );
            }
        }
    }

    /**
     * CR-1.2: the multi page uses the single look. The metric column fits the widest row label on one line, so the table
     * is as wide as label + data columns, has the plain 20/18 px rows and paints no ink outside its measured box.
     */
    @Test
    public void longRowLabelsStayOnOneLineAndInsideTheMeasuredBox() {
        final String[] headers = { "Early game [<37]", "Mid game [<67]", "Late game [<90]", "End game [90+]" };
        final String[] rowLabels = LarvaMultiAnalysisViewModelBuilder.PHASE_ROW_LABELS;
        final String[][] cells = new String[ rowLabels.length ][ 4 ];
        for ( final String[] row : cells )
            java.util.Arrays.fill( row, "top 100% gold" );
        cells[ 0 ][ 0 ] = "10:08 (1234/1234)";
        final LarvaTableViewModel model = new LarvaTableViewModel( "All games", "Metric", headers, rowLabels, cells );
        final FontMetrics fm = metrics();
        final Dimension size = new LarvaTablePainter().measure( fm, model );
        int widestLabel = 0;
        for ( final String label : rowLabels )
            widestLabel = Math.max( widestLabel, fm.stringWidth( label ) );
        assertTrue( "table " + size.width + " must hold the widest label " + widestLabel, size.width > widestLabel + 16 + 4 * 58 );
        assertEquals( LarvaTablePainter.TITLE_HEIGHT + LarvaTablePainter.HEADER_HEIGHT + rowLabels.length * LarvaTablePainter.ROW_HEIGHT + 1,
                size.height );

        final BufferedImage image = new BufferedImage( 1400, 400, BufferedImage.TYPE_INT_RGB );
        final Graphics2D g = image.createGraphics();
        try {
            g.setColor( Color.WHITE );
            g.fillRect( 0, 0, image.getWidth(), image.getHeight() );
            g.setFont( new java.awt.Font( LarvaPhaseTableRenderFixture.FIXTURE_FONT_FAMILY, java.awt.Font.PLAIN, LarvaPhaseTableRenderFixture.FIXTURE_FONT_SIZE ) );
            final int painted = new LarvaTablePainter().paint( g, g.getFontMetrics(), model, MARGIN, MARGIN, size.width );
            assertEquals( size.height - 1, painted );
        }
        finally {
            g.dispose();
        }
        for ( int y = 0; y < image.getHeight(); y++ )
            for ( int x = 0; x < image.getWidth(); x++ ) {
                final boolean inside = x >= MARGIN && x < MARGIN + size.width && y >= MARGIN && y < MARGIN + size.height;
                if ( !inside )
                    assertEquals( "ink outside measured box at " + x + "," + y, WHITE, image.getRGB( x, y ) & 0xFFFFFF );
            }
    }

    private static Dimension paintOnWhite( final BufferedImage image, final LarvaTableViewModel model, final int left, final int top ) {
        final Graphics2D g = image.createGraphics();
        try {
            g.setColor( Color.WHITE );
            g.fillRect( 0, 0, image.getWidth(), image.getHeight() );
            g.setFont( new java.awt.Font( LarvaPhaseTableRenderFixture.FIXTURE_FONT_FAMILY, java.awt.Font.PLAIN, LarvaPhaseTableRenderFixture.FIXTURE_FONT_SIZE ) );
            final Dimension size = new LarvaTablePainter().measure( g.getFontMetrics(), model );
            new LarvaTablePainter().paint( g, g.getFontMetrics(), model, left, top, size.width );
            return size;
        }
        finally {
            g.dispose();
        }
    }

    private static LarvaTableViewModel matrixModel( final int rows ) {
        final String[] labels = new String[ rows ];
        final String[][] cells = new String[ rows ][ 2 ];
        for ( int row = 0; row < rows; row++ ) {
            labels[ row ] = "row " + row;
            cells[ row ][ 0 ] = "1";
        }
        return new LarvaTableViewModel( null, "Metric", new String[] { "A", "B" }, labels, cells );
    }

    private static String[] rowLabels( final LarvaTableViewModel model ) {
        final String[] labels = new String[ model.getRowCount() ];
        for ( int row = 0; row < labels.length; row++ )
            labels[ row ] = model.getRowLabel( row );
        return labels;
    }

    private static FontMetrics metrics() {
        final BufferedImage image = new BufferedImage( 4, 4, BufferedImage.TYPE_INT_RGB );
        final Graphics2D g = image.createGraphics();
        g.setFont( new java.awt.Font( LarvaPhaseTableRenderFixture.FIXTURE_FONT_FAMILY, java.awt.Font.PLAIN, LarvaPhaseTableRenderFixture.FIXTURE_FONT_SIZE ) );
        final FontMetrics fm = g.getFontMetrics();
        g.dispose();
        return fm;
    }
}
