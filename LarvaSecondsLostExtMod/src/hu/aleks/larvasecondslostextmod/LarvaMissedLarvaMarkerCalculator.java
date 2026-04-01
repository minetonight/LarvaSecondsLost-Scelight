package hu.aleks.larvasecondslostextmod;

import java.util.ArrayList;
import java.util.Comparator;
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

        final List< LarvaSaturationWindow > normalizedWindowList = normalizeWindows( saturationWindowList );
        if ( normalizedWindowList.isEmpty() )
            return Collections.emptyList();

        final List< LarvaTimelineMarker > markerList = new ArrayList<>();
        int accumulatedLoops = 0;
        int missedLarvaCount = 0;

        for ( final LarvaSaturationWindow window : normalizedWindowList ) {
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
     * Normalizes and merges windows before threshold calculation so replay truncation or duplicate
     * transitions cannot double-count missed-larva markers.
     *
     * @param originalWindowList original saturation windows
     * @return normalized, merged windows in chronological order
     */
    private List< LarvaSaturationWindow > normalizeWindows( final List< LarvaSaturationWindow > originalWindowList ) {
        final List< LarvaSaturationWindow > sortedWindowList = new ArrayList<>();
        for ( final LarvaSaturationWindow window : originalWindowList ) {
            if ( window == null || window.getEndLoop() <= window.getStartLoop() || window.getStartLoop() < 0 )
                continue;
            sortedWindowList.add( window );
        }

        if ( sortedWindowList.isEmpty() )
            return Collections.emptyList();

        Collections.sort( sortedWindowList, new Comparator< LarvaSaturationWindow >() {
            @Override
            public int compare( final LarvaSaturationWindow left, final LarvaSaturationWindow right ) {
                if ( left.getStartLoop() != right.getStartLoop() )
                    return left.getStartLoop() < right.getStartLoop() ? -1 : 1;
                return left.getEndLoop() < right.getEndLoop() ? -1 : left.getEndLoop() == right.getEndLoop() ? 0 : 1;
            }
        } );

        final List< LarvaSaturationWindow > normalizedWindowList = new ArrayList<>();
        LarvaSaturationWindow current = sortedWindowList.get( 0 );
        for ( int i = 1; i < sortedWindowList.size(); i++ ) {
            final LarvaSaturationWindow next = sortedWindowList.get( i );
            if ( next.getStartLoop() <= current.getEndLoop() )
                current = mergeWindows( current, next );
            else {
                normalizedWindowList.add( current );
                current = next;
            }
        }
        normalizedWindowList.add( current );

        return normalizedWindowList;
    }

    /**
     * Merges two overlapping or abutting saturation windows.
     *
     * @param current current merged window
     * @param next next window to merge
     * @return merged window
     */
    private LarvaSaturationWindow mergeWindows( final LarvaSaturationWindow current, final LarvaSaturationWindow next ) {
        final int startLoop = Math.min( current.getStartLoop(), next.getStartLoop() );
        final int endLoop = Math.max( current.getEndLoop(), next.getEndLoop() );
        return new LarvaSaturationWindow( startLoop, endLoop, loopsToMs( startLoop ), loopsToMs( endLoop ),
                formatLoopTime( startLoop ), formatLoopTime( endLoop ) );
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