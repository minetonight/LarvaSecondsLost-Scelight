package hu.aleks.larvasecondslostextmod;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

public class LarvaAutoOpenPolicyTest {

    private static List< LarvaAutoOpenPolicy.RaceView > races( final boolean... zerg ) {
        final List< LarvaAutoOpenPolicy.RaceView > list = new ArrayList< LarvaAutoOpenPolicy.RaceView >();
        for ( final boolean z : zerg )
            list.add( new LarvaAutoOpenPolicy.RaceView( z ) );
        return list;
    }

    @Test
    public void zergVersusProtossQualifies() {
        assertTrue( LarvaAutoOpenPolicy.qualifies( races( true, false ) ) );
    }

    @Test
    public void protossVersusTerranDoesNotQualify() {
        assertFalse( LarvaAutoOpenPolicy.qualifies( races( false, false ) ) );
    }

    @Test
    public void zergVersusZergQualifies() {
        assertTrue( LarvaAutoOpenPolicy.qualifies( races( true, true ) ) );
    }

    @Test
    public void twoVersusTwoWithOneZergQualifies() {
        assertTrue( LarvaAutoOpenPolicy.qualifies( races( false, false, false, true ) ) );
    }

    @Test
    public void emptyOrNullPlayerListDoesNotQualify() {
        assertFalse( LarvaAutoOpenPolicy.qualifies( Collections.< LarvaAutoOpenPolicy.RaceView > emptyList() ) );
        assertFalse( LarvaAutoOpenPolicy.qualifies( null ) );
        assertFalse( LarvaAutoOpenPolicy.qualifies( Arrays.asList( (LarvaAutoOpenPolicy.RaceView) null ) ) );
    }

    @Test
    public void focusRuleSelectsOnlyWhenOnALarvaPage() {
        assertTrue( LarvaAutoOpenPolicy.shouldSelect( true ) );
        assertFalse( LarvaAutoOpenPolicy.shouldSelect( false ) );
    }

    @Test
    public void capBoundary() {
        assertTrue( LarvaAutoOpenPolicy.underCap( 0 ) );
        assertTrue( LarvaAutoOpenPolicy.underCap( 19 ) );
        assertFalse( LarvaAutoOpenPolicy.underCap( 20 ) );
        assertFalse( LarvaAutoOpenPolicy.underCap( 21 ) );
    }

    @Test
    public void admissionChecksReadyThenDedupeThenCap() {
        assertEquals( LarvaAutoOpenPolicy.Admission.SKIP_NOT_READY, LarvaAutoOpenPolicy.admit( false, true, 99 ) );
        assertEquals( LarvaAutoOpenPolicy.Admission.SKIP_ALREADY_OPEN, LarvaAutoOpenPolicy.admit( true, true, 99 ) );
        assertEquals( LarvaAutoOpenPolicy.Admission.SKIP_CAP, LarvaAutoOpenPolicy.admit( true, false, 20 ) );
        assertEquals( LarvaAutoOpenPolicy.Admission.OPEN, LarvaAutoOpenPolicy.admit( true, false, 19 ) );
    }

    @Test
    public void pendingLimitAdmitsFiveThenDropsUntilReleased() {
        final LarvaAutoOpenPendingLimit limit = new LarvaAutoOpenPendingLimit( LarvaAutoOpenPolicy.MAX_PENDING_AUTO_REQUESTS );
        for ( int i = 0; i < 5; i++ )
            assertTrue( limit.tryAcquire() );
        assertFalse( limit.tryAcquire() );
        assertEquals( 5, limit.getPending() );
        limit.release();
        assertTrue( limit.tryAcquire() );
        assertFalse( limit.tryAcquire() );
    }

    @Test
    public void pendingReleaseNeverGoesNegative() {
        final LarvaAutoOpenPendingLimit limit = new LarvaAutoOpenPendingLimit( 2 );
        limit.release();
        assertEquals( 0, limit.getPending() );
    }

    @Test
    public void statsKeepOnlyTheLastDecisions() {
        final LarvaAutoOpenStats stats = new LarvaAutoOpenStats();
        for ( int i = 0; i < LarvaAutoOpenStats.MAX_DECISIONS + 5; i++ )
            stats.recordNotZerg( "f" + i );
        assertEquals( LarvaAutoOpenStats.MAX_DECISIONS, stats.lastDecisions().size() );
        assertTrue( stats.lastDecisions().get( 0 ).endsWith( "f5" ) );
    }

}
