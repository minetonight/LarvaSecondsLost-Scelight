package hu.aleks.larvasecondslostextmod;

import java.util.Arrays;

/**
 * Immutable string-matrix view model of a painted table: optional title, corner label, column headers, row labels and cells.
 *
 * <p>The cell matrix is rows x columns. A <code>null</code> cell (or a missing row) reads as {@link LarvaStatFormat#NOT_AVAILABLE}.</p>
 *
 * <p>A cell may have one bold part ({@link Bold}): the milestone times of the multi page (E20-S11) and the league label of a ranking
 * inside the bands (E20-S12).</p>
 */
final class LarvaTableViewModel {

    /** Corner label of the single-replay phase table. */
    static final String DEFAULT_CORNER_LABEL = "Metric";

    private final String title;

    private final String cornerLabel;

    private final String[] columnHeaders;

    private final String[] rowLabels;

    private final String[][] cells;

    /** Bold part per cell, rows x columns, null entries for none; null for no bold at all. */
    private final Bold[][] bold;

    /** The bold part {@code [start, end)} of a cell text. Immutable. */
    static final class Bold {

        final int start;

        final int end;

        private Bold( final int start, final int end ) {
            if ( start < 0 || end <= start )
                throw new IllegalArgumentException( "Bold part [" + start + ", " + end + ") is empty or negative" );
            this.start = start;
            this.end = end;
        }
    }

    /** @return the bold part {@code [start, end)} of a cell text */
    static Bold bold( final int start, final int end ) {
        return new Bold( start, end );
    }

    /** @return the league label of the ranking as the bold part, or null when it has none (outside the bands, n/a) */
    static Bold boldLabel( final LarvaRanking ranking ) {
        return ranking == null || !ranking.hasLabel() ? null : new Bold( ranking.getLabelStart(), ranking.getLabelEnd() );
    }

    /**
     * Creates the view model without bold parts (all arrays are copied).
     *
     * @see #LarvaTableViewModel(String, String, String[], String[], String[][], int[][])
     */
    LarvaTableViewModel( final String title, final String cornerLabel, final String[] columnHeaders, final String[] rowLabels,
            final String[][] cells ) {
        this( title, cornerLabel, columnHeaders, rowLabels, cells, null );
    }

    /**
     * Creates the view model (all arrays are copied).
     *
     * @param title optional title painted above the table; <code>null</code> or empty for none
     * @param cornerLabel label of the top-left header cell; <code>null</code> reads as empty
     * @param columnHeaders column headers, at least one
     * @param rowLabels row labels (may be empty)
     * @param cells rows x columns; must have exactly <code>rowLabels.length</code> rows of at most <code>columnHeaders.length</code> cells
     * @param bold per cell the bold part or null, same shape as <code>cells</code>; <code>null</code> for none
     * @throws IllegalArgumentException if the shapes do not fit or a bold part is outside its cell text
     */
    LarvaTableViewModel( final String title, final String cornerLabel, final String[] columnHeaders, final String[] rowLabels,
            final String[][] cells, final Bold[][] bold ) {
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
        this.bold = bold == null ? null : copyBold( bold );
    }

    private Bold[][] copyBold( final Bold[][] parts ) {
        if ( parts.length != cells.length )
            throw new IllegalArgumentException( "bold must have one row per row label" );
        final Bold[][] copy = new Bold[ parts.length ][];
        for ( int row = 0; row < parts.length; row++ ) {
            if ( parts[ row ] == null || parts[ row ].length != cells[ row ].length )
                throw new IllegalArgumentException( "Row " + row + " of bold does not match its cells" );
            copy[ row ] = parts[ row ].clone();
            for ( int column = 0; column < copy[ row ].length; column++ )
                if ( copy[ row ][ column ] != null && copy[ row ][ column ].end > getCell( row, column ).length() )
                    throw new IllegalArgumentException( "Bold part ends after the text of cell " + row + "," + column );
        }
        return copy;
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

    /** @return the bold part of the cell text, or null for none */
    Bold getBold( final int row, final int column ) {
        if ( bold == null || column >= bold[ row ].length )
            return null;
        return bold[ row ][ column ];
    }

    /** @return the bold characters of the cell text, or {@code ""} for none */
    String getBoldText( final int row, final int column ) {
        final Bold part = getBold( row, column );
        return part == null ? "" : getCell( row, column ).substring( part.start, part.end );
    }

    @Override
    public String toString() {
        return "LarvaTableViewModel[title=" + title + ", corner=" + cornerLabel + ", columns=" + Arrays.toString( columnHeaders ) + ", rows="
                + Arrays.toString( rowLabels ) + "]";
    }
}
