package hu.aleks.larvasecondslostextmod;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;

import org.junit.Test;

/** Band file of the phase table rankings (written by benchmark-data/tools/benchmark_bands.py). */
public class LarvaPhaseBenchmarkRatingFormatterTest {

    private static Map< String, LarvaPhaseBenchmarkRatingFormatter.Bands > parse( final String key, final String value ) {
        final Properties properties = new Properties();
        properties.setProperty( key, value );
        return LarvaPhaseBenchmarkRatingFormatter.parse( properties );
    }

    /** Tests see the fixture bands (test/resources/larva-benchmark-bands-fixture.properties), copied ahead of the generated file. */
    @Test
    public void bandsLoadFromTheClasspath() {
        assertTrue( LarvaPhaseBenchmarkRatingFormatter.hasBands() );
    }

    /** The generated band file in src (packaged into the module) is valid. */
    @Test
    public void theGeneratedBandFileIsValid() throws IOException {
        final Properties properties = new Properties();
        try ( final InputStream in = new FileInputStream( "src/hu/aleks/larvasecondslostextmod/" + LarvaPhaseBenchmarkRatingFormatter.BANDS_RESOURCE ) ) {
            properties.load( in );
        }
        assertFalse( LarvaPhaseBenchmarkRatingFormatter.parse( properties ).isEmpty() );
    }

    @Test
    public void tiersAreRatedFromTheWeakestUp() {
        final LarvaPhaseBenchmarkRatingFormatter.Bands bands = parse( "spawned.MID", "gold:4.0,plat:5.0,pro:6.0" ).get( "spawned.MID" );
        final LarvaPhaseBenchmarkRatingFormatter formatter = new LarvaPhaseBenchmarkRatingFormatter();
        assertEquals( "top 100% gold", formatter.formatRanking( bands, Double.valueOf( 3.0d ), true ) );
        assertEquals( "top 55% gold", formatter.formatRanking( bands, Double.valueOf( 4.5d ), true ) );
        assertEquals( "top 100% plat", formatter.formatRanking( bands, Double.valueOf( 5.0d ), true ) );
        assertEquals( "top 5% pro", formatter.formatRanking( bands, Double.valueOf( 9.0d ), true ) );
        assertEquals( "n/a", formatter.formatRanking( null, Double.valueOf( 5.0d ), true ) );
    }

    @Test
    public void lowerIsBetterForMissedLarva() {
        final LarvaPhaseBenchmarkRatingFormatter.Bands bands = parse( "missed.LATE", "silver:3.0,GM:1.0" ).get( "missed.LATE" );
        assertEquals( "top 55% silver", new LarvaPhaseBenchmarkRatingFormatter().formatRanking( bands, Double.valueOf( 2.0d ), false ) );
    }

    @Test( expected = IllegalArgumentException.class )
    public void startsOutOfOrderAreRejected() {
        parse( "spawned.LATE", "gold:4.0,plat:3.9" );
    }

    @Test( expected = IllegalArgumentException.class )
    public void oneTierIsNotABand() {
        parse( "missed.MID", "gold:2.0" );
    }

    @Test( expected = IllegalArgumentException.class )
    public void unknownPhasesAreRejected() {
        parse( "missed.LATER", "gold:2.0,plat:1.0" );
    }
}
