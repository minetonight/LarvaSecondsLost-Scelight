package hu.aleks.larvasecondslostextmod;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics2D;

/**
 * Paints a {@link LarvaTableViewModel} as a grid table: header row, zebra body rows, centered phase cells.
 *
 * <p>All colors, paddings, minimum widths and baselines were moved unchanged from the former
 * <code>LarvaTimelinePreviewComp.drawPhaseTable</code>. Stateless; the caller sets the font and rendering hints.</p>
 */
final class LarvaTablePainter {

    /** Header height in pixels. */
    static final int HEADER_HEIGHT = 20;

    /** Body row height in pixels. */
    static final int ROW_HEIGHT = 18;

    /** Height of the optional title line above the table. */
    static final int TITLE_HEIGHT = 18;

    private static final Color HEADER_BG = new Color( 235, 241, 248 );

    private static final Color ALT_ROW_BG = new Color( 248, 250, 252 );

    private static final Color GRID_COLOR = new Color( 207, 214, 222 );

    private static final Color TEXT_COLOR = new Color( 90, 90, 90 );

    /** Horizontal padding on both sides of a text. */
    private static final int CELL_PADDING = 8;

    private static final int MIN_DATA_COLUMN_WIDTH = 58;

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
                titleOffset( model ) + tableHeight( model ) + 1 );
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
        final int tableHeight = tableHeight( model );

        if ( titleOffset > 0 ) {
            g.setColor( TEXT_COLOR );
            g.drawString( model.getTitle(), left, top + TITLE_BASELINE );
        }

        final int[] columnX = new int[ model.getColumnCount() + 2 ];
        columnX[ 0 ] = left;
        columnX[ 1 ] = left + metricColumnWidth;
        for ( int column = 0; column < columnWidths.length; column++ )
            columnX[ column + 2 ] = columnX[ column + 1 ] + columnWidths[ column ];

        paintBackground( g, model, left, tableTop, tableWidth, tableHeight );
        paintGrid( g, model, columnX, tableTop, tableWidth, tableHeight );
        paintTexts( g, fm, model, columnX, columnWidths, left, tableTop );
        return titleOffset + tableHeight;
    }

    private void paintBackground( final Graphics2D g, final LarvaTableViewModel model, final int left, final int top, final int tableWidth,
            final int tableHeight ) {
        g.setColor( Color.WHITE );
        g.fillRect( left, top, tableWidth, tableHeight );
        g.setColor( HEADER_BG );
        g.fillRect( left, top, tableWidth, HEADER_HEIGHT );
        for ( int row = 0; row < model.getRowCount(); row++ ) {
            if ( row % 2 == 1 ) {
                g.setColor( ALT_ROW_BG );
                g.fillRect( left, top + HEADER_HEIGHT + ROW_HEIGHT * row, tableWidth, ROW_HEIGHT );
            }
        }
    }

    private void paintGrid( final Graphics2D g, final LarvaTableViewModel model, final int[] columnX, final int top, final int tableWidth,
            final int tableHeight ) {
        final int left = columnX[ 0 ];
        g.setColor( GRID_COLOR );
        g.drawRect( left, top, tableWidth, tableHeight );
        for ( int i = 1; i < columnX.length - 1; i++ )
            g.drawLine( columnX[ i ], top, columnX[ i ], top + tableHeight );
        for ( int row = 0; row <= model.getRowCount(); row++ )
            g.drawLine( left, top + HEADER_HEIGHT + ROW_HEIGHT * row, left + tableWidth, top + HEADER_HEIGHT + ROW_HEIGHT * row );
    }

    private void paintTexts( final Graphics2D g, final FontMetrics fm, final LarvaTableViewModel model, final int[] columnX,
            final int[] columnWidths, final int left, final int top ) {
        g.setColor( TEXT_COLOR );
        g.drawString( model.getCornerLabel(), left + CELL_PADDING, top + HEADER_BASELINE );
        for ( int column = 0; column < columnWidths.length; column++ ) {
            final String header = model.getColumnHeader( column );
            if ( header != null )
                g.drawString( header, centeredX( fm, columnX[ column + 1 ], columnWidths[ column ], header ), top + HEADER_BASELINE );
        }

        for ( int row = 0; row < model.getRowCount(); row++ ) {
            final int baselineY = top + HEADER_HEIGHT + ROW_HEIGHT * row + ROW_BASELINE;
            g.setColor( TEXT_COLOR );
            if ( model.getRowLabel( row ) != null )
                g.drawString( model.getRowLabel( row ), left + CELL_PADDING, baselineY );
            for ( int column = 0; column < columnWidths.length; column++ ) {
                final String text = model.getCell( row, column );
                g.drawString( text, centeredX( fm, columnX[ column + 1 ], columnWidths[ column ], text ), baselineY );
            }
        }
    }

    private static int centeredX( final FontMetrics fm, final int columnLeft, final int columnWidth, final String text ) {
        return columnLeft + columnWidth / 2 - fm.stringWidth( text ) / 2;
    }

    private static int titleOffset( final LarvaTableViewModel model ) {
        return model.hasTitle() ? TITLE_HEIGHT : 0;
    }

    private static int tableHeight( final LarvaTableViewModel model ) {
        return HEADER_HEIGHT + ROW_HEIGHT * model.getRowCount();
    }

    private static int naturalTableWidth( final int metricColumnWidth, final int[] columnWidths ) {
        int tableWidth = metricColumnWidth + 1;
        for ( final int columnWidth : columnWidths )
            tableWidth += columnWidth;
        return tableWidth;
    }

    private static int resolveMetricColumnWidth( final FontMetrics fm, final LarvaTableViewModel model ) {
        int width = fm.stringWidth( model.getCornerLabel() ) + 2 * CELL_PADDING;
        for ( int row = 0; row < model.getRowCount(); row++ )
            if ( model.getRowLabel( row ) != null )
                width = Math.max( width, fm.stringWidth( model.getRowLabel( row ) ) + 2 * CELL_PADDING );
        return width;
    }

    private static int[] resolveColumnWidths( final FontMetrics fm, final LarvaTableViewModel model ) {
        final int[] widths = new int[ model.getColumnCount() ];
        for ( int column = 0; column < widths.length; column++ ) {
            final String header = model.getColumnHeader( column );
            int width = header == null ? MIN_DATA_COLUMN_WIDTH : fm.stringWidth( header ) + 2 * CELL_PADDING;
            for ( int row = 0; row < model.getRowCount(); row++ )
                width = Math.max( width, fm.stringWidth( model.getCell( row, column ) ) + 2 * CELL_PADDING );
            widths[ column ] = Math.max( MIN_DATA_COLUMN_WIDTH, width );
        }
        return widths;
    }
}
