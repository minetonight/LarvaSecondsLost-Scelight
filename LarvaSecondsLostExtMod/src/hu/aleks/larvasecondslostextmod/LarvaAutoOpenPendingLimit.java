package hu.aleks.larvasecondslostextmod;

import java.util.concurrent.atomic.AtomicInteger;

/** Thread-safe counter of auto-open requests waiting in the queue (D17-e pending limit). */
final class LarvaAutoOpenPendingLimit {

    private final int max;
    private final AtomicInteger pending = new AtomicInteger();

    LarvaAutoOpenPendingLimit( final int max ) {
        this.max = max;
    }

    /** @return <code>true</code> and counts the request if fewer than max requests wait; <code>false</code> to drop it */
    boolean tryAcquire() {
        while ( true ) {
            final int current = pending.get();
            if ( current >= max )
                return false;
            if ( pending.compareAndSet( current, current + 1 ) )
                return true;
        }
    }

    /** Called when a counted request leaves the waiting state (starts running or is discarded). */
    void release() {
        if ( pending.decrementAndGet() < 0 )
            pending.set( 0 );
    }

    int getPending() {
        return pending.get();
    }

    int getMax() {
        return max;
    }

}
