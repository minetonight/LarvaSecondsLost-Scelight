package hu.aleks.larvasecondslostextmod.update;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import hu.scelightapibase.service.log.ILogger;

/**
 * Runs the online update in the background and keeps the {@link UpdateStatus} the hub shows (E18-S09, E18-S10).
 *
 * <p>One daemon worker thread does all network and disk work, so the EDT and replay parsing are never blocked. Listeners are
 * called through the UI executor (the EDT in Scelight). At start the worker removes old version folders, then (if automatic
 * checks are on and the last check is older than a day) checks once after a short delay. A failed automatic check is only
 * logged; a failed check the user started is shown.</p>
 */
public final class UpdateService {

    /** Receives status changes, on the UI executor. */
    public interface Listener {
        void statusChanged( UpdateStatus status );
    }

    /** Current time, replaceable in tests. */
    public interface Clock {
        long nowMillis();
    }

    private final UpdateChecker checker;
    private final UpdateInstaller installer;
    private final OldVersionCleaner cleaner;
    private final UpdatePreferences preferences;
    private final ModuleVersion runningVersion;
    private final Path modRootFolder;
    private final Path runningVersionFolder;
    private final ILogger logger;
    private final Executor uiExecutor;
    private final Clock clock;
    private final ScheduledExecutorService worker;
    private final List< Listener > listeners = new CopyOnWriteArrayList<>();

    private volatile UpdateStatus status;

    public UpdateService( final UpdateChecker checker, final UpdateInstaller installer, final OldVersionCleaner cleaner, final UpdatePreferences preferences,
            final ModuleVersion runningVersion, final Path modRootFolder, final Path runningVersionFolder, final ILogger logger, final Executor uiExecutor,
            final Clock clock ) {
        this.checker = checker;
        this.installer = installer;
        this.cleaner = cleaner;
        this.preferences = preferences;
        this.runningVersion = runningVersion;
        this.modRootFolder = modRootFolder;
        this.runningVersionFolder = runningVersionFolder;
        this.logger = logger;
        this.uiExecutor = uiExecutor;
        this.clock = clock;
        this.status = idle();
        final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor( 1, new ThreadFactory() {
            @Override
            public Thread newThread( final Runnable r ) {
                final Thread thread = new Thread( r, "larva-update" );
                thread.setDaemon( true );
                thread.setPriority( Thread.MIN_PRIORITY );
                return thread;
            }
        } );
        executor.setExecuteExistingDelayedTasksAfterShutdownPolicy( false );
        this.worker = executor;
    }

    /** Starts the cleanup and the automatic check. Call once, after the module started. */
    public void start() {
        submit( 0, new Runnable() {
            @Override
            public void run() {
                cleaner.clean( modRootFolder, runningVersionFolder );
            }
        } );
        if ( !preferences.isAutoCheck() ) {
            logger.debug( "[larva-update] automatic update checks are off" );
            return;
        }
        final long sinceLast = clock.nowMillis() - preferences.getLastCheckMillis();
        if ( sinceLast >= 0 && sinceLast < UpdateConfig.AUTO_CHECK_INTERVAL_MS ) {
            logger.debug( "[larva-update] last check was " + sinceLast / 60000L + " min ago, next automatic check after 24 h" );
            return;
        }
        submit( UpdateConfig.AUTO_CHECK_STARTUP_DELAY_MS, new Runnable() {
            @Override
            public void run() {
                runCheck( false );
            }
        } );
    }

    /** Checks now (the user clicked). Shows the result, also "up to date" and failures, and also a hidden version. */
    public void checkNow() {
        setStatus( new UpdateStatus( UpdateStatus.Phase.CHECKING, runningVersion, null, "Checking for updates..." ) );
        submit( 0, new Runnable() {
            @Override
            public void run() {
                runCheck( true );
            }
        } );
    }

    /** Installs the available (or failed-to-install) release. Does nothing in other phases. */
    public void install() {
        final UpdateStatus current = status;
        final RemoteRelease release = current.getRelease();
        if ( release == null || ( current.getPhase() != UpdateStatus.Phase.AVAILABLE && current.getPhase() != UpdateStatus.Phase.FAILED ) )
            return;
        setStatus( new UpdateStatus( UpdateStatus.Phase.INSTALLING, runningVersion, release, "Installing version " + release.getVersion() + "..." ) );
        submit( 0, new Runnable() {
            @Override
            public void run() {
                runInstall( release );
            }
        } );
    }

    /** Hides the available version: it is not shown again until a newer one is published (or the user checks by hand). */
    public void hideAvailable() {
        final RemoteRelease release = status.getRelease();
        if ( release == null )
            return;
        preferences.setHiddenVersion( release.getVersion() );
        setStatus( idle() );
    }

    public void setAutoCheck( final boolean autoCheck ) {
        preferences.setAutoCheck( autoCheck );
    }

    public boolean isAutoCheck() {
        return preferences.isAutoCheck();
    }

    public UpdateStatus getStatus() {
        return status;
    }

    /** Adds a listener and sends it the current status right away (on the UI executor). */
    public void addListener( final Listener listener ) {
        listeners.add( listener );
        final UpdateStatus current = status;
        uiExecutor.execute( new Runnable() {
            @Override
            public void run() {
                listener.statusChanged( current );
            }
        } );
    }

