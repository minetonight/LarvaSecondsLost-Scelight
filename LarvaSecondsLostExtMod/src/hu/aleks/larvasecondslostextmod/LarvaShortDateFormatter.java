package hu.aleks.larvasecondslostextmod;

import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Short, locale-aware dates for the multi page label (E17-S14, FR-7, OQ-12, R-9). Uses
 * {@code DateFormat.getDateInstance(SHORT, locale)} and strips trailing tokens without a digit, because newer CLDR data
 * appends text such as the Bulgarian {@code г.} ({@code 24.11.26 г.} becomes {@code 24.11.26}).
 *
 * <p>{@link DateFormat} is not thread-safe, so every call creates its own instance.</p>
 */
final class LarvaShortDateFormatter {

    private LarvaShortDateFormatter() {
    }

    /** @return the short date in the given locale and the default time zone */
    static String format( final Date date, final Locale locale ) {
        return format( date, locale, TimeZone.getDefault() );
    }

    /** @return the short date in the given locale and time zone, without trailing digit-less tokens */
    static String format( final Date date, final Locale locale, final TimeZone timeZone ) {
        final DateFormat dateFormat = DateFormat.getDateInstance( DateFormat.SHORT, locale );
        dateFormat.setTimeZone( timeZone );
        return stripTrailingNonDigitTokens( dateFormat.format( date ) );
    }

    /** @return {@code from <start> to <end>}; a single-day range renders {@code from d to d} */
    static String formatRange( final Date start, final Date end, final Locale locale, final TimeZone timeZone ) {
        return "from " + format( start, locale, timeZone ) + " to " + format( end, locale, timeZone );
    }

    /** Removes whitespace-separated trailing tokens that contain no digit; the first token is always kept. */
    static String stripTrailingNonDigitTokens( final String formatted ) {
        final String[] tokens = formatted.trim().split( "\\s+" );
        int end = tokens.length;
        while ( end > 1 && !containsDigit( tokens[ end - 1 ] ) )
            end--;
        final StringBuilder sb = new StringBuilder();
        for ( int i = 0; i < end; i++ ) {
            if ( i > 0 )
                sb.append( ' ' );
            sb.append( tokens[ i ] );
        }
        return sb.toString();
    }

    private static boolean containsDigit( final String token ) {
        for ( int i = 0; i < token.length(); i++ )
            if ( Character.isDigit( token.charAt( i ) ) )
                return true;
        return false;
    }
}
