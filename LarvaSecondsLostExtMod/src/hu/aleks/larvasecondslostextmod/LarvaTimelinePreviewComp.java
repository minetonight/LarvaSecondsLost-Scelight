package hu.aleks.larvasecondslostextmod;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

/**
 * Simple chart-like preview used for Epic 03.
 *
 * <p>This is intentionally a module-owned fallback visualization. It renders one replay-derived
 * placeholder interval so the later larva windows can replace it without changing the page layout.</p>
 */
@SuppressWarnings("serial")
public class LarvaTimelinePreviewComp extends JPanel {

    /** Background rail color. */
    private static final Color RAIL_COLOR = new Color( 214, 219, 226 );

    /** Preview window color. */
    private static final Color WINDOW_COLOR = new Color( 220, 70, 70 );

    /** Outline color. */
    private static final Color OUTLINE_COLOR = new Color( 90, 90, 90 );

    /** Current summary to render. */
    private ReplaySummary summary;

    /**
     * Creates a new timeline preview component.
     */
    public LarvaTimelinePreviewComp() {
        setOpaque( true );
        setBackground( Color.WHITE );
        setBorder( BorderFactory.createCompoundBorder( BorderFactory.createLineBorder( new Color( 212, 212, 212 ) ),
                BorderFactory.createEmptyBorder( 8, 8, 8, 8 ) ) );
        setPreferredSize( new Dimension( 220, 110 ) );
    }

    /**
     * Updates the summary shown in the preview.
     *
     * @param summary replay summary to render; may be <code>null</code>
     */
    public void setSummary( final ReplaySummary summary ) {
        this.summary = summary;
        repaint();
    }

    @Override
    protected void paintComponent( final Graphics g_ ) {
        super.paintComponent( g_ );

        final Graphics2D g = (Graphics2D) g_;
        g.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );

        final int width = getWidth();
        final int height = getHeight();
        final FontMetrics fm = g.getFontMetrics();

        g.setColor( OUTLINE_COLOR );
        g.drawString( "Epic 03 preview timeline", 10, 16 );

        if ( summary == null || summary.getLengthMs() <= 0L ) {
            g.drawString( "Load a replay to render a placeholder time window.", 10, 40 );
            return;
        }

        final int railLeft = 12;
        final int railTop = 46;
        final int railWidth = Math.max( 60, width - 24 );
        final int railHeight = 20;

        g.setColor( RAIL_COLOR );
        g.fillRoundRect( railLeft, railTop, railWidth, railHeight, 12, 12 );
        g.setColor( OUTLINE_COLOR );
        g.drawRoundRect( railLeft, railTop, railWidth, railHeight, 12, 12 );

        final int startX = railLeft + scaleToWidth( summary.getPreviewWindowStartMs(), summary.getLengthMs(), railWidth );
        final int endX = railLeft + scaleToWidth( summary.getPreviewWindowEndMs(), summary.getLengthMs(), railWidth );
        final int windowWidth = Math.max( 8, endX - startX );

        g.setColor( WINDOW_COLOR );
        g.fillRoundRect( startX, railTop + 2, windowWidth, railHeight - 4, 10, 10 );
        g.setColor( OUTLINE_COLOR );
        g.drawRoundRect( startX, railTop + 2, windowWidth, railHeight - 4, 10, 10 );

        g.setStroke( new BasicStroke( 1f ) );
        g.drawLine( railLeft, railTop + railHeight + 10, railLeft + railWidth, railTop + railHeight + 10 );
        g.drawLine( railLeft, railTop + railHeight + 6, railLeft, railTop + railHeight + 14 );
        g.drawLine( railLeft + railWidth, railTop + railHeight + 6, railLeft + railWidth, railTop + railHeight + 14 );

        final String startLabel = "0:00";
        final String endLabel = summary.getLength();
        g.drawString( startLabel, railLeft, height - 12 );
        g.drawString( endLabel, railLeft + railWidth - fm.stringWidth( endLabel ), height - 12 );

        final String windowLabel = "Preview window: " + formatMs( summary.getPreviewWindowStartMs() ) + " - " + formatMs( summary.getPreviewWindowEndMs() );
        g.drawString( windowLabel, 10, 88 );
    }

    /**
     * Scales a replay time to the width of the preview rail.
     *
     * @param value time value to scale
     * @param maxValue total replay length
     * @param width target width
     * @return scaled x offset
     */
    private int scaleToWidth( final long value, final long maxValue, final int width ) {
        if ( maxValue <= 0L )
            return 0;
        return (int) ( ( value * width ) / maxValue );
    }

    /**
     * Formats milliseconds as a short $m:ss$ string.
     *
     * @param ms time in milliseconds
     * @return formatted time string
     */
    private String formatMs( final long ms ) {
        final long totalSeconds = ms / 1000L;
        final long minutes = totalSeconds / 60L;
        final long seconds = totalSeconds % 60L;
        return minutes + ":" + ( seconds < 10L ? "0" : "" ) + seconds;
    }

}