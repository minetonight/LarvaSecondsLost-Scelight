package hu.aleks.larvasecondslostextmod;

import java.awt.Component;
import java.awt.Container;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import hu.scelightapibase.gui.comp.IPopupIcon;

/**
 * E17-S05 dev commands: {@code hub-state} (buttons, checkbox, tooltip) and {@code set-ladder-only <true|false>}.
 * Both need the hub content, which Scelight creates lazily when the hub is first shown ({@code select Larva}).
 */
final class LarvaDevHubCommands {

    private static final String NOT_CREATED = "hub component not created yet (select Larva first)";

    private final LarvaSecondsLostModule module;

    private LarvaDevHubCommands( final LarvaSecondsLostModule module ) {
        this.module = module;
    }

    static void registerAll( final LarvaDevCommandRegistry registry, final LarvaSecondsLostModule module ) {
        final LarvaDevHubCommands c = new LarvaDevHubCommands( module );
        registry.register( c.hubState() );
        registry.register( c.setLadderOnly() );
        registry.register( c.showHelp() );
    }

    private LarvaDevCommand hubState() {
        return new EdtCommand( "hub-state" ) {
            @Override
            LarvaDevCommandResult run( final List< String > args ) {
                final LarvaHubPageComp comp = module.getHubComp();
                if ( comp == null )
                    return LarvaDevCommandResult.fail( NOT_CREATED );
                final List< String > lines = new ArrayList< String >();
                for ( final String label : comp.getButtonLabels() )
                    lines.add( "button '" + label + "'" );
                lines.add( "tipTooltip '" + comp.getTipToolTip() + "'" );
                lines.add( "tipText '" + LarvaHubPageComp.MULTI_TIP_TEXT + "'" );
                lines.add( "size " + comp.getWidth() + "x" + comp.getHeight() + " showing=" + comp.isShowing() );
                return LarvaDevCommandResult.ok( "ladderOnly=" + comp.isLadderOnly() + " buttons=" + comp.getButtonLabels().length, lines );
            }
        };
    }

    private LarvaDevCommand setLadderOnly() {
        return new EdtCommand( "set-ladder-only" ) {
            @Override
            LarvaDevCommandResult run( final List< String > args ) {
                if ( args.size() != 1 || !( "true".equals( args.get( 0 ) ) || "false".equals( args.get( 0 ) ) ) )
                    return LarvaDevCommandResult.fail( "usage: set-ladder-only <true|false>" );
                final LarvaHubPageComp comp = module.getHubComp();
                if ( comp == null )
                    return LarvaDevCommandResult.fail( NOT_CREATED );
                comp.setLadderOnly( Boolean.parseBoolean( args.get( 0 ) ) );
                return LarvaDevCommandResult.ok( "ladderOnly=" + module.isLadderOnly() );
            }
        };
    }

    /** {@code show-help [hide]}: opens (or closes) the help popup of the hub's help icon, as a click on it would. */
    private LarvaDevCommand showHelp() {
        return new EdtCommand( "show-help" ) {
            @Override
            LarvaDevCommandResult run( final List< String > args ) {
                final LarvaHubPageComp comp = module.getHubComp();
                if ( comp == null )
                    return LarvaDevCommandResult.fail( NOT_CREATED );
                final IPopupIcon icon = findPopupIcon( comp );
                if ( icon == null )
                    return LarvaDevCommandResult.fail( "no help icon in the hub" );
                if ( !args.isEmpty() && "hide".equals( args.get( 0 ) ) ) {
                    icon.hidePopup();
                    return LarvaDevCommandResult.ok( "help popup hidden" );
                }
                final Component source = (Component) icon;
                icon.showPopup( new MouseEvent( source, MouseEvent.MOUSE_PRESSED, System.currentTimeMillis(), 0, 1, 1, 1, false ) );
                return LarvaDevCommandResult.ok( "help popup shown" );
            }
        };
    }

    private static IPopupIcon findPopupIcon( final Container parent ) {
        for ( final Component child : parent.getComponents() ) {
            if ( child instanceof IPopupIcon && child.getClass().getName().endsWith( "HelpIcon" ) )
                return (IPopupIcon) child;
            if ( child instanceof Container ) {
                final IPopupIcon found = findPopupIcon( (Container) child );
                if ( found != null )
                    return found;
            }
        }
        return null;
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
