package hu.aleks.larvasecondslostextmod.update;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * Update row at the bottom of the Larva hub (E18-S09): the status text with the buttons that fit it on the left,
 * "Check for updates automatically" and "Check now" on the right. Create and use on the EDT.
 */
@SuppressWarnings( "serial" )
public final class UpdatePanel extends JPanel implements UpdateService.Listener {

    static final String INSTALL_LABEL = "Install";
    static final String RETRY_LABEL = "Try again";
    static final String WHATS_NEW_LABEL = "What's new";
    static final String DOWNLOAD_PAGE_LABEL = "Open download page";
    static final String HIDE_LABEL = "Hide";
    static final String CHECK_NOW_LABEL = "Check now";
    static final String AUTO_CHECK_LABEL = "Check for updates automatically";

    /** Opens the module home page in the browser. */
    public interface HomePageOpener {
        void openHomePage();
    }

    private final UpdateService service;
    private final JLabel statusLabel = new JLabel();
    private final JButton installButton = new JButton( INSTALL_LABEL );
    private final JButton homePageButton = new JButton( WHATS_NEW_LABEL );
    private final JButton hideButton = new JButton( HIDE_LABEL );
    private final JButton checkNowButton = new JButton( CHECK_NOW_LABEL );
    private final JCheckBox autoCheckBox = new JCheckBox( AUTO_CHECK_LABEL );

    public UpdatePanel( final UpdateService service, final HomePageOpener opener ) {
        super( new BorderLayout() );
        this.service = service;
        setBorder( BorderFactory.createEmptyBorder( 6, 0, 0, 0 ) );

        final JPanel left = new JPanel( new FlowLayout( FlowLayout.LEFT, 6, 0 ) );
        left.add( statusLabel );
        left.add( installButton );
        left.add( homePageButton );
        left.add( hideButton );
        final JPanel right = new JPanel( new FlowLayout( FlowLayout.RIGHT, 6, 0 ) );
        right.add( autoCheckBox );
        right.add( checkNowButton );
        add( left, BorderLayout.CENTER );
        add( right, BorderLayout.EAST );

        autoCheckBox.setSelected( service.isAutoCheck() );
        autoCheckBox.setToolTipText( "Once a day Larva Seconds Lost reads " + UpdateConfig.MODULE_XML_URL
                + ". No data about you or your replays is sent." );
        wire( opener );
        statusChanged( service.getStatus() );
    }

    private void wire( final HomePageOpener opener ) {
        installButton.addActionListener( new ActionListener() {
            @Override
            public void actionPerformed( final ActionEvent e ) {
                service.install();
            }
        } );
        homePageButton.addActionListener( new ActionListener() {
            @Override
            public void actionPerformed( final ActionEvent e ) {
                opener.openHomePage();
            }
        } );
        hideButton.addActionListener( new ActionListener() {
            @Override
            public void actionPerformed( final ActionEvent e ) {
                service.hideAvailable();
            }
        } );
        checkNowButton.addActionListener( new ActionListener() {
            @Override
            public void actionPerformed( final ActionEvent e ) {
                service.checkNow();
            }
        } );
        autoCheckBox.addActionListener( new ActionListener() {
            @Override
            public void actionPerformed( final ActionEvent e ) {
                service.setAutoCheck( autoCheckBox.isSelected() );
            }
        } );
    }

    @Override
    public void statusChanged( final UpdateStatus status ) {
        final UpdateStatus.Phase phase = status.getPhase();
        statusLabel.setText( status.getMessage() );
        final boolean available = phase == UpdateStatus.Phase.AVAILABLE;
        final boolean failedInstall = phase == UpdateStatus.Phase.FAILED && status.getRelease() != null;
        installButton.setText( failedInstall ? RETRY_LABEL : INSTALL_LABEL );
        installButton.setVisible( available || failedInstall );
        homePageButton.setText( phase == UpdateStatus.Phase.FAILED ? DOWNLOAD_PAGE_LABEL : WHATS_NEW_LABEL );
        homePageButton.setVisible( available || phase == UpdateStatus.Phase.FAILED || phase == UpdateStatus.Phase.INSTALLED );
        hideButton.setVisible( available );
        final boolean busy = phase == UpdateStatus.Phase.CHECKING || phase == UpdateStatus.Phase.INSTALLING;
        checkNowButton.setEnabled( !busy && phase != UpdateStatus.Phase.INSTALLED );
        revalidate();
        repaint();
    }

    String getStatusText() {
        return statusLabel.getText();
    }

    JButton getInstallButton() {
        return installButton;
    }

    JButton getHomePageButton() {
        return homePageButton;
    }

    JButton getHideButton() {
        return hideButton;
    }

    JButton getCheckNowButton() {
        return checkNowButton;
    }

    JCheckBox getAutoCheckBox() {
        return autoCheckBox;
    }

}
