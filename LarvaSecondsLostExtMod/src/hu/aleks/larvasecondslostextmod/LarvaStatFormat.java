package hu.aleks.larvasecondslostextmod;

/**
 * Shared, locale-independent text formatting for larva statistics tables (single page and multi page).
 *
 * <p>Moved verbatim from {@link LarvaTimelinePreviewComp} and {@link LarvaReplayPageComp}; outputs must stay identical because the single-replay
 * page output is a frozen contract (AC-4.1).</p>
 */
final class LarvaStatFormat {

    /** Text shown when a value does not exist. */
    static final String NOT_AVAILABLE = "n/a";

    private static final double CENTS_PER_UNIT = 100.0d;

    private static final long CENTS_PER_UNIT_LONG = 100L;

    private static final long MS_PER_SECOND = 1000L;

    private static final long SECONDS_PER_MINUTE = 60L;

    private LarvaStatFormat() {
    }

    /**
     * Formats a rate with exactly two fractional digits (rounded half up on the scaled value).
     *
     * <p>Quirk kept for output parity: for values in (-1, 0) that round to a non-zero fraction the minus sign is lost
     * (<code>-0.5</code> gives <code>0.50</code>), because the sign is carried by the integer part only.</p>
     *
     * @param value value; may be <code>null</code>
     * @return e.g. <code>1.68</code>, or {@link #NOT_AVAILABLE} for <code>null</code>
     */
    static String formatRate( final Double value ) {
        if ( value == null )
            return NOT_AVAILABLE;

        final long scaled = Math.round( value.doubleValue() * CENTS_PER_UNIT );
        final long whole = scaled / CENTS_PER_UNIT_LONG;
        final long fraction = Math.abs( scaled % CENTS_PER_UNIT_LONG );
        return String.valueOf( whole ) + '.' + ( fraction < 10L ? "0" : "" ) + fraction;
    }

    /**
     * Formats a percentage value (already in percent) with two fractional digits and a trailing <code>%</code>.
     *
     * @param value value in percent; may be <code>null</code>
     * @return e.g. <code>66.65%</code>, or {@link #NOT_AVAILABLE} for <code>null</code>
     */
    static String formatPercent( final Double value ) {
        return value == null ? NOT_AVAILABLE : formatRate( value ) + "%";
    }

    /**
     * Formats a duration as <code>m:ss</code> (minutes are not wrapped into hours).
     *
     * @param ms duration in milliseconds; negative values are treated as 0
     * @return e.g. <code>4:10</code>
     */
    static String formatMinSec( final long ms ) {
        final long totalSeconds = Math.max( 0L, ms ) / MS_PER_SECOND;
        final long minutes = totalSeconds / SECONDS_PER_MINUTE;
        final long seconds = totalSeconds % SECONDS_PER_MINUTE;
        return minutes + ":" + ( seconds < 10L ? "0" : "" ) + seconds;
    }
}
