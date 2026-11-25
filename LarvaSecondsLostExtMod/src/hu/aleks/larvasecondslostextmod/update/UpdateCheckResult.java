package hu.aleks.larvasecondslostextmod.update;

/**
 * Outcome of one update check (E18-S08). Immutable.
 */
public final class UpdateCheckResult {

    /** Kind of outcome. */
    public enum Kind {
        /** The published version is not newer than the running one. */
        UP_TO_DATE,
        /** A newer version is published; {@link UpdateCheckResult#getRelease()} describes it. */
        AVAILABLE,
        /** The check did not work; {@link UpdateCheckResult#getMessage()} says why. */
        FAILED
    }

    private final Kind kind;
    private final RemoteRelease release;
    private final String message;

    private UpdateCheckResult( final Kind kind, final RemoteRelease release, final String message ) {
        this.kind = kind;
        this.release = release;
        this.message = message;
    }

    static UpdateCheckResult upToDate( final RemoteRelease release ) {
        return new UpdateCheckResult( Kind.UP_TO_DATE, release, "Version " + release.getVersion() + " is the latest" );
    }

    static UpdateCheckResult available( final RemoteRelease release ) {
        return new UpdateCheckResult( Kind.AVAILABLE, release, "Version " + release.getVersion() + " is available" );
    }

    static UpdateCheckResult failed( final String message ) {
        return new UpdateCheckResult( Kind.FAILED, null, message );
    }

    public Kind getKind() {
        return kind;
    }

    /** @return the published release; <code>null</code> if the check failed */
    public RemoteRelease getRelease() {
        return release;
    }

    /** @return a short readable text */
    public String getMessage() {
        return message;
    }

}
