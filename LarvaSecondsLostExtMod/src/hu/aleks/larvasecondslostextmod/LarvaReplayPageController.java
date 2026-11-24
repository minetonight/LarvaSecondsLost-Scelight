package hu.aleks.larvasecondslostextmod;

import hu.scelightapibase.service.log.ILogger;

import java.nio.file.Path;

/**
 * State of one single-replay sub-page, bound to exactly one replay. It lives as long as the page, independent of the
 * page component: Scelight creates the component lazily on first selection (S01), so a page that was added without
 * selection (auto-open, S07) can finish its analysis before any component exists. {@link #attach} then renders the
 * current state.
 *
 * <p>Threading: every method except {@link #isDisposed()} runs on the EDT. The analysis worker only hands results
 * back through {@link LarvaReplayAnalysisQueue}, which posts to the EDT.</p>
 */
final class LarvaReplayPageController {

    /** Analysis state of a page. */
    enum State {
        /** Analysis requested or running. */
        BUSY,
        /** Analysis finished, the summary is available. */
        READY,
        /** Analysis failed, the error text is available. */
        ERROR
    }

    private final Path replayFile;
    private final LarvaReplayAnalysisQueue queue;
    private final ILogger logger;

    private State state = State.BUSY;
    private LarvaReplayPageSummary summary;
    private String errorMessage;
    private String lastSourceDescription;
    private long latestRequestId;
    private LarvaReplayPageView view;
    private volatile boolean disposed;

    /**
     * Creates the controller in the BUSY state; call {@link #requestAnalysis} to start the first analysis.
     *
     * @param replayFile absolute, normalized replay path (see {@link LarvaReplayPaths#normalize})
     * @param queue analysis queue
     * @param logger module logger
     */
    LarvaReplayPageController( final Path replayFile, final LarvaReplayAnalysisQueue queue, final ILogger logger ) {
        this.replayFile = replayFile;
        this.queue = queue;
        this.logger = logger;
    }

    /** @return the replay this page is bound to; never changes */
    Path getReplayFile() {
        return replayFile;
    }

    State getState() {
        return state;
    }

    LarvaReplayPageSummary getSummary() {
        return summary;
    }

    String getErrorMessage() {
        return errorMessage;
    }

    /** @return the attached component, or <code>null</code> if it was not created yet or the page is disposed */
    LarvaReplayPageView getView() {
        return view;
    }

    /** Readable from the analysis worker. */
    boolean isDisposed() {
        return disposed;
    }

    /**
     * Starts (or restarts, for Refresh) the analysis in place: the page shows busy and keeps its replay. An older
     * request that is still queued or running is superseded; its result is ignored.
     *
     * @param sourceDescription short label of where the request came from
     */
    void requestAnalysis( final String sourceDescription ) {
        if ( disposed ) {
            logger.debug( "[larva-replay] request ignored, page disposed: " + replayFile );
            return;
        }
        latestRequestId++;
        lastSourceDescription = sourceDescription;
        state = State.BUSY;
        summary = null;
        errorMessage = null;
        if ( view != null )
            view.showBusy( replayFile, sourceDescription );
        queue.submit( this, sourceDescription, latestRequestId );
    }

    /**
     * Auto-open (E17-S07): the replay was already analyzed on the worker before the page existed, so the page starts
     * READY (or ERROR) without a queue request. EDT only; ignored if the page is disposed.
     *
     * @param sourceDescription short label of where the replay came from
     * @param result the analysis result, or <code>null</code> if the analysis failed
     * @param failureMessage the error text when <code>result</code> is <code>null</code>
     */
    void presetResult( final String sourceDescription, final LarvaReplayPageSummary result, final String failureMessage ) {
        if ( disposed )
            return;
        lastSourceDescription = sourceDescription;
        summary = result;
        errorMessage = result == null ? failureMessage : null;
        state = result == null ? State.ERROR : State.READY;
    }

    /** Renders the current state on a freshly created component and keeps it for later updates. */
    void attach( final LarvaReplayPageView newView ) {
        if ( disposed )
            return;
        view = newView;
        switch ( state ) {
        case READY:
            newView.showSummary( summary );
            break;
        case ERROR:
            newView.showError( replayFile, lastSourceDescription, errorMessage );
            break;
        default:
            newView.showBusy( replayFile, lastSourceDescription );
        }
    }

    /** Called on the EDT with a successful result. Results of disposed pages and superseded requests are dropped. */
    void onAnalysisSucceeded( final long requestId, final LarvaReplayPageSummary result ) {
        if ( isStale( requestId ) )
            return;
        state = State.READY;
        summary = result;
        errorMessage = null;
        if ( view != null )
            view.showSummary( result );
    }

    /** Called on the EDT with a failed analysis. Results of disposed pages and superseded requests are dropped. */
    void onAnalysisFailed( final long requestId, final String message ) {
        if ( isStale( requestId ) )
            return;
        state = State.ERROR;
        summary = null;
        errorMessage = message;
        if ( view != null )
            view.showError( replayFile, lastSourceDescription, message );
    }

    /** Called when the page is closed: drops the results of pending work and releases the summary and component. */
    void dispose() {
        if ( disposed )
            return;
        disposed = true;
        view = null;
        summary = null;
        logger.debug( "[larva-replay] page disposed, pending results are dropped: " + replayFile );
    }

    private boolean isStale( final long requestId ) {
        if ( disposed ) {
            logger.debug( "[larva-replay] result dropped, page closed: " + replayFile );
            return true;
        }
        if ( requestId != latestRequestId ) {
            logger.debug( "[larva-replay] result dropped, superseded by a newer request: " + replayFile );
            return true;
        }
        return false;
    }

}
