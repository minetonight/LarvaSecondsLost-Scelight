package hu.aleks.larvasecondslostextmod;

import hu.scelightapi.BaseExtModule;
import hu.scelightapi.IModEnv;
import hu.scelightapi.IServices;
import hu.scelightapi.service.repfoldermonitor.INewRepEvent;
import hu.scelightapi.service.repfoldermonitor.INewRepListener;
import hu.scelightapibase.bean.IExtModManifestBean;
import hu.scelightapibase.gui.comp.multipage.IPage;
import hu.scelightapibase.gui.comp.multipage.IPageCompCreator;
import hu.scelightapibase.gui.icon.IRIcon;
import hu.scelightapibase.util.IRHtml;

import java.nio.file.Path;
import java.util.concurrent.Executor;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;

/**
 * Epic 01 baseline external module for the larva analysis feature.
 *
 * <p>This module intentionally keeps runtime behavior minimal: it adds a visible page
 * to Scelight and logs lifecycle transitions so packaging and installation can be
 * verified before replay-analysis work begins.</p>
 */
public class LarvaSecondsLostModule extends BaseExtModule {

    /** Startup step label used for lifecycle diagnostics. */
    private String initPhase = "not-started";

    /** Icon used by the placeholder page. */
    private IRIcon larvaIcon;

    /** Icon of single-replay sub-pages. */
    private IRIcon replayIcon;

    /** Icon of multi-analysis sub-pages. */
    private IRIcon multiAnalysisIcon;

    /** Help content for the placeholder page. */
    private IRHtml helpContent;

    /** Tip content of the multi analysis button. */
    private IRHtml multiTipContent;

    /** Hub content component; <code>null</code> until Scelight first shows the hub. */
    private volatile LarvaHubPageComp hubComp;

    /** The one FIFO analysis worker shared by all single-replay pages; <code>null</code> until init created it. */
    private LarvaSingleReplayAnalysisQueue analysisQueue;

    /** Behavior of the hub buttons (and the identical buttons on replay pages). */
    private volatile LarvaModuleHubActions hubActions;

    /** Resolves the latest replay even if the replay-folder event was missed. */
    private LatestReplayResolver latestReplayResolver;

    /** Owns native chart integration capability evaluation. */
    private ChartIntegrationManager chartIntegrationManager;

    /** Cached chart integration capability report. */
    private ChartIntegrationCapability chartIntegrationCapability;

    /** Owns Base Control augmentation capability evaluation. */
    private BaseControlAugmentationManager baseControlAugmentationManager;

    /** Cached Base Control augmentation capability report. */
    private BaseControlAugmentationCapability baseControlAugmentationCapability;

    /** Development-only diagnostic dump writer. */
    private DevDiagnosticDumpWriter devDiagnosticDumpWriter;

    /** Dev command channel; <code>null</code> unless the system property is set. */
    private LarvaDevCommandChannel devCommandChannel;

    /** Owns the Larva hub and all sub-pages; <code>null</code> until init created it. */
    private volatile LarvaPageNavigator pageNavigator;

    /** Listener registered at the replay folder monitor. */
    private INewRepListener newRepListener;

    /** Latest replay detected by the replay folder monitor. */
    private volatile Path lastDetectedReplayPath;

