package hu.aleks.larvasecondslostextmod;

import java.nio.file.Path;

/**
 * Immutable core replay metadata summary.
 *
 * <p>This model intentionally contains only replay-level metadata that is independent from
 * module-owned visualization and larva-analysis diagnostics. Page-specific diagnostics are
 * carried separately by {@link LarvaReplayPageSummary}.</p>
 */
public class ReplaySummary {

    /** Replay file that was analyzed. */
    private final Path replayFile;

    /** Description of how this replay was selected. */
    private final String sourceDescription;

    /** Replay map title. */
    private final String mapTitle;

    /** Grouped player string. */
    private final String players;

    /** Winners string. */
    private final String winners;

    /** Replay length. */
    private final String length;

    /** Replay length in milliseconds. */
    private final long lengthMs;

    /** Replay end time. */
    private final String replayEndTime;

    /** Replay version string. */
    private final String replayVersion;

    /** Base build string. */
    private final String baseBuild;

    /**
     * Creates a new replay summary.
     *
     * @param replayFile replay file that was analyzed
     * @param sourceDescription request source
     * @param mapTitle replay map title
     * @param players grouped player string
     * @param winners winners string
     * @param length replay length
         * @param lengthMs replay length in milliseconds
     * @param replayEndTime replay end time
     * @param replayVersion replay version string
     * @param baseBuild replay base build string
     */
    public ReplaySummary( final Path replayFile, final String sourceDescription, final String mapTitle, final String players, final String winners,
             final String length, final long lengthMs, final String replayEndTime, final String replayVersion, final String baseBuild ) {
        this.replayFile = replayFile;
        this.sourceDescription = sourceDescription;
        this.mapTitle = mapTitle;
        this.players = players;
        this.winners = winners;
        this.length = length;
        this.lengthMs = lengthMs;
        this.replayEndTime = replayEndTime;
        this.replayVersion = replayVersion;
        this.baseBuild = baseBuild;
    }

    public Path getReplayFile() {
        return replayFile;
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

    public String getLength() {
        return length;
    }

    public long getLengthMs() {
        return lengthMs;
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

}