package hu.aleks.larvasecondslostextmod;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Immutable headless benchmark-export record for one replay analysis.
 */
public class LarvaBenchmarkReplayRecord {

    /** Analyzed replay file path. */
    private final String replayFilePath;

    /** SHA-256 of the replay file. */
    private final String replaySha256;

    /** Short description of how the replay was selected. */
    private final String sourceDescription;

    /** Replay map title. */
    private final String mapTitle;

    /** Grouped player label. */
    private final String players;

    /** Winners label. */
    private final String winners;

    /** Replay length in milliseconds. */
    private final long replayLengthMs;

    /** Replay length in raw loops. */
    private final int replayLengthLoops;

    /** Replay end time label. */
    private final String replayEndTime;

    /** Replay version label. */
    private final String replayVersion;

    /** Replay base build label. */
    private final String baseBuild;

    /** Tells if a full replay reparse was required. */
    private final boolean fullReplayParseUsed;

    /** Tracked hatchery count from core analysis. */
    private final int trackedHatcheryCount;

    /** Assigned larva count from core analysis. */
    private final int assignedLarvaCount;

    /** Unassigned larva count from core analysis. */
    private final int unassignedLarvaCount;

    /** Inject-window count from core analysis. */
    private final int injectWindowCount;

    /** Idle-inject window count from core analysis. */
    private final int idleInjectWindowCount;

    /** Replay-level diagnostic flags. */
    private final List< String > diagnosticFlagList;

    /** Per-player benchmark export blocks. */
    private final List< LarvaBenchmarkPlayerRecord > playerRecordList;

    /**
     * Creates a new replay export record.
     */
    public LarvaBenchmarkReplayRecord( final String replayFilePath, final String replaySha256, final String sourceDescription, final String mapTitle,
            final String players, final String winners, final long replayLengthMs, final int replayLengthLoops, final String replayEndTime,
            final String replayVersion, final String baseBuild, final boolean fullReplayParseUsed, final int trackedHatcheryCount,
            final int assignedLarvaCount, final int unassignedLarvaCount, final int injectWindowCount, final int idleInjectWindowCount,
            final List< String > diagnosticFlagList, final List< LarvaBenchmarkPlayerRecord > playerRecordList ) {
        this.replayFilePath = replayFilePath;
        this.replaySha256 = replaySha256;
        this.sourceDescription = sourceDescription;
        this.mapTitle = mapTitle;
        this.players = players;
        this.winners = winners;
        this.replayLengthMs = replayLengthMs;
        this.replayLengthLoops = replayLengthLoops;
        this.replayEndTime = replayEndTime;
        this.replayVersion = replayVersion;
        this.baseBuild = baseBuild;
        this.fullReplayParseUsed = fullReplayParseUsed;
        this.trackedHatcheryCount = trackedHatcheryCount;
        this.assignedLarvaCount = assignedLarvaCount;
        this.unassignedLarvaCount = unassignedLarvaCount;
        this.injectWindowCount = injectWindowCount;
        this.idleInjectWindowCount = idleInjectWindowCount;
        this.diagnosticFlagList = diagnosticFlagList == null ? Collections.< String >emptyList()
                : Collections.unmodifiableList( new ArrayList<>( diagnosticFlagList ) );
        this.playerRecordList = playerRecordList == null ? Collections.< LarvaBenchmarkPlayerRecord >emptyList()
                : Collections.unmodifiableList( new ArrayList<>( playerRecordList ) );
    }

    public String getReplayFilePath() {
        return replayFilePath;
    }

    public String getReplaySha256() {
        return replaySha256;
    }

    public String getSourceDescription() {
        return sourceDescription;
    }

    public String getMapTitle() {
        return mapTitle;
    }

    public String getPlayers() {
        return players;
    }

    public String getWinners() {
        return winners;
    }

    public long getReplayLengthMs() {
        return replayLengthMs;
    }

    public int getReplayLengthLoops() {
        return replayLengthLoops;
    }

    public String getReplayEndTime() {
        return replayEndTime;
    }

    public String getReplayVersion() {
        return replayVersion;
    }

    public String getBaseBuild() {
        return baseBuild;
    }

    public boolean isFullReplayParseUsed() {
        return fullReplayParseUsed;
    }

    public int getTrackedHatcheryCount() {
        return trackedHatcheryCount;
    }

    public int getAssignedLarvaCount() {
        return assignedLarvaCount;
    }

    public int getUnassignedLarvaCount() {
        return unassignedLarvaCount;
    }

    public int getInjectWindowCount() {
        return injectWindowCount;
    }

    public int getIdleInjectWindowCount() {
        return idleInjectWindowCount;
    }

    public List< String > getDiagnosticFlagList() {
        return diagnosticFlagList;
    }

    public List< LarvaBenchmarkPlayerRecord > getPlayerRecordList() {
        return playerRecordList;
    }

}
