package hu.aleks.larvasecondslostextmod;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;

/**
 * Results of a finished multi analysis (E17-S15): header, filter summary and help line on top, a two-column grid of equal
 * width (player 1 left, player 2 right) below, and a collapsible skipped list at the bottom (FR-8, FR-12 to FR-15, OQ-1,
 * OQ-3). Each column stacks the player heading, the milestone table and the All / ZvP / ZvT / ZvZ tables (CR-1.3).
 *
 * <p>The grid is built row by row: every grid row (heading, one table row) is a {@code GridLayout(1, 2)} panel, so both columns
 * have the same width and the tables of both players always have the same top and height, even when one of them wraps a wide
 * cell. One player leaves the right cell of every row an empty placeholder of the same width (OQ-1). The tables use exactly the
 * single-replay look (CR-1.2). Two columns can be wider than Scelight's page area; its page scroll pane then scrolls
 * horizontally (the panel is never squeezed).</p>
 *
 * <p>Pure view of an immutable {@link LarvaMultiAnalysisViewModel}. It does no scrolling of its own: Scelight's page scroll pane
 * scrolls it vertically. EDT only.</p>
 */
@SuppressWarnings("serial")
final class LarvaMultiAnalysisResultsPanel extends JPanel {

    private static final int PAGE_GAP = 8;
    private static final int COLUMN_GAP = 16;
    private static final int TABLE_GAP = 10;
    private static final int PLAYER_COLUMNS = 2;
    private static final float HEADING_FONT_SCALE = 1.25f;
    private static final String EXPANDED_MARK = "[-] ";
    private static final String COLLAPSED_MARK = "[+] ";

    private final LarvaMultiAnalysisViewModel model;
    private final JLabel headerLabel = new JLabel();
    private final JLabel summaryLabel = new JLabel();
    private final JLabel helpLabel = new JLabel();
    private final JLabel emptyLabel = new JLabel();
    private final List< JPanel > columnCells = new ArrayList< JPanel >();
    private final List< List< LarvaTableComp > > tablesByColumn = new ArrayList< List< LarvaTableComp > >();
    private final JButton skippedToggle = new JButton();
    private final JTextArea skippedList = new JTextArea();

    /** @param model what to show */
    LarvaMultiAnalysisResultsPanel( final LarvaMultiAnalysisViewModel model ) {
        super( new GridBagLayout() );
        this.model = model;
        setBorder( BorderFactory.createEmptyBorder( PAGE_GAP, PAGE_GAP, PAGE_GAP, PAGE_GAP ) );
        buildTop();
        buildGrid();
        buildSkipped();
    }

    private void buildTop() {
        int row = 0;
        if ( model.getHeader() != null ) {
            headerLabel.setText( model.getHeader() );
            headerLabel.setFont( headerLabel.getFont().deriveFont( Font.BOLD ) );
            addRow( headerLabel, row++, 0.0 );
        }
        summaryLabel.setText( model.getFilterSummary() );
        addRow( summaryLabel, row++, 0.0 );
        if ( model.getHelpLine() != null ) {
            helpLabel.setText( model.getHelpLine() );
            helpLabel.setFont( helpLabel.getFont().deriveFont( Font.ITALIC ) );
            addRow( helpLabel, row++, 0.0 );
        }
        if ( model.getEmptyMessage() != null ) {
            emptyLabel.setText( model.getEmptyMessage() );
            emptyLabel.setFont( emptyLabel.getFont().deriveFont( Font.BOLD ) );
            addRow( emptyLabel, row++, 0.0 );
        }
    }

    private void buildGrid() {
        if ( model.isEmpty() )
            return;
        for ( int column = 0; column < PLAYER_COLUMNS; column++ )
            tablesByColumn.add( new ArrayList< LarvaTableComp >() );
        final int tableRows = model.getPlayers().get( 0 ).getTables().size();
        final JPanel[] headingRow = newGridRow();
        for ( int column = 0; column < PLAYER_COLUMNS; column++ ) {
            final boolean present = column < model.getPlayers().size();
            headingRow[ column ].add( present ? headingLabel( model.getPlayers().get( column ).getHeading() ) : new JPanel(), BorderLayout.WEST );
            columnCells.add( headingRow[ column ] );
        }
        for ( int row = 0; row < tableRows; row++ ) {
            final JPanel[] cells = newGridRow();
            for ( int column = 0; column < PLAYER_COLUMNS; column++ ) {
                if ( column >= model.getPlayers().size() )
                    continue; // empty placeholder cell, keeps the width of the left one (OQ-1)
                final LarvaTableComp table = new LarvaTableComp( model.getPlayers().get( column ).getTables().get( row ) );
                tablesByColumn.get( column ).add( table );
                cells[ column ].add( table, BorderLayout.WEST );
            }
        }
    }

