package hu.aleks.larvasecondslostextmod;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/** Controller state machine, late callbacks and dispose (E17-S14). */
public class LarvaMultiAnalysisPageControllerTest {

    private final List< String > labelsSet = new ArrayList< String >();
    private final List< String > viewCalls = new ArrayList< String >();
    private final List< Path > files = new ArrayList< Path >();
    private int cancels;
    private int starts;
    private LarvaMultiAnalysisListener listener;
    private Locale savedLocale;
    private TimeZone savedZone;

    private final LarvaMultiAnalysisView view = new LarvaMultiAnalysisView() {
        @Override
        public void showProgress( final int k, final int n, final String fileName, final int failedCount ) {
            viewCalls.add( "progress " + k + "/" + n + " " + fileName + " f=" + failedCount );
        }

        @Override
        public void showResult( final LarvaMultiAnalysisViewModel result ) {
            viewCalls.add( "result" );
        }

        @Override
        public void showStopped( final String message ) {
            viewCalls.add( "stopped " + message );
        }
    };

    @Before
    public void setUp() {
        savedLocale = Locale.getDefault();
        savedZone = TimeZone.getDefault();
        Locale.setDefault( Locale.US );
        TimeZone.setDefault( TimeZone.getTimeZone( "UTC" ) );
        for ( int i = 1; i <= 4; i++ )
            files.add( Paths.get( "g" + i + ".SC2Replay" ) );
    }

    @After
    public void tearDown() {
        Locale.setDefault( savedLocale );
        TimeZone.setDefault( savedZone );
    }

    private void complete( final LarvaAggregationResult r ) {
        listener.onCompleted( r, LarvaMultiAnalysisViewModelBuilder.build( r, Locale.US, TimeZone.getTimeZone( "UTC" ) ) );
    }

    private LarvaMultiAnalysisPageController create() {
        final LarvaMultiAnalysisPageController controller = new LarvaMultiAnalysisPageController( files, true, new LarvaMultiPageLabels( true, 1 ),
                new LarvaMultiAnalysisPageController.RunFactory() {
                    @Override
                    public LarvaMultiAnalysisPageController.RunHandle create( final List< Path > f, final boolean ladderOnly,
                            final LarvaMultiAnalysisListener l ) {
                        listener = l;
                        return new LarvaMultiAnalysisPageController.RunHandle() {
                            @Override
                            public void start() {
                                starts++;
                            }

                            @Override
                            public void cancel() {
                                cancels++;
                            }
                        };
                    }
                }, new LarvaMultiAnalysisPageController.LabelSink() {
                    @Override
                    public void setLabel( final String label ) {
                        labelsSet.add( label );
                    }
                }, new StdErrLogger( false, false ) );
        controller.start();
        return controller;
    }

    private static LarvaAggregationResult result( final Date start, final Date end, final int failed ) {
        final LarvaFilterSummary.Builder b = new LarvaFilterSummary.Builder();
        for ( int i = 0; i < 3; i++ )
            b.add( LarvaEligibility.ELIGIBLE );
        for ( int i = 0; i < failed; i++ )
            b.add( LarvaEligibility.FAILED_TO_PARSE );
        return new LarvaAggregationResult( Collections.< LarvaAggregationResult.PlayerResult >emptyList(), 3, start, end, b.build(),
                Collections.< LarvaSkippedReplay >emptyList() );
    }

    @Test
    public void startsRunningWithPlaceholderLabel() {
        final LarvaMultiAnalysisPageController c = create();
        assertEquals( 1, starts );
        assertEquals( LarvaMultiAnalysisPageController.State.RUNNING, c.getState() );
        assertEquals( "Multi analysis (running…)", c.getLabel() );
        assertTrue( labelsSet.isEmpty() );
    }

