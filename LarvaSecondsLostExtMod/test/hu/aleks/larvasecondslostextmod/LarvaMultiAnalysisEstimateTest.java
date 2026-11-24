package hu.aleks.larvasecondslostextmod;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** OQ-14: confirm dialog above 500 files with an estimate. */
public class LarvaMultiAnalysisEstimateTest {

    @Test
    public void thresholdIsExclusive() {
        assertFalse( LarvaMultiAnalysisEstimate.needsConfirmation( 500 ) );
        assertTrue( LarvaMultiAnalysisEstimate.needsConfirmation( 501 ) );
        assertFalse( LarvaMultiAnalysisEstimate.needsConfirmation( 1 ) );
    }

    @Test
    public void dialogTextFor501Files() {
        assertEquals( "Analyze 501 replays? Estimated time: ~1 min.", LarvaMultiAnalysisEstimate.confirmMessage( 501 ) );
    }

    @Test
    public void estimateRoundsUpToWholeMinutesAndScalesWithCount() {
        assertEquals( 1L, LarvaMultiAnalysisEstimate.estimatedMinutes( 1 ) );
        assertEquals( 1L, LarvaMultiAnalysisEstimate.estimatedMinutes( 1200 ) ); // 60 s exactly
        assertEquals( 2L, LarvaMultiAnalysisEstimate.estimatedMinutes( 1201 ) );
        assertEquals( 7L, LarvaMultiAnalysisEstimate.estimatedMinutes( 8352 ) ); // whole library, 417 s
        assertEquals( "Analyze 8352 replays? Estimated time: ~7 min.", LarvaMultiAnalysisEstimate.confirmMessage( 8352 ) );
    }
}