    public void removeListener( final Listener listener ) {
        listeners.remove( listener );
    }

    /** Stops the worker. A running download is abandoned; its staging folder is removed on the next start. */
    public void shutdown() {
        worker.shutdownNow();
    }

    /** Queues the automatic check without the startup delay; for tests only. */
    void runAutomaticCheckForTest() {
        submit( 0, new Runnable() {
            @Override
            public void run() {
                runCheck( false );
            }
        } );
    }

    /** Blocks until queued work is done; for tests only. */
    boolean awaitIdle( final long timeoutMillis ) throws InterruptedException {
        final java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch( 1 );
        worker.execute( new Runnable() {
            @Override
            public void run() {
                latch.countDown();
            }
        } );
        return latch.await( timeoutMillis, TimeUnit.MILLISECONDS );
    }

    private void runCheck( final boolean manual ) {
        if ( !manual && status.getPhase() != UpdateStatus.Phase.IDLE ) {
            // The user already checked, installed or is installing: an automatic result must not replace that status.
            logger.debug( "[larva-update] automatic check skipped, status is " + status.getPhase() );
            return;
        }
        final UpdateCheckResult result = checker.check();
        switch ( result.getKind() ) {
        case FAILED:
            logger.info( "[larva-update] update check failed (" + checker.getModuleXmlUrl() + "): " + result.getMessage() );
            setStatus( manual ? new UpdateStatus( UpdateStatus.Phase.FAILED, runningVersion, null, "Update check failed: " + result.getMessage() )
                    : idle() );
            return;
        case UP_TO_DATE:
            preferences.setLastCheckMillis( clock.nowMillis() );
            logger.debug( "[larva-update] " + result.getMessage() );
            setStatus( manual ? new UpdateStatus( UpdateStatus.Phase.UP_TO_DATE, runningVersion, result.getRelease(), "Version " + runningVersion
                    + " is the latest." ) : idle() );
            return;
        default:
            preferences.setLastCheckMillis( clock.nowMillis() );
            final RemoteRelease release = result.getRelease();
            logger.info( "[larva-update] " + result.getMessage() + " (running " + runningVersion + ")" );
            if ( installer.isInstalled( release ) ) {
                setStatus( installed( release ) );
                return;
            }
            final ModuleVersion hidden = preferences.getHiddenVersion();
            if ( !manual && hidden != null && !release.getVersion().isNewerThan( hidden ) ) {
                setStatus( idle() );
                return;
            }
            setStatus( new UpdateStatus( UpdateStatus.Phase.AVAILABLE, runningVersion, release, "Version " + release.getVersion() + " is available." ) );
        }
    }

    private void runInstall( final RemoteRelease release ) {
        try {
            installer.install( release, new UpdateInstaller.Progress() {
                @Override
                public void report( final String text ) {
                    setStatus( new UpdateStatus( UpdateStatus.Phase.INSTALLING, runningVersion, release, "Version " + release.getVersion() + ": " + text ) );
                }
            } );
            preferences.setHiddenVersion( null );
            setStatus( installed( release ) );
        } catch ( final UpdateException e ) {
            logger.warning( "[larva-update] install of " + release.getVersion() + " failed: " + e.getMessage(), e );
            setStatus( new UpdateStatus( UpdateStatus.Phase.FAILED, runningVersion, release, "Update failed: " + e.getMessage() ) );
        } catch ( final RuntimeException e ) {
            logger.error( "[larva-update] install of " + release.getVersion() + " failed unexpectedly", e );
            setStatus( new UpdateStatus( UpdateStatus.Phase.FAILED, runningVersion, release, "Update failed: " + e ) );
        }
    }

    private UpdateStatus idle() {
        return new UpdateStatus( UpdateStatus.Phase.IDLE, runningVersion, null, "Larva Seconds Lost " + runningVersion );
    }

    private UpdateStatus installed( final RemoteRelease release ) {
        return new UpdateStatus( UpdateStatus.Phase.INSTALLED, runningVersion, release, "Version " + release.getVersion()
                + " is installed. Restart Scelight to use it." );
    }

    private void setStatus( final UpdateStatus newStatus ) {
        status = newStatus;
        for ( final Listener listener : listeners )
            uiExecutor.execute( new Runnable() {
                @Override
                public void run() {
                    listener.statusChanged( newStatus );
                }
            } );
    }

    /** Runs a task on the worker; exceptions are logged, never lost. Ignored after {@link #shutdown()}. */
    private void submit( final long delayMillis, final Runnable task ) {
        if ( worker.isShutdown() )
            return;
        try {
            worker.schedule( new Runnable() {
                @Override
                public void run() {
                    try {
                        task.run();
                    } catch ( final RuntimeException e ) {
                        logger.error( "[larva-update] background task failed", e );
                    }
                }
            }, delayMillis, TimeUnit.MILLISECONDS );
        } catch ( final java.util.concurrent.RejectedExecutionException e ) {
            logger.debug( "[larva-update] update worker is stopped, task skipped" );
        }
    }

}
