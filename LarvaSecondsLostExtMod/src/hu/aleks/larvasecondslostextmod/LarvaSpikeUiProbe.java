package hu.aleks.larvasecondslostextmod;

import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionEvent;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

/**
 * E17-S01 spike: read-only lookups in the Swing hierarchy of Scelight's page component, used to drive the real
 * close button, history buttons and Ctrl+F4 action without a mouse. Matching is by tool tip text; if Scelight
 * changes those, lookups return null and the commands FAIL (spike only, not for production).
 */
final class LarvaSpikeUiProbe {

    static final String CLOSE_TIP = "Close Page (CTRL+F4)";
    static final String BACK_TIP = "Go Back in History";
    static final String FORWARD_TIP = "Go Forward in History";
    static final String PREV_TIP = "Select Previous Page";
    static final String NEXT_TIP = "Select Next Page";

    private final JComponent root;

    LarvaSpikeUiProbe( final JComponent root ) {
        this.root = root;
    }

    AbstractButton findButton( final String toolTip ) {
        return (AbstractButton) find( root, toolTip, null );
    }

    JTree findTree() {
        return (JTree) find( root, null, JTree.class );
    }

    /** The title label is the JLabel sibling of the close button. */
    JLabel findTitleLabel() {
        final AbstractButton close = findButton( CLOSE_TIP );
        if ( close == null || close.getParent() == null )
            return null;
        for ( final Component c : close.getParent().getComponents() )
            if ( c instanceof JLabel )
                return (JLabel) c;
        return null;
    }

    /** Runs the registered Ctrl+F4 action found in an ancestor's input map; false if not found. */
    boolean fireCtrlF4() {
        final KeyStroke ks = KeyStroke.getKeyStroke( java.awt.event.KeyEvent.VK_F4, java.awt.event.InputEvent.CTRL_MASK );
        for ( Component c = root; c != null; c = c.getParent() ) {
            if ( !( c instanceof JComponent ) )
                continue;
            final InputMap im = ( (JComponent) c ).getInputMap( JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT );
            final Object key = im == null ? null : im.get( ks );
            final ActionMap am = ( (JComponent) c ).getActionMap();
            final Action a = key == null || am == null ? null : am.get( key );
            if ( a != null ) {
                a.actionPerformed( new ActionEvent( c, ActionEvent.ACTION_PERFORMED, "ctrl-f4" ) );
                return true;
            }
        }
        return false;
    }

    private static Component find( final Component c, final String toolTip, final Class< ? > type ) {
        if ( toolTip != null && c instanceof AbstractButton && toolTip.equals( ( (AbstractButton) c ).getToolTipText() ) )
            return c;
        if ( type != null && type.isInstance( c ) )
            return c;
        if ( c instanceof Container )
            for ( final Component child : ( (Container) c ).getComponents() ) {
                final Component r = find( child, toolTip, type );
                if ( r != null )
                    return r;
            }
        return null;
    }

    static boolean onEdt() {
        return SwingUtilities.isEventDispatchThread();
    }

}
