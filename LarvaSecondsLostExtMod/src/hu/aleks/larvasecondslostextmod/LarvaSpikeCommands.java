package hu.aleks.larvasecondslostextmod;

import hu.scelightapibase.gui.comp.multipage.IMultiPageComp;
import hu.scelightapibase.gui.comp.multipage.IPage;
import hu.scelightapibase.gui.comp.multipage.IPageCompCreator;
import hu.scelightapibase.gui.icon.IRIcon;

import java.awt.Component;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractButton;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.tree.TreePath;

/**
 * E17-S01 spike commands ({@code spike-*}) for the dev command channel. Throwaway: S04 promotes the pattern into
 * production classes and deletes this file.
 */
final class LarvaSpikeCommands {

    private final LarvaSecondsLostModule module;
    private final Map< String, LarvaSpikeCounters > counters = new LinkedHashMap< String, LarvaSpikeCounters >();
    private IRIcon ownIcon;
    private IRIcon factoryIcon;
    private int burstSeq;

    private LarvaSpikeCommands( final LarvaSecondsLostModule module ) {
        this.module = module;
    }

    static void registerAll( final LarvaDevCommandRegistry registry, final LarvaSecondsLostModule module ) {
        final LarvaSpikeCommands c = new LarvaSpikeCommands( module );
        registry.register( c.addChild() );
        registry.register( c.addBurst() );
        registry.register( c.rename() );
        registry.register( c.select() );
        registry.register( c.click() );
        registry.register( c.ctrlF4() );
        registry.register( c.state() );
        registry.register( c.veto() );
        registry.register( c.resize() );
        registry.register( c.clearHistory() );
    }

    // ---- helpers -------------------------------------------------------------------------------------------

    private IMultiPageComp mpc() {
        return module.services.getMainFrame().getMultiPageComp();
    }

    private LarvaSpikeHubPage hub() {
        return (LarvaSpikeHubPage) module.getLarvaPage();
    }

    private LarvaSpikeUiProbe probe() {
        return new LarvaSpikeUiProbe( mpc().asComponent() );
    }

    private void loadIcons() {
        if ( ownIcon == null ) {
            ownIcon = module.guiFactory.newRIcon( getClass().getResource( "icon/larva-replay-icon.png" ) );
            factoryIcon = module.guiFactory.newRIcon( getClass().getResource( "icon/larva-multi-analysis-icon.png" ) );
        }
    }

    private LarvaSpikeCounters countersFor( final String name ) {
        LarvaSpikeCounters c = counters.get( name );
        if ( c == null )
            counters.put( name, c = new LarvaSpikeCounters( name, module.logger ) );
        return c;
    }

    /** Finds a page by display name anywhere under the hub (hub included); null if absent. */
    private IPage< ? > find( final String label ) {
        return find( hub(), label );
    }

    private static IPage< ? > find( final IPage< ? > page, final String label ) {
        if ( label.equals( page.getDisplayName() ) )
            return page;
        if ( page.getChildList() != null )
            for ( final IPage< ? > child : page.getChildList() ) {
                final IPage< ? > r = find( child, label );
                if ( r != null )
                    return r;
            }
        return null;
    }

    /** The question-3/-4 pattern: setParent + add to parent's list, rebuild, optionally select. EDT only. */
    private IPage< ? > addChildNow( final String name, final boolean factoryKind, final boolean select, final IPage< ? > parent ) {
        loadIcons();
        final LarvaSpikeCounters cnt = countersFor( name );
        final IPage< ? > child;
        if ( factoryKind ) {
            child = module.guiFactory.newPage( name, factoryIcon, true, new IPageCompCreator< JComponent >() {
                @Override
                public JComponent createPageComp() {
                    cnt.created.incrementAndGet();
                    cnt.event( "createPageComp(factory)" );
                    return new LarvaSpikePageComp( cnt, "Factory page: " + name );
                }
            } );
        } else
            child = new LarvaSpikePage( name, ownIcon, true, cnt );
        if ( parent instanceof LarvaSpikeHubPage )
            ( (LarvaSpikeHubPage) parent ).addChild( child );
        else
            ( (LarvaSpikePage) parent ).addChild( child );
        mpc().rebuildPageTree( false );
        if ( select )
            mpc().selectPage( child );
        return child;
    }

    // ---- commands ------------------------------------------------------------------------------------------

