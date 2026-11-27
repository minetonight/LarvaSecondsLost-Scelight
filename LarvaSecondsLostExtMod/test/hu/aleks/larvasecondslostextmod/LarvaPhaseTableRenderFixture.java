package hu.aleks.larvasecondslostextmod;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Synthetic data and headless rendering of the whole {@link LarvaTimelinePreviewComp} (AC-4.1 pixel parity).
 * Not a test class itself.
 */
final class LarvaPhaseTableRenderFixture {

    static final String FIXTURE_FONT_FAMILY = Font.DIALOG;
    static final int FIXTURE_FONT_SIZE = 12;

    private LarvaPhaseTableRenderFixture() {
    }

    /** Player with all four phases: realistic numbers, last phase has some zero counts. */
    static LarvaPlayerPhaseTable fullTable( final String name ) {
        final Map< LarvaGamePhase, LarvaPhaseInterval > intervals = new EnumMap<>( LarvaGamePhase.class );
        final Map< LarvaGamePhase, LarvaPhaseStats > stats = new EnumMap<>( LarvaGamePhase.class );
        final int[] startLoops = { 0, 4000, 9500, 15000 };
        final int[] endLoops = { 4000, 9500, 15000, 21000 };
        final int[][] counts = { { 3, 2, 14 }, { 9, 4, 61 }, { 17, 6, 120 }, { 0, 0, 33 } };
        final LarvaGamePhase[] phases = LarvaGamePhase.values();
        for ( int i = 0; i < phases.length; i++ ) {
            final LarvaPhaseInterval interval = new LarvaPhaseInterval( phases[ i ], startLoops[ i ], endLoops[ i ] );
            intervals.put( phases[ i ], interval );
            final long hatchLoops = ( endLoops[ i ] - startLoops[ i ] ) * 2L;
            stats.put( phases[ i ], new LarvaPhaseStats( phases[ i ], interval, counts[ i ][ 0 ], counts[ i ][ 1 ], counts[ i ][ 2 ], hatchLoops,
                    hatchLoops / 2, hatchLoops / 3 ) );
        }
        return new LarvaPlayerPhaseTable( name, intervals, stats );
    }

    /** Player that never left the early game: later phases have neither interval nor stats (all n/a). */
    static LarvaPlayerPhaseTable earlyOnlyTable( final String name ) {
        final Map< LarvaGamePhase, LarvaPhaseInterval > intervals = new EnumMap<>( LarvaGamePhase.class );
        final Map< LarvaGamePhase, LarvaPhaseStats > stats = new EnumMap<>( LarvaGamePhase.class );
        final LarvaPhaseInterval interval = new LarvaPhaseInterval( LarvaGamePhase.EARLY, 0, 3100 );
        intervals.put( LarvaGamePhase.EARLY, interval );
        stats.put( LarvaGamePhase.EARLY, new LarvaPhaseStats( LarvaGamePhase.EARLY, interval, 1, 0, 9, 6200L, 3100L, 1000L ) );
        return new LarvaPlayerPhaseTable( name, intervals, stats );
    }

    /** Two groups, each with a phase table and one row, no report (fallback loop time formatting). */
    static LarvaTimelineModel twoPlayerModel() {
        final Map< String, String > overview = new LinkedHashMap<>();
        overview.put( "Alpha", "Alpha overview line" );
        overview.put( "Beta", "Beta overview line" );
        final Map< String, LarvaPlayerPhaseTable > tables = new LinkedHashMap<>();
        tables.put( "Alpha", fullTable( "Alpha" ) );
        tables.put( "Beta", earlyOnlyTable( "Beta" ) );
        final Map< String, Color > colors = new LinkedHashMap<>();
        colors.put( "Alpha", new Color( 30, 90, 200 ) );
        colors.put( "Beta", new Color( 200, 90, 30 ) );
        final List< LarvaTimelineRow > rows = new ArrayList<>();
        rows.add( new LarvaTimelineRow( "Alpha", "Hatchery 1", "detail", 0L, 900000L, 0, Collections.< LarvaTimelineSegment >emptyList(),
                Collections.< LarvaTimelineMarker >emptyList() ) );
        rows.add( new LarvaTimelineRow( "Beta", "Hatchery 1", "detail", 0L, 900000L, 0, Collections.< LarvaTimelineSegment >emptyList(),
                Collections.< LarvaTimelineMarker >emptyList() ) );
        // No milestone tables: the pixel references show the phase tables only (E20-S09 adds the milestone table below them).
        return new LarvaTimelineModel( "Fixture title", "Fixture subtitle", "Fixture mode", overview, tables,
                Collections.< String, LarvaPlayerMilestones >emptyMap(), colors, null, 900000L, "15:00",
                "empty", rows );
    }

    /** Renders the preview component with the fixed font at the given width (height = preferred height). */
    static BufferedImage render( final LarvaTimelineModel model, final int width ) {
        final LarvaTimelinePreviewComp comp = new LarvaTimelinePreviewComp();
        comp.setFont( new Font( FIXTURE_FONT_FAMILY, Font.PLAIN, FIXTURE_FONT_SIZE ) );
        comp.setTimelineModel( model );
        final int height = comp.getPreferredSize().height;
        comp.setSize( width, height );
        final BufferedImage image = new BufferedImage( width, height, BufferedImage.TYPE_INT_RGB );
        final Graphics2D g = image.createGraphics();
        try {
            comp.paint( g );
        }
        finally {
            g.dispose();
        }
        return image;
    }

    /** Fingerprint of the installed fixed font (reference images are only comparable with identical metrics). */
    static String fontFingerprint() {
        final BufferedImage image = new BufferedImage( 4, 4, BufferedImage.TYPE_INT_RGB );
        final Graphics2D g = image.createGraphics();
        try {
            g.setFont( new Font( FIXTURE_FONT_FAMILY, Font.PLAIN, FIXTURE_FONT_SIZE ) );
            return g.getFont().getFamily() + "|" + g.getFontMetrics().stringWidth( "Larva missed [lrv / htch / min] 0123456789" ) + "|"
                    + g.getFontMetrics().getHeight();
        }
        finally {
            g.dispose();
        }
    }
}
