package hu.aleks.larvasecondslostextmod;

import hu.scelightapi.IServices;
import hu.scelightapibase.gui.comp.multipage.IMultiPageComp;
import hu.scelightapibase.gui.comp.multipage.IPage;
import hu.scelightapibase.gui.comp.multipage.IPageCompCreator;
import hu.scelightapibase.gui.icon.IRIcon;
import hu.scelightapibase.service.log.ILogger;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;

/**
 * Owns the "Larva" hub and all page-tree changes under it. Every method must run on the EDT: a violation is logged
 * at error level and the call is ignored (the host keeps running, the tree stays untouched).
 *
 * <p>The hub's child list is the only registry of sub-pages; the host removes closed pages from it itself.</p>
 */
final class LarvaPageNavigator {

    private final ILogger logger;
    private final IServices services;
    private final IRIcon hubIcon;
    private final IPageCompCreator< JComponent > hubContentCreator;
    private final LarvaSinglePageHost singleHost;

    private volatile LarvaHubPage hub;
    private volatile boolean ready;

    LarvaPageNavigator( final ILogger logger, final IServices services, final IRIcon hubIcon, final IPageCompCreator< JComponent > hubContentCreator,
            final LarvaSinglePageHost singleHost ) {
        this.logger = logger;
        this.services = services;
        this.hubIcon = hubIcon;
        this.hubContentCreator = hubContentCreator;
        this.singleHost = singleHost;
    }

    /** Adds the hub as top-level page and marks the navigator ready. */
    void install() {
        if ( !requireEdt( "install" ) )
            return;
        if ( hub != null ) {
            logger.error( "[larva-nav] install ignored: hub already installed" );
            return;
        }
        final LarvaHubPage newHub = new LarvaHubPage( hubIcon, hubContentCreator );
        multiPageComp().addPage( newHub );
        multiPageComp().rebuildPageTree( false );
        hub = newHub;
        ready = true;
        logger.debug( "[larva-nav] hub installed" );
    }

    /** @return <code>true</code> once {@link #install()} completed on the EDT (readable from any thread) */
    boolean isReady() {
        return ready;
    }

    /** Marks the navigator not ready. No tree cleanup: Scelight disposes the pages on exit. */
    void shutdown() {
        ready = false;
    }

    /** @return the hub, or <code>null</code> before {@link #install()} (readable from any thread) */
    LarvaHubPage getHub() {
        return hub;
    }

    /** Appends the page under the hub (creation order) and optionally selects it. */
    void addSubPage( final LarvaSubPage page, final boolean select ) {
        if ( !requireEdt( "addSubPage" ) || !requireReady( "addSubPage" ) )
            return;
        page.setParent( hub );
        hub.getChildList().add( page );
        multiPageComp().rebuildPageTree( false );
        if ( select )
            multiPageComp().selectPage( page );
        logger.debug( "[larva-nav] added sub-page '" + page.getDisplayName() + "' kind=" + page.getKind() + " select=" + select );
    }

    /** Selects the hub or one of its sub-pages. */
    boolean selectPage( final IPage< ? > page ) {
        if ( !requireEdt( "selectPage" ) || !requireReady( "selectPage" ) )
            return false;
        return multiPageComp().selectPage( page );
    }

    /** Renames a sub-page; the rebuild updates tree label and title bar (and re-fires pageSelected on the current page). */
    void relabel( final LarvaSubPage page, final String newName ) {
        if ( !requireEdt( "relabel" ) || !requireReady( "relabel" ) )
            return;
        page.setDisplayName( newName );
        multiPageComp().rebuildPageTree( false );
        logger.debug( "[larva-nav] relabeled sub-page to '" + newName + "'" );
    }

    /** @return the open sub-pages of the given kind in creation order; empty off the EDT or before install */
    List< LarvaSubPage > getSubPages( final LarvaSubPageKind kind ) {
        if ( !requireEdt( "getSubPages" ) || !requireReady( "getSubPages" ) )
            return Collections.emptyList();
        return hub.getSubPages( kind );
    }

    /** @return <code>true</code> if the hub or any sub-page is the selected page */
    boolean isLarvaPageSelected() {
        if ( !requireEdt( "isLarvaPageSelected" ) || !requireReady( "isLarvaPageSelected" ) )
            return false;
        final IPage< ? > selected = multiPageComp().getSelectedPage();
        return selected != null && ( selected == hub || hub.hasChild( selected ) );
    }

    /** @return the sub-page with exactly this label, or <code>null</code> (EDT only) */
    LarvaSubPage findSubPage( final String label ) {
        if ( !requireEdt( "findSubPage" ) || !requireReady( "findSubPage" ) )
            return null;
        for ( final IPage< ? > page : hub.getChildList() )
            if ( page instanceof LarvaSubPage && label.equals( page.getDisplayName() ) )
                return (LarvaSubPage) page;
        return null;
    }