    /** {@code spike-add-child <name> [own|factory] [select|noselect] [parentLabel]} */
    private LarvaDevCommand addChild() {
        return new SpikeCmd( "spike-add-child" ) {
            @Override
            LarvaDevCommandResult run( final List< String > a ) {
                if ( a.isEmpty() )
                    return LarvaDevCommandResult.fail( "usage: <name> [own|factory] [select|noselect] [parentLabel]" );
                final boolean factory = a.size() > 1 && "factory".equals( a.get( 1 ) );
                final boolean select = !( a.size() > 2 && "noselect".equals( a.get( 2 ) ) );
                final IPage< ? > parent = a.size() > 3 ? find( a.get( 3 ) ) : hub();
                if ( parent == null || find( a.get( 0 ) ) != null )
                    return LarvaDevCommandResult.fail( "parent missing or name already used" );
                if ( !( parent instanceof LarvaSpikeHubPage || parent instanceof LarvaSpikePage ) )
                    return LarvaDevCommandResult.fail( "parent cannot take children (factory page)" );
                final IPage< ? > before = mpc().getSelectedPage();
                addChildNow( a.get( 0 ), factory, select, parent );
                return LarvaDevCommandResult.ok( "added '" + a.get( 0 ) + "' kind=" + ( factory ? "factory" : "own" ) + " selectedBefore="
                        + name( before ) + " selectedAfter=" + name( mpc().getSelectedPage() ) );
            }
        };
    }

    /** {@code spike-add-burst <n> [select|noselect]}: n adds posted with invokeLater from a worker thread, as S07 would. */
    private LarvaDevCommand addBurst() {
        return new SpikeCmd( "spike-add-burst" ) {
            @Override
            LarvaDevCommandResult run( final List< String > a ) {
                final int n = a.isEmpty() ? 2 : Integer.parseInt( a.get( 0 ) );
                final boolean select = !( a.size() > 1 && "noselect".equals( a.get( 1 ) ) );
                final int seq = ++burstSeq;
                new Thread( new Runnable() {
                    @Override
                    public void run() {
                        for ( int i = 1; i <= n; i++ ) {
                            final String nm = "burst" + seq + "-" + i;
                            SwingUtilities.invokeLater( new Runnable() {
                                @Override
                                public void run() {
                                    addChildNow( nm, false, select, hub() );
                                    module.logger.info( "[larva-spike] burst added " + nm + " selected=" + name( mpc().getSelectedPage() ) );
                                }
                            } );
                        }
                    }
                }, "larva-spike-burst" ).start();
                return LarvaDevCommandResult.ok( "burst of " + n + " posted" );
            }
        };
    }

    /** {@code spike-rename <label> <newName> [norebuild]} */
    private LarvaDevCommand rename() {
        return new SpikeCmd( "spike-rename" ) {
            @Override
            LarvaDevCommandResult run( final List< String > a ) {
                final IPage< ? > p = a.size() < 2 ? null : find( a.get( 0 ) );
                if ( !( p instanceof LarvaSpikePage ) )
                    return LarvaDevCommandResult.fail( "usage: <label of own page> <newName> [norebuild]" );
                final IPage< ? > before = mpc().getSelectedPage();
                ( (LarvaSpikePage) p ).setDisplayName( a.get( 1 ) );
                if ( !( a.size() > 2 && "norebuild".equals( a.get( 2 ) ) ) )
                    mpc().rebuildPageTree( false );
                return LarvaDevCommandResult.ok( "renamed '" + a.get( 0 ) + "' -> '" + a.get( 1 ) + "' selectedBefore=" + name( before )
                        + " selectedAfter=" + name( mpc().getSelectedPage() ) );
            }
        };
    }

    /** {@code spike-select <label>} */
    private LarvaDevCommand select() {
        return new SpikeCmd( "spike-select" ) {
            @Override
            LarvaDevCommandResult run( final List< String > a ) {
                final IPage< ? > p = a.isEmpty() ? null : find( a.get( 0 ) );
                if ( p == null )
                    return LarvaDevCommandResult.fail( "no such page" );
                final boolean ok = mpc().selectPage( p );
                return ok ? LarvaDevCommandResult.ok( "selected " + name( mpc().getSelectedPage() ) ) : LarvaDevCommandResult.fail( "selectPage false" );
            }
        };
    }

    /** {@code spike-click <close|back|forward|prev|next>}: doClick on the real host button (same path as the mouse). */
    private LarvaDevCommand click() {
        return new SpikeCmd( "spike-click" ) {
            @Override
            LarvaDevCommandResult run( final List< String > a ) {
                final String which = a.isEmpty() ? "" : a.get( 0 );
                final String tip = "close".equals( which ) ? LarvaSpikeUiProbe.CLOSE_TIP : "back".equals( which ) ? LarvaSpikeUiProbe.BACK_TIP
                        : "forward".equals( which ) ? LarvaSpikeUiProbe.FORWARD_TIP : "prev".equals( which ) ? LarvaSpikeUiProbe.PREV_TIP
                                : "next".equals( which ) ? LarvaSpikeUiProbe.NEXT_TIP : null;
                final AbstractButton b = tip == null ? null : probe().findButton( tip );
                if ( b == null )
                    return LarvaDevCommandResult.fail( "usage: close|back|forward|prev|next (button not found)" );
                final String sel = name( mpc().getSelectedPage() );
                if ( !b.isEnabled() )
                    return LarvaDevCommandResult.ok( which + " button DISABLED, not clicked (selected=" + sel + ")" );
                b.doClick( 0 );
                return LarvaDevCommandResult.ok( which + " clicked: " + sel + " -> " + name( mpc().getSelectedPage() ) );
            }
        };
    }

