package hu.aleks.larvasecondslostextmod.update;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import hu.aleks.larvasecondslostextmod.update.UpdateTestFixtures.FakeDownloader;
import hu.aleks.larvasecondslostextmod.update.UpdateTestFixtures.ReleaseBuilder;

/** Check, hide, install and the "automatic failures stay quiet" rule of the update service (E18-S09, E18-S10). */
public class UpdateServiceTest {

    private static final ModuleVersion RUNNING = new ModuleVersion( 3, 1, 0 );
    private static final long NOW = 1000L * 60L * 60L * 24L * 365L;

    private Path temp;
    private Path modRoot;
    private Path running;
    private FakeDownloader downloader;
    private UpdatePreferences preferences;
    private UpdateService service;
    private final List< UpdateStatus > seen = new ArrayList<>();

    @Before
    public void setUp() throws IOException {
        temp = Files.createTempDirectory( "larva-service-test" );
        modRoot = Files.createDirectories( temp.resolve( "larva-seconds-lost" ) );
        running = UpdateTestFixtures.versionFolder( modRoot, "3.1" );
        downloader = new FakeDownloader();
        preferences = new UpdatePreferences( temp.resolve( "cache" ).resolve( UpdatePreferences.FILE_NAME ), UpdateTestFixtures.LOGGER );
        service = newService();
    }

    private UpdateService newService() {
        final UpdateService s = new UpdateService( new UpdateChecker( downloader, UpdateTestFixtures.MODULE_XML_URL, UpdateTestFixtures.NAME,
                UpdateTestFixtures.FOLDER, RUNNING ), new UpdateInstaller( downloader, modRoot, temp.resolve( "cache/update" ), running,
                UpdateTestFixtures.LOGGER ), new OldVersionCleaner( UpdateTestFixtures.LOGGER ), preferences, RUNNING, modRoot, running,
                UpdateTestFixtures.LOGGER, new Executor() {
                    @Override
                    public void execute( final Runnable command ) {
                        command.run();
                    }
                }, new UpdateService.Clock() {
                    @Override
                    public long nowMillis() {
                        return NOW;
                    }
                } );
        s.addListener( new UpdateService.Listener() {
            @Override
            public void statusChanged( final UpdateStatus status ) {
                synchronized ( seen ) {
                    seen.add( status );
                }
            }
        } );
        return s;
    }

    @After
    public void tearDown() throws IOException {
        service.shutdown();
        UpdateTestFixtures.deleteTree( temp );
    }

    private UpdateStatus.Phase phaseAfterWork() throws InterruptedException {
        assertTrue( service.awaitIdle( 10000 ) );
        return service.getStatus().getPhase();
    }

    @Test
    public void manualCheckFindsNewerVersionAndInstallsIt() throws Exception {
        final RemoteRelease release = new ReleaseBuilder( "3.2" ).publish( downloader );
        service.checkNow();
        assertEquals( UpdateStatus.Phase.AVAILABLE, phaseAfterWork() );
        assertEquals( "Version 3.2 is available.", service.getStatus().getMessage() );
        assertEquals( NOW, preferences.getLastCheckMillis() );

        service.install();
        assertEquals( UpdateStatus.Phase.INSTALLED, phaseAfterWork() );
        assertTrue( service.getStatus().getMessage().contains( "Restart Scelight" ) );
        assertTrue( Files.isDirectory( modRoot.resolve( "3.2" ) ) );
        boolean sawProgress = false;
        synchronized ( seen ) {
            for ( final UpdateStatus status : seen )
                sawProgress |= status.getPhase() == UpdateStatus.Phase.INSTALLING && status.getMessage().contains( "Downloading" );
        }
        assertTrue( sawProgress );
        assertEquals( release.getVersion(), service.getStatus().getRelease().getVersion() );
    }

    @Test
    public void checkAfterInstallShowsInstalledNotAvailable() throws Exception {
        final RemoteRelease release = new ReleaseBuilder( "3.2" ).publish( downloader );
        new UpdateInstaller( downloader, modRoot, temp.resolve( "dl" ), running, UpdateTestFixtures.LOGGER ).install( release,
                new UpdateInstaller.Progress() {
                    @Override
                    public void report( final String text ) {
                        // ignored
                    }
                } );
        service.checkNow();
        assertEquals( UpdateStatus.Phase.INSTALLED, phaseAfterWork() );
    }

    @Test
    public void manualCheckSaysUpToDate() throws Exception {
        new ReleaseBuilder( "3.1" ).publish( downloader );
        service.checkNow();
        assertEquals( UpdateStatus.Phase.UP_TO_DATE, phaseAfterWork() );
    }

