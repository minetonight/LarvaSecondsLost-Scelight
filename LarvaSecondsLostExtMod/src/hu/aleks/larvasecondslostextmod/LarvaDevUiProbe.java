package hu.aleks.larvasecondslostextmod;

import java.awt.Component;
import java.awt.Container;

import javax.swing.AbstractButton;
import javax.swing.JComponent;

/**
 * Dev-channel helper: read-only lookup of Scelight's page close button in the Swing hierarchy of the multi page
 * component, so {@code close <label>} runs the very same handler as a click on the X. Matching is by tool tip
 * text; if Scelight changes it, the lookup returns <code>null</code> and the command fails (dev tooling only).
 */
final class LarvaDevUiProbe {

    static final String CLOSE_TIP = "Close Page (CTRL+F4)";

    private final JComponent root;

    LarvaDevUiProbe( final JComponent root ) {
        this.root = root;
    }

    /** @return the host's close button, or <code>null</code> if not found */
    AbstractButton findCloseButton() {
        return find( root );
    }

    private static AbstractButton find( final Component c ) {
        if ( c instanceof AbstractButton && CLOSE_TIP.equals( ( (AbstractButton) c ).getToolTipText() ) )
            return (AbstractButton) c;
        if ( c instanceof Container )
            for ( final Component child : ( (Container) c ).getComponents() ) {
                final AbstractButton r = find( child );
                if ( r != null )
                    return r;
            }
        return null;
    }

}
