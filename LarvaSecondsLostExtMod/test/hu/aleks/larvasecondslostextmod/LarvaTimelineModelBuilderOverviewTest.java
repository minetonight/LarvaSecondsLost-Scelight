package hu.aleks.larvasecondslostextmod;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/** The bold player summary line of the single-replay page (E20-S13). */
public class LarvaTimelineModelBuilderOverviewTest {

    @Test
    public void everyPlayerGetsGeneratedMissedAndInjectMissed() {
        assertEquals( "412 larva generated, 85 potential larva missed, 72 inject larva missed", LarvaTimelineModelBuilder.overviewLabel( 412, 85, 72 ) );
        assertEquals( "0 larva generated, 0 potential larva missed, 0 inject larva missed", LarvaTimelineModelBuilder.overviewLabel( 0, 0, 0 ) );
    }
}
