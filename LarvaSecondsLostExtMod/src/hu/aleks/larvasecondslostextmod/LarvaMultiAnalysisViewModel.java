package hu.aleks.larvasecondslostextmod;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Immutable, display-ready model of a finished multi analysis (E17-S15): header, filter summary, per-player table models
 * and the skipped list, all as strings. Built off the EDT by {@link LarvaMultiAnalysisViewModelBuilder}; the results panel
 * only paints it. {@link #toText()} is the plain-text form used by the dev commands and the golden check.
 */
final class LarvaMultiAnalysisViewModel {

    /** Text of the empty state (AC-15.3). */
    static final String EMPTY_MESSAGE = "No eligible Zerg games in the selection";

    private final String header;
    private final String filterSummary;
    private final String helpLine;
    private final String emptyMessage;
    private final List< PlayerSection > players;
    private final List< String > skippedLines;

    LarvaMultiAnalysisViewModel( final String header, final String filterSummary, final String helpLine, final String emptyMessage,
            final List< PlayerSection > players, final List< String > skippedLines ) {
        this.header = header;
        this.filterSummary = filterSummary;
        this.helpLine = helpLine;
        this.emptyMessage = emptyMessage;
        this.players = Collections.unmodifiableList( new ArrayList< PlayerSection >( players ) );
        this.skippedLines = Collections.unmodifiableList( new ArrayList< String >( skippedLines ) );
    }

    /** @return the header line, or null in the empty state (FR-8, AC-15.3) */
    String getHeader() {
        return header;
    }

    String getFilterSummary() {
        return filterSummary;
    }

    /** @return the short benchmark caveat under the header, or null in the empty state */
    String getHelpLine() {
        return helpLine;
    }

    /** @return the empty-state message, or null if there are tables */
    String getEmptyMessage() {
        return emptyMessage;
    }

    boolean isEmpty() {
        return emptyMessage != null;
    }

    /** @return one section per tracked player (left column first); empty in the empty state */
    List< PlayerSection > getPlayers() {
        return players;
    }

    /** @return {@code file — reason} lines */
    List< String > getSkippedLines() {
        return skippedLines;
    }

    /** @return the plain-text form (stable line format, parsed by the golden check) */
    String toText() {
        final StringBuilder sb = new StringBuilder();
        if ( header != null )
            sb.append( "header: " ).append( header ).append( '\n' );
        sb.append( "summary: " ).append( filterSummary ).append( '\n' );
        if ( helpLine != null )
            sb.append( "help: " ).append( helpLine ).append( '\n' );
        if ( emptyMessage != null )
            sb.append( "empty: " ).append( emptyMessage ).append( '\n' );
        int rank = 0;
        for ( final PlayerSection player : players ) {
            sb.append( "player " ).append( ++rank ).append( ": " ).append( player.getHeading() ).append( '\n' );
            for ( final LarvaTableViewModel table : player.getTables() )
                appendTable( sb, table );
        }
        sb.append( "skipped (" ).append( skippedLines.size() ).append( "):\n" );
        for ( final String line : skippedLines )
            sb.append( "  " ).append( line ).append( '\n' );
        return sb.toString();
    }

    private static void appendTable( final StringBuilder sb, final LarvaTableViewModel table ) {
        sb.append( "table: " ).append( table.getTitle() ).append( '\n' );
        sb.append( "  " ).append( table.getCornerLabel() );
        for ( int column = 0; column < table.getColumnCount(); column++ )
            sb.append( " | " ).append( table.getColumnHeader( column ) );
        sb.append( '\n' );
        for ( int row = 0; row < table.getRowCount(); row++ ) {
            sb.append( "  " ).append( table.getRowLabel( row ) );
            for ( int column = 0; column < table.getColumnCount(); column++ )
                sb.append( " | " ).append( table.getCell( row, column ) );
            sb.append( '\n' );
        }
    }

    /** One player's column: heading, the milestone table, then the four matchup tables (OQ-3, CR-1.3). */
    static final class PlayerSection {

        private final String heading;
        private final List< LarvaTableViewModel > tables;

        PlayerSection( final String heading, final List< LarvaTableViewModel > tables ) {
            this.heading = heading;
            this.tables = Collections.unmodifiableList( new ArrayList< LarvaTableViewModel >( tables ) );
        }

        String getHeading() {
            return heading;
        }

        /** @return the milestone table first, then All, ZvP, ZvT, ZvZ (CR-1.3) */
        List< LarvaTableViewModel > getTables() {
            return tables;
        }
    }
}
