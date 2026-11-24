package hu.aleks.larvasecondslostextmod;

/** Immutable entry of the "skipped" list of a multi-replay analysis (file and reason, e.g. {@code missing-phase-table}). */
public final class LarvaSkippedReplay {

    /** Reason code of a player-game without phase table (OQ-15). */
    public static final String REASON_MISSING_PHASE_TABLE = "missing-phase-table";

    /** Reason code of a player-game whose opponent race is unknown, so it has no matchup (user decision 2026-11-24). */
    public static final String REASON_UNKNOWN_OPPONENT_RACE = "unknown-opponent-race";

    /** Reason of a replay that Scelight could not parse (null processor, parser exception). Details may follow after a colon. */
    public static final String REASON_FAILED_TO_PARSE = "replay could not be parsed";

    private final String file;

    private final String reason;

    /**
     * @param file
     *            file name or description, never null
     * @param reason
     *            reason code, never null
     */
    public LarvaSkippedReplay( final String file, final String reason ) {
        if ( file == null || reason == null )
            throw new IllegalArgumentException( "File and reason are required." );
        this.file = file;
        this.reason = reason;
    }

    public String getFile() {
        return file;
    }

    public String getReason() {
        return reason;
    }
}
