package hu.aleks.larvasecondslostextmod;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;


import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

/** Unit tests for {@link LarvaAggregationRecordBuilder} and the record classes (no Scelight runtime needed). */
public class LarvaAggregationRecordBuilderTest {

    private static final int SCALE = 4096;

    /** Display conversion used by the tests: 10 ms per loop. */
    private static final LarvaAggregationRecordBuilder.LoopTimeConverter TEN_MS = new LarvaAggregationRecordBuilder.LoopTimeConverter() {
        @Override
        public long loopToTimeMs( final int loop ) {
            return loop * 10L;
        }
    };

    private static LarvaPlayerPhaseTable table( final int earlyEnd, final int midEnd ) {
        final Map< LarvaGamePhase, LarvaPhaseInterval > intervals = new EnumMap<>( LarvaGamePhase.class );
        final Map< LarvaGamePhase, LarvaPhaseStats > stats = new EnumMap<>( LarvaGamePhase.class );
        int start = 0;
        final int[] ends = { earlyEnd, midEnd };
        final LarvaGamePhase[] phases = LarvaGamePhase.values();
        for ( int i = 0; i < phases.length; i++ ) {
            final int end = i < ends.length ? ends[ i ] : start;
            final LarvaPhaseInterval interval = new LarvaPhaseInterval( phases[ i ], start, end );
            intervals.put( phases[ i ], interval );
            stats.put( phases[ i ], new LarvaPhaseStats( phases[ i ], interval, 1 + i, 2 + i, 3 + i, 100L + i, 50L + i, 25L + i ) );
            start = end;
        }
        return new LarvaPlayerPhaseTable( "P", intervals, stats );
    }

    private static LarvaPlayerResourceSnapshot snap( final int loop, final int supply, final int workers ) {
        return new LarvaPlayerResourceSnapshot( "P", loop, "t", 0, 0, Integer.valueOf( supply * SCALE ), null, Integer.valueOf( workers ) );
    }

    private static LarvaAggregationRecordBuilder.PlayerInput input() {
        final LarvaAggregationRecordBuilder.PlayerInput in = new LarvaAggregationRecordBuilder.PlayerInput();
        in.identityKey = "key";
        in.name = "P";
        return in;
    }

    // The IToon overload only forwards the getters; IToon cannot be loaded without the Scelight runtime (integration-checked).

    @Test
    public void identityKeyUsesToonFieldsAndNoPlayerName() {
        final String key = LarvaAggregationRecordBuilder.buildIdentityKey( 2, "S2", 1, 1234567, false, "SomePlayer" );
        assertEquals( "2-S2-1-1234567", key );
        assertFalse( key.contains( "SomePlayer" ) );
    }

    @Test
    public void sameAccountWithDifferentNameOrClanTagGivesSameKey() {
        assertEquals( LarvaAggregationRecordBuilder.buildIdentityKey( 2, "S2", 1, 99, false, "[CLAN]Name" ),
                LarvaAggregationRecordBuilder.buildIdentityKey( 2, "S2", 1, 99, false, "Name" ) );
    }

    @Test
    public void zeroNullAndIncompleteToonsFallBackToName() {
        assertEquals( "name:Bot", LarvaAggregationRecordBuilder.buildIdentityKey( 0, "", 0, 0, true, "Bot" ) );
        assertEquals( "name:Bot", LarvaAggregationRecordBuilder.buildIdentityKey( null, null, null, null, true, "Bot" ) );
        assertEquals( "name:Bot", LarvaAggregationRecordBuilder.buildIdentityKey( 2, null, 1, 5, false, "Bot" ) );
        assertEquals( "name:Bot", LarvaAggregationRecordBuilder.buildIdentityKey( 2, "S2", 1, null, false, "Bot" ) );
    }

    @Test
    public void phaseDataIsCarriedThroughWithDisplayMs() {
        final LarvaPlayerPhaseTable t = table( 1000, 3000 );
        final LarvaAggregationRecordBuilder.PlayerInput in = input();
        in.phaseTable = t;
        final LarvaAggregationPlayerRecord rec = LarvaAggregationRecordBuilder.buildPlayerRecord( in, TEN_MS );
        assertFalse( rec.isMissingPhaseTable() );
        for ( final LarvaGamePhase phase : LarvaGamePhase.values() )
            assertSame( t.getPhaseStats( phase ), rec.getPhaseStats().get( phase ) );
        final LarvaGamePhase[] phases = LarvaGamePhase.values();
        // CR-1.1: the record carries the end of each reached phase (end loop and display ms), not its length.
        assertEquals( Integer.valueOf( 1000 ), rec.getPhaseEndLoops().get( phases[ 0 ] ) );
        assertEquals( Long.valueOf( 10000L ), rec.getPhaseEndDisplayMs().get( phases[ 0 ] ) );
        assertEquals( Integer.valueOf( 3000 ), rec.getPhaseEndLoops().get( phases[ 1 ] ) );
        assertEquals( Long.valueOf( 30000L ), rec.getPhaseEndDisplayMs().get( phases[ 1 ] ) );
    }