    @Override
    public void init( final IExtModManifestBean manifest, final IServices services, final IModEnv modEnv ) {
        super.init( manifest, services, modEnv );

        try {
            updateInitPhase( "starting module initialization" );
            loadResources();
            devDiagnosticDumpWriter = new DevDiagnosticDumpWriter( logger );
            recordLifecycleUpdate( "initializing", "Resources loaded successfully." );
            updateInitPhase( "creating replay services" );
            analysisQueue = createAnalysisQueue();
            latestReplayResolver = new LatestReplayResolver( this );
            chartIntegrationManager = new ChartIntegrationManager( new ChartIntegrationCapabilityDetector() );
            chartIntegrationCapability = chartIntegrationManager.initialize( logger );
            baseControlAugmentationManager = new BaseControlAugmentationManager( new BaseControlAugmentationCapabilityDetector() );
            baseControlAugmentationCapability = baseControlAugmentationManager.initialize( logger );
            if ( devDiagnosticDumpWriter != null && devDiagnosticDumpWriter.isEnabled() )
                devDiagnosticDumpWriter.recordCapabilitySummary( chartIntegrationCapability, baseControlAugmentationCapability );
            recordLifecycleUpdate( "initializing", "Replay services and capability detectors created successfully." );
            updateInitPhase( "installing replay monitor listener" );
            installReplayMonitorListener();
            recordLifecycleUpdate( "initializing", "Replay monitor listener installed." );
            updateInitPhase( "installing Larva page" );
            installReplayPage();
            recordLifecycleUpdate( "initializing", "Larva page installation requested on the EDT." );
            if ( devDiagnosticDumpWriter.isEnabled() )
                devDiagnosticDumpWriter.recordStartup( manifest );
            startDevCommandChannel();
            updateInitPhase( "startup complete" );
            logger.debug( manifest.getName() + " module started successfully." );
            if ( devDiagnosticDumpWriter.isEnabled() )
                logger.debug( manifest.getName() + " dev diagnostic dump file enabled at: " + devDiagnosticDumpWriter.getDumpFile() );
        } catch ( final RuntimeException e ) {
            final String failureMessage = buildInitFailureMessage( e );
            logger.error( failureMessage, e );
            if ( devDiagnosticDumpWriter != null && devDiagnosticDumpWriter.isEnabled() )
                devDiagnosticDumpWriter.recordInitFailure( failureMessage );
            throw e;
        } catch ( final Error e ) {
            final String failureMessage = buildInitFailureMessage( e );
            logger.error( failureMessage, e );
            if ( devDiagnosticDumpWriter != null && devDiagnosticDumpWriter.isEnabled() )
                devDiagnosticDumpWriter.recordInitFailure( failureMessage );
            throw e;
        }
    }

    /**
     * Loads static resources that are packaged inside the module jar.
     */
    private void loadResources() {
        updateInitPhase( "loading packaged resources" );
        larvaIcon = guiFactory.newRIcon( requireResource( "icon/larva-module-icon.png" ) );
        replayIcon = guiFactory.newRIcon( requireResource( "icon/larva-replay-icon.png" ) );
        multiAnalysisIcon = guiFactory.newRIcon( requireResource( "icon/larva-multi-analysis-icon.png" ) );
        helpContent = guiFactory.newRHtml( "Larva Module Help", requireResource( "help/larva-module-help.html" ) );
        multiTipContent = guiFactory.newRHtml( "Multi Analysis", requireResource( "help/larva-multi-tip.html" ) );
    }

    /**
     * Resolves a packaged resource and fails init (via the init failure message) if it is missing.
     *
     * @param path resource path relative to the module package
     * @return the resource URL, never <code>null</code>
     */
    private java.net.URL requireResource( final String path ) {
        final java.net.URL url = getClass().getResource( path );
        if ( url == null )
            throw new IllegalStateException( "Missing packaged resource: " + path );
        return url;
    }

    /**
     * Creates the shared analysis queue. Its single worker thread is the only user of the {@link ReplaySummaryService}
     * (the service is not thread-safe); results are posted to the EDT without blocking the worker.
     */
    private LarvaSingleReplayAnalysisQueue createAnalysisQueue() {
        final ReplaySummaryService service = new ReplaySummaryService( this );
        return new LarvaSingleReplayAnalysisQueue( logger, new LarvaReplayAnalysisRunner() {
            @Override
            public LarvaReplayPageSummary analyze( final Path replayFile, final String sourceDescription ) {
                return service.analyze( replayFile, sourceDescription );
            }

            @Override
            public LarvaAutoOpenOutcome analyzeForAutoOpen( final Path replayFile, final String sourceDescription ) {
                return service.analyzeForAutoOpen( replayFile, sourceDescription );
            }
        }, new Executor() {
            @Override
            public void execute( final Runnable command ) {
                SwingUtilities.invokeLater( command );
            }
        }, devDiagnosticDumpWriter );
    }

