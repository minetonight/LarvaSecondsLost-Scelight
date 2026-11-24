package hu.aleks.larvasecondslostextmod;

import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeTrue;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;

import javax.imageio.ImageIO;

import org.junit.Test;

/**
 * AC-4.1: the whole {@link LarvaTimelinePreviewComp} (timeline + phase tables) renders pixel-identical to the images captured from the
 * pre-S13 code (<code>drawPhaseTable</code> inline). The references live in <code>test/resources</code>.
 *
 * <p>Pixels depend on the installed fonts, so the test is skipped (assumption) when the font fingerprint differs from the one stored next to
 * the images. To regenerate on another machine, run with <code>-Dlarva.parity.writeDir=&lt;dir&gt;</code> against a known-good build.</p>
 */
public class LarvaPhaseTablePixelParityTest {

    private static final File RESOURCES = new File( "test/resources" );

    private static final int WIDE_WIDTH = 1000;

    private static final int NARROW_WIDTH = 420;

    @Test
    public void wideRenderingMatchesPreRefactorReference() throws IOException {
        assertMatchesReference( WIDE_WIDTH, "phase-table-wide.png" );
    }

    /** Narrow width clamps the table fill (min 220) while column positions keep their natural size, as before. */
    @Test
    public void narrowRenderingMatchesPreRefactorReference() throws IOException {
        assertMatchesReference( NARROW_WIDTH, "phase-table-narrow.png" );
    }

    @Test
    public void painterHeightForSevenRowTableEqualsLayoutConstant() {
        final LarvaTableViewModel model = LarvaPhaseTableViewModels.fromPlayerPhaseTable( LarvaPhaseTableRenderFixture.fullTable( "A" ), null );
        final BufferedImage image = new BufferedImage( 800, 300, BufferedImage.TYPE_INT_RGB );
        final java.awt.Graphics2D g = image.createGraphics();
        try {
            assertEquals( LarvaTimelinePreviewComp.PHASE_TABLE_HEIGHT, new LarvaTablePainter().paint( g, g.getFontMetrics(), model, 0, 0, 800 ) );
        }
        finally {
            g.dispose();
        }
    }

    private void assertMatchesReference( final int width, final String referenceName ) throws IOException {
        final File fingerprintFile = new File( RESOURCES, "font-fingerprint.txt" );
        final File referenceFile = new File( RESOURCES, referenceName );
        assumeTrue( "reference files not found (run from the module folder)", fingerprintFile.isFile() && referenceFile.isFile() );
        final String expectedFingerprint = new String( Files.readAllBytes( fingerprintFile.toPath() ), Charset.forName( "UTF-8" ) );
        assumeTrue( "font metrics differ from the machine that captured the reference", expectedFingerprint.equals( LarvaPhaseTableRenderFixture.fontFingerprint() ) );

        final BufferedImage actual = LarvaPhaseTableRenderFixture.render( LarvaPhaseTableRenderFixture.twoPlayerModel(), width );
        final String writeDir = System.getProperty( "larva.parity.writeDir" );
        if ( writeDir != null )
            ImageIO.write( actual, "png", new File( writeDir, referenceName ) );

        final BufferedImage expected = ImageIO.read( referenceFile );
        assertEquals( "width", expected.getWidth(), actual.getWidth() );
        assertEquals( "height", expected.getHeight(), actual.getHeight() );
        for ( int y = 0; y < expected.getHeight(); y++ )
            for ( int x = 0; x < expected.getWidth(); x++ )
                assertEquals( "pixel " + x + "," + y, expected.getRGB( x, y ) & 0xFFFFFF, actual.getRGB( x, y ) & 0xFFFFFF );
    }
}