    @Test
    public void progressUpdatesSnapshotAndAttachedView() {
        final LarvaMultiAnalysisPageController c = create();
        c.attach( view );
        listener.onProgress( 2, 4, "g2.SC2Replay", 1 );
        assertEquals( 2, c.getCurrentK() );
        assertEquals( 1, c.getFailedCount() );
        assertEquals( "g2.SC2Replay", c.getCurrentFileName() );
        assertEquals( "progress 0/4  f=0", viewCalls.get( 0 ) );
        assertEquals( "progress 2/4 g2.SC2Replay f=1", viewCalls.get( 1 ) );
    }

    @Test
    public void completionRenamesToTheDateRangeAndCountsFailedFiles() {
        final LarvaMultiAnalysisPageController c = create();
        c.attach( view );
        complete( result( new Date( 1700000000000L ), new Date( 1700000000000L + 5L * 86400000L ), 1 ) );
        assertEquals( LarvaMultiAnalysisPageController.State.DONE, c.getState() );
        assertEquals( "from 11/14/23 to 11/19/23", c.getLabel() );
        assertEquals( labelsSet, Collections.singletonList( "from 11/14/23 to 11/19/23" ) );
        assertEquals( 1, c.getFailedCount() );
        assertEquals( 4, c.getCurrentK() );
        assertEquals( "result", viewCalls.get( viewCalls.size() - 1 ) );
    }

    @Test
    public void completionWithoutEligibleGamesKeepsANumberedLabel() {
        final LarvaMultiAnalysisPageController c = create();
        complete( result( null, null, 0 ) );
        assertEquals( "Multi analysis #1 (no games)", c.getLabel() );
    }

    @Test
    public void cancelledAndFailedRunsGetTheirLabels() {
        final LarvaMultiAnalysisPageController cancelled = create();
        listener.onProgress( 2, 4, "g2.SC2Replay", 0 );
        listener.onCancelled();
        assertEquals( LarvaMultiAnalysisPageController.State.CANCELLED, cancelled.getState() );
        assertEquals( "Multi analysis #1 (cancelled)", cancelled.getLabel() );

        final LarvaMultiAnalysisPageController failed = create();
        listener.onFailed( new IllegalStateException( "boom" ) );
        assertEquals( LarvaMultiAnalysisPageController.State.FAILED, failed.getState() );
        assertEquals( "Multi analysis #1 (failed)", failed.getLabel() );
    }

    @Test
    public void cancelRunOnlyWhileRunning() {
        final LarvaMultiAnalysisPageController c = create();
        c.cancelRun();
        assertEquals( 1, cancels );
        listener.onCancelled();
        c.cancelRun();
        assertEquals( 1, cancels );
    }

    @Test
    public void disposeCancelsTheRunAndIgnoresLateCallbacks() {
        final LarvaMultiAnalysisPageController c = create();
        c.attach( view );
        viewCalls.clear();
        c.dispose();
        assertEquals( 1, cancels );
        assertTrue( c.isDisposed() );
        assertNull( c.getView() );
        listener.onProgress( 3, 4, "g3.SC2Replay", 0 );
        complete( result( new Date( 0L ), new Date( 0L ), 0 ) );
        listener.onCancelled();
        listener.onFailed( new RuntimeException() );
        assertTrue( viewCalls.isEmpty() );
        assertTrue( labelsSet.isEmpty() );
        assertEquals( LarvaMultiAnalysisPageController.State.RUNNING, c.getState() );
        assertNull( c.getResult() );
    }

    @Test
    public void disposeIsIdempotentAndDoesNotCancelAFinishedRun() {
        final LarvaMultiAnalysisPageController c = create();
        complete( result( new Date( 0L ), new Date( 0L ), 0 ) );
        c.dispose();
        c.dispose();
        assertEquals( 0, cancels );
        assertNull( c.getResult() );
    }

    @Test
    public void attachAfterCompletionRendersTheResult() {
        final LarvaMultiAnalysisPageController c = create();
        complete( result( new Date( 0L ), new Date( 0L ), 0 ) );
        c.attach( view );
        assertEquals( Collections.singletonList( "result" ), viewCalls );
    }
}
