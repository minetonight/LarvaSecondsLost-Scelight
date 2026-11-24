package hu.aleks.larvasecondslostextmod;

import static org.junit.Assert.assertEquals;

import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import org.junit.Test;

/** Label state machine of a multi page (E17-S14, OQ-7). */
public class LarvaMultiPageLabelsTest {

    private static final TimeZone UTC = TimeZone.getTimeZone( "UTC" );
    private static final Date START = new Date( 1700000000000L );
    private static final Date END = new Date( 1700000000000L + 5L * 86400000L );

    @Test
    public void renameSupportedRunsThenShowsDates() {
        final LarvaMultiPageLabels labels = new LarvaMultiPageLabels( true, 3 );
        assertEquals( "Multi analysis (running…)", labels.running() );
        assertEquals( "from 11/14/23 to 11/19/23", labels.completed( START, END, Locale.US, UTC ) );
    }

    @Test
    public void renameSupportedCancelledFailedAndNoGames() {
        final LarvaMultiPageLabels labels = new LarvaMultiPageLabels( true, 3 );
        assertEquals( "Multi analysis #3 (cancelled)", labels.cancelled() );
        assertEquals( "Multi analysis #3 (failed)", labels.failed() );
        assertEquals( "Multi analysis #3 (no games)", labels.completed( null, null, Locale.US, UTC ) );
    }

    @Test
    public void renameUnsupportedKeepsTheNumberedLabelInEveryState() {
        final LarvaMultiPageLabels labels = new LarvaMultiPageLabels( false, 2 );
        assertEquals( "Multi analysis #2", labels.running() );
        assertEquals( "Multi analysis #2", labels.completed( START, END, Locale.US, UTC ) );
        assertEquals( "Multi analysis #2", labels.completed( null, null, Locale.US, UTC ) );
        assertEquals( "Multi analysis #2", labels.cancelled() );
        assertEquals( "Multi analysis #2", labels.failed() );
    }
}
