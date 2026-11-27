package hu.aleks.larvasecondslostextmod.queens;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

/** Presence when the player's queen orders are followed (E19-S11, scenario replay E19-S02). */
public class QueenPresenceCommandsTest {

    private static final int MAIN = 1;
    private static final int NATURAL = 2;
    private static final int HOLD = 600;
    private static final int BRIDGE = 2000;
    private static final int[] AT_MAIN = { MAIN };
    private static final int[] AT_NATURAL = { NATURAL };
    private static final int[] NOWHERE = {};

    private static QueenPresence.Point seen( final int loop, final double x, final int[] near, final String label ) {
        return QueenPresence.Point.evidence( loop, x, 0, near.length == 0 ? QueenPresence.AWAY : near[ 0 ], near, label, false );
    }

    private static QueenPresence.Point move( final int loop, final double x, final double y, final int[] near ) {
        return QueenPresence.Point.move( loop, x, y, near.length == 0 ? QueenPresence.AWAY : near[ 0 ], near, "move", false, false );
    }

    private static QueenPresence presence( final int lifeEnd, final int commandsEnd, final double cellsPerLoop, final QueenPresence.Point... facts ) {
        return new QueenPresence( Arrays.asList( facts ), lifeEnd, new QueenPresence.Settings( HOLD, BRIDGE, true, cellsPerLoop, commandsEnd ) );
    }

    private static QueenPresence presence( final QueenPresence.Point... facts ) {
        return presence( 50000, Integer.MAX_VALUE, 1.0d, facts );
    }

    private static String text( final List< QueenPresence.Interval > intervals ) {
        final StringBuilder b = new StringBuilder();
        for ( final QueenPresence.Interval interval : intervals )
            b.append( '[' ).append( interval.getStart() ).append( ',' ).append( interval.getEnd() ).append( ' ' ).append( interval.getEndReason() )
                    .append( interval.isFromArrival() ? " arrival" : "" ).append( ')' );
        return b.toString();
    }

    /** Scenario 8:16-9:24: queens left idle at their hatcheries on purpose, no orders: red the whole time. */
    @Test
    public void aQueenWithoutOrdersStaysAsLongAsItTakes() {
        assertEquals( "[1000,6000 BRIDGE)[6000,50000 LIFE_END)",
                text( presence( seen( 1000, 0, AT_MAIN, "inject" ), seen( 6000, 0, AT_MAIN, "inject" ) ).intervalsAt( MAIN ) ) );
        assertEquals( "[1000,50000 LIFE_END)", text( presence( seen( 1000, 0, AT_MAIN, "inject" ) ).intervalsAt( MAIN ) ) );
    }

    /** Scenario 6:08: the queens walk to the watchtower and back; nothing is red while they are away. */
    @Test
    public void aMoveAwayEndsPresenceAtTheOrder() {
        final QueenPresence walk = presence( seen( 1000, 0, AT_MAIN, "inject" ), move( 3000, 80, 50, NOWHERE ), seen( 6000, 0, AT_MAIN, "inject" ) );
        assertEquals( "[1000,3000 MOVED)[6000,50000 LIFE_END)", text( walk.intervalsAt( MAIN ) ) );
    }

    @Test
    public void aMoveWithinTheRadiusKeepsHerThereAndRestartsTheHold() {
        final QueenPresence local = presence( seen( 1000, 0, AT_MAIN, "inject" ), move( 2000, 3, 3, AT_MAIN ), seen( 9000, 40, AT_NATURAL, "inject" ) );
        assertEquals( "[1000,2600 HOLD)", text( local.intervalsAt( MAIN ) ) );
    }

    @Test
    public void aMoveToAHatcheryCountsFromTheEstimatedArrival() {
        final QueenPresence walk = presence( seen( 1000, 0, AT_NATURAL, "inject" ), move( 2000, 100, 0, AT_MAIN ) );
        assertEquals( "[1000,2000 MOVED)", text( walk.intervalsAt( NATURAL ) ) );
        assertEquals( "[2100,50000 LIFE_END arrival)", text( walk.intervalsAt( MAIN ) ) );
    }

