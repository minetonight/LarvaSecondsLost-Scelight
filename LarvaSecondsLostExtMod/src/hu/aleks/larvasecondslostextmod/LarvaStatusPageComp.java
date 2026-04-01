package hu.aleks.larvasecondslostextmod;

import java.awt.BorderLayout;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/**
 * Minimal page component used to prove that the module is enabled at runtime.
 */
@SuppressWarnings("serial")
public class LarvaStatusPageComp extends JPanel {

    /**
     * Creates a new status page component.
     *
     * @param module owning module instance
     */
    public LarvaStatusPageComp( final LarvaSecondsLostModule module ) {
        super( new BorderLayout( 10, 10 ) );

        final JLabel titleLabel = new JLabel( "Larva Seconds Lost module is active.", module.getLarvaIcon().get(), SwingConstants.LEADING );
        titleLabel.setBorder( BorderFactory.createEmptyBorder( 12, 12, 0, 12 ) );

        final JLabel helpLabel = module.guiFactory.newHelpIcon( module.getHelpContent() ).asLabel();
        helpLabel.setBorder( BorderFactory.createEmptyBorder( 12, 0, 0, 12 ) );

        final JPanel headerPanel = new JPanel( new BorderLayout() );
        headerPanel.add( titleLabel, BorderLayout.CENTER );
        headerPanel.add( helpLabel, BorderLayout.EAST );

        final JLabel bodyLabel = new JLabel( buildHtmlMessage() );
        bodyLabel.setBorder( BorderFactory.createEmptyBorder( 0, 12, 12, 12 ) );
        bodyLabel.setVerticalAlignment( SwingConstants.TOP );

        add( headerPanel, BorderLayout.NORTH );
        add( bodyLabel, BorderLayout.CENTER );
    }

    /**
     * Builds the placeholder status message shown on the page.
     *
     * @return HTML message for the page body
     */
    private String buildHtmlMessage() {
        return "<html><body style='width:460px'>"
                + "<h3>Epic 01 baseline confirmed</h3>"
                + "<p>This page is the visible hello-world proof for the Larva Seconds Lost external module.</p>"
                + "<ul>"
                + "<li>Scelight can discover and instantiate the module entry point.</li>"
                + "<li>The module contributes a dedicated <b>Larva</b> page at runtime.</li>"
                + "<li>Startup and shutdown transitions are logged for troubleshooting.</li>"
                + "</ul>"
                + "<p><b>Next technical question:</b> where can a mod surface something during replay analysis?</p>"
                + "</body></html>";
    }

}
