package hu.aleks.larvasecondslostextmod;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Normalized inferred inject-window diagnostics for one hatchery identity.
 */
public class HatcheryInjectTimeline {

    /** Hatchery tag. */
    private final int hatcheryTag;

    /** Formatted hatchery tag text. */
    private final String hatcheryTagText;

    /** Player owning the hatchery. */
    private final String playerName;

    /** Final hatchery type on the tag. */
    private final String hatcheryType;

    /** Tells if the hatchery was confirmed completed. */
    private final boolean completed;

    /** Completion loop, or <code>-1</code> if unknown. */
    private final int completionLoop;

    /** Completion time label, or <code>null</code> if unknown. */
    private final String completionTimeLabel;

    /** Destroyed loop, or <code>-1</code> if the hatchery survived. */
    private final int destroyedLoop;

    /** Destroyed time label, or <code>null</code> if the hatchery survived. */
    private final String destroyedTimeLabel;

    /** Number of raw inferred inject evidence points detected for this hatchery. */
    private final int rawInjectEvidenceCount;

    /** Number of previous windows discarded because a later inject overlapped them. */
    private final int overlapDiscardCount;

    /** Number of windows discarded because replay or hatchery lifetime bounds collapsed them. */
    private final int boundsDiscardCount;

    /** Number of kept windows that had to be trimmed to valid bounds. */
    private final int trimmedWindowCount;

    /** Normalized inject-active windows kept for this hatchery. */
    private final List< HatcheryInjectWindow > injectWindowList;

    /** Deterministic diagnostics explaining kept, trimmed, and discarded decisions. */
    private final List< String > diagnosticLineList;

    /**
     * Creates a new per-hatchery inject timeline.
     *
     * @param hatcheryTag hatchery tag
     * @param hatcheryTagText formatted hatchery tag text
     * @param playerName player owning the hatchery
     * @param hatcheryType final hatchery type on the tag
     * @param completed tells if the hatchery was confirmed completed
     * @param completionLoop completion loop
     * @param completionTimeLabel completion time label
     * @param destroyedLoop destroyed loop
     * @param destroyedTimeLabel destroyed time label
    * @param rawInjectEvidenceCount number of raw inferred inject evidence points detected for this hatchery
     * @param overlapDiscardCount number of overlap discards
     * @param boundsDiscardCount number of bounds discards
     * @param trimmedWindowCount number of kept windows trimmed to valid bounds
     * @param injectWindowList normalized inject-active windows kept for this hatchery
     * @param diagnosticLineList deterministic diagnostics explaining reconstruction decisions
     */
    public HatcheryInjectTimeline( final int hatcheryTag, final String hatcheryTagText, final String playerName, final String hatcheryType,
            final boolean completed, final int completionLoop, final String completionTimeLabel, final int destroyedLoop,
            final String destroyedTimeLabel, final int rawInjectEvidenceCount, final int overlapDiscardCount, final int boundsDiscardCount,
            final int trimmedWindowCount, final List< HatcheryInjectWindow > injectWindowList, final List< String > diagnosticLineList ) {
        this.hatcheryTag = hatcheryTag;
        this.hatcheryTagText = hatcheryTagText;
        this.playerName = playerName;
        this.hatcheryType = hatcheryType;
        this.completed = completed;
        this.completionLoop = completionLoop;
        this.completionTimeLabel = completionTimeLabel;
        this.destroyedLoop = destroyedLoop;
        this.destroyedTimeLabel = destroyedTimeLabel;
        this.rawInjectEvidenceCount = rawInjectEvidenceCount;
        this.overlapDiscardCount = overlapDiscardCount;
        this.boundsDiscardCount = boundsDiscardCount;
        this.trimmedWindowCount = trimmedWindowCount;
        this.injectWindowList = Collections.unmodifiableList( new ArrayList<>( injectWindowList ) );
        this.diagnosticLineList = Collections.unmodifiableList( new ArrayList<>( diagnosticLineList ) );
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

    public int getDestroyedLoop() {
        return destroyedLoop;
    }

    public String getDestroyedTimeLabel() {
        return destroyedTimeLabel;
    }

    public int getRawInjectEvidenceCount() {
        return rawInjectEvidenceCount;
    }

    public int getOverlapDiscardCount() {
        return overlapDiscardCount;
    }

    public int getBoundsDiscardCount() {
        return boundsDiscardCount;
    }

    public int getTrimmedWindowCount() {
        return trimmedWindowCount;
    }

    public List< HatcheryInjectWindow > getInjectWindowList() {
        return injectWindowList;
    }

    public List< String > getDiagnosticLineList() {
        return diagnosticLineList;
    }

    public int getKeptWindowCount() {
        return injectWindowList.size();
    }

    public int getRawInjectCommandCount() {
        return getRawInjectEvidenceCount();
    }

}