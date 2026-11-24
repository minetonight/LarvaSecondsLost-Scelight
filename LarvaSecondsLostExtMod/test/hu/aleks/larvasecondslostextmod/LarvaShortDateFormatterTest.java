package hu.aleks.larvasecondslostextmod;

import static org.junit.Assert.assertEquals;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import org.junit.Test;

/** E17-S14 / OQ-12 / R-9: short dates for the multi page label. */
public class LarvaShortDateFormatterTest {

    private static final TimeZone UTC = TimeZone.getTimeZone( "UTC" );
    private static final Locale BG = new Locale( "bg", "BG" );

    private static Date date( final int year, final int month, final int day ) {
        final Calendar c = Calendar.getInstance( UTC );
        c.clear();
        c.set( year, month - 1, day, 12, 0, 0 );
        return c.getTime();
    }

    @Test
    public void bulgarianHasNoTrailingSuffix() {
        // JDK 8 data gives dd.MM.yy, newer CLDR data appends " г.": both must end up identical
        assertEquals( "24.11.26", LarvaShortDateFormatter.format( date( 2026, 11, 24 ), BG, UTC ) );
        assertEquals( "03.11.26", LarvaShortDateFormatter.format( date( 2026, 11, 3 ), BG, UTC ).replaceFirst( "^0?3\\.", "03." ) );
    }

    @Test
    public void usEnglishIsMonthDayYear() {
        assertEquals( "11/24/26", LarvaShortDateFormatter.format( date( 2026, 11, 24 ), Locale.US, UTC ) );
        assertEquals( "11/3/26", LarvaShortDateFormatter.format( date( 2026, 11, 3 ), Locale.US, UTC ) );
    }

    @Test
    public void rangeUsesFromAndTo() {
        assertEquals( "from 11/3/26 to 11/24/26", LarvaShortDateFormatter.formatRange( date( 2026, 11, 3 ), date( 2026, 11, 24 ), Locale.US, UTC ) );
    }

    @Test
    public void singleDayRangeRendersTheSameDateTwice() {
        assertEquals( "from 11/24/26 to 11/24/26", LarvaShortDateFormatter.formatRange( date( 2026, 11, 24 ), date( 2026, 11, 24 ), Locale.US, UTC ) );
    }

    @Test
    public void stripsOnlyTrailingTokensWithoutDigits() {
        assertEquals( "24.11.26", LarvaShortDateFormatter.stripTrailingNonDigitTokens( "24.11.26 г." ) );
        assertEquals( "11/24/26", LarvaShortDateFormatter.stripTrailingNonDigitTokens( "11/24/26" ) );
        assertEquals( "24.11.26", LarvaShortDateFormatter.stripTrailingNonDigitTokens( "24.11.26" ) );
        assertEquals( "24.11.26", LarvaShortDateFormatter.stripTrailingNonDigitTokens( " 24.11.26  г.  x " ) );
        assertEquals( "a b 1", LarvaShortDateFormatter.stripTrailingNonDigitTokens( "a b 1" ) );
    }

    @Test
    public void firstTokenIsKeptEvenWithoutDigit() {
        assertEquals( "abc", LarvaShortDateFormatter.stripTrailingNonDigitTokens( "abc" ) );
    }

    @Test
    public void usesTheTimeZoneGiven() {
        final Date lateUtc = new Date( date( 2026, 11, 24 ).getTime() + 11L * 3600000L + 30L * 60000L ); // 23:30 UTC
        assertEquals( "11/24/26", LarvaShortDateFormatter.format( lateUtc, Locale.US, UTC ) );
        assertEquals( "11/25/26", LarvaShortDateFormatter.format( lateUtc, Locale.US, TimeZone.getTimeZone( "Asia/Tokyo" ) ) );
    }
}