    @Test
    public void zeroLengthPhaseIsNotReached() {
        final LarvaAggregationRecordBuilder.PlayerInput in = input();
        in.phaseTable = table( 1000, 1000 );
        final LarvaAggregationPlayerRecord rec = LarvaAggregationRecordBuilder.buildPlayerRecord( in, TEN_MS );
        final LarvaGamePhase[] phases = LarvaGamePhase.values();
        assertTrue( rec.getPhaseEndLoops().containsKey( phases[ 0 ] ) );
        assertFalse( rec.getPhaseEndLoops().containsKey( phases[ 1 ] ) );
        assertFalse( rec.getPhaseEndDisplayMs().containsKey( phases[ 1 ] ) );
        assertFalse( rec.getPhaseEndLoops().containsKey( phases[ phases.length - 1 ] ) );
    }

    @Test
    public void missingPhaseTableIsFlaggedAndMapsAreEmpty() {
        final LarvaAggregationPlayerRecord rec = LarvaAggregationRecordBuilder.buildPlayerRecord( input(), TEN_MS );
        assertTrue( rec.isMissingPhaseTable() );
        assertTrue( rec.getPhaseStats().isEmpty() );
        assertTrue( rec.getPhaseEndLoops().isEmpty() );
        assertTrue( rec.getMilestoneDisplayMs().isEmpty() );
    }

    @Test
    public void milestonesUseExactDroneLoopsAndDisplayMs() {
        final LarvaAggregationRecordBuilder.PlayerInput in = input();
        in.snapshots = Arrays.asList( snap( 1000, 100, 66 ), snap( 2000, 150, 80 ), snap( 3000, 200, 80 ) );
        in.exactDrone66Loop = Integer.valueOf( 900 );
        final LarvaAggregationPlayerRecord rec = LarvaAggregationRecordBuilder.buildPlayerRecord( in, TEN_MS );
        final Map< LarvaMilestone, Long > m = rec.getMilestoneDisplayMs();
        assertEquals( Long.valueOf( 10000L ), m.get( LarvaMilestone.SUPPLY_100 ) );
        assertEquals( Long.valueOf( 9000L ), m.get( LarvaMilestone.DRONES_66 ) );
        assertEquals( Long.valueOf( 20000L ), m.get( LarvaMilestone.DRONES_80 ) );
        assertEquals( Long.valueOf( 30000L ), m.get( LarvaMilestone.SUPPLY_200 ) );
    }

    @Test
    public void unreachedMilestonesAreAbsent() {
        final LarvaAggregationRecordBuilder.PlayerInput in = input();
        in.snapshots = Arrays.asList( snap( 1000, 50, 30 ) );
        assertTrue( LarvaAggregationRecordBuilder.buildPlayerRecord( in, TEN_MS ).getMilestoneDisplayMs().isEmpty() );
    }

    @Test( expected = UnsupportedOperationException.class )
    public void playerRecordMapsAreUnmodifiable() {
        final LarvaAggregationRecordBuilder.PlayerInput in = input();
        in.phaseTable = table( 1000, 3000 );
        LarvaAggregationRecordBuilder.buildPlayerRecord( in, TEN_MS ).getPhaseEndLoops().clear();
    }

    @Test
    public void playerRecordCopiesBuilderInputs() {
        final Map< LarvaMilestone, Long > source = new HashMap<>();
        source.put( LarvaMilestone.SUPPLY_100, Long.valueOf( 5L ) );
        final LarvaAggregationPlayerRecord rec = new LarvaAggregationPlayerRecord.Builder().identityKey( "k" ).name( "n" ).milestoneDisplayMs( source ).build();
        source.put( LarvaMilestone.SUPPLY_200, Long.valueOf( 9L ) );
        source.remove( LarvaMilestone.SUPPLY_100 );
        assertEquals( 1, rec.getMilestoneDisplayMs().size() );
        assertEquals( Long.valueOf( 5L ), rec.getMilestoneDisplayMs().get( LarvaMilestone.SUPPLY_100 ) );
    }

    @Test( expected = IllegalStateException.class )
    public void playerRecordRequiresIdentityKey() {
        new LarvaAggregationPlayerRecord.Builder().name( "n" ).build();
    }

    @Test
    public void gameRecordIsImmutableAndDefensivelyCopied() {
        final Date date = new Date( 1000L );
        final java.util.List< String > flags = new java.util.ArrayList<>( Arrays.asList( "a" ) );
        final LarvaAggregationPlayerRecord p = new LarvaAggregationPlayerRecord.Builder().identityKey( "k" ).name( "n" ).build();
        final LarvaAggregationGameRecord rec = new LarvaAggregationGameRecord.Builder().replayFile( Paths.get( "x.SC2Replay" ) ).replayDate( date )
                .lengthLoops( 6000 ).fullParse( true ).addZergPlayer( p ).diagnosticFlags( flags ).build();
        date.setTime( 5L );
        flags.add( "b" );
        rec.getReplayDate().setTime( 7L );
        assertEquals( 1000L, rec.getReplayDate().getTime() );
        assertEquals( Arrays.asList( "a" ), rec.getDiagnosticFlags() );
        assertEquals( 6000, rec.getLengthLoops() );
        assertTrue( rec.isFullParse() );
        assertNull( rec.getGameMode() );
        assertSame( p, rec.getZergPlayers().get( 0 ) );
        try {
            rec.getZergPlayers().clear();
            org.junit.Assert.fail( "list must be unmodifiable" );
        } catch ( final UnsupportedOperationException expected ) {
            // expected
        }
    }

    @Test( expected = IllegalStateException.class )
    public void gameRecordRequiresReplayFile() {
        new LarvaAggregationGameRecord.Builder().build();
    }
}
