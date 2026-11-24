package hu.aleks.larvasecondslostextmod;

import hu.scelightapibase.service.log.ILogger;

import java.util.concurrent.atomic.AtomicInteger;

/** E17-S01 spike: lifecycle event counters of one spike page, kept after the page is closed. Delete or promote in S04. */
final class LarvaSpikeCounters {

    final String name;
    final AtomicInteger created = new AtomicInteger();
    final AtomicInteger selected = new AtomicInteger();
    final AtomicInteger closing = new AtomicInteger();
    final AtomicInteger disposed = new AtomicInteger();
    /** If true, {@code pageClosing()} vetoes the close. */
    volatile boolean vetoClose;

    private final ILogger logger;

    LarvaSpikeCounters( final String name, final ILogger logger ) {
        this.name = name;
        this.logger = logger;
    }

    void event( final String event ) {
        logger.info( "[larva-spike] event page='" + name + "' " + event + " " + summary() );
    }

    String summary() {
        return "(created=" + created + " selected=" + selected + " closing=" + closing + " disposed=" + disposed + ")";
    }

}
