package hu.aleks.larvasecondslostextmod;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Derives missed-larva threshold markers from replay-derived 3+ larva windows.
 */
public class LarvaMissedLarvaMarkerCalculator {

    /** Replay loops per second in SC2 timelines. */
    private static final int LOOPS_PER_SECOND = 16;

    /** One missed larva every 11 seconds of accumulated 3+ larva saturation. */
    static final int MISSED_LARVA_THRESHOLD_LOOPS = 11 * LOOPS_PER_SECOND;

    /**
     * Builds the missed-larva threshold markers for a hatchery.
     *
     * @param saturationWindowList replay-derived 3+ larva windows
     * @return threshold markers in chronological order
     */
    public List< LarvaTimelineMarker > buildMarkers( final List< LarvaSaturationWindow > saturationWindowList ) {
        if ( saturationWindowList == null || saturationWindowList.isEmpty() )
            return Collections.emptyList();

        final List< LarvaTimelineMarker > markerList = new ArrayList<>();
        int accumulatedLoops = 0;
        int missedLarvaCount = 0;

        for ( final LarvaSaturationWindow window : saturationWindowList ) {
            final int windowDurationLoops = window.getEndLoop() - window.getStartLoop();
            if ( windowDurationLoops <= 0 )
                continue;

            int loopsRemainingInWindow = windowDurationLoops;
            int markerLoop = window.getStartLoop();

            while ( accumulatedLoops + loopsRemainingInWindow >= MISSED_LARVA_THRESHOLD_LOOPS ) {
                final int loopsUntilThreshold = MISSED_LARVA_THRESHOLD_LOOPS - accumulatedLoops;
                markerLoop += loopsUntilThreshold;
                missedLarvaCount++;
                markerList.add( new LarvaTimelineMarker( markerLoop, loopsToMs( markerLoop ),
                        "Missed larva " + missedLarvaCount + " at " + formatLoopTime( markerLoop ), LarvaTimelineMarker.Kind.MISSED_LARVA ) );

                loopsRemainingInWindow -= loopsUntilThreshold;
                accumulatedLoops = 0;
            }

            accumulatedLoops += loopsRemainingInWindow;
        }

        return markerList;
    }

    /**
     * Formats a replay loop as a short time label.
     *
     * @param loop replay loop
     * @return formatted time label
     */
    public String formatLoopTime( final int loop ) {
        return formatMs( loopsToMs( loop ) );
    }

    /**
     * Converts loops to milliseconds.
     *
     * @param loops replay loops
     * @return milliseconds
     */
    private long loopsToMs( final int loops ) {
        if ( loops <= 0 )
            return 0L;
        return ( loops * 1000L ) / LOOPS_PER_SECOND;
    }

    /**
     * Formats a replay time as m:ss.
     *
     * @param ms time in milliseconds
     * @return formatted time
     */
    private String formatMs( final long ms ) {
        final long totalSeconds = ms / 1000L;
        final long minutes = totalSeconds / 60L;
        final long seconds = totalSeconds % 60L;
        return minutes + ":" + ( seconds < 10L ? "0" : "" ) + seconds;
    }

}