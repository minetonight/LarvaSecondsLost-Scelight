package hu.aleks.larvasecondslostextmod;

/** Immutable entry of the "skipped" list of a multi-replay analysis (file and reason, e.g. {@code missing-phase-table}). */
public final class LarvaSkippedReplay {

    /** Reason code of a player-game without phase table (OQ-15). */
    public static final String REASON_MISSING_PHASE_TABLE = "missing-phase-table";

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
