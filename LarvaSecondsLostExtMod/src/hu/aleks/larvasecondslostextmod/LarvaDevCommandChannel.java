package hu.aleks.larvasecondslostextmod;

import hu.scelightapibase.service.log.ILogger;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.SwingUtilities;

/**
 * Development-only command channel: polls a text file for new lines and executes them as commands
 * inside the running Scelight, so an agent can drive and verify the module without clicking.
 *
 * <p>Off by default: it exists only if the system property {@value #PROP_FILE} is set.
 * Results are written to the module log as {@code [larva-dev] <id> OK|FAIL <command> ...}.
 * See knowhow/running-scelight.md.</p>
 */
final class LarvaDevCommandChannel {

    /** System property holding the command file path; enables the channel. */
    static final String PROP_FILE = "larva.dev.commands.file";

    private static final long POLL_INTERVAL_MS = 500L;
    private static final long STOP_JOIN_TIMEOUT_MS = 2000L;
    private static final String LOG_PREFIX = "[larva-dev] ";

    private final ILogger logger;
    private final LarvaDevCommandFileTail tail;
    private final LarvaDevCommandRegistry registry;
    private volatile boolean stopRequested;
    private Thread thread;

    LarvaDevCommandChannel( final ILogger logger, final Path file, final LarvaDevCommandRegistry registry ) {
        this.logger = logger;
        this.tail = new LarvaDevCommandFileTail( file );
        this.registry = registry;
    }

    /** @return the configured command file, or <code>null</code> if the channel is disabled */
    static Path configuredFile() {
        final String value = System.getProperty( PROP_FILE );
        return value == null || value.trim().isEmpty() ? null : Paths.get( value.trim() );
    }

    /** Skips existing file content and starts the polling thread. */
    void start() {
        try {
            tail.skipExistingContent();
        } catch ( final IOException e ) {
            logger.error( LOG_PREFIX + "cannot read command file, channel not started", e );
            return;
        }
        thread = new Thread( new Runnable() {
            @Override
            public void run() {
                pollLoop();
            }
        }, "larva-dev-command-channel" );
        thread.setDaemon( true );
        thread.start();
        logger.info( LOG_PREFIX + "channel started, offset=" + tail.getOffset() );
    }

    /** Stops the polling thread; safe to call more than once. */
    void stop() {
        stopRequested = true;
        final Thread t = thread;
        if ( t == null )
            return;
        t.interrupt();
        if ( t != Thread.currentThread() ) {
            try {
                t.join( STOP_JOIN_TIMEOUT_MS );
            } catch ( final InterruptedException e ) {
                Thread.currentThread().interrupt();
            }
        }
        logger.info( LOG_PREFIX + "channel stopped" );
    }

    private void pollLoop() {
        while ( !stopRequested ) {
            try {
                final List< String > lines = tail.readNewLines();
                for ( final String line : lines ) {
                    if ( stopRequested )
                        break;
                    processLine( line );
                }
            } catch ( final Throwable t ) { // the channel thread must never die
                logger.error( LOG_PREFIX + "poll failed", t );
            }
            try {
                Thread.sleep( POLL_INTERVAL_MS );
            } catch ( final InterruptedException e ) {
                if ( stopRequested )
                    return;
            }
        }
    }

    private void processLine( final String rawLine ) {
        LarvaDevCommandLine line;
        try {
            line = LarvaDevCommandLine.parse( rawLine );
        } catch ( final IllegalArgumentException e ) {
            logger.info( LOG_PREFIX + "? FAIL ? bad line '" + rawLine + "': " + e.getMessage() );
            return;
        }
        if ( line == null )
            return;

        final String id = line.getId();
        final String name = line.getCommand();
        LarvaDevCommandResult result;
        try {
            result = execute( line );
        } catch ( final Throwable t ) {
            logger.error( LOG_PREFIX + id + " command " + name + " threw an exception", t );
            result = LarvaDevCommandResult.fail( "exception " + t );
        }
        report( id, name, result );
    }

    private LarvaDevCommandResult execute( final LarvaDevCommandLine line ) throws Exception {
        final LarvaDevCommand command = registry.find( line.getCommand().toLowerCase() );
        if ( command == null )
            return LarvaDevCommandResult.fail( "unknown command" );
        if ( !command.runsOnEdt() )
            return command.execute( line.getArgs() );
        return executeOnEdt( command, line.getArgs() );
    }

    private LarvaDevCommandResult executeOnEdt( final LarvaDevCommand command, final List< String > args ) throws Exception {
        final AtomicReference< LarvaDevCommandResult > resultRef = new AtomicReference< LarvaDevCommandResult >();
        final AtomicReference< Throwable > errorRef = new AtomicReference< Throwable >();
        SwingUtilities.invokeAndWait( new Runnable() {
            @Override
            public void run() {
                try {
                    resultRef.set( command.execute( args ) );
                } catch ( final Throwable t ) {
                    errorRef.set( t );
                }
            }
        } );
        if ( errorRef.get() != null ) {
            final Throwable t = errorRef.get();
            throw t instanceof Exception ? (Exception) t : new RuntimeException( t );
        }
        return resultRef.get();
    }

    private void report( final String id, final String name, final LarvaDevCommandResult result ) {
        logger.info( LOG_PREFIX + id + ( result.isOk() ? " OK " : " FAIL " ) + name + " " + result.getSummary() );
        for ( final String detail : result.getDetailLines() )
            logger.info( LOG_PREFIX + id + " | " + detail );
    }

}
