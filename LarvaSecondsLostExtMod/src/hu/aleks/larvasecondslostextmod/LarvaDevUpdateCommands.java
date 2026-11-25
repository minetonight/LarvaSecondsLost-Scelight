package hu.aleks.larvasecondslostextmod;

import java.util.ArrayList;
import java.util.List;

import hu.aleks.larvasecondslostextmod.update.RemoteRelease;
import hu.aleks.larvasecondslostextmod.update.UpdateConfig;
import hu.aleks.larvasecondslostextmod.update.UpdateService;
import hu.aleks.larvasecondslostextmod.update.UpdateStatus;

/**
 * E18 dev commands: {@code update-state}, {@code update-check} (like "Check now") and {@code update-install} (like "Install").
 * Check and install return at once; poll {@code update-state} for the result.
 */
final class LarvaDevUpdateCommands {

    private static final String NOT_AVAILABLE = "online update not available in this session (see the [larva-update] log lines)";

    private final LarvaSecondsLostModule module;

    private LarvaDevUpdateCommands( final LarvaSecondsLostModule module ) {
        this.module = module;
    }

    static void registerAll( final LarvaDevCommandRegistry registry, final LarvaSecondsLostModule module ) {
        final LarvaDevUpdateCommands c = new LarvaDevUpdateCommands( module );
        registry.register( c.state() );
        registry.register( c.check() );
        registry.register( c.install() );
    }

    private LarvaDevCommand state() {
        return new Command( "update-state" ) {
            @Override
            LarvaDevCommandResult run( final UpdateService service ) {
                final UpdateStatus status = service.getStatus();
                final List< String > lines = new ArrayList< String >();
                lines.add( "moduleXmlUrl " + UpdateConfig.moduleXmlUrl() );
                lines.add( "autoCheck " + service.isAutoCheck() );
                final RemoteRelease release = status.getRelease();
                if ( release != null )
                    lines.add( "release " + release.getVersion() + " " + release.getArchiveUrlList().get( 0 ) );
                lines.add( "hubFooter " + ( module.getHubComp() == null ? "hub not created" : "present" ) );
                return LarvaDevCommandResult.ok( "phase=" + status.getPhase() + " running=" + status.getRunningVersion() + " message='"
                        + status.getMessage() + "'", lines );
            }
        };
    }

    private LarvaDevCommand check() {
        return new Command( "update-check" ) {
            @Override
            LarvaDevCommandResult run( final UpdateService service ) {
                service.checkNow();
                return LarvaDevCommandResult.ok( "check started" );
            }
        };
    }

    private LarvaDevCommand install() {
        return new Command( "update-install" ) {
            @Override
            LarvaDevCommandResult run( final UpdateService service ) {
                final UpdateStatus.Phase phase = service.getStatus().getPhase();
                if ( phase != UpdateStatus.Phase.AVAILABLE && phase != UpdateStatus.Phase.FAILED )
                    return LarvaDevCommandResult.fail( "nothing to install, phase=" + phase );
                service.install();
                return LarvaDevCommandResult.ok( "install started" );
            }
        };
    }

    /** Runs off the EDT: the service methods only queue work and the status is a volatile snapshot. */
    private abstract class Command implements LarvaDevCommand {
        private final String name;

        Command( final String name ) {
            this.name = name;
        }

        abstract LarvaDevCommandResult run( UpdateService service );

        @Override
        public String getName() {
            return name;
        }

        @Override
        public boolean runsOnEdt() {
            return false;
        }

        @Override
        public LarvaDevCommandResult execute( final List< String > args ) {
            final UpdateService service = module.getUpdateService();
            return service == null ? LarvaDevCommandResult.fail( NOT_AVAILABLE ) : run( service );
        }
    }

}
