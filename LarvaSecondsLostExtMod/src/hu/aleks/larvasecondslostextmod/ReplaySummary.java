package hu.aleks.larvasecondslostextmod;

import java.nio.file.Path;

/**
 * Immutable replay diagnostics summary shown on the module-owned replay page.
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

    /** Diagnostic note describing the fallback integration. */
    private final String integrationMode;

    /** Preview window start in milliseconds. */
    private final long previewWindowStartMs;

    /** Preview window end in milliseconds. */
    private final long previewWindowEndMs;

    /** Epic 6 larva analysis foundation report. */
    private final LarvaAnalysisReport larvaAnalysisReport;

    /**
     * Creates a new replay summary.
     *
     * @param replayFile replay file that was analyzed
     * @param sourceDescription request source
     * @param mapTitle replay map title
     * @param players grouped player string
     * @param winners winners string
     * @param length replay length
     * @param replayEndTime replay end time
     * @param replayVersion replay version string
     * @param baseBuild replay base build string
     * @param integrationMode fallback integration mode description
     */
    public ReplaySummary( final Path replayFile, final String sourceDescription, final String mapTitle, final String players, final String winners,
            final String length, final long lengthMs, final String replayEndTime, final String replayVersion, final String baseBuild,
            final String integrationMode, final long previewWindowStartMs, final long previewWindowEndMs, final LarvaAnalysisReport larvaAnalysisReport ) {
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
        this.integrationMode = integrationMode;
        this.previewWindowStartMs = previewWindowStartMs;
        this.previewWindowEndMs = previewWindowEndMs;
        this.larvaAnalysisReport = larvaAnalysisReport;
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

    public String getIntegrationMode() {
        return integrationMode;
    }

    public long getPreviewWindowStartMs() {
        return previewWindowStartMs;
    }

    public long getPreviewWindowEndMs() {
        return previewWindowEndMs;
    }

    public LarvaAnalysisReport getLarvaAnalysisReport() {
        return larvaAnalysisReport;
    }

}