    private LarvaDevCommand ctrlF4() {
        return new SpikeCmd( "spike-ctrl-f4" ) {
            @Override
            LarvaDevCommandResult run( final List< String > a ) {
                final String sel = name( mpc().getSelectedPage() );
                if ( !probe().fireCtrlF4() )
                    return LarvaDevCommandResult.fail( "Ctrl+F4 action not found" );
                return LarvaDevCommandResult.ok( "Ctrl+F4 action fired: " + sel + " -> " + name( mpc().getSelectedPage() ) );
            }
        };
    }

    /** {@code spike-resize}: calls the API's resizeNavTree() so a long label fits the navigation tree. */
    private LarvaDevCommand resize() {
        return new SpikeCmd( "spike-resize" ) {
            @Override
            LarvaDevCommandResult run( final List< String > a ) {
                mpc().resizeNavTree();
                return LarvaDevCommandResult.ok( "resizeNavTree called, autoResize=" + mpc().isNavTreeAutoResize() + " maxAutoWidth="
                        + mpc().getNavTreeMaxAutoWidth() );
            }
        };
    }

    /** {@code spike-clear-history}: lets the close path fall back to sibling/parent selection. */
    private LarvaDevCommand clearHistory() {
        return new SpikeCmd( "spike-clear-history" ) {
            @Override
            LarvaDevCommandResult run( final List< String > a ) {
                mpc().clearHistory();
                return LarvaDevCommandResult.ok( "history cleared" );
            }
        };
    }

    /** {@code spike-veto <label> <true|false>}: make pageClosing() veto the close. */
    private LarvaDevCommand veto() {
        return new SpikeCmd( "spike-veto" ) {
            @Override
            LarvaDevCommandResult run( final List< String > a ) {
                if ( a.size() < 2 || !counters.containsKey( a.get( 0 ) ) )
                    return LarvaDevCommandResult.fail( "usage: <label> <true|false>" );
                counters.get( a.get( 0 ) ).vetoClose = Boolean.parseBoolean( a.get( 1 ) );
                return LarvaDevCommandResult.ok( "veto=" + counters.get( a.get( 0 ) ).vetoClose );
            }
        };
    }

    /** Logs host UI state (title bar, buttons, tree rows with renderer output) and all lifecycle counters. */
    private LarvaDevCommand state() {
        return new SpikeCmd( "spike-state" ) {
            @Override
            LarvaDevCommandResult run( final List< String > a ) {
                final LarvaSpikeUiProbe p = probe();
                final List< String > lines = new ArrayList< String >();
                final JLabel title = p.findTitleLabel();
                final AbstractButton close = p.findButton( LarvaSpikeUiProbe.CLOSE_TIP );
                final AbstractButton back = p.findButton( LarvaSpikeUiProbe.BACK_TIP );
                final AbstractButton fwd = p.findButton( LarvaSpikeUiProbe.FORWARD_TIP );
                lines.add( "title='" + ( title == null ? null : title.getText() ) + "' titleIcon=" + dim( title == null ? null : title.getIcon() )
                        + " closeEnabled=" + ( close != null && close.isEnabled() ) + " backEnabled=" + ( back != null && back.isEnabled() )
                        + " forwardEnabled=" + ( fwd != null && fwd.isEnabled() ) );
                appendTreeRows( lines, p.findTree() );
                for ( final LarvaSpikeCounters c : counters.values() )
                    lines.add( "counters '" + c.name + "' " + c.summary() );
                return LarvaDevCommandResult.ok( "selected=" + name( mpc().getSelectedPage() ), lines );
            }
        };
    }

    private static void appendTreeRows( final List< String > lines, final JTree tree ) {
        if ( tree == null ) {
            lines.add( "tree not found" );
            return;
        }
        for ( int row = 0; row < tree.getRowCount(); row++ ) {
            final TreePath path = tree.getPathForRow( row );
            final Object node = path.getLastPathComponent();
            final Component r = tree.getCellRenderer().getTreeCellRendererComponent( tree, node, tree.isRowSelected( row ), tree.isExpanded( row ),
                    tree.getModel().isLeaf( node ), row, false );
            final JLabel l = (JLabel) r;
            lines.add( "row " + row + " depth=" + path.getPathCount() + " text='" + l.getText() + "' icon=" + dim( l.getIcon() ) + " expanded="
                    + tree.isExpanded( row ) + " selected=" + tree.isRowSelected( row ) );
        }
    }

    private static String dim( final Icon icon ) {
        return icon == null ? "none" : icon.getIconWidth() + "x" + icon.getIconHeight();
    }

    private static String name( final IPage< ? > page ) {
        return page == null ? "null" : "'" + page.getDisplayName() + "'";
    }

    /** Spike commands all run on the EDT. */
    private abstract static class SpikeCmd implements LarvaDevCommand {
        private final String name;

        SpikeCmd( final String name ) {
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
