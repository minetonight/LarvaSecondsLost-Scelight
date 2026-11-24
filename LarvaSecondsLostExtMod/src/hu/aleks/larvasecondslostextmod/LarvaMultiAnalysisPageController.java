package hu.aleks.larvasecondslostextmod;

import hu.scelightapibase.service.log.ILogger;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * State of one multi-analysis sub-page (E17-S14): the run, its progress snapshot, the result and the tree label. It lives
 * as long as the page, independent of the lazily created component (S01). Every method runs on the EDT, except
 * {@link #isDisposed()}.
 *
 * <p>{@link #dispose()} cancels the run and drops the result (FR-15). Callbacks that arrive afterwards (the runner posts
 * them to the EDT, so they can be queued already) are ignored.</p>
 */
final class LarvaMultiAnalysisPageController implements LarvaMultiAnalysisListener {

    /** Life cycle of a page. */
    enum State {
        RUNNING, DONE, CANCELLED, FAILED
    }

    /** A started batch run. */
    interface RunHandle {
        void start();

        void cancel();
    }

    /** Creates the run of a page; production wiring uses {@link LarvaMultiAnalysisRunnerFactory}. */
    interface RunFactory {
        RunHandle create( List< Path > files, boolean ladderOnly, LarvaMultiAnalysisListener listener );
    }

    /** Receives the new tree label of the page. */
    interface LabelSink {
        void setLabel( String label );
    }

    private final List< Path > files;
    private final boolean ladderOnly;
    private final LarvaMultiPageLabels labels;
    private final RunFactory runFactory;
    private final LabelSink labelSink;
    private final ILogger logger;

    private State state = State.RUNNING;
    private int currentK;
    private int failedCount;
    private String currentFileName = "";
    private String label;
    private LarvaAggregationResult result;
    private LarvaMultiAnalysisViewModel viewModel;
    private String stopMessage;
    private RunHandle run;
    private LarvaMultiAnalysisView view;
    private volatile boolean disposed;

    LarvaMultiAnalysisPageController( final List< Path > files, final boolean ladderOnly, final LarvaMultiPageLabels labels,
            final RunFactory runFactory, final LabelSink labelSink, final ILogger logger ) {
        this.files = Collections.unmodifiableList( new ArrayList< Path >( files ) );
        this.ladderOnly = ladderOnly;
        this.labels = labels;
        this.runFactory = runFactory;
        this.labelSink = labelSink;
        this.logger = logger;
        this.label = labels.running();
    }

    /** Creates and starts the run. May be called once. */
    void start() {
        if ( run != null )
            throw new IllegalStateException( "Multi analysis already started." );
        run = runFactory.create( files, ladderOnly, this );
        run.start();
    }

    /** Asks the run to stop; the page ends in CANCELLED when the runner reports it. No-op when not running. */
    void cancelRun() {
        if ( run != null && state == State.RUNNING && !disposed )
            run.cancel();
    }

    /** Renders the current state on a freshly created component and keeps it for later updates. */
    void attach( final LarvaMultiAnalysisView newView ) {
        if ( disposed )
            return;
        view = newView;
        switch ( state ) {
        case DONE:
            newView.showResult( viewModel );
            break;
        case RUNNING:
            newView.showProgress( currentK, files.size(), currentFileName, failedCount );
            break;
        default:
            newView.showStopped( stopMessage );
        }
    }

    @Override
    public void onProgress( final int k, final int n, final String fileName, final int failed ) {
        if ( isLate( "progress" ) || state != State.RUNNING )
            return;
        currentK = k;
        currentFileName = fileName;
        failedCount = failed;
        if ( view != null )
            view.showProgress( k, n, fileName, failed );
    }

    @Override
    public void onCompleted( final LarvaAggregationResult aggregated, final LarvaMultiAnalysisViewModel model ) {
        if ( isLate( "completion" ) )
            return;
        state = State.DONE;
        result = aggregated;
        viewModel = model;
        currentK = files.size();
        failedCount = aggregated.getFilterSummary().getCount( LarvaEligibility.FAILED_TO_PARSE );
        setLabel( labels.completed( aggregated.getStartDate(), aggregated.getEndDate(), Locale.getDefault(), TimeZone.getDefault() ) );
        if ( view != null )
            view.showResult( model );
    }

    @Override
    public void onCancelled() {
        if ( isLate( "cancellation" ) )
            return;
        state = State.CANCELLED;
        stopMessage = "Analysis cancelled after " + currentK + " of " + files.size() + " replays.";
        setLabel( labels.cancelled() );
        if ( view != null )
            view.showStopped( stopMessage );
    }

    @Override
    public void onFailed( final Throwable error ) {
        if ( isLate( "failure" ) )
            return;
        state = State.FAILED;
        stopMessage = "Analysis failed: " + error;
        setLabel( labels.failed() );
        if ( view != null )
            view.showStopped( stopMessage );
    }

    /** Closing the page: cancels the run, drops result and component. Idempotent. */
    void dispose() {
        if ( disposed )
            return;
        disposed = true;
        view = null;
        result = null;
        viewModel = null;
        if ( run != null && state == State.RUNNING )
            run.cancel();
        logger.debug( "[larva-multi] page disposed, run cancelled, pending callbacks are dropped (" + files.size() + " files)" );
    }

    private void setLabel( final String newLabel ) {
        label = newLabel;
        labelSink.setLabel( newLabel );
    }

    private boolean isLate( final String what ) {
        if ( !disposed )
            return false;
        logger.debug( "[larva-multi] late " + what + " callback ignored, page closed" );
        return true;
    }

    State getState() {
        return state;
    }

    int getCurrentK() {
        return currentK;
    }

    int getTotal() {
        return files.size();
    }

    int getFailedCount() {
        return failedCount;
    }

    String getCurrentFileName() {
        return currentFileName;
    }

    String getLabel() {
        return label;
    }

    boolean isLadderOnly() {
        return ladderOnly;
    }

    LarvaMultiAnalysisViewModel getViewModel() {
        return viewModel;
    }

    LarvaAggregationResult getResult() {
        return result;
    }

    /** @return the attached component, or <code>null</code> if not created yet or disposed */
    LarvaMultiAnalysisView getView() {
        return view;
    }

    /** Readable from any thread. */
    boolean isDisposed() {
        return disposed;
    }
}
