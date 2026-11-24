package hu.aleks.larvasecondslostextmod;

import hu.scelightapibase.gui.comp.multipage.IPage;

import java.util.List;

import javax.swing.AbstractButton;

/**
 * E17-S04 dev commands: {@code select <label>} and {@code close <label>}. The temporary {@code add-test-page} hook was
 * removed in E17-S06 (real pages exist now: use {@code open-replay}). All commands run on the EDT and use the navigator
 * or the host's own close button.
 */
final class LarvaDevPageCommands {

    private final LarvaSecondsLostModule module;

    private LarvaDevPageCommands( final LarvaSecondsLostModule module ) {
        this.module = module;
    }

    static void registerAll( final LarvaDevCommandRegistry registry, final LarvaSecondsLostModule module ) {
        final LarvaDevPageCommands c = new LarvaDevPageCommands( module );
        registry.register( c.select() );
        registry.register( c.close() );
    }

    /** Finds the hub (label "Larva") or a sub-page by label; <code>null</code> if absent. */
    private IPage< ? > find( final String label ) {
        if ( LarvaHubPage.DISPLAY_NAME.equals( label ) )
            return module.getPageNavigator().getHub();
        return module.getPageNavigator().findSubPage( label );
    }

    private LarvaDevCommand select() {
        return new Cmd( "select" ) {
            @Override
            LarvaDevCommandResult run( final List< String > args ) {
                if ( args.size() != 1 )
                    return LarvaDevCommandResult.fail( "usage: select <label>" );
                final IPage< ? > page = find( args.get( 0 ) );
                if ( page == null )
                    return LarvaDevCommandResult.fail( "no such page '" + args.get( 0 ) + "'" );
                if ( !module.getPageNavigator().selectPage( page ) )
                    return LarvaDevCommandResult.fail( "selectPage returned false" );
                return LarvaDevCommandResult.ok( "selected '" + page.getDisplayName() + "'" );
            }
        };
    }

    /** Selects the page, then clicks the host's close button: the same handler as the X (and Ctrl+F4). */
    private LarvaDevCommand close() {
        return new Cmd( "close" ) {
            @Override
            LarvaDevCommandResult run( final List< String > args ) {
                if ( args.size() != 1 )
                    return LarvaDevCommandResult.fail( "usage: close <label>" );
                final IPage< ? > page = find( args.get( 0 ) );
                if ( page == null )
                    return LarvaDevCommandResult.fail( "no such page '" + args.get( 0 ) + "'" );
                if ( !module.getPageNavigator().selectPage( page ) )
                    return LarvaDevCommandResult.fail( "selectPage returned false" );
                final AbstractButton closeButton = new LarvaDevUiProbe( module.services.getMainFrame().getMultiPageComp().asComponent() )
                        .findCloseButton();
                if ( closeButton == null )
                    return LarvaDevCommandResult.fail( "host close button not found" );
                if ( !closeButton.isEnabled() )
                    return LarvaDevCommandResult.fail( "close button disabled for '" + page.getDisplayName() + "' (closable=" + page.isCloseable() + ")" );
                closeButton.doClick( 0 );
                final IPage< ? > now = module.services.getMainFrame().getMultiPageComp().getSelectedPage();
                return LarvaDevCommandResult.ok( "closed '" + page.getDisplayName() + "' stillOpen=" + ( find( args.get( 0 ) ) != null )
                        + " selected='" + ( now == null ? "null" : now.getDisplayName() ) + "'" );
            }
        };
    }

    private abstract static class Cmd implements LarvaDevCommand {
        private final String name;

        Cmd( final String name ) {
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
