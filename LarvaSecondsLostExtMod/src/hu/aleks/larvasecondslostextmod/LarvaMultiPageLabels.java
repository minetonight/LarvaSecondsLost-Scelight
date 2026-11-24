package hu.aleks.larvasecondslostextmod;

import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Tree labels of one multi-analysis page over its life (E17-S14, OQ-7). With rename support (S01: yes) the label is the
 * placeholder {@code Multi analysis (running...)} while running and {@code from <start> to <end>} when done. Without
 * rename support the label is fixed to {@code Multi analysis #k} (k = session counter) and the dates live in the page header.
 */
final class LarvaMultiPageLabels {

    static final String RUNNING_LABEL = "Multi analysis (running…)";

    private static final String BASE = "Multi analysis #";

    private final boolean renameSupported;
    private final String numbered;

    /**
     * @param renameSupported whether the host can rename a page after creation
     * @param sessionNumber 1-based number of this multi page in the session
     */
    LarvaMultiPageLabels( final boolean renameSupported, final int sessionNumber ) {
        this.renameSupported = renameSupported;
        this.numbered = BASE + sessionNumber;
    }

    /** @return the label at creation */
    String running() {
        return renameSupported ? RUNNING_LABEL : numbered;
    }

    /** @return the label of a finished run; start and end are the dates of the eligible games, <code>null</code> if there are none */
    String completed( final Date start, final Date end, final Locale locale, final TimeZone timeZone ) {
        if ( !renameSupported )
            return numbered;
        if ( start == null || end == null )
            return numbered + " (no games)";
        return LarvaShortDateFormatter.formatRange( start, end, locale, timeZone );
    }

    /** @return the label of a cancelled run */
    String cancelled() {
        return renameSupported ? numbered + " (cancelled)" : numbered;
    }

    /** @return the label of a failed run */
    String failed() {
        return renameSupported ? numbered + " (failed)" : numbered;
    }
}
