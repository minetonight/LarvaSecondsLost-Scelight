package hu.aleks.larvasecondslostextmod;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
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

    /** Inject-uptime lane color. */
    private static final Color INJECT_WINDOW_COLOR = new Color( 67, 160, 71 );

    /** Idle-inject lane color. */
    private static final Color IDLE_INJECT_WINDOW_COLOR = new Color( 196, 40, 40 );

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

    /** Subtle gray used for larva count dots and accumulation labels. */
    private static final Color RHYTHM_HINT_COLOR = new Color( 132, 140, 148 );

    /** Left padding. */
    private static final int LEFT_PAD = 12;

    /** Right padding. */
    private static final int RIGHT_PAD = 12;

    /** Top padding. */
    private static final int TOP_PAD = 12;

    /** Space reserved for row labels. */
    private static final int LABEL_WIDTH = 280;

    /** Height of a timeline rail. */
    private static final int RAIL_HEIGHT = 16;

    /** Height of the dedicated inject lane. */
    private static final int INJECT_LANE_HEIGHT = 6;

    /** Vertical gap between the main rail and the inject lane. */
    private static final int INJECT_LANE_GAP = 4;

    /** Height of the dedicated idle-inject lane. */
    private static final int IDLE_INJECT_LANE_HEIGHT = 6;

    /** Vertical gap between the inject lane and the idle-inject lane. */
    private static final int IDLE_INJECT_LANE_GAP = 3;

    /** Width of a missed-larva threshold marker. */
    private static final int MARKER_WIDTH = 4;

    /** Vertical step between rows. */
    private static final int ROW_STEP = 52;

    /** Extra gap inserted when a new player group begins. */
    private static final int GROUP_GAP = 16;

    /** Space reserved below the axis for labels and legend. */
    private static final int AXIS_BOTTOM_PAD = 68;

    /** Size of one small larva rhythm dot. */
    private static final int RHYTHM_DOT_SIZE = 4;

    /** Minimum horizontal spacing between accumulation labels to avoid clutter. */
    private static final int MIN_ACCUMULATION_LABEL_GAP = 16;

    /** Horizontal padding between lane labels and the timeline rail. */
    private static final int STATUS_LABEL_GAP = 8;

    /** Current normalized timeline model to render. */
    private LarvaTimelineModel timelineModel;

    /** Tooltip hotspots rebuilt on every paint pass. */
    private final List< TooltipHotspot > tooltipHotspotList = new ArrayList<>();

    /**
     * Creates a new timeline preview component.
     */
    public LarvaTimelinePreviewComp() {
        setOpaque( true );
        setBackground( Color.WHITE );
        setBorder( BorderFactory.createCompoundBorder( BorderFactory.createLineBorder( new Color( 212, 212, 212 ) ),
                BorderFactory.createEmptyBorder( 8, 8, 8, 8 ) ) );
        setPreferredSize( new Dimension( 220, 150 ) );
        setToolTipText( "" );
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

        tooltipHotspotList.clear();

        final Graphics2D g = (Graphics2D) g_;
        g.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );

        final int width = getWidth();
        final int height = getHeight();
        final FontMetrics fm = g.getFontMetrics();

        g.setColor( OUTLINE_COLOR );
        g.drawString( timelineModel == null ? "Larva timeline" : timelineModel.getTitle(), LEFT_PAD, TOP_PAD + 4 );
        g.setColor( SUBTLE_TEXT_COLOR );
        g.drawString( timelineModel == null ? "Load a replay to see 3+ larva windows, inject uptime, conservative idle inject windows, and missed larva pressure."
                : timelineModel.getSubtitle(), LEFT_PAD, TOP_PAD + 22 );

        if ( timelineModel == null || timelineModel.getReplayLengthMs() <= 0L ) {
            g.drawString( "Choose a replay to fill the timeline.", LEFT_PAD, TOP_PAD + 48 );
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
        final Map< String, Color > groupColorMap = timelineModel.getGroupColorMap();

        for ( final LarvaTimelineRow row : rowList ) {
            final String groupLabel = row.getGroupLabel();
            if ( groupLabel != null && groupLabel.length() > 0 && !groupLabel.equals( previousGroup ) ) {
                if ( previousGroup != null )
                    y += GROUP_GAP;
                g.setColor( resolveGroupColor( groupLabel, groupColorMap ) );
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

        final int axisY = Math.min( height - AXIS_BOTTOM_PAD, y + 4 );
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
        if ( row.getSecondaryDetailLabel() != null && row.getSecondaryDetailLabel().length() > 0 )
            g.drawString( row.getSecondaryDetailLabel(), LEFT_PAD, y + 30 );

        final int railY = y + 2;
        final int rowStartX = railLeft + scaleToWidth( row.getStartMs(), timelineModel.getReplayLengthMs(), railWidth );
        final int rowEndX = railLeft + scaleToWidth( row.getEndMs(), timelineModel.getReplayLengthMs(), railWidth );
        final int lifetimeWidth = Math.max( 6, rowEndX - rowStartX );
        final int injectLaneY = railY + RAIL_HEIGHT + INJECT_LANE_GAP;
        final int idleInjectLaneY = injectLaneY + INJECT_LANE_HEIGHT + IDLE_INJECT_LANE_GAP;
        final int statusLabelRightX = railLeft - STATUS_LABEL_GAP;

        g.setColor( RAIL_COLOR );
        g.fillRoundRect( rowStartX, railY, lifetimeWidth, RAIL_HEIGHT, 10, 10 );
        g.setColor( LIFETIME_RAIL_COLOR );
        g.fillRoundRect( rowStartX + 1, railY + 1, Math.max( 4, lifetimeWidth - 2 ), Math.max( 4, RAIL_HEIGHT - 2 ), 9, 9 );
        g.setColor( OUTLINE_COLOR );
        g.drawRoundRect( rowStartX, railY, lifetimeWidth, RAIL_HEIGHT, 10, 10 );

        g.setColor( RAIL_COLOR );
        g.fillRoundRect( rowStartX, injectLaneY, lifetimeWidth, INJECT_LANE_HEIGHT, 6, 6 );
        g.setColor( LIFETIME_RAIL_COLOR );
        g.fillRoundRect( rowStartX + 1, injectLaneY + 1, Math.max( 4, lifetimeWidth - 2 ), Math.max( 3, INJECT_LANE_HEIGHT - 2 ), 5, 5 );
        g.setColor( OUTLINE_COLOR );
        g.drawRoundRect( rowStartX, injectLaneY, lifetimeWidth, INJECT_LANE_HEIGHT, 6, 6 );
        g.setColor( INJECT_WINDOW_COLOR );
        drawRightAlignedString( g, fm, "Inject", statusLabelRightX, injectLaneY + INJECT_LANE_HEIGHT );

        g.setColor( RAIL_COLOR );
        g.fillRoundRect( rowStartX, idleInjectLaneY, lifetimeWidth, IDLE_INJECT_LANE_HEIGHT, 6, 6 );
        g.setColor( LIFETIME_RAIL_COLOR );
        g.fillRoundRect( rowStartX + 1, idleInjectLaneY + 1, Math.max( 4, lifetimeWidth - 2 ), Math.max( 3, IDLE_INJECT_LANE_HEIGHT - 2 ), 5, 5 );
        g.setColor( OUTLINE_COLOR );
        g.drawRoundRect( rowStartX, idleInjectLaneY, lifetimeWidth, IDLE_INJECT_LANE_HEIGHT, 6, 6 );
        g.setColor( IDLE_INJECT_WINDOW_COLOR );
        drawRightAlignedString( g, fm, "Missed", statusLabelRightX, idleInjectLaneY + IDLE_INJECT_LANE_HEIGHT );

        for ( final LarvaTimelineSegment segment : row.getSegmentList() )
            drawSegment( g, fm, segment, railLeft, railY, injectLaneY, idleInjectLaneY, railWidth );

        drawDecorations( g, fm, row, railLeft, railY, railWidth );

        for ( final LarvaTimelineMarker marker : row.getMarkerList() )
            drawMarker( g, marker, railLeft, railY, idleInjectLaneY, railWidth );
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
                final int injectLaneY, final int idleInjectLaneY, final int railWidth ) {
        final int startX = railLeft + scaleToWidth( segment.getStartMs(), timelineModel.getReplayLengthMs(), railWidth );
        final int endX = railLeft + scaleToWidth( segment.getEndMs(), timelineModel.getReplayLengthMs(), railWidth );
        final boolean marker = segment.getKind() == LarvaTimelineSegment.Kind.PREVIEW_MARKER;
        final int segmentWidth = Math.max( marker ? 4 : 8, endX - startX );
        final SegmentVisual segmentVisual = resolveSegmentVisual( segment, railY, injectLaneY, idleInjectLaneY, marker );

        g.setColor( segmentVisual.color );
        g.fillRoundRect( startX, segmentVisual.y, segmentWidth, segmentVisual.height, segmentVisual.arc, segmentVisual.arc );
        g.setColor( OUTLINE_COLOR );
        g.drawRoundRect( startX, segmentVisual.y, segmentWidth, segmentVisual.height, segmentVisual.arc, segmentVisual.arc );

        if ( segment.getTooltipText() != null && segment.getTooltipText().length() > 0 )
            tooltipHotspotList.add( new TooltipHotspot( new Rectangle( startX, segmentVisual.y, segmentWidth, segmentVisual.height ), segment.getTooltipText() ) );

    }

    /**
     * Resolves the visual placement and color for a timeline segment.
     *
     * @param segment segment to render
     * @param railY main rail top position
     * @param injectLaneY inject lane top position
     * @param marker tells if the segment is a narrow marker
     * @return visual settings for the segment
     */
    private SegmentVisual resolveSegmentVisual( final LarvaTimelineSegment segment, final int railY, final int injectLaneY,
            final int idleInjectLaneY, final boolean marker ) {
        if ( segment != null && segment.getKind() == LarvaTimelineSegment.Kind.INJECT_WINDOW )
            return new SegmentVisual( injectLaneY + 1, Math.max( 3, INJECT_LANE_HEIGHT - 2 ), 6, INJECT_WINDOW_COLOR );

        if ( segment != null && segment.getKind() == LarvaTimelineSegment.Kind.IDLE_INJECT_WINDOW )
            return new SegmentVisual( idleInjectLaneY + 1, Math.max( 3, IDLE_INJECT_LANE_HEIGHT - 2 ), 6, IDLE_INJECT_WINDOW_COLOR );

        if ( marker )
            return new SegmentVisual( railY + 2, RAIL_HEIGHT - 4, 8, MARKER_COLOR );

        if ( segment != null && segment.getKind() == LarvaTimelineSegment.Kind.SATURATION_WINDOW )
            return new SegmentVisual( railY + 2, RAIL_HEIGHT - 4, 8, WINDOW_COLOR );

        return new SegmentVisual( railY + 2, RAIL_HEIGHT - 4, 8, PREVIEW_INTERVAL_COLOR );
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
    private void drawMarker( final Graphics2D g, final LarvaTimelineMarker marker, final int railLeft, final int railY, final int idleInjectLaneY,
            final int railWidth ) {
        final int centerX = railLeft + scaleToWidth( marker.getTimeMs(), timelineModel.getReplayLengthMs(), railWidth );
        final int markerX = centerX - MARKER_WIDTH / 2;
        final boolean injectLossMarker = marker.getKind() == LarvaTimelineMarker.Kind.MISSED_INJECT_LARVA;
        final int markerY = injectLossMarker ? idleInjectLaneY - 1 : railY - 1;
        final int markerHeight = injectLossMarker ? IDLE_INJECT_LANE_HEIGHT + 2 : RAIL_HEIGHT + 2;

        g.setColor( MISSED_LARVA_MARKER_COLOR );
        g.fillRoundRect( markerX, markerY, MARKER_WIDTH, markerHeight, 3, 3 );

        if ( marker.getTooltipText() != null && marker.getTooltipText().length() > 0 )
            tooltipHotspotList.add( new TooltipHotspot( new Rectangle( markerX - 2, markerY - 1, MARKER_WIDTH + 4, markerHeight + 2 ), marker.getTooltipText() ) );
    }

    /**
     * Draws a right-aligned label.
     *
     * @param g graphics context
     * @param fm font metrics
     * @param text text to draw
     * @param rightX right edge position
     * @param baselineY baseline y position
     */
    private void drawRightAlignedString( final Graphics2D g, final FontMetrics fm, final String text, final int rightX, final int baselineY ) {
        if ( text == null || text.length() == 0 )
            return;

        g.drawString( text, rightX - fm.stringWidth( text ), baselineY );
    }

    /**
     * Draws one small rhythm decoration without adding tooltip hotspots.
     *
     * @param g graphics context
     * @param fm font metrics
     * @param decoration decoration to draw
     * @param railLeft rail left position
     * @param railY rail top position
     * @param railWidth rail width
     */
    private void drawDecoration( final Graphics2D g, final FontMetrics fm, final LarvaTimelineDecoration decoration, final int railLeft, final int railY,
            final int railWidth ) {
        if ( decoration == null )
            return;

        final int centerX = railLeft + scaleToWidth( decoration.getTimeMs(), timelineModel.getReplayLengthMs(), railWidth );
        g.setColor( RHYTHM_HINT_COLOR );

        if ( decoration.getKind() == LarvaTimelineDecoration.Kind.LARVA_DOT_COLUMN ) {
            final int dotCount = Math.max( 1, Math.min( 2, decoration.getLarvaCount() ) );
            final int firstDotY = railY + RAIL_HEIGHT / 2 - ( dotCount == 2 ? 5 : 2 );
            for ( int i = 0; i < dotCount; i++ )
                g.fillOval( centerX - RHYTHM_DOT_SIZE / 2, firstDotY + i * 6, RHYTHM_DOT_SIZE, RHYTHM_DOT_SIZE );
            return;
        }

        if ( decoration.getKind() == LarvaTimelineDecoration.Kind.ACCUMULATION_LABEL && decoration.getLabel() != null ) {
            final int textX = centerX - fm.stringWidth( decoration.getLabel() ) / 2;
            g.drawString( decoration.getLabel(), textX, railY + RAIL_HEIGHT - 3 );
        }
    }

    /**
     * Draws row decorations while thinning accumulation labels so long windows stay readable.
     *
     * @param g graphics context
     * @param fm font metrics
     * @param row row whose decorations should be drawn
     * @param railLeft rail left position
     * @param railY rail top position
     * @param railWidth rail width
     */
    private void drawDecorations( final Graphics2D g, final FontMetrics fm, final LarvaTimelineRow row, final int railLeft, final int railY,
            final int railWidth ) {
        if ( row == null || row.getDecorationList().isEmpty() )
            return;

        int lastAccumulationLabelRight = Integer.MIN_VALUE;
        for ( final LarvaTimelineDecoration decoration : row.getDecorationList() ) {
            if ( decoration == null )
                continue;

            if ( decoration.getKind() != LarvaTimelineDecoration.Kind.ACCUMULATION_LABEL ) {
                drawDecoration( g, fm, decoration, railLeft, railY, railWidth );
                continue;
            }

            final String label = decoration.getLabel();
            if ( label == null || label.length() == 0 )
                continue;

            final int centerX = railLeft + scaleToWidth( decoration.getTimeMs(), timelineModel.getReplayLengthMs(), railWidth );
            final int textWidth = fm.stringWidth( label );
            final int textX = centerX - textWidth / 2;
            if ( textX <= lastAccumulationLabelRight + MIN_ACCUMULATION_LABEL_GAP )
                continue;

            drawDecoration( g, fm, decoration, railLeft, railY, railWidth );
            lastAccumulationLabelRight = textX + textWidth;
        }
    }

    @Override
    public String getToolTipText( final MouseEvent event ) {
        if ( event == null )
            return null;

        for ( int i = tooltipHotspotList.size() - 1; i >= 0; i-- ) {
            final TooltipHotspot hotspot = tooltipHotspotList.get( i );
            if ( hotspot.bounds.contains( event.getPoint() ) )
                return hotspot.tooltipText;
        }

        return null;
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
        g.drawString( "Legend: gray dots = 1-2 larva waiting; red bars = 3+ larva unspent; green lanes = inject uptime; dark red lanes = missed inject windows", LEFT_PAD, axisY + 32 );
        g.drawString( "Black ticks = 11s missed larva on the main rail, and 29s missed inject thresholds on the dark red lane (3 larva each)", LEFT_PAD, axisY + 46 );
        g.drawString( "Gray 6/9/12... labels mark moments when a hatchery reaches those idle larva counts", LEFT_PAD, axisY + 60 );
    }

    /**
     * Resolves the color used for a player header.
     *
     * @param groupLabel player name
     * @param groupColorMap available player colors
     * @return header color
     */
    private Color resolveGroupColor( final String groupLabel, final Map< String, Color > groupColorMap ) {
        if ( groupColorMap == null || groupLabel == null )
            return GROUP_TEXT_COLOR;

        final Color groupColor = groupColorMap.get( groupLabel );
        return groupColor == null ? GROUP_TEXT_COLOR : groupColor;
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

        final int preferredHeight = 132 + timelineModel.getRowList().size() * ROW_STEP + Math.max( 0, groupCount - 1 ) * GROUP_GAP + groupCount * 14
            + groupOverviewCount * 14 + 18;
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

    /** Tooltip hotspot used for hit-testing. */
    private static class TooltipHotspot {

        /** Hoverable bounds. */
        private final Rectangle bounds;

        /** Tooltip text associated with the bounds. */
        private final String tooltipText;

        /**
         * Creates a new tooltip hotspot.
         *
         * @param bounds hoverable bounds
         * @param tooltipText tooltip text
         */
        private TooltipHotspot( final Rectangle bounds, final String tooltipText ) {
            this.bounds = bounds;
            this.tooltipText = tooltipText;
        }

    }

    /** Resolved visual settings for one timeline segment. */
    private static class SegmentVisual {

        /** Top y position. */
        private final int y;

        /** Segment height. */
        private final int height;

        /** Rounded-corner arc size. */
        private final int arc;

        /** Fill color. */
        private final Color color;

        /**
         * Creates a new visual descriptor.
         *
         * @param y top y position
         * @param height segment height
         * @param arc rounded-corner arc size
         * @param color fill color
         */
        private SegmentVisual( final int y, final int height, final int arc, final Color color ) {
            this.y = y;
            this.height = height;
            this.arc = arc;
            this.color = color;
        }

    }

}