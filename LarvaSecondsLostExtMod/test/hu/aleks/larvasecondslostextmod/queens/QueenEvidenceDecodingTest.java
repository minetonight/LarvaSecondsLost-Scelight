package hu.aleks.larvasecondslostextmod.queens;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

/** Decoding rules for queen evidence (E19-S07). */
public class QueenEvidenceDecodingTest {

    @Test
    public void unitPositionsUseIndexDeltas() {
        final List< int[] > positions = QueenEvidenceDecoding.decodeUnitPositions( 262,
                new Object[] { Integer.valueOf( 0 ), Integer.valueOf( 158 ), Integer.valueOf( 78 ), Long.valueOf( 30 ), Integer.valueOf( 60 ),
                        Integer.valueOf( 61 ), Integer.valueOf( 1 ), Integer.valueOf( 5 ) } );
        assertEquals( 2, positions.size() ); // the incomplete last triple is ignored
        assertEquals( "262,158,78", positions.get( 0 )[ 0 ] + "," + positions.get( 0 )[ 1 ] + "," + positions.get( 0 )[ 2 ] );
        assertEquals( "292,60,61", positions.get( 1 )[ 0 ] + "," + positions.get( 1 )[ 1 ] + "," + positions.get( 1 )[ 2 ] );
        assertTrue( QueenEvidenceDecoding.decodeUnitPositions( 0, null ).isEmpty() );
    }

    @Test
    public void tagAndUnitIndexMatchTheTrackerScheme() {
        final int tag = QueenEvidenceDecoding.tagOf( 262, 1 );
        assertEquals( 68681729, tag );
        assertEquals( 262, QueenEvidenceDecoding.unitIndexOf( tag ) );
        assertEquals( 9000, QueenEvidenceDecoding.unitIndexOf( QueenEvidenceDecoding.tagOf( 9000, 3 ) ) ); // negative int tag
    }

    @Test
    public void targetPointsAreRawOver4096() {
        // a tumor at tracker (149, 76) was ordered at Scelight getXFloat() (74.75, 38.25), raw x = 74.75 * 8192
        assertEquals( 149.5d, QueenEvidenceDecoding.toTrackerScale( Integer.valueOf( 612352 ) ), 1e-9 );
        assertTrue( Double.isNaN( QueenEvidenceDecoding.toTrackerScale( null ) ) );
        final Map< String, Object > snapshot = new HashMap<>();
        snapshot.put( "x", Integer.valueOf( 4096 * 10 ) );
        snapshot.put( "y", Integer.valueOf( 4096 * 20 ) );
        assertEquals( 20.0d, QueenEvidenceDecoding.snapshotPointOf( snapshot )[ 1 ], 1e-9 );
        assertTrue( Double.isNaN( QueenEvidenceDecoding.snapshotPointOf( null )[ 0 ] ) );
    }

    @Test
    public void tumorMatchesTheLatestNearbyCommandWithinSixtySeconds() {
        final QueenEvidenceDecoding.TumorCommand old = new QueenEvidenceDecoding.TumorCommand( 100, 50.0d, 50.0d, new int[] { 1 } );
        final QueenEvidenceDecoding.TumorCommand latest = new QueenEvidenceDecoding.TumorCommand( 500, 50.5d, 49.0d, new int[] { 2, 3 } );
        final QueenEvidenceDecoding.TumorCommand far = new QueenEvidenceDecoding.TumorCommand( 600, 53.0d, 50.0d, new int[] { 4 } );
        final QueenEvidenceDecoding.TumorCommand after = new QueenEvidenceDecoding.TumorCommand( 900, 50.0d, 50.0d, new int[] { 5 } );
        final List< QueenEvidenceDecoding.TumorCommand > commands = Arrays.asList( old, latest, far, after );
        assertSame( latest, QueenEvidenceDecoding.matchTumor( commands, 800, 50.0d, 50.0d ) );
        assertSame( old, QueenEvidenceDecoding.matchTumor( Arrays.asList( old ), 100 + QueenEvidenceDecoding.TUMOR_COMMAND_MAX_AGE_LOOPS, 50, 50 ) );
        assertNull( QueenEvidenceDecoding.matchTumor( Arrays.asList( old ), 101 + QueenEvidenceDecoding.TUMOR_COMMAND_MAX_AGE_LOOPS, 50, 50 ) );
        assertNull( QueenEvidenceDecoding.matchTumor( Arrays.asList( far ), 700, 50.0d, 50.0d ) );
    }

    @Test
    public void commandClassification() {
        assertTrue( QueenEvidenceDecoding.isInjectCommand( "SpawnLarva", null ) );
        assertTrue( QueenEvidenceDecoding.isQueenTumorCommand( "Queen", "CreepTumorQueen" ) );
        assertFalse( QueenEvidenceDecoding.isQueenTumorCommand( "CreepTumorBurrowed", "CreepTumor" ) );
        assertFalse( QueenEvidenceDecoding.isQueenTumorCommand( "Hatchery", "Queen" ) );
        assertTrue( QueenEvidenceDecoding.isTransfusionCommand( "Transfusion", null ) );
        assertFalse( QueenEvidenceDecoding.isInjectCommand( null, null ) );
    }
}
