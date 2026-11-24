package hu.aleks.larvasecondslostextmod;

import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;

import javax.swing.JComponent;

/** Swing wrapper around {@link LarvaTablePainter} for use in layouts (multi-analysis page). */
@SuppressWarnings("serial")
final class LarvaTableComp extends JComponent {

    private final LarvaTablePainter painter = new LarvaTablePainter();

    private LarvaTableViewModel model;

    /** Creates a table in the single-replay look (CR-1.2: the only look). */
    LarvaTableComp( final LarvaTableViewModel model ) {
        setOpaque( false );
        this.model = model;
    }

    /** Replaces the shown model (EDT only). */
    void setModel( final LarvaTableViewModel model ) {
        this.model = model;
        revalidate();
        repaint();
    }

    LarvaTableViewModel getModel() {
        return model;
    }

    @Override
    public Dimension getPreferredSize() {
        if ( isPreferredSizeSet() )
            return super.getPreferredSize();

        final Insets insets = getInsets();
        final FontMetrics fm = getFontMetrics( getFont() );
        final Dimension size = model == null ? new Dimension() : painter.measure( fm, model );
        return new Dimension( size.width + insets.left + insets.right, size.height + insets.top + insets.bottom );
    }

    @Override
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    @Override
    protected void paintComponent( final Graphics g_ ) {
        super.paintComponent( g_ );
        if ( model == null )
            return;

        final Graphics2D g = (Graphics2D) g_.create();
        try {
            g.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );
            g.setFont( getFont() );
            final Insets insets = getInsets();
            painter.paint( g, g.getFontMetrics(), model, insets.left, insets.top, getWidth() - insets.left - insets.right - 1 );
        }
        finally {
            g.dispose();
        }
    }
}
