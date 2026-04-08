package hu.aleks.larvasecondslostextmod;

import hu.scelightapibase.service.log.ILogger;

/**
 * Simple stderr logger for standalone benchmark extraction utilities.
 */
public class StdErrLogger implements ILogger {

    /** Debug enabled flag. */
    private final boolean debugEnabled;

    /** Trace enabled flag. */
    private final boolean traceEnabled;

    /**
     * Creates a new stderr logger.
     */
    public StdErrLogger() {
        this( false, false );
    }

    /**
     * Creates a new stderr logger.
     *
     * @param debugEnabled tells if debug logging is enabled
     * @param traceEnabled tells if trace logging is enabled
     */
    public StdErrLogger( final boolean debugEnabled, final boolean traceEnabled ) {
        this.debugEnabled = debugEnabled;
        this.traceEnabled = traceEnabled;
    }

    @Override
    public void error( final String msg ) {
        log( "ERROR", msg, null );
    }

    @Override
    public void error( final String msg, final Throwable t ) {
        log( "ERROR", msg, t );
    }

    @Override
    public void warning( final String msg ) {
        log( "WARN", msg, null );
    }

    @Override
    public void warning( final String msg, final Throwable t ) {
        log( "WARN", msg, t );
    }

    @Override
    public void info( final String msg ) {
        log( "INFO", msg, null );
    }

    @Override
    public void info( final String msg, final Throwable t ) {
        log( "INFO", msg, t );
    }

    @Override
    public void debug( final String msg ) {
        if ( debugEnabled )
            log( "DEBUG", msg, null );
    }

    @Override
    public void debug( final String msg, final Throwable t ) {
        if ( debugEnabled )
            log( "DEBUG", msg, t );
    }

    @Override
    public void trace( final String msg ) {
        if ( traceEnabled )
            log( "TRACE", msg, null );
    }

    @Override
    public void trace( final String msg, final Throwable t ) {
        if ( traceEnabled )
            log( "TRACE", msg, t );
    }

    @Override
    public boolean testTrace() {
        return traceEnabled;
    }

    @Override
    public boolean testDebug() {
        return debugEnabled;
    }

    /**
     * Writes one log line and optional throwable.
     */
    private void log( final String level, final String msg, final Throwable t ) {
        System.err.println( '[' + level + "] " + msg );
        if ( t != null )
            t.printStackTrace( System.err );
    }

}