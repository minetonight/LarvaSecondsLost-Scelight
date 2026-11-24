package hu.aleks.larvasecondslostextmod;

import java.util.List;

/**
 * Pure decisions of the auto-open feature for newly detected replays (E17-S07, FR-17). No Scelight runtime needed.
 */
final class LarvaAutoOpenPolicy {

    /** Source label of auto-opened pages (same string the old monitor path used, D17-i). */
    static final String SOURCE_MONITOR = "Replay Folder Monitor";

    /** D17-e: auto-open creates no page while this many single-replay pages are open (manual opens are never capped). */
    static final int MAX_AUTO_OPEN_SINGLE_PAGES = 20;

    /** D17-e: auto-open requests that may wait in the queue; further events are dropped. */
    static final int MAX_PENDING_AUTO_REQUESTS = 5;

    /** Outcome of the checks done on the EDT before a page is created (order: ready, dedupe, cap). */
    enum Admission {
        /** Create the page. */
        OPEN,
        /** The hub is not installed (yet or any more). */
        SKIP_NOT_READY,
        /** A page for this replay is already open (OQ-6, D17-d). */
        SKIP_ALREADY_OPEN,
        /** Too many single-replay pages are open (D17-e). */
        SKIP_CAP
    }

    /** The only fact of a replay player the trigger needs, so the policy is testable without Scelight types. */
    static final class RaceView {
        private final boolean zerg;

        RaceView( final boolean zerg ) {
            this.zerg = zerg;
        }

        boolean isZerg() {
            return zerg;
        }
    }

    private LarvaAutoOpenPolicy() {
    }

    /** D17-a: at least one player is Zerg (any format, any opponent). Null or empty list: no. */
    static boolean qualifies( final List< RaceView > players ) {
        if ( players == null )
            return false;
        for ( final RaceView player : players )
            if ( player != null && player.isZerg() )
                return true;
        return false;
    }

    /** D17-c: select the new page only if the user is already on the hub or a Larva sub-page. */
    static boolean shouldSelect( final boolean larvaPageSelected ) {
        return larvaPageSelected;
    }

    /** D17-e: <code>true</code> while fewer than {@link #MAX_AUTO_OPEN_SINGLE_PAGES} single-replay pages are open. */
    static boolean underCap( final int openSinglePages ) {
        return openSinglePages < MAX_AUTO_OPEN_SINGLE_PAGES;
    }

    /** Ready, then dedupe, then cap (D17-g, D17-d, D17-e). */
    static Admission admit( final boolean ready, final boolean alreadyOpen, final int openSinglePages ) {
        if ( !ready )
            return Admission.SKIP_NOT_READY;
        if ( alreadyOpen )
            return Admission.SKIP_ALREADY_OPEN;
        if ( !underCap( openSinglePages ) )
            return Admission.SKIP_CAP;
        return Admission.OPEN;
    }

}
