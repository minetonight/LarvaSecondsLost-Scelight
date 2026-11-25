package hu.aleks.larvasecondslostextmod.update;

/**
 * A failed update step. The message is short and readable, because it is shown to the user ("Update failed: ...").
 */
public final class UpdateException extends Exception {

    private static final long serialVersionUID = 1L;

    public UpdateException( final String message ) {
        super( message );
    }

    public UpdateException( final String message, final Throwable cause ) {
        super( message, cause );
    }

}