    /**
     * Creates the page navigator and installs the Larva hub on the EDT.
     */
    private void installReplayPage() {
        pageNavigator = new LarvaPageNavigator( logger, services, larvaIcon, new IPageCompCreator< JComponent >() {
            @Override
            public JComponent createPageComp() {
                return createHubComp();
            }
        }, new LarvaSinglePageHost() {
            @Override
            public IRIcon getReplayIcon() {
                return replayIcon;
            }

            @Override
            public LarvaReplayAnalysisQueue getAnalysisQueue() {
                return analysisQueue;
            }

            @Override
            public JComponent createReplayPageComp( final LarvaReplayPageController controller ) {
                return new LarvaReplayPageComp( LarvaSecondsLostModule.this, controller, hubActions );
            }
        } );
        hubActions = new LarvaModuleHubActions( this, pageNavigator, new LarvaReplayFileChooser( guiFactory ) );
        analysisQueue.setAutoOpenHost( new LarvaAutoOpenHost() {
            @Override
            public LarvaAutoOpenPolicy.Admission admit( final Path replayFile ) {
                return pageNavigator.checkAutoOpenAdmission( replayFile );
            }

            @Override
            public LarvaReplayPageController open( final Path replayFile, final LarvaAutoOpenOutcome outcome ) {
                final LarvaReplayPageController controller = pageNavigator.autoOpenSingleReplay( replayFile, outcome );
                if ( controller != null )
                    analysisQueue.getAutoOpenStats().recordOpened( controller.getReplayFile().toString(), pageNavigator.isLarvaPageSelected() );
                return controller;
            }
        } );
        guiUtils.runInEDT( new Runnable() {
            @Override
            public void run() {
                pageNavigator.install();
            }
        } );
    }

    /**
     * Creates the hub content (EDT, called once by Scelight when the hub is first shown). Starts no analysis (AC-1.4).
     */
    private JComponent createHubComp() {
        final LarvaHubPageComp comp = new LarvaHubPageComp( hubActions, guiFactory.newTipIcon( multiTipContent ).asLabel(),
                guiFactory.newHelpIcon( helpContent ).asLabel() );
        hubComp = comp;
        return comp;
    }

    /**
     * Returns the state of the hub's "Ladder games only" checkbox for the multi analysis (E17-S14).
     *
     * @return the checkbox state; <code>true</code> (the default, OQ-8) while the hub content was not created yet
     */
    boolean isLadderOnly() {
        final LarvaHubPageComp comp = hubComp;
        return comp == null || comp.isLadderOnly();
    }

    /**
     * Returns the hub content component.
     *
     * @return the hub component; <code>null</code> until the hub was shown for the first time
     */
    LarvaHubPageComp getHubComp() {
        return hubComp;
    }

    /**
     * Starts the dev command channel if (and only if) the system property is set.
     */
    private void startDevCommandChannel() {
        final java.nio.file.Path commandFile = LarvaDevCommandChannel.configuredFile();
        if ( commandFile == null )
            return;
        final LarvaDevCommandRegistry registry = new LarvaDevCommandRegistry();
        LarvaDevBuiltinCommands.registerAll( registry, this );
        devCommandChannel = new LarvaDevCommandChannel( logger, commandFile, registry );
        devCommandChannel.start();
    }

    /**
     * Returns the installed Larva hub page.
     *
     * @return the Larva hub; may be <code>null</code> before installation
     */
    IPage< ? > getLarvaPage() {
        final LarvaPageNavigator navigator = pageNavigator;
        return navigator == null ? null : navigator.getHub();
    }

    /**
     * Returns the page navigator.
     *
     * @return the navigator; <code>null</code> before init reached page installation
     */
    LarvaPageNavigator getPageNavigator() {
        return pageNavigator;
    }

    /**
     * Returns the shared analysis queue (auto-open counters for the dev commands).
     *
     * @return the queue; <code>null</code> before init created it
     */
    LarvaSingleReplayAnalysisQueue getAnalysisQueue() {
        return analysisQueue;
    }

    /**
     * Hands a detected replay to the analysis queue and returns at once: the listener runs on Scelight's replay-handler
     * job thread, so no parsing and no EDT work happens here (AC-17.6). Never throws.
     */
    private void handOffToAutoOpen( final Path replayFile ) {
        final LarvaSingleReplayAnalysisQueue queue = analysisQueue;
        if ( queue == null || replayFile == null )
            return;
        final long startNanos = System.nanoTime();
        try {
            queue.submitAutoOpen( replayFile );
        } catch ( final RuntimeException e ) {
            logger.error( "[larva-auto] hand-off failed: " + replayFile, e );
            return;
        }
        logger.debug( "[larva-auto] hand-off took " + ( System.nanoTime() - startNanos ) / 1000L + " us: " + replayFile );
    }

