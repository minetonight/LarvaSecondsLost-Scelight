package hu.aleks.larvasecondslostextmod;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Per-hatchery larva count timeline produced by the Epic 6 replay analysis foundation.
 */
public class HatcheryLarvaTimeline {

    /** One larva-count sample. */
    public static class CountPoint {

        /** Replay loop of the sample. */
        private final int loop;

        /** Formatted replay time. */
        private final String timeLabel;

        /** Larva count after the change. */
        private final int larvaCount;

        /**
         * Creates a new count point.
         *
         * @param loop replay loop of the sample
         * @param timeLabel formatted replay time
         * @param larvaCount larva count after the change
         */
        public CountPoint( final int loop, final String timeLabel, final int larvaCount ) {
            this.loop = loop;
            this.timeLabel = timeLabel;
            this.larvaCount = larvaCount;
        }

        public int getLoop() {
            return loop;
        }

        public String getTimeLabel() {
            return timeLabel;
        }

        public int getLarvaCount() {
            return larvaCount;
        }

    }

    /** Hatchery tag. */
    private final int hatcheryTag;

    /** Hatchery tag formatted with the active tag transformation. */
    private final String hatcheryTagText;

    /** Player name owning the hatchery. */
    private final String playerName;

    /** Last known hatchery type. */
    private final String hatcheryType;

    /** Tells if the hatchery completed at any point. */
    private final boolean completed;

    /** Completion loop, or <code>-1</code> if unknown. */
    private final int completionLoop;

    /** Completion time label, or <code>null</code> if unknown. */
    private final String completionTimeLabel;

    /** First replay loop where at least one larva was assigned to this hatchery. */
    private final int firstLarvaLoop;

    /** First larva time label, or <code>null</code> if no larva were assigned. */
    private final String firstLarvaTimeLabel;

    /** Destroyed loop, or <code>-1</code> if the hatchery survived to replay end. */
    private final int destroyedLoop;

    /** Destroyed time label, or <code>null</code> if the hatchery survived to replay end. */
    private final String destroyedTimeLabel;

    /** Count changes over time. */
    private final List< CountPoint > countPointList;

    /** Maximum larva count reached on this hatchery timeline. */
    private final int maxLarvaCount;

    /** Direct creator-based assignments. */
    private final int directAssignmentCount;

    /** Inject-correlated assignments. */
    private final int injectCorrelatedAssignmentCount;

    /** Pure heuristic assignments. */
    private final int heuristicAssignmentCount;

    /**
     * Creates a new hatchery larva timeline.
     *
     * @param hatcheryTag hatchery tag
     * @param hatcheryTagText formatted hatchery tag
     * @param playerName player name
     * @param hatcheryType last known hatchery type
     * @param completed tells if the hatchery completed at any point
     * @param completionLoop completion loop, or <code>-1</code> if unknown
     * @param completionTimeLabel completion time label
     * @param firstLarvaLoop first assigned larva loop, or <code>-1</code> if no larva were assigned
     * @param firstLarvaTimeLabel first assigned larva time label
     * @param destroyedLoop destroyed loop, or <code>-1</code> if the hatchery survived to replay end
     * @param destroyedTimeLabel destroyed time label
     * @param countPointList count changes over time
     * @param maxLarvaCount maximum larva count reached
     * @param directAssignmentCount direct creator-based assignments
     * @param injectCorrelatedAssignmentCount inject-correlated assignments
     * @param heuristicAssignmentCount heuristic assignments
     */
    public HatcheryLarvaTimeline( final int hatcheryTag, final String hatcheryTagText, final String playerName, final String hatcheryType,
            final boolean completed, final int completionLoop, final String completionTimeLabel, final int firstLarvaLoop,
            final String firstLarvaTimeLabel, final int destroyedLoop, final String destroyedTimeLabel,
            final List< CountPoint > countPointList, final int maxLarvaCount, final int directAssignmentCount,
            final int injectCorrelatedAssignmentCount, final int heuristicAssignmentCount ) {
        this.hatcheryTag = hatcheryTag;
        this.hatcheryTagText = hatcheryTagText;
        this.playerName = playerName;
        this.hatcheryType = hatcheryType;
        this.completed = completed;
        this.completionLoop = completionLoop;
        this.completionTimeLabel = completionTimeLabel;
        this.firstLarvaLoop = firstLarvaLoop;
        this.firstLarvaTimeLabel = firstLarvaTimeLabel;
        this.destroyedLoop = destroyedLoop;
        this.destroyedTimeLabel = destroyedTimeLabel;
        this.countPointList = Collections.unmodifiableList( new ArrayList<>( countPointList ) );
        this.maxLarvaCount = maxLarvaCount;
        this.directAssignmentCount = directAssignmentCount;
        this.injectCorrelatedAssignmentCount = injectCorrelatedAssignmentCount;
        this.heuristicAssignmentCount = heuristicAssignmentCount;
    }

    public int getHatcheryTag() {
        return hatcheryTag;
    }

    public String getHatcheryTagText() {
        return hatcheryTagText;
    }

    public String getPlayerName() {
        return playerName;
    }

    public String getHatcheryType() {
        return hatcheryType;
    }

    public boolean isCompleted() {
        return completed;
    }

    public int getCompletionLoop() {
        return completionLoop;
    }

    public String getCompletionTimeLabel() {
        return completionTimeLabel;
    }

    public int getFirstLarvaLoop() {
        return firstLarvaLoop;
    }

    public String getFirstLarvaTimeLabel() {
        return firstLarvaTimeLabel;
    }

    public int getDestroyedLoop() {
        return destroyedLoop;
    }

    public String getDestroyedTimeLabel() {
        return destroyedTimeLabel;
    }

    public List< CountPoint > getCountPointList() {
        return countPointList;
    }

    public int getMaxLarvaCount() {
        return maxLarvaCount;
    }

    public int getDirectAssignmentCount() {
        return directAssignmentCount;
    }

    public int getInjectCorrelatedAssignmentCount() {
        return injectCorrelatedAssignmentCount;
    }

    public int getHeuristicAssignmentCount() {
        return heuristicAssignmentCount;
    }

    public int getCreatedLarvaCount() {
        return directAssignmentCount + injectCorrelatedAssignmentCount + heuristicAssignmentCount;
    }

}