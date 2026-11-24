package hu.aleks.larvasecondslostextmod;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/** Counters and the last decisions of auto-open, for the {@code auto-open-state} dev command. Thread-safe. */
final class LarvaAutoOpenStats {

    static final int MAX_DECISIONS = 20;

    private final LinkedList< String > decisions = new LinkedList< String >();
    private int detected;
    private int droppedPendingLimit;
    private int skipped;
    private int notZerg;
    private int failedPrecheck;
    private int opened;

    synchronized void recordDetected() {
        detected++;
    }

    synchronized void recordDroppedPendingLimit( final String file ) {
        droppedPendingLimit++;
        addDecision( "dropped (pending limit) " + file );
    }

    synchronized void recordSkipped( final String reason, final String file ) {
        skipped++;
        addDecision( "skipped (" + reason + ") " + file );
    }

    synchronized void recordNotZerg( final String file ) {
        notZerg++;
        addDecision( "no Zerg player " + file );
    }

    synchronized void recordPrecheckFailed( final String file ) {
        failedPrecheck++;
        addDecision( "pre-check failed " + file );
    }

    synchronized void recordOpened( final String file, final boolean selected ) {
        opened++;
        addDecision( "opened (selected=" + selected + ") " + file );
    }

    synchronized String summary() {
        return "detected=" + detected + " opened=" + opened + " notZerg=" + notZerg + " skipped=" + skipped + " droppedPendingLimit="
                + droppedPendingLimit + " precheckFailed=" + failedPrecheck;
    }

    synchronized List< String > lastDecisions() {
        return new ArrayList< String >( decisions );
    }

    private void addDecision( final String text ) {
        decisions.add( text );
        while ( decisions.size() > MAX_DECISIONS )
            decisions.removeFirst();
    }

}
