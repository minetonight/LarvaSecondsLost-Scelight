package hu.aleks.larvasecondslostextmod;

import hu.scelightapi.BaseExtModule;
import hu.scelightapi.IModEnv;
import hu.scelightapi.IServices;
import hu.scelightapi.service.repfoldermonitor.INewRepEvent;
import hu.scelightapi.service.repfoldermonitor.INewRepListener;
import hu.scelightapibase.bean.IExtModManifestBean;
import hu.scelightapibase.gui.comp.multipage.IPageCompCreator;
import hu.scelightapibase.gui.icon.IRIcon;
import hu.scelightapibase.util.IRHtml;

import java.nio.file.Path;

import javax.swing.JComponent;

/**
 * Epic 01 baseline external module for the larva analysis feature.
 *
 * <p>This module intentionally keeps runtime behavior minimal: it adds a visible page
 * to Scelight and logs lifecycle transitions so packaging and installation can be
 * verified before replay-analysis work begins.</p>
 */
public class LarvaSecondsLostModule extends BaseExtModule {

    /** Icon used by the placeholder page. */
    private IRIcon larvaIcon;

    /** Help content for the placeholder page. */
    private IRHtml helpContent;

    /** Service that produces replay diagnostics for the replay page. */
    private ReplaySummaryService replaySummaryService;

    /** Resolves the latest replay even if the replay-folder event was missed. */
    private LatestReplayResolver latestReplayResolver;

    /** Detects native chart integration capability. */
    private ChartIntegrationCapabilityDetector chartIntegrationCapabilityDetector;

    /** Cached chart integration capability report. */
    private ChartIntegrationCapability chartIntegrationCapability;

    /** Listener registered at the replay folder monitor. */
    private INewRepListener newRepListener;

    /** Latest replay detected by the replay folder monitor. */
    private volatile Path lastDetectedReplayPath;

    /** Latest successful replay summary produced by this module. */
    private volatile ReplaySummary latestReplaySummary;

    /** Replay page component if the page was instantiated. */
    private volatile LarvaReplayPageComp replayPageComp;

    @Override
    public void init( final IExtModManifestBean manifest, final IServices services, final IModEnv modEnv ) {
        super.init( manifest, services, modEnv );

        try {
            loadResources();
            replaySummaryService = new ReplaySummaryService( this );
            latestReplayResolver = new LatestReplayResolver( this );
            chartIntegrationCapabilityDetector = new ChartIntegrationCapabilityDetector();
            chartIntegrationCapability = chartIntegrationCapabilityDetector.detect();
            installReplayMonitorListener();
            installReplayPage();
            logger.debug( manifest.getName() + " module started successfully." );
        } catch ( final RuntimeException e ) {
            logger.error( "Failed to initialize " + manifest.getName() + " module.", e );
            throw e;
        } catch ( final Error e ) {
            logger.error( "Fatal error while initializing " + manifest.getName() + " module.", e );
            throw e;
        }
    }

    /**
     * Loads static resources that are packaged inside the module jar.
     */
    private void loadResources() {
        larvaIcon = guiFactory.newRIcon( getClass().getResource( "icon/larva-module-icon.png" ) );
        helpContent = guiFactory.newRHtml( "Larva Module Help", getClass().getResource( "help/larva-module-help.html" ) );
    }

    /**
     * Adds the replay diagnostics page to Scelight.
     */
    private void installReplayPage() {
        guiUtils.runInEDT( new Runnable() {
            @Override
            public void run() {
                services.getMainFrame().getMultiPageComp().addPage( guiFactory.newPage( "Larva", larvaIcon, false,
                        new IPageCompCreator< JComponent >() {
                            @Override
                            public JComponent createPageComp() {
                                replayPageComp = new LarvaReplayPageComp( LarvaSecondsLostModule.this );
                                if ( latestReplaySummary != null )
                                    replayPageComp.showSummary( latestReplaySummary );
                                return replayPageComp;
                            }
                        } ) );
                services.getMainFrame().getMultiPageComp().rebuildPageTree( false );
            }
        } );
    }