    @Test
    public void manualCheckFailureIsShownAndDoesNotCountAsACheck() throws Exception {
        service.checkNow(); // nothing published: 404
        assertEquals( UpdateStatus.Phase.FAILED, phaseAfterWork() );
        assertTrue( service.getStatus().getMessage(), service.getStatus().getMessage().contains( "cannot reach the update page" ) );
        assertEquals( 0, preferences.getLastCheckMillis() );
    }

    @Test
    public void moduleXmlOfAnotherModuleIsRefused() throws Exception {
        final ReleaseBuilder builder = new ReleaseBuilder( "9.0" );
        final byte[] zip = builder.zip();
        downloader.content.put( UpdateTestFixtures.MODULE_XML_URL, builder.moduleXml( zip ).replace( "<folder>larva-seconds-lost</folder>",
                "<folder>other-mod</folder>" ).getBytes( UpdateTestFixtures.UTF8 ) );
        service.checkNow();
        assertEquals( UpdateStatus.Phase.FAILED, phaseAfterWork() );
        assertTrue( service.getStatus().getMessage().contains( "other-mod" ) );
    }

    @Test
    public void hiddenVersionStaysHiddenForAutomaticChecksButNotForANewerOne() throws Exception {
        new ReleaseBuilder( "3.2" ).publish( downloader );
        service.checkNow();
        phaseAfterWork();
        service.hideAvailable();
        assertEquals( UpdateStatus.Phase.IDLE, service.getStatus().getPhase() );
        assertEquals( new ModuleVersion( 3, 2, 0 ), preferences.getHiddenVersion() );

        preferences.setLastCheckMillis( 0 ); // due again
        final UpdateService restarted = restartedServiceWithoutStartupDelay();
        assertEquals( UpdateStatus.Phase.IDLE, restarted.getStatus().getPhase() );
        restarted.shutdown();

        new ReleaseBuilder( "3.3" ).publish( downloader );
        preferences.setLastCheckMillis( 0 );
        final UpdateService again = restartedServiceWithoutStartupDelay();
        assertEquals( UpdateStatus.Phase.AVAILABLE, again.getStatus().getPhase() );
        again.shutdown();
    }

    @Test
    public void automaticCheckDoesNotReplaceAStatusTheUserIsActingOn() throws Exception {
        new ReleaseBuilder( "3.2" ).publish( downloader );
        service.checkNow();
        phaseAfterWork();
        service.install();
        assertEquals( UpdateStatus.Phase.INSTALLED, phaseAfterWork() );
        new ReleaseBuilder( "3.3" ).publish( downloader );
        final int opens = downloader.openCount;
        service.runAutomaticCheckForTest();
        assertEquals( UpdateStatus.Phase.INSTALLED, phaseAfterWork() );
        assertEquals( "3.2", service.getStatus().getRelease().getVersion().toString() );
        assertEquals( "no download", opens, downloader.openCount );
    }

    @Test
    public void automaticCheckFailureStaysQuiet() throws Exception {
        final UpdateService restarted = restartedServiceWithoutStartupDelay(); // nothing published
        assertEquals( UpdateStatus.Phase.IDLE, restarted.getStatus().getPhase() );
        restarted.shutdown();
    }

    @Test
    public void startSkipsTheCheckWhenOffOrRecent() throws Exception {
        new ReleaseBuilder( "3.2" ).publish( downloader );
        preferences.setAutoCheck( false );
        service.start();
        assertTrue( service.awaitIdle( 10000 ) );
        assertEquals( 0, downloader.openCount );

        preferences.setAutoCheck( true );
        preferences.setLastCheckMillis( NOW - 60000L );
        final UpdateService recent = newService();
        recent.start();
        assertTrue( recent.awaitIdle( 10000 ) );
        assertEquals( 0, downloader.openCount );
        recent.shutdown();
    }

    @Test
    public void startRemovesOldVersionFolders() throws Exception {
        UpdateTestFixtures.versionFolder( modRoot, "3.0" );
        preferences.setAutoCheck( false );
        service.start();
        assertTrue( service.awaitIdle( 10000 ) );
        assertFalse( Files.exists( modRoot.resolve( "3.0" ) ) );
        assertTrue( Files.exists( running ) );
    }

    @Test
    public void installIsIgnoredWithoutAnAvailableRelease() throws Exception {
        service.install();
        assertEquals( UpdateStatus.Phase.IDLE, phaseAfterWork() );
    }

    /** A new service (like after a Scelight restart) that has run its automatic check, without the 30 s startup delay. */
    private UpdateService restartedServiceWithoutStartupDelay() throws Exception {
        final UpdateService s = newService();
        s.runAutomaticCheckForTest();
        assertTrue( s.awaitIdle( 10000 ) );
        return s;
    }

}
