package hu.aleks.larvasecondslostextmod;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * E17-S06 dev commands for single-replay pages. They bypass only the file chooser and the mouse and call the same
 * methods as the buttons.
 *
 * <ul>
 * <li>{@code open-replay <path>}: opens a new page, or focuses the page of an already open replay</li>
 * <li>{@code analyze-latest}: the "Analyze Latest Replay" button; FAILs (without the info dialog) if no latest replay resolves</li>
 * <li>{@code refresh <label>}: the "Refresh Current Replay" button of that page</li>
 * <li>{@code page-text <label>}: state, status line and details text of that page</li>
 * </ul>
 */
final class LarvaDevReplayCommands {

    private final LarvaSecondsLostModule module;

    private LarvaDevReplayCommands( final LarvaSecondsLostModule module ) {
        this.module = module;
    }

    static void registerAll( final LarvaDevCommandRegistry registry, final LarvaSecondsLostModule module ) {
        final LarvaDevReplayCommands c = new LarvaDevReplayCommands( module );
        registry.register( c.openReplay() );
        registry.register( c.analyzeLatest() );
        registry.register( c.refresh() );
        registry.register( c.pageText() );
    }

    private LarvaDevCommand openReplay() {
        return new EdtCommand( "open-replay" ) {
            @Override
            LarvaDevCommandResult run( final List< String > args ) {
                if ( args.size() != 1 )
                    return LarvaDevCommandResult.fail( "usage: open-replay <path>" );
                final Path file = Paths.get( args.get( 0 ) );
                if ( !Files.isRegularFile( file ) )
                    return LarvaDevCommandResult.fail( "not a file: " + file );
                final boolean alreadyOpen = module.getPageNavigator().findSingleReplayPage( file ) != null;
                return describeOpen( module.getHubActions().openChosenReplay( file ), alreadyOpen );
            }
        };
    }

    private LarvaDevCommand analyzeLatest() {
        return new EdtCommand( "analyze-latest" ) {
            @Override
            LarvaDevCommandResult run( final List< String > args ) {
                final Path file = module.resolveLatestReplayPath();
                if ( file == null )
                    return LarvaDevCommandResult.fail( "no latest replay resolvable (the button shows an info message and creates no page)" );
                final boolean alreadyOpen = module.getPageNavigator().findSingleReplayPage( file ) != null;
                return describeOpen( module.getHubActions().openLatestReplay( file ), alreadyOpen );
            }
        };
    }

    private LarvaDevCommandResult describeOpen( final LarvaReplayPageController controller, final boolean alreadyOpen ) {
        if ( controller == null )
            return LarvaDevCommandResult.fail( "page not opened (navigator not ready)" );
        final LarvaSubPage page = module.getPageNavigator().findSingleReplayPage( controller.getReplayFile() );
        return LarvaDevCommandResult.ok( ( alreadyOpen ? "focused existing page" : "opened new page" ) + " '" + ( page == null ? "?" : page.getDisplayName() )
                + "' state=" + controller.getState() + " replay=" + controller.getReplayFile() );
    }

    private LarvaDevCommand refresh() {
        return new EdtCommand( "refresh" ) {
            @Override
            LarvaDevCommandResult run( final List< String > args ) {
                if ( args.size() != 1 )
                    return LarvaDevCommandResult.fail( "usage: refresh <label>" );
                final LarvaReplayPageController controller = controllerOf( args.get( 0 ) );
                if ( controller == null )
                    return LarvaDevCommandResult.fail( "no single-replay page '" + args.get( 0 ) + "'" );
                controller.requestAnalysis( LarvaModuleHubActions.SOURCE_REFRESH );
                return LarvaDevCommandResult.ok( "refresh requested for '" + args.get( 0 ) + "' state=" + controller.getState() );
            }
        };
    }

    private LarvaDevCommand pageText() {
        return new EdtCommand( "page-text" ) {
            @Override
            LarvaDevCommandResult run( final List< String > args ) {
                if ( args.size() != 1 )
                    return LarvaDevCommandResult.fail( "usage: page-text <label>" );
                final LarvaReplayPageController controller = controllerOf( args.get( 0 ) );
                if ( controller == null )
                    return LarvaDevCommandResult.fail( "no single-replay page '" + args.get( 0 ) + "'" );
                final LarvaReplayPageView view = controller.getView();
                if ( !( view instanceof LarvaReplayPageComp ) )
                    return LarvaDevCommandResult.fail( "page component not created yet (select the page first); state=" + controller.getState() );
                final LarvaReplayPageComp comp = (LarvaReplayPageComp) view;
                final List< String > lines = new ArrayList< String >();
                lines.add( "status: " + comp.getStatusText() );
                lines.addAll( Arrays.asList( comp.getDetailsText().split( "\n" ) ) );
                return LarvaDevCommandResult.ok( "state=" + controller.getState() + " replay=" + controller.getReplayFile() + " lines=" + lines.size(), lines );
            }
        };
    }

    private LarvaReplayPageController controllerOf( final String label ) {
        final LarvaSubPage page = module.getPageNavigator().findSubPage( label );
        return page == null ? null : page.getReplayController();
    }

    private abstract static class EdtCommand implements LarvaDevCommand {
        private final String name;

        EdtCommand( final String name ) {
            this.name = name;
        }

        abstract LarvaDevCommandResult run( List< String > args );

        @Override
        public String getName() {
            return name;
        }

        @Override
        public boolean runsOnEdt() {
            return true;
        }

        @Override
        public LarvaDevCommandResult execute( final List< String > args ) {
            return run( args );
        }
    }

}
