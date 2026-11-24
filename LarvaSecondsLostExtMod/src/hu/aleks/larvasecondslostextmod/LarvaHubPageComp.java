package hu.aleks.larvasecondslostextmod;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;

/**
 * Content of the "Larva" hub: three centered, vertically stacked actions and the "Ladder games only" checkbox.
 *
 * <p>Deliberately passive (AC-1.4): no page-selected or hierarchy listener, so showing the hub never starts an
 * analysis. The checkbox state is not persisted (OQ-8); it lives in this instance, which Scelight caches, so it
 * survives navigation and resets on restart.</p>
 *
 * <p>Must be created and used on the EDT. Icons are passed in as components so the class is testable without Scelight.</p>
 */
@SuppressWarnings( "serial" )
final class LarvaHubPageComp extends JPanel {

    static final String OPEN_REPLAY_LABEL = "Open Replay...";
    static final String ANALYZE_LATEST_LABEL = "Analyze Latest Replay";
    static final String MULTI_ANALYSIS_LABEL = "Multi Analysis...";
    static final String LADDER_ONLY_LABEL = "Ladder games only";

    /** FR-1 tip text; must match {@code help/larva-multi-tip.html}. */
    static final String MULTI_TIP_TEXT = "Select multiple replays; you will receive aggregated larva stats for the most common Zerg player(s) across them.";

    private static final int BUTTON_GAP = 10;
    private static final int PAGE_PADDING = 12;

    private final LarvaHubActions actions;
    private final JComponent tipIcon;
    private final JButton openButton = new JButton( OPEN_REPLAY_LABEL );
    private final JButton latestButton = new JButton( ANALYZE_LATEST_LABEL );
    private final JButton multiButton = new JButton( MULTI_ANALYSIS_LABEL );
    private final JCheckBox ladderOnlyBox = new JCheckBox( LADDER_ONLY_LABEL, true );

    /** Mirror of the checkbox so non-EDT readers (S14) get a safe value. */
    private volatile boolean ladderOnly = true;

    /**
     * @param actions the actions behind the buttons
     * @param tipIcon the "?" tip icon shown left of the multi analysis button
     * @param helpIcon the module help icon shown in the top-right corner; may be <code>null</code> (tests)
     */
    LarvaHubPageComp( final LarvaHubActions actions, final JComponent tipIcon, final JComponent helpIcon ) {
        super( new BorderLayout() );
        this.actions = actions;
        this.tipIcon = tipIcon;
        setBorder( BorderFactory.createEmptyBorder( PAGE_PADDING, PAGE_PADDING, PAGE_PADDING, PAGE_PADDING ) );
        wireListeners();
        equalizeButtonWidths();
        if ( helpIcon != null ) {
            final JPanel header = new JPanel( new BorderLayout() );
            header.add( helpIcon, BorderLayout.EAST );
            add( header, BorderLayout.NORTH );
        }
        add( buildCenteredStack(), BorderLayout.CENTER );
    }

    private void wireListeners() {
        openButton.addActionListener( new ActionListener() {
            @Override
            public void actionPerformed( final ActionEvent e ) {
                actions.openReplay( LarvaHubPageComp.this );
            }
        } );
        latestButton.addActionListener( new ActionListener() {
            @Override
            public void actionPerformed( final ActionEvent e ) {
                actions.analyzeLatestReplay();
            }
        } );
        multiButton.addActionListener( new ActionListener() {
            @Override
            public void actionPerformed( final ActionEvent e ) {
                actions.startMultiAnalysis( LarvaHubPageComp.this, ladderOnlyBox.isSelected() );
            }
        } );
        ladderOnlyBox.addItemListener( new ItemListener() {
            @Override
            public void itemStateChanged( final ItemEvent e ) {
                ladderOnly = ladderOnlyBox.isSelected();
            }
        } );
    }

    /** One preferred width (the widest label) for all three buttons. */
    private void equalizeButtonWidths() {
        int width = 0;
        for ( final JButton b : new JButton[] { openButton, latestButton, multiButton } )
            width = Math.max( width, b.getPreferredSize().width );
        for ( final JButton b : new JButton[] { openButton, latestButton, multiButton } )
            b.setPreferredSize( new Dimension( width, b.getPreferredSize().height ) );
    }

    /**
     * Three columns: [tip side][button][checkbox side]. Both side columns get the same width so the button column
     * stays exactly centered whatever the tip icon and checkbox widths are.
     */
    private JComponent buildCenteredStack() {
        final int sideWidth = Math.max( tipIcon.getPreferredSize().width, ladderOnlyBox.getPreferredSize().width ) + BUTTON_GAP;
        final JPanel stack = new JPanel( new GridBagLayout() );
        addRow( stack, 0, null, openButton, null, sideWidth );
        addRow( stack, 1, null, latestButton, null, sideWidth );
        addRow( stack, 2, tipIcon, multiButton, ladderOnlyBox, sideWidth );

        // GridBagLayout with no weights centers the whole block in the outer panel.
        final JPanel outer = new JPanel( new GridBagLayout() );
        outer.add( stack );
        return outer;
    }

    private void addRow( final JPanel stack, final int row, final JComponent left, final JComponent middle, final JComponent right, final int sideWidth ) {
        final Insets rowInsets = new Insets( BUTTON_GAP / 2, 0, BUTTON_GAP / 2, 0 );
        stack.add( sidePanel( left, true, sideWidth ), constraints( 0, row, rowInsets ) );
        stack.add( middle, constraints( 1, row, rowInsets ) );
        stack.add( sidePanel( right, false, sideWidth ), constraints( 2, row, rowInsets ) );
    }

    private static GridBagConstraints constraints( final int column, final int row, final Insets insets ) {
        final GridBagConstraints c = new GridBagConstraints();
        c.gridx = column;
        c.gridy = row;
        c.insets = insets;
        c.fill = GridBagConstraints.NONE;
        return c;
    }

    /** Fixed-width spacer holding an optional component, hugging the button side. */
    private static JPanel sidePanel( final JComponent content, final boolean leftSide, final int width ) {
        final JPanel panel = new JPanel( new BorderLayout( BUTTON_GAP, 0 ) );
        panel.setOpaque( false );
        if ( content != null )
            panel.add( content, leftSide ? BorderLayout.EAST : BorderLayout.WEST );
        panel.setPreferredSize( new Dimension( width, Math.max( 1, content == null ? 1 : content.getPreferredSize().height ) ) );
        return panel;
    }

    /** @return the current state of the "Ladder games only" checkbox (any thread) */
    boolean isLadderOnly() {
        return ladderOnly;
    }

    /** Sets the checkbox like a user click would (EDT). */
    void setLadderOnly( final boolean value ) {
        ladderOnlyBox.setSelected( value );
    }

    /** @return the labels of the three action buttons, top to bottom */
    String[] getButtonLabels() {
        return new String[] { openButton.getText(), latestButton.getText(), multiButton.getText() };
    }

    JButton getOpenButton() {
        return openButton;
    }

    JButton getLatestButton() {
        return latestButton;
    }

    JButton getMultiButton() {
        return multiButton;
    }

    /** @return the tip icon component */
    Component getTipIcon() {
        return tipIcon;
    }

    /** @return the tooltip of the tip icon (may be <code>null</code>) */
    String getTipToolTip() {
        return tipIcon.getToolTipText();
    }

}
