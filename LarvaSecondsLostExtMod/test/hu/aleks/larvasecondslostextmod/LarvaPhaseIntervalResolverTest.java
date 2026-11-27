package hu.aleks.larvasecondslostextmod;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Test;

/** Phase boundaries from worker samples (E19-S03), synthetic data only. */
public class LarvaPhaseIntervalResolverTest {

    private static final int END = 20000;

    private static LarvaPlayerResourceSnapshot snap( final int loop, final Integer workers ) {
        return new LarvaPlayerResourceSnapshot( "P", loop, "t", 0, 0, 0, 0, workers );
    }

    private static int start( final Map< LarvaGamePhase, LarvaPhaseInterval > m, final LarvaGamePhase phase ) {
        return m.get( phase ).getStartLoop();
    }

    @Test
    public void phaseStartsAtFirstSampleAboveTheThreshold() {
        final List< LarvaPlayerResourceSnapshot > s = Arrays.asList( snap( 160, 12 ), snap( 3200, 36 ), snap( 3360, 37 ), snap( 8000, 66 ),
                snap( 8160, 67 ), snap( 12000, 89 ), snap( 12160, 90 ) );
        final Map< LarvaGamePhase, LarvaPhaseInterval > m = LarvaPhaseIntervalResolver.resolve( s, END );
        assertEquals( 0, start( m, LarvaGamePhase.EARLY ) );
        assertEquals( 3360, m.get( LarvaGamePhase.EARLY ).getEndLoop() );
        assertEquals( 3360, start( m, LarvaGamePhase.MID ) );
        assertEquals( 8160, start( m, LarvaGamePhase.LATE ) );
        assertEquals( 12160, start( m, LarvaGamePhase.END ) );
        assertEquals( END, m.get( LarvaGamePhase.END ).getEndLoop() );
    }

    @Test
    public void unreachedPhasesStartAtReplayEnd() {
        final Map< LarvaGamePhase, LarvaPhaseInterval > m = LarvaPhaseIntervalResolver.resolve( Arrays.asList( snap( 5000, 50 ) ), END );
        assertEquals( 5000, start( m, LarvaGamePhase.MID ) );
        assertEquals( END, start( m, LarvaGamePhase.LATE ) );
        assertEquals( END, start( m, LarvaGamePhase.END ) );
        assertEquals( 0, m.get( LarvaGamePhase.END ).getDurationLoops() );
    }

    @Test
    public void samplesOutOfOrderGiveTheEarliestLoop() {
        final Map< LarvaGamePhase, LarvaPhaseInterval > m = LarvaPhaseIntervalResolver.resolve( Arrays.asList( snap( 9000, 70 ),
                snap( 4000, 40 ), snap( 6000, 70 ) ), END );
        assertEquals( 4000, start( m, LarvaGamePhase.MID ) );
        assertEquals( 6000, start( m, LarvaGamePhase.LATE ) );
    }

    @Test
    public void droppingBackDoesNotEndAPhaseAndZeroSamplesAfterLeavingAreIgnored() {
        final Map< LarvaGamePhase, LarvaPhaseInterval > m = LarvaPhaseIntervalResolver.resolve( Arrays.asList( snap( 4000, 40 ),
                snap( 4160, 20 ), snap( 9000, 0 ), snap( 9160, 0 ) ), END );
        assertEquals( 4000, start( m, LarvaGamePhase.MID ) );
        assertEquals( END, start( m, LarvaGamePhase.LATE ) );
    }

    @Test
    public void noSamplesMeansTheWholeGameIsEarly() {
        for ( final List< LarvaPlayerResourceSnapshot > s : Arrays.asList( null, Collections.< LarvaPlayerResourceSnapshot >emptyList(),
                Arrays.asList( snap( 100, null ) ) ) ) {
            final Map< LarvaGamePhase, LarvaPhaseInterval > m = LarvaPhaseIntervalResolver.resolve( s, END );
            assertEquals( END, m.get( LarvaGamePhase.EARLY ).getEndLoop() );
            assertEquals( END, start( m, LarvaGamePhase.END ) );
        }
    }

    @Test
    public void sampleAfterReplayEndIsClamped() {
        assertEquals( END, LarvaPhaseIntervalResolver.firstLoopAbove( Arrays.asList( snap( END + 50, 40 ) ), 36, END ) );
        assertEquals( -1, LarvaPhaseIntervalResolver.firstLoopAbove( Arrays.asList( snap( 100, 40 ) ), 36, 0 ) );
    }
}
