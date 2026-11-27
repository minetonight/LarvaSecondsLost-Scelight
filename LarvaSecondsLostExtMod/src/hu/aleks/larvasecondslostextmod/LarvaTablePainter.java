package hu.aleks.larvasecondslostextmod;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;

/**
 * Paints a {@link LarvaTableViewModel} as a grid table: header row, zebra body rows, centered phase cells.
 *
 * <p>All colors, paddings, minimum widths and baselines were moved unchanged from the former
 * <code>LarvaTimelinePreviewComp.drawPhaseTable</code>. The caller sets the font and rendering hints.</p>
 *
 * <p>The look is the single-replay look everywhere (one line per cell, CR-1.2): the multi-analysis page uses exactly the same
 * painter. The metric column is as wide as the widest row label (or the corner label), so no label ever wraps; a table
 * that is wider than its space is scrolled by the page, never squeezed.</p>
 *
 * <p>A cell may have a bold part ({@link LarvaTableViewModel#getBold(int, int)}); it is measured and centered with it.</p>
 */
final class LarvaTablePainter {

    /** Header height in pixels. */
    static final int HEADER_HEIGHT = 20;

    /** Body row height in pixels. */
    static final int ROW_HEIGHT = 18;

    /** Height of the optional title line above the table. */
    static final int TITLE_HEIGHT = 18;

    /** Padding left and right of a text in a cell. */
    private static final int CELL_PADDING = 8;

    /** Minimum width of a data column. */
    private static final int MIN_DATA_COLUMN_WIDTH = 58;

    private static final Color HEADER_BG = new Color( 235, 241, 248 );

    private static final Color ALT_ROW_BG = new Color( 248, 250, 252 );

    private static final Color GRID_COLOR = new Color( 207, 214, 222 );

    private static final Color TEXT_COLOR = new Color( 90, 90, 90 );

    private static final int MIN_TABLE_WIDTH = 220;

    private static final int HEADER_BASELINE = 14;

    private static final int ROW_BASELINE = 13;

    private static final int TITLE_BASELINE = 13;

    /**
     * Measures the bounding box of the painted table (title included), including the one-pixel grid line right and below.
     *
     * @param fm font metrics of the font used for painting
     * @param model model
     * @return size to reserve; width is the natural width (at least 220), unconstrained by any maximum
     */
    Dimension measure( final FontMetrics fm, final LarvaTableViewModel model ) {
        final int[] columnWidths = resolveColumnWidths( fm, model );
        return new Dimension( Math.max( MIN_TABLE_WIDTH, naturalTableWidth( resolveMetricColumnWidth( fm, model ), columnWidths ) ) + 1,
                titleOffset( model ) + rowTops( model )[ model.getRowCount() ] + 1 );
    }

    /**
     * Paints the table.
     *
     * @param g graphics (font and color are changed)
     * @param fm font metrics of the graphics' font
     * @param model model
     * @param left left edge
     * @param top top edge (of the title when there is one)
     * @param maxWidth maximum width the table may fill (the fill is clamped to it, but never below 220)
     * @return the painted layout height (title + header + rows, without the final grid pixel); 146 for the 7-row phase table
     */
    int paint( final Graphics2D g, final FontMetrics fm, final LarvaTableViewModel model, final int left, final int top, final int maxWidth ) {
        final int metricColumnWidth = resolveMetricColumnWidth( fm, model );
        final int[] columnWidths = resolveColumnWidths( fm, model );
        final int tableWidth = Math.min( Math.max( MIN_TABLE_WIDTH, naturalTableWidth( metricColumnWidth, columnWidths ) ),
                Math.max( MIN_TABLE_WIDTH, maxWidth ) );
        final int titleOffset = titleOffset( model );
        final int tableTop = top + titleOffset;
        final int[] rowTops = rowTops( model );
        final int tableHeight = rowTops[ model.getRowCount() ];

        if ( titleOffset > 0 ) {
            g.setColor( TEXT_COLOR );
            g.drawString( model.getTitle(), left, top + TITLE_BASELINE );
        }

        final int[] columnX = new int[ model.getColumnCount() + 2 ];
        columnX[ 0 ] = left;
        columnX[ 1 ] = left + metricColumnWidth;
        for ( int column = 0; column < columnWidths.length; column++ )
            columnX[ column + 2 ] = columnX[ column + 1 ] + columnWidths[ column ];

        paintBackground( g, model, rowTops, left, tableTop, tableWidth );
        paintGrid( g, rowTops, columnX, tableTop, tableWidth );
        paintTexts( g, fm, model, rowTops, columnX, columnWidths, left, tableTop );
        return titleOffset + tableHeight;
    }

    private void paintBackground( final Graphics2D g, final LarvaTableViewModel model, final int[] rowTops, final int left, final int top,
            final int tableWidth ) {
        g.setColor( Color.WHITE );
        g.fillRect( left, top, tableWidth, rowTops[ model.getRowCount() ] );
        g.setColor( HEADER_BG );
        g.fillRect( left, top, tableWidth, rowTops[ 0 ] );
        for ( int row = 0; row < model.getRowCount(); row++ ) {
            if ( row % 2 == 1 ) {
                g.setColor( ALT_ROW_BG );
                g.fillRect( left, top + rowTops[ row ], tableWidth, rowTops[ row + 1 ] - rowTops[ row ] );
            }
        }
    }

    private void paintGrid( final Graphics2D g, final int[] rowTops, final int[] columnX, final int top, final int tableWidth ) {
        final int left = columnX[ 0 ];
        final int tableHeight = rowTops[ rowTops.length - 1 ];
        g.setColor( GRID_COLOR );
        g.drawRect( left, top, tableWidth, tableHeight );
        for ( int i = 1; i < columnX.length - 1; i++ )
            g.drawLine( columnX[ i ], top, columnX[ i ], top + tableHeight );
        for ( final int rowTop : rowTops )
            g.drawLine( left, top + rowTop, left + tableWidth, top + rowTop );
    }

