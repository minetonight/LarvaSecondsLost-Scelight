package hu.aleks.larvasecondslostextmod;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

/**
 * Simple chart-like visualization used on the supported module-owned Larva page.
 *
 * <p>This renders a normalized replay timeline model so replay-analysis code can evolve without
 * pushing chart logic down into Swing painting.</p>
 */
@SuppressWarnings("serial")
public class LarvaTimelinePreviewComp extends JPanel {

    /** Background rail color. */
    private static final Color RAIL_COLOR = new Color( 214, 219, 226 );

    /** Preview window color. */
    private static final Color WINDOW_COLOR = new Color( 220, 70, 70 );

    /** Placeholder interval color. */
    private static final Color PREVIEW_INTERVAL_COLOR = new Color( 239, 170, 170 );

    /** Outline color. */
    private static final Color OUTLINE_COLOR = new Color( 90, 90, 90 );

    /** Secondary text color. */
    private static final Color SUBTLE_TEXT_COLOR = new Color( 92, 104, 117 );

    /** Player group text color. */
    private static final Color GROUP_TEXT_COLOR = new Color( 54, 72, 92 );

    /** Overview text color. */
    private static final Color OVERVIEW_TEXT_COLOR = new Color( 36, 48, 64 );

    /** Marker color. */
    private static final Color MARKER_COLOR = new Color( 246, 156, 63 );

    /** Supported hatchery lifetime rail color. */
    private static final Color LIFETIME_RAIL_COLOR = new Color( 194, 201, 210 );

    /** Missed-larva marker color. */
    private static final Color MISSED_LARVA_MARKER_COLOR = Color.BLACK;

    /** Left padding. */
    private static final int LEFT_PAD = 12;

    /** Right padding. */
    private static final int RIGHT_PAD = 12;

    /** Top padding. */
    private static final int TOP_PAD = 12;

    /** Space reserved for row labels. */
    private static final int LABEL_WIDTH = 168;

    /** Height of a timeline rail. */
    private static final int RAIL_HEIGHT = 16;

    /** Width of a missed-larva threshold marker. */
    private static final int MARKER_WIDTH = 4;

    /** Vertical step between rows. */
    private static final int ROW_STEP = 34;

    /** Extra gap inserted when a new player group begins. */
    private static final int GROUP_GAP = 16;

    /** Current normalized timeline model to render. */
    private LarvaTimelineModel timelineModel;

    /**
     * Creates a new timeline preview component.
     */
    public LarvaTimelinePreviewComp() {
        setOpaque( true );
        setBackground( Color.WHITE );
        setBorder( BorderFactory.createCompoundBorder( BorderFactory.createLineBorder( new Color( 212, 212, 212 ) ),
                BorderFactory.createEmptyBorder( 8, 8, 8, 8 ) ) );
        setPreferredSize( new Dimension( 220, 150 ) );
    }

