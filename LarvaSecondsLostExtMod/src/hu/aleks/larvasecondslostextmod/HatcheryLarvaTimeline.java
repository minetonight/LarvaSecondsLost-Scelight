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
     * @param countPointList count changes over time
     * @param maxLarvaCount maximum larva count reached
     * @param directAssignmentCount direct creator-based assignments
     * @param injectCorrelatedAssignmentCount inject-correlated assignments
     * @param heuristicAssignmentCount heuristic assignments
     */
    public HatcheryLarvaTimeline( final int hatcheryTag, final String hatcheryTagText, final String playerName, final String hatcheryType,
            final List< CountPoint > countPointList, final int maxLarvaCount, final int directAssignmentCount,
            final int injectCorrelatedAssignmentCount, final int heuristicAssignmentCount ) {
        this.hatcheryTag = hatcheryTag;
        this.hatcheryTagText = hatcheryTagText;
        this.playerName = playerName;
        this.hatcheryType = hatcheryType;
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

}