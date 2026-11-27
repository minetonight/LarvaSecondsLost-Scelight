package hu.aleks.larvasecondslostextmod;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

/** Smoke test: rate getters against hand-computed values. */
public class LarvaPhaseStatsTest {

    private static final double EPS = 1e-9;

    private static LarvaPhaseStats stats( final int missed, final int missedInject, final int spawned, final long hatchLoops,
            final long injectEligible, final long injectActive ) {
        return new LarvaPhaseStats( LarvaGamePhase.EARLY, new LarvaPhaseInterval( LarvaGamePhase.EARLY, 0, 16000 ), missed,
                missedInject, spawned, hatchLoops, injectEligible, injectActive );
    }

    @Test
    public void ratesMatchHandComputedValues() {
        // 1344 loops = 60 s on the game clock = 1 minute of one hatchery.
        final LarvaPhaseStats s = stats( 3, 2, 7, 1344L, 400L, 100L );
        assertEquals( 3.0, s.getMissedLarvaPerHatchPerMinute().doubleValue(), EPS );
        assertEquals( 2.0, s.getMissedInjectLarvaPerHatchPerMinute().doubleValue(), EPS );
        assertEquals( 7.0, s.getSpawnedLarvaPerHatchPerMinute().doubleValue(), EPS );
        assertEquals( 25.0, s.getInjectUptimePercentage().doubleValue(), EPS );
    }

    @Test
    public void twoHatcheryMinutesHalveTheRate() {
        assertEquals( 2.0, stats( 4, 0, 0, 2688L, 0L, 0L ).getMissedLarvaPerHatchPerMinute().doubleValue(), EPS );
    }

    @Test
    public void zeroDenominatorsGiveNull() {
        final LarvaPhaseStats s = stats( 3, 2, 7, 0L, 0L, 0L );
        assertNull( s.getMissedLarvaPerHatchPerMinute() );
        assertNull( s.getMissedInjectLarvaPerHatchPerMinute() );
        assertNull( s.getSpawnedLarvaPerHatchPerMinute() );
        assertNull( s.getInjectUptimePercentage() );
    }

    @Test
    public void zeroNumeratorGivesZeroNotNull() {
        final LarvaPhaseStats s = stats( 0, 0, 0, 1344L, 100L, 0L );
        assertEquals( 0.0, s.getMissedLarvaPerHatchPerMinute().doubleValue(), EPS );
        assertEquals( 0.0, s.getInjectUptimePercentage().doubleValue(), EPS );
    }
}