    private void paintTexts( final Graphics2D g, final FontMetrics fm, final LarvaTableViewModel model, final int[] rowTops, final int[] columnX,
            final int[] columnWidths, final int left, final int top ) {
        g.setColor( TEXT_COLOR );
        g.drawString( model.getCornerLabel(), left + CELL_PADDING, top + HEADER_BASELINE );
        for ( int column = 0; column < columnWidths.length; column++ ) {
            final String header = model.getColumnHeader( column );
            if ( header != null )
                g.drawString( header, centeredX( fm, columnX[ column + 1 ], columnWidths[ column ], header ), top + HEADER_BASELINE );
        }

        for ( int row = 0; row < model.getRowCount(); row++ ) {
            final int baseline = top + rowTops[ row ] + ROW_BASELINE;
            g.setColor( TEXT_COLOR );
            if ( model.getRowLabel( row ) != null )
                g.drawString( model.getRowLabel( row ), left + CELL_PADDING, baseline );
            for ( int column = 0; column < columnWidths.length; column++ ) {
                final String cell = model.getCell( row, column );
                final LarvaTableViewModel.Bold bold = model.getBold( row, column );
                final int x = columnX[ column + 1 ] + columnWidths[ column ] / 2 - textWidth( fm, cell, bold ) / 2;
                drawCell( g, fm, cell, bold, x, baseline );
            }
        }
    }

    private static int centeredX( final FontMetrics fm, final int columnLeft, final int columnWidth, final String text ) {
        return columnLeft + columnWidth / 2 - fm.stringWidth( text ) / 2;
    }

    /** Draws a cell text, its bold part (if any) in the bold variant of the font. */
    private static void drawCell( final Graphics2D g, final FontMetrics fm, final String text, final LarvaTableViewModel.Bold bold,
            final int x, final int baseline ) {
        if ( bold == null ) {
            g.drawString( text, x, baseline );
            return;
        }
        final Font regular = g.getFont();
        final String before = text.substring( 0, bold.start );
        final String boldPart = text.substring( bold.start, bold.end );
        final int boldX = x + fm.stringWidth( before );
        g.drawString( before, x, baseline );
        g.setFont( regular.deriveFont( Font.BOLD ) );
        g.drawString( boldPart, boldX, baseline );
        g.setFont( regular );
        g.drawString( text.substring( bold.end ), boldX + boldWidth( fm, boldPart ), baseline );
    }

    /** @return width of a cell text with its bold part (if any) */
    private static int textWidth( final FontMetrics fm, final String text, final LarvaTableViewModel.Bold bold ) {
        if ( bold == null )
            return fm.stringWidth( text );
        return fm.stringWidth( text.substring( 0, bold.start ) ) + boldWidth( fm, text.substring( bold.start, bold.end ) )
                + fm.stringWidth( text.substring( bold.end ) );
    }

    /** @return width of the text in the bold variant of the metrics' font, measured as the metrics measure */
    private static int boldWidth( final FontMetrics fm, final String text ) {
        final Font bold = fm.getFont().deriveFont( Font.BOLD );
        return (int) Math.ceil( bold.getStringBounds( text, fm.getFontRenderContext() ).getWidth() );
    }

    private static int titleOffset( final LarvaTableViewModel model ) {
        return model.hasTitle() ? TITLE_HEIGHT : 0;
    }

    /**
     * @return {@code tops[i]} = top of row i relative to the table top (so {@code tops[0]} is the header height), and
     *         {@code tops[rowCount]} = the table height
     */
    private int[] rowTops( final LarvaTableViewModel model ) {
        final int[] tops = new int[ model.getRowCount() + 1 ];
        tops[ 0 ] = HEADER_HEIGHT;
        for ( int row = 0; row < model.getRowCount(); row++ )
            tops[ row + 1 ] = tops[ row ] + ROW_HEIGHT;
        return tops;
    }

    private int naturalTableWidth( final int metricColumnWidth, final int[] columnWidths ) {
        int tableWidth = metricColumnWidth + 1;
        for ( final int columnWidth : columnWidths )
            tableWidth += columnWidth;
        return tableWidth;
    }

    /** Widest of the corner label and every row label, so a label never wraps (CR-1.2). */
    private int resolveMetricColumnWidth( final FontMetrics fm, final LarvaTableViewModel model ) {
        int width = fm.stringWidth( model.getCornerLabel() ) + 2 * CELL_PADDING;
        for ( int row = 0; row < model.getRowCount(); row++ )
            if ( model.getRowLabel( row ) != null )
                width = Math.max( width, fm.stringWidth( model.getRowLabel( row ) ) + 2 * CELL_PADDING );
        return width;
    }

    private int[] resolveColumnWidths( final FontMetrics fm, final LarvaTableViewModel model ) {
        final int[] widths = new int[ model.getColumnCount() ];
        for ( int column = 0; column < widths.length; column++ ) {
            final String header = model.getColumnHeader( column );
            int width = MIN_DATA_COLUMN_WIDTH;
            if ( header != null )
                width = Math.max( width, fm.stringWidth( header ) + 2 * CELL_PADDING );
            for ( int row = 0; row < model.getRowCount(); row++ )
                width = Math.max( width, textWidth( fm, model.getCell( row, column ), model.getBold( row, column ) ) + 2 * CELL_PADDING );
            widths[ column ] = width;
        }
        return widths;
    }
}
