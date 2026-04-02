package hu.aleks.larvasecondslostextmod;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.List;

/**
 * Converts per-hatchery larva count timelines into stable replay-derived 3+ larva windows.
 */
public class LarvaSaturationWindowCalculator {

    /** Replay loops per second in SC2 timelines. */
    private static final long LOOPS_PER_SECOND = 16L;

    /** Missing loop sentinel. */
    private static final int NO_LOOP = -1;

    /**
     * Resolves the visible row start loop for a hatchery timeline.
     *
     * @param timeline hatchery timeline
     * @return visible start loop, or <code>-1</code> if unavailable
     */
    public int resolveVisibleStartLoop( final HatcheryLarvaTimeline timeline ) {
        if ( timeline == null || !timeline.isCompleted() || timeline.getFirstLarvaLoop() < 0 )
            return NO_LOOP;

        return Math.max( timeline.getCompletionLoop(), timeline.getFirstLarvaLoop() );
    }

    /**
     * Resolves the visible row end loop for a hatchery timeline.
     *
     * @param timeline hatchery timeline
     * @param replayEndLoop replay end loop from the replay header
     * @return visible end loop, or <code>-1</code> if unavailable
     */
    public int resolveVisibleEndLoop( final HatcheryLarvaTimeline timeline, final int replayEndLoop ) {
        if ( timeline == null )
            return NO_LOOP;

        if ( replayEndLoop <= 0 )
            return NO_LOOP;

        if ( timeline.getDestroyedLoop() >= 0 )
            return Math.min( timeline.getDestroyedLoop(), replayEndLoop );

        return replayEndLoop;
    }

    /**
     * Converts a hatchery timeline into stable 3+ larva windows.
     *
     * @param timeline hatchery timeline
     * @param replayEndLoop replay end loop from the replay header
     * @return stable saturation windows
     */
    public List< LarvaSaturationWindow > buildWindows( final HatcheryLarvaTimeline timeline, final int replayEndLoop ) {
        if ( timeline == null || timeline.getCountPointList().isEmpty() )
            return Collections.emptyList();

        final int visibleStartLoop = resolveVisibleStartLoop( timeline );
        final int visibleEndLoop = resolveVisibleEndLoop( timeline, replayEndLoop );
        if ( visibleStartLoop < 0 || visibleEndLoop <= visibleStartLoop )
            return Collections.emptyList();

        final List< HatcheryLarvaTimeline.CountPoint > pointList = normalizeCountPoints( timeline.getCountPointList(), visibleEndLoop );
        if ( pointList.isEmpty() )
            return Collections.emptyList();

        final List< LarvaSaturationWindow > windowList = new ArrayList<>();
        Integer openWindowStartLoop = null;

        for ( final HatcheryLarvaTimeline.CountPoint point : pointList ) {
            if ( point.getLoop() > visibleEndLoop )
                break;

            final int pointLoop = point.getLoop();
            if ( point.getLarvaCount() >= 3 ) {
                if ( openWindowStartLoop == null && pointLoop <= visibleEndLoop )
                    openWindowStartLoop = Integer.valueOf( Math.max( pointLoop, visibleStartLoop ) );
            } else if ( openWindowStartLoop != null ) {
                addWindow( windowList, openWindowStartLoop.intValue(), Math.max( pointLoop, visibleStartLoop ) );
                openWindowStartLoop = null;
            }
        }

        if ( openWindowStartLoop != null )
            addWindow( windowList, openWindowStartLoop.intValue(), visibleEndLoop );

        return windowList;
    }

    /**
     * Normalizes count points so downstream calculations stay deterministic even if replay data
     * contains duplicate loops, sparse tails, or slightly out-of-order transitions.
     *
     * @param originalPointList raw count points
     * @param visibleEndLoop visible row end loop
     * @return normalized count points sorted by loop and deduplicated by last value per loop
     */
    private List< HatcheryLarvaTimeline.CountPoint > normalizeCountPoints( final List< HatcheryLarvaTimeline.CountPoint > originalPointList,
            final int visibleEndLoop ) {
        if ( originalPointList == null || originalPointList.isEmpty() )
            return Collections.emptyList();

        final List< HatcheryLarvaTimeline.CountPoint > sortedPointList = new ArrayList<>();
        for ( final HatcheryLarvaTimeline.CountPoint point : originalPointList ) {
            if ( point == null || point.getLoop() < 0 || point.getLoop() > visibleEndLoop )
                continue;
            sortedPointList.add( point );
        }

        if ( sortedPointList.isEmpty() )
            return Collections.emptyList();

        Collections.sort( sortedPointList, new Comparator< HatcheryLarvaTimeline.CountPoint >() {
            @Override
            public int compare( final HatcheryLarvaTimeline.CountPoint left, final HatcheryLarvaTimeline.CountPoint right ) {
                return left.getLoop() < right.getLoop() ? -1 : left.getLoop() == right.getLoop() ? 0 : 1;
            }
        } );

        final List< HatcheryLarvaTimeline.CountPoint > normalizedPointList = new ArrayList<>( sortedPointList.size() );
        for ( final HatcheryLarvaTimeline.CountPoint point : sortedPointList ) {
            if ( !normalizedPointList.isEmpty() && normalizedPointList.get( normalizedPointList.size() - 1 ).getLoop() == point.getLoop() )
                normalizedPointList.set( normalizedPointList.size() - 1, point );
            else
                normalizedPointList.add( point );
        }

        return normalizedPointList;
    }

    /**
     * Converts loops to milliseconds.
     *
     * @param loops replay loops
     * @return milliseconds
     */
    public long loopsToMs( final int loops ) {
        if ( loops <= 0 )
            return 0L;
        return ( loops * 1000L ) / LOOPS_PER_SECOND;
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
     * Formats a replay time as m:ss.
     *
     * @param ms time in milliseconds
     * @return formatted time
     */
    public String formatMs( final long ms ) {
        final long totalSeconds = ms / 1000L;
        final long minutes = totalSeconds / 60L;
        final long seconds = totalSeconds % 60L;
        return minutes + ":" + ( seconds < 10L ? "0" : "" ) + seconds;
    }

    /**
     * Adds one saturation window if the range has positive duration.
     *
     * @param windowList target window list
     * @param startLoop window start loop
     * @param endLoop window end loop
     */
    private void addWindow( final List< LarvaSaturationWindow > windowList, final int startLoop, final int endLoop ) {
        if ( endLoop <= startLoop )
            return;

        windowList.add( new LarvaSaturationWindow( startLoop, endLoop, loopsToMs( startLoop ), loopsToMs( endLoop ),
                formatLoopTime( startLoop ), formatLoopTime( endLoop ) ) );
    }

}