    /**
     * Installs a replay-folder listener so newly detected replays can be surfaced on the module page.
     */
    private void installReplayMonitorListener() {
        newRepListener = new INewRepListener() {
            @Override
            public void newRepDetected( final INewRepEvent event ) {
                lastDetectedReplayPath = event.getFile();
                logger.debug( manifest.getName() + " detected replay for fallback replay page: " + lastDetectedReplayPath );
                requestReplayAnalysis( lastDetectedReplayPath, "Replay Folder Monitor" );
            }
        };

        services.getRepFolderMonitor().addNewRepListener( newRepListener );
    }

    /**
     * Requests replay analysis on a worker thread and refreshes the replay page when complete.
     *
     * @param replayFile replay file to analyze
     * @param sourceDescription short label describing where the request came from
     */
    void requestReplayAnalysis( final Path replayFile, final String sourceDescription ) {
        if ( replayFile == null )
            return;

        final LarvaReplayPageComp pageComp = replayPageComp;
        if ( pageComp != null )
            pageComp.showBusy( replayFile, sourceDescription );

        final Thread worker = new Thread( new Runnable() {
            @Override
            public void run() {
                try {
                    final ReplaySummary summary = replaySummaryService.analyze( replayFile, sourceDescription );
                    latestReplaySummary = summary;
                    refreshReplayPage( summary );
                    logger.debug( manifest.getName() + " replay diagnostics ready for: " + replayFile );
                } catch ( final RuntimeException e ) {
                    logger.error( "Failed to analyze replay for the Larva page: " + replayFile, e );
                    showReplayError( replayFile, sourceDescription, e.getMessage() );
                }
            }
        }, "larva-replay-loader" );

        worker.setDaemon( true );
        worker.start();
    }

    /**
     * Refreshes the replay page with a fresh summary.
     *
     * @param summary replay summary to display
     */
    private void refreshReplayPage( final ReplaySummary summary ) {
        guiUtils.runInEDT( new Runnable() {
            @Override
            public void run() {
                if ( replayPageComp != null )
                    replayPageComp.showSummary( summary );
            }
        } );
    }

    /**
     * Refreshes the replay page with an error state.
     *
     * @param replayFile replay file that failed to analyze
     * @param sourceDescription source of the request
     * @param message error message to display
     */
    private void showReplayError( final Path replayFile, final String sourceDescription, final String message ) {
        guiUtils.runInEDT( new Runnable() {
            @Override
            public void run() {
                if ( replayPageComp != null )
                    replayPageComp.showError( replayFile, sourceDescription, message );
            }
        } );
    }

    /**
     * Returns the page icon.
     *
     * @return the page icon
     */
    IRIcon getLarvaIcon() {
        return larvaIcon;
    }

    /**
     * Returns the placeholder help content.
     *
     * @return the help content
     */
    IRHtml getHelpContent() {
        return helpContent;
    }

    /**
     * Returns the last replay detected by the replay folder monitor.
     *
     * @return the last detected replay file; may be <code>null</code>
     */
    Path getLastDetectedReplayPath() {
        return lastDetectedReplayPath;
    }

    /**
     * Resolves the best available "latest replay" candidate.
     *
     * <p>The replay-folder monitor event is preferred when available, but if the event was missed
     * (for example because the module was enabled later), a best-effort native replay-folder scan
     * is used to mirror Scelight's own "Quick Open Last Replay" behavior as closely as possible.</p>
     *
     * @return best available latest replay file; may be <code>null</code>
     */
    Path resolveLatestReplayPath() {
        return latestReplayResolver.resolveLatestReplay( lastDetectedReplayPath );
    }

    /**
     * Returns the latest replay summary if one has already been produced.
     *
     * @return the latest replay summary; may be <code>null</code>
     */
    ReplaySummary getLatestReplaySummary() {
        return latestReplaySummary;
    }

    /**
     * Returns the cached chart integration capability report.
     *
     * @return chart integration capability report
     */
    ChartIntegrationCapability getChartIntegrationCapability() {
        return chartIntegrationCapability;
    }

    @Override
    public void destroy() {
        if ( newRepListener != null )
            services.getRepFolderMonitor().removeNewRepListener( newRepListener );

        logger.debug( manifest.getName() + " module stopped." );
    }

}