    /** She casts when she gets there, and the cast is evidence: an inject order adds no presence before the inject. */
    @Test
    public void aCastOrderCountsFromTheCast() {
        final QueenPresence walk = presence( seen( 1000, 0, AT_NATURAL, "inject" ),
                QueenPresence.Point.move( 2000, 100, 0, MAIN, AT_MAIN, "inject order", false, true ), seen( 2300, 100, AT_MAIN, "inject" ) );
        assertEquals( "[2300,50000 LIFE_END)", text( walk.intervalsAt( MAIN ) ) );
    }

    /** Scenario 4:53: a tumor order in the main, then a queued move to the fourth base; she leaves once the tumor is down. */
    @Test
    public void anOrderQueuedBehindACastStartsAtTheCast() {
        final QueenPresence queued = presence( seen( 1000, 0, AT_MAIN, "born" ), QueenPresence.Point.move( 2000, 2, 0, MAIN, AT_MAIN, "tumor order", false, true ),
                QueenPresence.Point.move( 2010, 80, 40, QueenPresence.AWAY, NOWHERE, "move", true, false ), seen( 2500, 2, AT_MAIN, "tumor" ) );
        assertEquals( "[1000,2500 BRIDGE)", text( queued.intervalsAt( MAIN ) ) );
    }

    @Test
    public void aStopOnTheWayLeavesHerSomewhereUnknown() {
        final QueenPresence stopped = presence( 50000, Integer.MAX_VALUE, 0.1d, seen( 1000, 0, AT_NATURAL, "inject" ), move( 2000, 100, 0, AT_MAIN ),
                QueenPresence.Point.stop( 2500, "stop" ) );
        assertEquals( "", text( stopped.intervalsAt( MAIN ) ) );
    }

    /** A retarget starts from where she is on her walk, not from the old target. */
    @Test
    public void aRetargetStartsFromHerEstimatedPosition() {
        final QueenPresence retarget = presence( seen( 0, 0, AT_NATURAL, "born" ), move( 0, 100, 0, NOWHERE ), move( 50, 50, 100, AT_MAIN ) );
        final List< QueenPresence.Interval > intervals = retarget.intervalsAt( MAIN );
        assertEquals( 1, intervals.size() );
        assertEquals( 150, intervals.get( 0 ).getStart() );
    }

    /** Scelight stops decoding some replays early: after the last game event missing orders prove nothing. */
    @Test
    public void afterTheLastDecodedCommandTheEvidenceLimitsApply() {
        assertEquals( "[1000,3000 HOLD)", text( presence( 50000, 3000, 1.0d, seen( 1000, 0, AT_MAIN, "inject" ) ).intervalsAt( MAIN ) ) );
        assertEquals( "[1000,3000 BRIDGE_LIMIT)[9000,9600 HOLD)",
                text( presence( 50000, 3000, 1.0d, seen( 1000, 0, AT_MAIN, "inject" ), seen( 9000, 0, AT_MAIN, "inject" ) ).intervalsAt( MAIN ) ) );
        assertEquals( "[1000,8000 HOLD)", text( presence( 50000, 8000, 1.0d, seen( 1000, 0, AT_MAIN, "inject" ) ).intervalsAt( MAIN ) ) );
    }

    @Test
    public void evidenceElsewhereWithoutAnOrderMeansSheLeftUnseen() {
        final QueenPresence unseen = presence( seen( 1000, 0, AT_MAIN, "inject" ), seen( 1300, 40, AT_NATURAL, "inject" ) );
        assertEquals( "[1000,1300 NEXT_AWAY)", text( unseen.intervalsAt( MAIN ) ) );
        assertTrue( unseen.hasPointAt( NATURAL ) );
    }

    @Test
    public void ordersAreIgnoredWithoutFollowing() {
        final QueenPresence evidenceOnly = new QueenPresence( Arrays.asList( seen( 1000, 0, AT_MAIN, "inject" ), move( 1100, 80, 50, NOWHERE ) ), 50000,
                new QueenPresence.Settings( HOLD, BRIDGE, false, 1.0d ) );
        assertEquals( "[1000,1600 HOLD)", text( evidenceOnly.intervalsAt( MAIN ) ) );
    }
}