    /** Adds a grid row (two equal cells) to the page and returns its two cell panels. */
    private JPanel[] newGridRow() {
        final JPanel row = new JPanel( new GridLayout( 1, PLAYER_COLUMNS, COLUMN_GAP, 0 ) );
        final JPanel[] cells = new JPanel[ PLAYER_COLUMNS ];
        for ( int column = 0; column < PLAYER_COLUMNS; column++ ) {
            cells[ column ] = new JPanel( new BorderLayout() );
            row.add( cells[ column ] );
        }
        final GridBagConstraints c = rowConstraints( getComponentCount(), 0.0 );
        c.insets = new Insets( 0, 0, TABLE_GAP, 0 );
        add( row, c );
        return cells;
    }

    private static JLabel headingLabel( final String text ) {
        final JLabel heading = new JLabel( text, SwingConstants.LEFT );
        heading.setFont( heading.getFont().deriveFont( Font.BOLD, heading.getFont().getSize2D() * HEADING_FONT_SCALE ) );
        return heading;
    }

    private void buildSkipped() {
        final int count = model.getSkippedLines().size();
        skippedToggle.setText( collapsedText( count ) );
        skippedToggle.setEnabled( count > 0 );
        skippedToggle.setHorizontalAlignment( SwingConstants.LEFT );
        skippedToggle.addActionListener( new ActionListener() {
            @Override
            public void actionPerformed( final ActionEvent e ) {
                toggleSkipped();
            }
        } );
        final StringBuilder lines = new StringBuilder();
        for ( final String line : model.getSkippedLines() )
            lines.append( line ).append( '\n' );
        skippedList.setText( lines.toString() );
        skippedList.setEditable( false );
        skippedList.setOpaque( false );
        skippedList.setVisible( false );
        final int row = getComponentCount();
        final GridBagConstraints toggle = rowConstraints( row, 0.0 );
        toggle.fill = GridBagConstraints.NONE;
        toggle.anchor = GridBagConstraints.WEST;
        add( skippedToggle, toggle );
        final GridBagConstraints list = rowConstraints( row + 1, 1.0 );
        list.anchor = GridBagConstraints.NORTHWEST;
        add( skippedList, list );
    }

    private void addRow( final JComponent comp, final int row, final double weightY ) {
        add( comp, rowConstraints( row, weightY ) );
    }

    private static GridBagConstraints rowConstraints( final int row, final double weightY ) {
        final GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = row;
        c.weightx = 1.0;
        c.weighty = weightY;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.insets = new Insets( 0, 0, PAGE_GAP, 0 );
        return c;
    }

    private String collapsedText( final int count ) {
        return COLLAPSED_MARK + "Skipped replays (" + count + ")";
    }

    /** Expands or collapses the skipped list (the toggle button's action). */
    void toggleSkipped() {
        final boolean expand = !skippedList.isVisible();
        final int count = model.getSkippedLines().size();
        skippedList.setVisible( expand );
        skippedToggle.setText( expand ? EXPANDED_MARK + "Skipped replays (" + count + ")" : collapsedText( count ) );
        revalidate();
        repaint();
    }

    boolean isSkippedExpanded() {
        return skippedList.isVisible();
    }

    String getSkippedToggleText() {
        return skippedToggle.getText();
    }

    String getSkippedListText() {
        return skippedList.getText();
    }

    String getHeaderText() {
        return model.getHeader() == null ? "" : headerLabel.getText();
    }

    String getSummaryText() {
        return summaryLabel.getText();
    }

    String getHelpText() {
        return model.getHelpLine() == null ? "" : helpLabel.getText();
    }

    String getEmptyText() {
        return model.getEmptyMessage() == null ? "" : emptyLabel.getText();
    }

    LarvaMultiAnalysisViewModel getModel() {
        return model;
    }

    /** @return the number of columns shown (0 in the empty state, else 2) */
    int getColumnCount() {
        return columnCells.size();
    }

    /** @return bounds of the heading cell of a column, relative to this panel; its x and width are those of the whole column */
    Rectangle getColumnBounds( final int column ) {
        return boundsInPanel( columnCells.get( column ) );
    }

    /** @return the tables of one column, top to bottom */
    List< LarvaTableComp > getTables( final int column ) {
        return tablesByColumn.get( column );
    }

    /**
     * @param table one of this panel's tables
     * @return true if the table reaches beyond the grid cell it sits in, so part of it is not visible
     */
    static boolean isClipped( final LarvaTableComp table ) {
        final java.awt.Container cell = table.getParent();
        return cell != null && ( table.getX() + table.getWidth() > cell.getWidth() || table.getY() + table.getHeight() > cell.getHeight() );
    }

    /** @return bounds of a component relative to this panel */
    Rectangle boundsInPanel( final Component comp ) {
        final Rectangle r = new Rectangle( comp.getBounds() );
        Container parent = comp.getParent();
        while ( parent != null && parent != this ) {
            r.translate( parent.getX(), parent.getY() );
            parent = parent.getParent();
        }
        return r;
    }
}
