package hu.aleks.larvasecondslostextmod.update;

/**
 * What the update UI shows (E18-S09). Immutable.
 */
public final class UpdateStatus {

    /** State of the online update. */
    public enum Phase {
        /** Nothing to report (no check yet, a hidden version, or an automatic check that failed). */
        IDLE,
        /** A check is running. */
        CHECKING,
        /** A check (started by the user) found nothing newer. */
        UP_TO_DATE,
        /** A newer version can be installed. */
        AVAILABLE,
        /** Download and install are running. */
        INSTALLING,
        /** The new version is installed and is used after a restart. */
        INSTALLED,
        /** The last action failed; the message says why. */
        FAILED
    }

    private final Phase phase;
    private final ModuleVersion runningVersion;
    private final RemoteRelease release;
    private final String message;

    UpdateStatus( final Phase phase, final ModuleVersion runningVersion, final RemoteRelease release, final String message ) {
        this.phase = phase;
        this.runningVersion = runningVersion;
        this.release = release;
        this.message = message;
    }

    public Phase getPhase() {
        return phase;
    }

    public ModuleVersion getRunningVersion() {
        return runningVersion;
    }

    /** @return the published release this status is about; <code>null</code> if none is known */
    public RemoteRelease getRelease() {
        return release;
    }

    /** @return the text for the user */
    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return phase + ": " + message;
    }

}