    /**
     * Opens a replay on its own sub-page, or only focuses the page if one for this replay is already open (OQ-6): an
     * open page is never re-rendered with another replay and an already open replay is not analyzed again.
     *
     * @param replayFile replay file (normalized here)
     * @param sourceDescription short label of where the request came from, kept for the dev-dump goldens
     * @param select whether a new page, or the already open page, is selected
     * @return the controller of the (new or existing) page; <code>null</code> if not ready or off the EDT
     */
    LarvaReplayPageController openSingleReplay( final Path replayFile, final String sourceDescription, final boolean select ) {
        if ( !requireEdt( "openSingleReplay" ) || !requireReady( "openSingleReplay" ) )
            return null;
        final Path key = LarvaReplayPaths.normalize( replayFile );
        final LarvaSubPage existing = hub.findSingleReplayPage( key );
        if ( existing != null ) {
            logger.debug( "[larva-nav] replay already open, page only focused (select=" + select + "): " + key );
            if ( select )
                multiPageComp().selectPage( existing );
            return existing.getReplayController();
        }
        final LarvaReplayPageController controller = createSingleReplayPage( key, select );
        controller.requestAnalysis( sourceDescription );
        return controller;
    }

    /**
     * Admission checks of auto-open (E17-S07) in the order ready, dedupe, cap. Safe to call off nothing but the EDT;
     * a hub that is not installed (or already shut down) answers {@code SKIP_NOT_READY} without an error log (D17-g).
     */
    LarvaAutoOpenPolicy.Admission checkAutoOpenAdmission( final Path replayFile ) {
        if ( !ready || hub == null )
            return LarvaAutoOpenPolicy.Admission.SKIP_NOT_READY;
        if ( !requireEdt( "checkAutoOpenAdmission" ) )
            return LarvaAutoOpenPolicy.Admission.SKIP_NOT_READY;
        final boolean alreadyOpen = hub.findSingleReplayPage( LarvaReplayPaths.normalize( replayFile ) ) != null;
        return LarvaAutoOpenPolicy.admit( true, alreadyOpen, hub.getSubPages( LarvaSubPageKind.SINGLE_REPLAY ).size() );
    }

    /**
     * Adds the sub-page of an auto-detected Zerg replay (E17-S07, FR-17) whose analysis already ran on the queue worker.
     * Re-checks ready, dedupe and cap (a manual open may have happened meanwhile), then creates the page already
     * READY (or ERROR), selected only if the user is on the hub or a Larva sub-page (D17-c). The page component is not
     * created unless the page gets selected; the controller holds the result.
     *
     * @param replayFile replay file (normalized here)
     * @param outcome analysis outcome of kind ANALYZED or ANALYSIS_FAILED
     * @return the new page's controller, or <code>null</code> if no page was created
     */
    LarvaReplayPageController autoOpenSingleReplay( final Path replayFile, final LarvaAutoOpenOutcome outcome ) {
        final LarvaAutoOpenPolicy.Admission admission = checkAutoOpenAdmission( replayFile );
        if ( admission != LarvaAutoOpenPolicy.Admission.OPEN ) {
            logger.debug( "[larva-nav] auto-open skipped (" + admission + "): " + replayFile );
            return null;
        }
        final boolean select = LarvaAutoOpenPolicy.shouldSelect( isLarvaPageSelected() );
        final LarvaReplayPageController controller = createSingleReplayPage( LarvaReplayPaths.normalize( replayFile ), select );
        controller.presetResult( LarvaAutoOpenPolicy.SOURCE_MONITOR, outcome.getSummary(), outcome.getMessage() );
        logger.debug( "[larva-nav] auto-opened '" + LarvaReplayPaths.nodeName( controller.getReplayFile() ) + "' select=" + select );
        return controller;
    }

    private LarvaReplayPageController createSingleReplayPage( final Path key, final boolean select ) {
        final LarvaReplayPageController controller = new LarvaReplayPageController( key, singleHost.getAnalysisQueue(), logger );
        final LarvaSubPage page = new LarvaSubPage( LarvaSubPageKind.SINGLE_REPLAY, LarvaReplayPaths.nodeName( key ), singleHost.getReplayIcon(),
                new IPageCompCreator< JComponent >() {
                    @Override
                    public JComponent createPageComp() {
                        return singleHost.createReplayPageComp( controller );
                    }
                } );
        page.setReplayController( controller );
        addSubPage( page, select );
        return controller;
    }

    /** @return the open single-replay page for this replay (any path form), or <code>null</code> (EDT only) */
    LarvaSubPage findSingleReplayPage( final Path replayFile ) {
        if ( !requireEdt( "findSingleReplayPage" ) || !requireReady( "findSingleReplayPage" ) )
            return null;
        return hub.findSingleReplayPage( LarvaReplayPaths.normalize( replayFile ) );
    }

    /** @return the replay of the selected single-replay page, or <code>null</code> if another page is selected (EDT only) */
    Path getSelectedReplayFile() {
        if ( !requireEdt( "getSelectedReplayFile" ) || !requireReady( "getSelectedReplayFile" ) )
            return null;
        final IPage< ? > selected = multiPageComp().getSelectedPage();
        if ( !( selected instanceof LarvaSubPage ) || ( (LarvaSubPage) selected ).getReplayController() == null )
            return null;
        return ( (LarvaSubPage) selected ).getReplayController().getReplayFile();
    }

    private IMultiPageComp multiPageComp() {
        return services.getMainFrame().getMultiPageComp();
    }

    private boolean requireEdt( final String operation ) {
        if ( SwingUtilities.isEventDispatchThread() )
            return true;
        logger.error( "[larva-nav] " + operation + " called off the EDT on thread '" + Thread.currentThread().getName() + "'; ignored" );
        return false;
    }

    private boolean requireReady( final String operation ) {
        if ( ready )
            return true;
        logger.error( "[larva-nav] " + operation + " called before the hub was installed; ignored" );
        return false;
    }

}