    /**
     * Installs the replay-folder listener. It records the latest detected replay (for "Analyze Latest Replay" and the
     * file chooser start folder) and hands the replay to the queue for auto-open (E17-S07, FR-17).
     */
    private void installReplayMonitorListener() {
        newRepListener = new INewRepListener() {
            @Override
            public void newRepDetected( final INewRepEvent event ) {
                lastDetectedReplayPath = event.getFile();
                logger.debug( manifest.getName() + " detected replay: " + lastDetectedReplayPath );
                handOffToAutoOpen( lastDetectedReplayPath );
            }
        };

        services.getRepFolderMonitor().addNewRepListener( newRepListener );
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
     * Returns the icon of single-replay sub-pages.
     *
     * @return the replay icon
     */
    IRIcon getReplayIcon() {
        return replayIcon;
    }

    /**
     * Returns the icon of multi-analysis sub-pages.
     *
     * @return the multi-analysis icon
     */
    IRIcon getMultiAnalysisIcon() {
        return multiAnalysisIcon;
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
     * Returns the hub button behavior (also used by the dev commands).
     *
     * @return the hub actions; <code>null</code> before page installation
     */
    LarvaModuleHubActions getHubActions() {
        return hubActions;
    }

    /**
     * Returns the cached chart integration capability report.
     *
     * @return chart integration capability report
     */
    ChartIntegrationCapability getChartIntegrationCapability() {
        return chartIntegrationCapability;
    }

    /**
     * Returns the cached Base Control augmentation capability report.
     *
     * @return Base Control augmentation capability report
     */
    BaseControlAugmentationCapability getBaseControlAugmentationCapability() {
        return baseControlAugmentationCapability;
    }

    /**
     * Returns the development diagnostic dump writer.
     *
     * @return development diagnostic dump writer; may be <code>null</code>
     */
    DevDiagnosticDumpWriter getDevDiagnosticDumpWriter() {
        return devDiagnosticDumpWriter;
    }

    /**
     * Updates the current initialization phase for diagnostics.
     *
     * @param initPhase initialization phase label
     */
    private void updateInitPhase( final String initPhase ) {
        this.initPhase = initPhase;
        logger.debug( manifest == null ? "Larva Seconds Lost init phase: " + initPhase : manifest.getName() + " init phase: " + initPhase );
    }

    /**
     * Records a lifecycle update in the optional development dump.
     *
     * @param lifecycleState lifecycle state label
     * @param lifecycleDetails lifecycle details text
     */
    private void recordLifecycleUpdate( final String lifecycleState, final String lifecycleDetails ) {
        if ( devDiagnosticDumpWriter != null && devDiagnosticDumpWriter.isEnabled() )
            devDiagnosticDumpWriter.recordLifecycleUpdate( lifecycleState, lifecycleDetails );
    }

    /**
     * Builds a readable initialization failure message.
     *
     * @param throwable startup failure
     * @return readable failure message
     */
    private String buildInitFailureMessage( final Throwable throwable ) {
        final StringBuilder builder = new StringBuilder();
        builder.append( "Failed to initialize " )
                .append( manifest.getName() )
                .append( " during phase: " )
                .append( initPhase )
                .append( ". " );
        builder.append( "Common causes: missing packaged resources, incompatible external module API/runtime version, or a replay-page startup error. " );
        if ( throwable != null && throwable.getMessage() != null && throwable.getMessage().length() > 0 )
            builder.append( "Cause: " ).append( throwable.getMessage() );
        else
            builder.append( "Cause: see stack trace for details." );
        return builder.toString();
    }

    @Override
    public void destroy() {
        if ( pageNavigator != null )
            pageNavigator.shutdown();

        if ( analysisQueue != null )
            analysisQueue.shutdown();

        if ( devCommandChannel != null )
            devCommandChannel.stop();

        if ( newRepListener != null )
            services.getRepFolderMonitor().removeNewRepListener( newRepListener );

        if ( devDiagnosticDumpWriter != null && devDiagnosticDumpWriter.isEnabled() )
            devDiagnosticDumpWriter.recordShutdown( manifest );

        logger.debug( manifest.getName() + " destroy sequence completed." );
        logger.debug( manifest.getName() + " module stopped." );
    }

}
