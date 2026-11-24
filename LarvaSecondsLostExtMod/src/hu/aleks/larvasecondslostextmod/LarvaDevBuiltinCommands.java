package hu.aleks.larvasecondslostextmod;

import hu.scelightapi.gui.IMainFrame;
import hu.scelightapibase.gui.comp.multipage.IMultiPageComp;
import hu.scelightapibase.gui.comp.multipage.IPage;
import hu.scelightapibase.gui.icon.IRIcon;

import java.awt.Frame;
import java.awt.Rectangle;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractButton;
import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;

/** Commands delivered by E17-S00 (ping, dump-tree, select-larva, window-info, exit); dump-tree lists sub-pages since E17-S04. */
final class LarvaDevBuiltinCommands {

    private static final String MAIN_FRAME_TITLE_PREFIX = "Scelight";

    private final LarvaSecondsLostModule module;

    private LarvaDevBuiltinCommands( final LarvaSecondsLostModule module ) {
        this.module = module;
    }

    /** Registers all S00 commands. */
    static void registerAll( final LarvaDevCommandRegistry registry, final LarvaSecondsLostModule module ) {
        final LarvaDevBuiltinCommands c = new LarvaDevBuiltinCommands( module );
        registry.register( c.ping() );
        registry.register( c.dumpTree() );
        registry.register( c.selectLarva() );
        registry.register( c.windowInfo() );
        registry.register( c.setFrameSize() );
        registry.register( c.exit() );
        LarvaDevPageCommands.registerAll( registry, module ); // E17-S04
        LarvaDevHubCommands.registerAll( registry, module ); // E17-S05
        LarvaDevReplayCommands.registerAll( registry, module ); // E17-S06
        LarvaDevAutoOpenCommands.registerAll( registry, module ); // E17-S07
        LarvaDevMultiCommands.registerAll( registry, module ); // E17-S12
        LarvaDevMultiPageCommands.registerAll( registry, module ); // E17-S14
        LarvaDevChooserCommands.registerAll( registry, module ); // E17-S17
    }

    private IMultiPageComp multiPageComp() {
        return module.services.getMainFrame().getMultiPageComp();
    }

    private LarvaDevCommand ping() {
        return new Cmd( "ping", false ) {
            @Override
            LarvaDevCommandResult run( final List< String > args ) {
                return LarvaDevCommandResult.ok( module.manifest.getVersion().toString() );
            }
        };
    }

    private LarvaDevCommand dumpTree() {
        return new Cmd( "dump-tree", true ) {
            @Override
            LarvaDevCommandResult run( final List< String > args ) {
                final IPage< ? > larva = module.getLarvaPage();
                if ( larva == null )
                    return LarvaDevCommandResult.fail( "Larva page not installed yet" );
                final IPage< ? > selected = multiPageComp().getSelectedPage();
                final List< String > lines = new ArrayList< String >();
                appendNode( lines, larva, selected, 0 );
                final AbstractButton close = new LarvaDevUiProbe( multiPageComp().asComponent() ).findCloseButton();
                return LarvaDevCommandResult.ok( "larvaSelected=" + ( selected == larva ) + " nodes=" + lines.size() + " selected='"
                        + ( selected == null ? "null" : selected.getDisplayName() ) + "' hostCloseEnabled=" + ( close != null && close.isEnabled() ), lines );
            }
        };
    }

    private static void appendNode( final List< String > lines, final IPage< ? > page, final IPage< ? > selected, final int depth ) {
        final StringBuilder sb = new StringBuilder();
        for ( int i = 0; i < depth; i++ )
            sb.append( "  " );
        sb.append( "label='" ).append( page.getDisplayName() ).append( "' type=" ).append( typeName( page ) )
                .append( " icon=" ).append( iconName( page ) ).append( " closable=" ).append( page.isCloseable() )
                .append( page == selected ? " [SELECTED]" : "" );
        lines.add( sb.toString() );
        final List< IPage< ? > > children = page.getChildList(); // null for leaf pages
        if ( children != null )
            for ( final IPage< ? > child : children )
                appendNode( lines, child, selected, depth + 1 );
    }

    private static String iconName( final IPage< ? > page ) {
        final IRIcon icon = page.getRicon();
        if ( icon == null )
            return "none";
        // IRIcon exposes no resource name (toString is an opaque token), so the loaded size is logged as well.
        final ImageIcon image = icon.get();
        return icon + "(" + image.getIconWidth() + "x" + image.getIconHeight() + ")";
    }

