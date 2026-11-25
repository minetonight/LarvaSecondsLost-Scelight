package hu.aleks.larvasecondslostextmod.update;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.file.Paths;
import java.util.Collections;

import org.junit.Test;

/** Which buttons the hub's update row shows in each phase (E18-S09). */
public class UpdatePanelTest {

    private static final ModuleVersion RUNNING = new ModuleVersion( 3, 1, 0 );
    private static final RemoteRelease RELEASE = new RemoteRelease( "Larva Seconds Lost", "larva-seconds-lost", new ModuleVersion( 3, 2, 0 ),
            "LarvaSecondsLost-3.2.zip", "00", 1, Collections.singletonList( "https://x.invalid/a.zip" ), Collections.< String, String > emptyMap() );

    private static UpdatePanel panel() {
        final UpdateService service = new UpdateService( null, null, null, new UpdatePreferences( null, UpdateTestFixtures.LOGGER ), RUNNING, Paths.get(
                "." ), Paths.get( "." ), UpdateTestFixtures.LOGGER, null, null );
        service.shutdown();
        return new UpdatePanel( service, new UpdatePanel.HomePageOpener() {
            @Override
            public void openHomePage() {
                // not clicked
            }
        } );
    }

    @Test
    public void idleShowsTheRunningVersionAndOnlyTheSettings() {
        final UpdatePanel panel = panel();
        assertEquals( "Larva Seconds Lost 3.1", panel.getStatusText() );
        assertFalse( panel.getInstallButton().isVisible() );
        assertFalse( panel.getHomePageButton().isVisible() );
        assertFalse( panel.getHideButton().isVisible() );
        assertTrue( panel.getCheckNowButton().isEnabled() );
        assertTrue( "automatic checks default on", panel.getAutoCheckBox().isSelected() );
    }

    @Test
    public void availableShowsInstallWhatsNewAndHide() {
        final UpdatePanel panel = panel();
        panel.statusChanged( new UpdateStatus( UpdateStatus.Phase.AVAILABLE, RUNNING, RELEASE, "Version 3.2 is available." ) );
        assertTrue( panel.getInstallButton().isVisible() );
        assertEquals( UpdatePanel.INSTALL_LABEL, panel.getInstallButton().getText() );
        assertEquals( UpdatePanel.WHATS_NEW_LABEL, panel.getHomePageButton().getText() );
        assertTrue( panel.getHideButton().isVisible() );
    }

    @Test
    public void installingDisablesCheckNowAndHidesButtons() {
        final UpdatePanel panel = panel();
        panel.statusChanged( new UpdateStatus( UpdateStatus.Phase.INSTALLING, RUNNING, RELEASE, "Version 3.2: Downloading 40%" ) );
        assertFalse( panel.getInstallButton().isVisible() );
        assertFalse( panel.getCheckNowButton().isEnabled() );
        assertEquals( "Version 3.2: Downloading 40%", panel.getStatusText() );
    }

    @Test
    public void failedInstallOffersRetryAndTheDownloadPage() {
        final UpdatePanel panel = panel();
        panel.statusChanged( new UpdateStatus( UpdateStatus.Phase.FAILED, RUNNING, RELEASE, "Update failed: download failed" ) );
        assertTrue( panel.getInstallButton().isVisible() );
        assertEquals( UpdatePanel.RETRY_LABEL, panel.getInstallButton().getText() );
        assertEquals( UpdatePanel.DOWNLOAD_PAGE_LABEL, panel.getHomePageButton().getText() );
    }

    @Test
    public void failedCheckOffersOnlyTheDownloadPage() {
        final UpdatePanel panel = panel();
        panel.statusChanged( new UpdateStatus( UpdateStatus.Phase.FAILED, RUNNING, null, "Update check failed: offline" ) );
        assertFalse( panel.getInstallButton().isVisible() );
        assertTrue( panel.getHomePageButton().isVisible() );
    }

    @Test
    public void installedAsksForARestart() {
        final UpdatePanel panel = panel();
        panel.statusChanged( new UpdateStatus( UpdateStatus.Phase.INSTALLED, RUNNING, RELEASE, "Version 3.2 is installed. Restart Scelight to use it." ) );
        assertFalse( panel.getInstallButton().isVisible() );
        assertFalse( panel.getCheckNowButton().isEnabled() );
        assertTrue( panel.getHomePageButton().isVisible() );
    }

}
