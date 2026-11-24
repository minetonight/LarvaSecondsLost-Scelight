package hu.aleks.larvasecondslostextmod;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryType;
import java.lang.management.MemoryUsage;

import javax.swing.SwingUtilities;

/**
 * Dev-only measurements around a batch run (E17-S12, criteria 1 and 5): an EDT heartbeat that records how late a posted task
 * runs (a stall of the UI shows up as a large delay), and a heap sampler. Started by {@code multi-run}, stopped when the run
 * ends. Not used in normal operation.
 */
final class LarvaDevRunMonitor {

    /** Heartbeat period and heap sampling period. */
    private static final long PERIOD_MS = 50L;

    /** An EDT delay above this is a stall (story criterion 1). */
    static final long STALL_THRESHOLD_MS = 200L;

    private static final long BYTES_PER_MB = 1048576L;

    private final LarvaMultiAnalysisRunner runner;
    private final long baselineUsedBytes;
    private volatile long peakUsedBytes;
    private volatile long peakLiveAfterGcBytes;
    private volatile long maxEdtDelayMs;
    private volatile int stallCount;
    private volatile int heartbeatCount;
    private volatile boolean stopped;

    /** Takes the baseline (after a GC) and starts the sampler thread; it ends when the runner is not running any more. */
    LarvaDevRunMonitor( final LarvaMultiAnalysisRunner runner ) {
        this.runner = runner;
        System.gc();
        baselineUsedBytes = usedBytes();
        peakUsedBytes = baselineUsedBytes;
        final Thread thread = new Thread( new Runnable() {
            @Override
            public void run() {
                sampleUntilDone();
            }
        }, "larva-dev-run-monitor" );
        thread.setDaemon( true );
        thread.start();
    }

    private void sampleUntilDone() {
        try {
            while ( !stopped && ( runner.getState() == LarvaMultiAnalysisRunner.State.RUNNING || runner.getState() == LarvaMultiAnalysisRunner.State.NEW ) ) {
                peakUsedBytes = Math.max( peakUsedBytes, usedBytes() );
                peakLiveAfterGcBytes = Math.max( peakLiveAfterGcBytes, liveAfterLastGcBytes() );
                postHeartbeat();
                Thread.sleep( PERIOD_MS );
            }
            peakUsedBytes = Math.max( peakUsedBytes, usedBytes() );
        } catch ( final InterruptedException e ) {
            Thread.currentThread().interrupt();
        }
    }

    private void postHeartbeat() {
        final long posted = System.nanoTime();
        SwingUtilities.invokeLater( new Runnable() {
            @Override
            public void run() {
                final long delayMs = ( System.nanoTime() - posted ) / 1000000L;
                heartbeatCount++;
                if ( delayMs > maxEdtDelayMs )
                    maxEdtDelayMs = delayMs;
                if ( delayMs > STALL_THRESHOLD_MS )
                    stallCount++;
            }
        } );
    }

    void stop() {
        stopped = true;
    }

    /**
     * Heap that survived the most recent collection of each heap pool, summed. Unlike {@code total - free} it does not count
     * garbage that the collector has not reached yet, so its peak over a run approximates the peak of live data.
     */
    private static long liveAfterLastGcBytes() {
        long sum = 0L;
        for ( final MemoryPoolMXBean pool : ManagementFactory.getMemoryPoolMXBeans() ) {
            final MemoryUsage afterGc = pool.getType() == MemoryType.HEAP ? pool.getCollectionUsage() : null;
            if ( afterGc != null )
                sum += afterGc.getUsed();
        }
        return sum;
    }

    private static long usedBytes() {
        final Runtime rt = Runtime.getRuntime();
        return rt.totalMemory() - rt.freeMemory();
    }

    /** @return one line with baseline, peak and growth in MB and the EDT numbers */
    String describe() {
        return "heapBaselineMb=" + baselineUsedBytes / BYTES_PER_MB + " heapPeakUsedMb(withGarbage)=" + peakUsedBytes / BYTES_PER_MB
                + " heapPeakLiveAfterGcMb=" + peakLiveAfterGcBytes / BYTES_PER_MB + " heapPeakLiveGrowthMb="
                + ( peakLiveAfterGcBytes - baselineUsedBytes ) / BYTES_PER_MB + " edtHeartbeats=" + heartbeatCount + " edtMaxDelayMs=" + maxEdtDelayMs
                + " edtStallsOver" + STALL_THRESHOLD_MS + "ms=" + stallCount;
    }

    long getBaselineMb() {
        return baselineUsedBytes / BYTES_PER_MB;
    }
}