    /** Simple class name; anonymous classes (Scelight's guiFactory.newPage result) fall back to the full name. */
    private static String typeName( final IPage< ? > page ) {
        final String simple = page.getClass().getSimpleName();
        if ( page instanceof LarvaSubPage )
            return simple + "(" + ( (LarvaSubPage) page ).getKind() + ")";
        return simple.isEmpty() ? page.getClass().getName() : simple;
    }

    private LarvaDevCommand selectLarva() {
        return new Cmd( "select-larva", true ) {
            @Override
            LarvaDevCommandResult run( final List< String > args ) {
                final IPage< ? > larva = module.getLarvaPage();
                if ( larva == null )
                    return LarvaDevCommandResult.fail( "Larva page not installed yet" );
                final boolean selected = multiPageComp().selectPage( larva );
                return selected ? LarvaDevCommandResult.ok( "selected" ) : LarvaDevCommandResult.fail( "selectPage returned false" );
            }
        };
    }

    private LarvaDevCommand windowInfo() {
        return new Cmd( "window-info", true ) {
            @Override
            LarvaDevCommandResult run( final List< String > args ) {
                final Frame frame = findMainFrame();
                if ( frame == null )
                    return LarvaDevCommandResult.fail( "main frame not found" );
                final Rectangle b = frame.getBounds();
                return LarvaDevCommandResult.ok( "title='" + frame.getTitle() + "' bounds=" + b.x + "," + b.y + " " + b.width + "x" + b.height
                        + " visible=" + frame.isVisible() + " state=" + frame.getExtendedState() );
            }
        };
    }

    /** {@code set-frame-size <width> <height>}: un-maximizes and resizes the main frame (E17-S15 layout checks at 1280 / 1920 px). */
    private LarvaDevCommand setFrameSize() {
        return new Cmd( "set-frame-size", true ) {
            @Override
            LarvaDevCommandResult run( final List< String > args ) {
                final Frame frame = findMainFrame();
                if ( frame == null )
                    return LarvaDevCommandResult.fail( "main frame not found" );
                final int width;
                final int height;
                try {
                    if ( args.size() != 2 )
                        throw new NumberFormatException();
                    width = Integer.parseInt( args.get( 0 ) );
                    height = Integer.parseInt( args.get( 1 ) );
                } catch ( final NumberFormatException e ) {
                    return LarvaDevCommandResult.fail( "usage: set-frame-size <width> <height>" );
                }
                frame.setExtendedState( Frame.NORMAL );
                frame.setSize( width, height );
                frame.validate();
                final Rectangle b = frame.getBounds();
                return LarvaDevCommandResult.ok( "bounds=" + b.x + "," + b.y + " " + b.width + "x" + b.height + " state=" + frame.getExtendedState() );
            }
        };
    }

    /**
     * Closes the main window through Scelight's normal close path. Scelight's close handler either runs its
     * exit sequence or, if "Minimize to Tray on Close" is on, only hides the window (see knowhow file).
     * Posted with invokeLater: the exit sequence must not run inside the channel's invokeAndWait.
     */
    private LarvaDevCommand exit() {
        return new Cmd( "exit", false ) {
            @Override
            LarvaDevCommandResult run( final List< String > args ) {
                final Frame frame = findMainFrame();
                if ( frame == null )
                    return LarvaDevCommandResult.fail( "main frame not found" );
                SwingUtilities.invokeLater( new Runnable() {
                    @Override
                    public void run() {
                        frame.dispatchEvent( new WindowEvent( frame, WindowEvent.WINDOW_CLOSING ) );
                    }
                } );
                return LarvaDevCommandResult.ok( "WINDOW_CLOSING posted to '" + frame.getTitle() + "'" );
            }
        };
    }

    /** @return the Scelight main frame (title starts with "Scelight"), or <code>null</code> */
    private Frame findMainFrame() {
        final IMainFrame mainFrame = module.services.getMainFrame();
        if ( mainFrame != null && mainFrame.asFrame() != null )
            return mainFrame.asFrame();
        for ( final Frame f : Frame.getFrames() )
            if ( f.getTitle() != null && f.getTitle().startsWith( MAIN_FRAME_TITLE_PREFIX ) )
                return f;
        return null;
    }

    /** Small base class that fixes the name and EDT flag. */
    private abstract static class Cmd implements LarvaDevCommand {
        private final String name;
        private final boolean edt;

        Cmd( final String name, final boolean edt ) {
            this.name = name;
            this.edt = edt;
        }

        abstract LarvaDevCommandResult run( List< String > args );

        @Override
        public String getName() {
            return name;
        }

        @Override
        public boolean runsOnEdt() {
            return edt;
        }

        @Override
        public LarvaDevCommandResult execute( final List< String > args ) {
            return run( args );
        }
    }

}
