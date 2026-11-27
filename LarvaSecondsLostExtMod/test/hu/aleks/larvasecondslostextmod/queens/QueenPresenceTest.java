package hu.aleks.larvasecondslostextmod.queens;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

/** Presence at a hatchery from evidence points only (E19-S08). */
public class QueenPresenceTest {

    private static final int MAIN = 1;
    private static final int NATURAL = 2;
    private static final int HOLD = 600;
    private static final int BRIDGE = 2000;

    private static QueenPresence.Point at( final int loop, final int home, final String label ) {
        return new QueenPresence.Point( loop, 0, 0, home, label, false );
    }

    private static QueenPresence.Point died( final int loop, final int home ) {
        return new QueenPresence.Point( loop, 0, 0, home, "died", true );
    }

    private static String text( final List< QueenPresence.Interval > intervals ) {
        final StringBuilder b = new StringBuilder();
        for ( final QueenPresence.Interval interval : intervals )
            b.append( '[' ).append( interval.getStart() ).append( ',' ).append( interval.getEnd() ).append( ' ' ).append( interval.getEndReason() ).append( ')' );
        return b.toString();
    }

    @Test
    public void queenBornAtTheMainThenTumorsFarAwayLeavesAtTheFirstTumor() {
        final QueenPresence presence = new QueenPresence( Arrays.asList( at( 1000, MAIN, "born" ), at( 1300, QueenPresence.AWAY, "tumor" ),
                at( 2000, QueenPresence.AWAY, "tumor" ) ), 50000, HOLD, BRIDGE );
        assertEquals( "[1000,1300 NEXT_AWAY)", text( presence.intervalsAt( MAIN ) ) );
    }

    @Test
    public void queenBornAtTheMainWithNoLaterEvidenceStaysForTheHoldOnly() {
        final QueenPresence presence = new QueenPresence( Arrays.asList( at( 1000, MAIN, "born" ), at( 5000, QueenPresence.AWAY, "tumor" ) ),
                50000, HOLD, BRIDGE );
        assertEquals( "[1000,1600 HOLD)", text( presence.intervalsAt( MAIN ) ) );
    }

    @Test
    public void injectChainBridgesGapsUpToTheLimit() {
        final QueenPresence presence = new QueenPresence( Arrays.asList( at( 1000, MAIN, "inject" ), at( 2500, MAIN, "inject" ),
                at( 6000, MAIN, "inject" ) ), 50000, HOLD, BRIDGE );
        assertEquals( "[1000,2500 BRIDGE)[2500,4500 BRIDGE_LIMIT)[6000,6600 HOLD)", text( presence.intervalsAt( MAIN ) ) );
    }

    @Test
    public void deathEndsPresence() {
        final QueenPresence presence = new QueenPresence( Arrays.asList( at( 1000, MAIN, "inject" ), died( 1200, MAIN ) ), 1200, HOLD, BRIDGE );
        assertEquals( "[1000,1200 BRIDGE)", text( presence.intervalsAt( MAIN ) ) );
        final QueenPresence diedAway = new QueenPresence( Arrays.asList( at( 1000, MAIN, "inject" ), died( 1300, QueenPresence.AWAY ) ), 1300,
                HOLD, BRIDGE );
        assertEquals( "[1000,1300 LIFE_END)", text( diedAway.intervalsAt( MAIN ) ) );
        final QueenPresence replayEnd = new QueenPresence( Arrays.asList( at( 1000, MAIN, "inject" ) ), 1100, HOLD, BRIDGE );
        assertEquals( "[1000,1100 LIFE_END)", text( replayEnd.intervalsAt( MAIN ) ) );
    }

    @Test
    public void walkingToTheNaturalEndsMainPresenceAndStartsNaturalPresence() {
        final QueenPresence presence = new QueenPresence( Arrays.asList( at( 1000, MAIN, "inject" ), at( 1400, NATURAL, "inject" ),
                at( 2000, NATURAL, "tumor" ) ), 50000, HOLD, BRIDGE );
        assertEquals( "[1000,1400 NEXT_AWAY)", text( presence.intervalsAt( MAIN ) ) );
        assertEquals( "[1400,2000 BRIDGE)[2000,2600 HOLD)", text( presence.intervalsAt( NATURAL ) ) );
        assertTrue( presence.hasPointAt( NATURAL ) );
        assertFalse( presence.hasPointAt( 99 ) );
    }
}
