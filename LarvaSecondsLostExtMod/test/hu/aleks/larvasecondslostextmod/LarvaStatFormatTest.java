package hu.aleks.larvasecondslostextmod;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/** Parity of the shared formatters with the old private methods (values captured from the pre-S13 code). */
public class LarvaStatFormatTest {

    @Test
    public void formatRateMatchesOldPrivateFormatters() {
        assertEquals( "1.00", LarvaStatFormat.formatRate( 1.005 ) );
        assertEquals( "0.00", LarvaStatFormat.formatRate( -0.004 ) );
        assertEquals( "0.00", LarvaStatFormat.formatRate( 0.0 ) );
        assertEquals( "12.35", LarvaStatFormat.formatRate( 12.345 ) );
        assertEquals( "1.00", LarvaStatFormat.formatRate( 0.995 ) );
        assertEquals( "100.00", LarvaStatFormat.formatRate( 100.0 ) );
        assertEquals( "-1.50", LarvaStatFormat.formatRate( -1.5 ) );
        assertEquals( "2.68", LarvaStatFormat.formatRate( 2.675 ) );
        assertEquals( "0.00", LarvaStatFormat.formatRate( 1e-9 ) );
        assertEquals( "n/a", LarvaStatFormat.formatRate( null ) );
    }

    /** Old behavior kept on purpose: the sign of a value in (-1, 0) is lost when the fraction is non-zero. */
    @Test
    public void formatRateKeepsOldSignQuirkForSmallNegatives() {
        assertEquals( "0.50", LarvaStatFormat.formatRate( -0.5 ) );
    }

    @Test
    public void formatPercentMatchesOldPrivateFormatters() {
        assertEquals( "n/a", LarvaStatFormat.formatPercent( null ) );
        assertEquals( "1.00%", LarvaStatFormat.formatPercent( 1.005 ) );
        assertEquals( "0.00%", LarvaStatFormat.formatPercent( -0.004 ) );
        assertEquals( "66.65%", LarvaStatFormat.formatPercent( 66.65 ) );
        assertEquals( "100.00%", LarvaStatFormat.formatPercent( 100.0 ) );
    }

    @Test
    public void formatMinSecHandlesBoundaries() {
        assertEquals( "0:00", LarvaStatFormat.formatMinSec( 0L ) );
        assertEquals( "0:00", LarvaStatFormat.formatMinSec( 999L ) );
        assertEquals( "0:09", LarvaStatFormat.formatMinSec( 9999L ) );
        assertEquals( "1:00", LarvaStatFormat.formatMinSec( 60000L ) );
        assertEquals( "4:10", LarvaStatFormat.formatMinSec( 250000L ) );
        assertEquals( "75:01", LarvaStatFormat.formatMinSec( 4501000L ) );
        assertEquals( "0:00", LarvaStatFormat.formatMinSec( -5000L ) );
    }

    @Test
    public void phaseHeaderLabelsMatchOldText() {
        assertEquals( "Early game [<37]", LarvaPhaseHeaderLabels.resolve( LarvaGamePhase.EARLY ) );
        assertEquals( "Mid game [<67]", LarvaPhaseHeaderLabels.resolve( LarvaGamePhase.MID ) );
        assertEquals( "Late game [<90]", LarvaPhaseHeaderLabels.resolve( LarvaGamePhase.LATE ) );
        assertEquals( "End game [90+]", LarvaPhaseHeaderLabels.resolve( LarvaGamePhase.END ) );
        assertEquals( "n/a", LarvaPhaseHeaderLabels.resolve( null ) );
    }
}