    /**
     * Updates the normalized timeline model shown in the preview.
     *
     * @param timelineModel timeline model to render; may be <code>null</code>
     */
    public void setTimelineModel( final LarvaTimelineModel timelineModel ) {
        this.timelineModel = timelineModel;
        updatePreferredHeight();
        revalidate();
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
        g.drawString( timelineModel == null ? "Larva timeline" : timelineModel.getTitle(), LEFT_PAD, TOP_PAD + 4 );
        g.setColor( SUBTLE_TEXT_COLOR );
        g.drawString( timelineModel == null ? "Module-owned supported visualization; load a replay to populate it."
                : timelineModel.getSubtitle(), LEFT_PAD, TOP_PAD + 22 );

        if ( timelineModel == null || timelineModel.getReplayLengthMs() <= 0L ) {
            g.drawString( "Load a replay to render replay-derived hatchery rows.", LEFT_PAD, TOP_PAD + 48 );
            return;
        }

        final int headerBottomY = TOP_PAD + 40;
        g.setColor( SUBTLE_TEXT_COLOR );
        g.drawString( timelineModel.getModeLabel(), LEFT_PAD, headerBottomY );

        final List< LarvaTimelineRow > rowList = timelineModel.getRowList();
        if ( rowList.isEmpty() ) {
            g.drawString( timelineModel.getEmptyMessage(), LEFT_PAD, headerBottomY + 24 );
            return;
        }

        final int railLeft = LEFT_PAD + LABEL_WIDTH;
        final int railWidth = Math.max( 80, width - railLeft - RIGHT_PAD );
        int y = headerBottomY + 22;
        String previousGroup = null;
        final Map< String, String > groupOverviewLabelMap = timelineModel.getGroupOverviewLabelMap();

        for ( final LarvaTimelineRow row : rowList ) {
            final String groupLabel = row.getGroupLabel();
            if ( groupLabel != null && groupLabel.length() > 0 && !groupLabel.equals( previousGroup ) ) {
                if ( previousGroup != null )
                    y += GROUP_GAP;
                g.setColor( GROUP_TEXT_COLOR );
                g.drawString( groupLabel, LEFT_PAD, y );
                y += 14;
                final String groupOverviewLabel = groupOverviewLabelMap.get( groupLabel );
                if ( groupOverviewLabel != null && groupOverviewLabel.length() > 0 ) {
                    g.setColor( OVERVIEW_TEXT_COLOR );
                    g.drawString( groupOverviewLabel, LEFT_PAD, y );
                    y += 14;
                }
                previousGroup = groupLabel;
            }

            drawRow( g, fm, row, railLeft, y, railWidth );
            y += ROW_STEP;
        }

        final int axisY = Math.min( height - 24, y + 4 );
        drawAxis( g, fm, railLeft, railWidth, axisY );
    }

    /**
     * Draws one normalized timeline row.
     *
     * @param g graphics context
     * @param fm font metrics
     * @param row row to draw
     * @param railLeft rail left position
     * @param y top y position of the row
     * @param railWidth rail width
     */
    private void drawRow( final Graphics2D g, final FontMetrics fm, final LarvaTimelineRow row, final int railLeft, final int y, final int railWidth ) {
        g.setColor( OUTLINE_COLOR );
        g.drawString( row.getRowLabel(), LEFT_PAD, y + 2 );
        g.setColor( SUBTLE_TEXT_COLOR );
        g.drawString( row.getDetailLabel(), LEFT_PAD, y + 16 );

        final int railY = y + 2;
        final int rowStartX = railLeft + scaleToWidth( row.getStartMs(), timelineModel.getReplayLengthMs(), railWidth );
        final int rowEndX = railLeft + scaleToWidth( row.getEndMs(), timelineModel.getReplayLengthMs(), railWidth );
        final int lifetimeWidth = Math.max( 6, rowEndX - rowStartX );

        g.setColor( RAIL_COLOR );
        g.fillRoundRect( rowStartX, railY, lifetimeWidth, RAIL_HEIGHT, 10, 10 );
        g.setColor( LIFETIME_RAIL_COLOR );
        g.fillRoundRect( rowStartX + 1, railY + 1, Math.max( 4, lifetimeWidth - 2 ), Math.max( 4, RAIL_HEIGHT - 2 ), 9, 9 );
        g.setColor( OUTLINE_COLOR );
        g.drawRoundRect( rowStartX, railY, lifetimeWidth, RAIL_HEIGHT, 10, 10 );

        for ( final LarvaTimelineSegment segment : row.getSegmentList() )
            drawSegment( g, fm, segment, railLeft, railY, railWidth );

        for ( final LarvaTimelineMarker marker : row.getMarkerList() )
            drawMarker( g, marker, railLeft, railY, railWidth );
    }

