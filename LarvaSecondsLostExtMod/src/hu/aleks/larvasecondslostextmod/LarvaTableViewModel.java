package hu.aleks.larvasecondslostextmod;

import java.util.Arrays;

/**
 * Immutable string-matrix view model of a painted table: optional title, corner label, column headers, row labels and cells.
 *
 * <p>The cell matrix is rows x columns. A <code>null</code> cell (or a missing row) reads as {@link LarvaStatFormat#NOT_AVAILABLE}.</p>
 */
final class LarvaTableViewModel {

    /** Corner label of the single-replay phase table. */
    static final String DEFAULT_CORNER_LABEL = "Metric";

    private final String title;

    private final String cornerLabel;

    private final String[] columnHeaders;

    private final String[] rowLabels;

    private final String[][] cells;

    /**
     * Creates the view model (all arrays are copied).
     *
     * @param title optional title painted above the table; <code>null</code> or empty for none
     * @param cornerLabel label of the top-left header cell; <code>null</code> reads as empty
     * @param columnHeaders column headers, at least one
     * @param rowLabels row labels (may be empty)
     * @param cells rows x columns; must have exactly <code>rowLabels.length</code> rows of at most <code>columnHeaders.length</code> cells
     * @throws IllegalArgumentException if the shapes do not fit
     */
    LarvaTableViewModel( final String title, final String cornerLabel, final String[] columnHeaders, final String[] rowLabels,
            final String[][] cells ) {
        if ( columnHeaders == null || columnHeaders.length < 1 )
            throw new IllegalArgumentException( "At least one column header is required" );
        if ( rowLabels == null || cells == null || cells.length != rowLabels.length )
            throw new IllegalArgumentException( "cells must have one row per row label" );

        this.title = title;
        this.cornerLabel = cornerLabel == null ? "" : cornerLabel;
        this.columnHeaders = columnHeaders.clone();
        this.rowLabels = rowLabels.clone();
        this.cells = new String[ cells.length ][];
        for ( int row = 0; row < cells.length; row++ ) {
            if ( cells[ row ] == null || cells[ row ].length > columnHeaders.length )
                throw new IllegalArgumentException( "Row " + row + " has more cells than columns" );
            this.cells[ row ] = cells[ row ].clone();
        }
    }

    String getTitle() {
        return title;
    }

    boolean hasTitle() {
        return title != null && title.length() > 0;
    }

    String getCornerLabel() {
        return cornerLabel;
    }

    int getColumnCount() {
        return columnHeaders.length;
    }

    int getRowCount() {
        return rowLabels.length;
    }

    /** @return header of the column, may be <code>null</code> */
    String getColumnHeader( final int column ) {
        return columnHeaders[ column ];
    }

    /** @return label of the row, may be <code>null</code> */
    String getRowLabel( final int row ) {
        return rowLabels[ row ];
    }

    /** @return the cell text; {@link LarvaStatFormat#NOT_AVAILABLE} for a <code>null</code> or missing cell */
    String getCell( final int row, final int column ) {
        if ( column >= cells[ row ].length || cells[ row ][ column ] == null )
            return LarvaStatFormat.NOT_AVAILABLE;
        return cells[ row ][ column ];
    }

    @Override
    public String toString() {
        return "LarvaTableViewModel[title=" + title + ", corner=" + cornerLabel + ", columns=" + Arrays.toString( columnHeaders ) + ", rows="
                + Arrays.toString( rowLabels ) + "]";
    }
}