    /**
     * Draws one semantic segment onto a timeline rail.
     *
     * @param g graphics context
     * @param fm font metrics
     * @param segment segment to draw
     * @param railLeft rail left position
     * @param railY rail top position
     * @param railWidth rail width
     */
    private void drawSegment( final Graphics2D g, final FontMetrics fm, final LarvaTimelineSegment segment, final int railLeft, final int railY,
            final int railWidth ) {
        final int startX = railLeft + scaleToWidth( segment.getStartMs(), timelineModel.getReplayLengthMs(), railWidth );
        final int endX = railLeft + scaleToWidth( segment.getEndMs(), timelineModel.getReplayLengthMs(), railWidth );
        final boolean marker = segment.getKind() == LarvaTimelineSegment.Kind.PREVIEW_MARKER;
        final int segmentWidth = Math.max( marker ? 4 : 8, endX - startX );
        final Color segmentColor = marker ? MARKER_COLOR
            : segment.getKind() == LarvaTimelineSegment.Kind.SATURATION_WINDOW ? WINDOW_COLOR : PREVIEW_INTERVAL_COLOR;

        g.setColor( segmentColor );
        g.fillRoundRect( startX, railY + 2, segmentWidth, RAIL_HEIGHT - 4, 8, 8 );
        g.setColor( OUTLINE_COLOR );
        g.drawRoundRect( startX, railY + 2, segmentWidth, RAIL_HEIGHT - 4, 8, 8 );

        if ( !marker && segmentWidth >= 36 ) {
            final int labelX = Math.max( railLeft, Math.min( startX, railLeft + railWidth - fm.stringWidth( segment.getLabel() ) ) );
            g.setColor( SUBTLE_TEXT_COLOR );
            g.drawString( segment.getLabel(), labelX, railY - 4 );
        }
    }

    /**
     * Draws one semantic threshold marker onto a timeline rail.
     *
     * @param g graphics context
     * @param marker marker to draw
     * @param railLeft rail left position
     * @param railY rail top position
     * @param railWidth rail width
     */
    private void drawMarker( final Graphics2D g, final LarvaTimelineMarker marker, final int railLeft, final int railY, final int railWidth ) {
        final int centerX = railLeft + scaleToWidth( marker.getTimeMs(), timelineModel.getReplayLengthMs(), railWidth );
        final int markerX = centerX - MARKER_WIDTH / 2;

        g.setColor( MISSED_LARVA_MARKER_COLOR );
        g.fillRoundRect( markerX, railY - 1, MARKER_WIDTH, RAIL_HEIGHT + 2, 3, 3 );
    }

    /**
     * Draws the shared time axis.
     *
     * @param g graphics context
     * @param fm font metrics
     * @param railLeft rail left position
     * @param railWidth rail width
     * @param axisY y coordinate of the axis baseline
     */
    private void drawAxis( final Graphics2D g, final FontMetrics fm, final int railLeft, final int railWidth, final int axisY ) {
        g.setColor( OUTLINE_COLOR );
        g.setStroke( new BasicStroke( 1f ) );
        g.drawLine( railLeft, axisY, railLeft + railWidth, axisY );
        g.drawLine( railLeft, axisY - 4, railLeft, axisY + 4 );
        g.drawLine( railLeft + railWidth, axisY - 4, railLeft + railWidth, axisY + 4 );

        final String startLabel = "0:00";
        final String endLabel = timelineModel.getReplayLengthLabel();
        g.drawString( startLabel, railLeft, axisY + 16 );
        g.drawString( endLabel, railLeft + railWidth - fm.stringWidth( endLabel ), axisY + 16 );
        g.setColor( SUBTLE_TEXT_COLOR );
        g.drawString( "Supported module-owned larva timeline", LEFT_PAD, axisY + 16 );
    }

    /**
     * Updates preferred height based on the number of visible rows.
     */
    private void updatePreferredHeight() {
        if ( timelineModel == null || timelineModel.getRowList().isEmpty() ) {
            setPreferredSize( new Dimension( 220, 150 ) );
            return;
        }

        int groupCount = 0;
        String previousGroup = null;
        for ( final LarvaTimelineRow row : timelineModel.getRowList() ) {
            if ( row.getGroupLabel() != null && row.getGroupLabel().length() > 0 && !row.getGroupLabel().equals( previousGroup ) ) {
                groupCount++;
                previousGroup = row.getGroupLabel();
            }
        }

        int groupOverviewCount = 0;
        for ( final String label : timelineModel.getGroupOverviewLabelMap().values() ) {
            if ( label != null && label.length() > 0 )
                groupOverviewCount++;
        }

        final int preferredHeight = 118 + timelineModel.getRowList().size() * ROW_STEP + Math.max( 0, groupCount - 1 ) * GROUP_GAP + groupCount * 14
            + groupOverviewCount * 14;
        setPreferredSize( new Dimension( 220, preferredHeight ) );
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

}