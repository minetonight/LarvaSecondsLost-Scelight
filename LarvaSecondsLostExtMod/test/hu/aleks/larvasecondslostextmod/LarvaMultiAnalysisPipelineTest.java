package hu.aleks.larvasecondslostextmod;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

/** Batch pipeline with a fake record source (E17-S12): ordering, failures, pre-filter, cancellation, progress. */
public class LarvaMultiAnalysisPipelineTest {

    private final LarvaFakeAggregationSource source = new LarvaFakeAggregationSource();

    private final List< String > progress = new ArrayList<>();

    private final LarvaProgressSink sink = new LarvaProgressSink() {
        @Override
        public void onProgress( final int k, final int n, final String fileName, final int failedCount ) {
            progress.add( k + "/" + n + " " + fileName + " failed=" + failedCount );
        }
    };

    private static List< Path > files( final String... names ) {
        final List< Path > files = new ArrayList<>();
        for ( final String name : names )
            files.add( Paths.get( "/some/folder", name ) );
        return files;
    }

    private static LarvaAggregationGameRecord eligible( final String name, final long day ) {
        return new LarvaAggregationGameRecord.Builder().replayFile( Paths.get( "/some/folder", name ) ).replayDate( new java.util.Date( day * LarvaTestFixtures.DAY_MS ) )
                .oneVsOne( true ).ladder( true ).lengthLoops( 10000 ).fullParse( true )
                .addZergPlayer( LarvaTestFixtures.player( "Me", LarvaMatchupBucket.ZVT ).build() ).build();
    }

    private static LarvaAggregationGameRecord notOneVsOne( final String name ) {
        return new LarvaAggregationGameRecord.Builder().replayFile( Paths.get( "/some/folder", name ) ).oneVsOne( false ).ladder( true )
                .lengthLoops( 10000 ).build();
    }

    private LarvaAggregationResult run( final boolean preFilter, final boolean ladderOnly, final LarvaCancellationToken token, final String... names ) {
        return new LarvaMultiAnalysisPipeline( new StdErrLogger( false, false ), source, preFilter ).run( files( names ), ladderOnly, sink, token );
    }

    @Test
    public void everySelectedFileIsCountedExactlyOnce() {
        source.add( "a.SC2Replay", eligible( "a.SC2Replay", 1 ) ).add( "b.SC2Replay", notOneVsOne( "b.SC2Replay" ) )
                .add( "c.SC2Replay", eligible( "c.SC2Replay", 2 ) );
        source.metadata.put( "d.SC2Replay", null ); // unparsable
        final LarvaAggregationResult result = run( true, true, new LarvaCancellationToken(), "a.SC2Replay", "b.SC2Replay", "c.SC2Replay", "d.SC2Replay" );
        final LarvaFilterSummary summary = result.getFilterSummary();
        assertEquals( 4, summary.getSelected() );
        assertEquals( 2, summary.getEligible() );
        assertEquals( 1, summary.getCount( LarvaEligibility.NOT_1V1 ) );
        assertEquals( 1, summary.getCount( LarvaEligibility.FAILED_TO_PARSE ) );
        assertEquals( 2, result.getDistinctGameCount() );
        assertEquals( 1, result.getPlayers().size() );
    }

    @Test
    public void progressIsReportedBeforeEachFileWithOneBasedIndexAndFailedCount() {
        source.add( "a.SC2Replay", eligible( "a.SC2Replay", 1 ) ).add( "c.SC2Replay", eligible( "c.SC2Replay", 2 ) );
        source.metadataFailures.put( "b.SC2Replay", new IllegalStateException( ReplaySummaryService.MESSAGE_COULD_NOT_PARSE ) );
        run( true, true, new LarvaCancellationToken(), "a.SC2Replay", "b.SC2Replay", "c.SC2Replay" );
        assertEquals( Arrays.asList( "1/3 a.SC2Replay failed=0", "2/3 b.SC2Replay failed=0", "3/3 c.SC2Replay failed=1" ), progress );
    }

    @Test
    public void resultOrderFollowsSelectionOrderNotTheNames() {
        // The tracked player's most recent name comes from the newest game; the skipped list keeps selection order.
        source.metadata.put( "z.SC2Replay", null );
        source.metadata.put( "a.SC2Replay", null );
        final LarvaAggregationResult result = run( true, true, new LarvaCancellationToken(), "z.SC2Replay", "a.SC2Replay" );
        assertEquals( "z.SC2Replay", result.getSkipped().get( 0 ).getFile() );
        assertEquals( "a.SC2Replay", result.getSkipped().get( 1 ).getFile() );
    }

    @Test
    public void nullRecordFromThePreFilterIsListedAsFailedToParse() {
        source.metadata.put( "x.SC2Replay", null );
        final LarvaAggregationResult result = run( true, true, new LarvaCancellationToken(), "x.SC2Replay" );
        assertEquals( 1, result.getFilterSummary().getCount( LarvaEligibility.FAILED_TO_PARSE ) );
        assertEquals( 1, result.getSkipped().size() );
        assertEquals( "x.SC2Replay", result.getSkipped().get( 0 ).getFile() );
        assertEquals( LarvaSkippedReplay.REASON_FAILED_TO_PARSE, result.getSkipped().get( 0 ).getReason() );
        assertTrue( "no full analysis for an unparsable file", source.fullCalls.isEmpty() );
    }

    @Test
    public void nullRecordFromTheFullPathIsListedAsFailedToParse() {
        source.full.put( "x.SC2Replay", null );
        final LarvaAggregationResult result = run( false, true, new LarvaCancellationToken(), "x.SC2Replay" );
        assertEquals( 1, result.getFilterSummary().getCount( LarvaEligibility.FAILED_TO_PARSE ) );
        assertEquals( LarvaSkippedReplay.REASON_FAILED_TO_PARSE, result.getSkipped().get( 0 ).getReason() );
        assertTrue( "no pre-filter call", source.metadataCalls.isEmpty() );
    }

    @Test
    public void nullProcessorExceptionOfTheServiceIsListedWithTheStandardReason() {
        // ReplaySummaryService throws this when getRepProc returns null (3+ player and Archon games).
        final RuntimeException nullProcessor = new IllegalStateException( ReplaySummaryService.MESSAGE_COULD_NOT_PARSE );
        source.metadataFailures.put( "archon.SC2Replay", nullProcessor );
        source.fullFailures.put( "archon2.SC2Replay", nullProcessor );
        final LarvaAggregationResult viaPreFilter = run( true, true, new LarvaCancellationToken(), "archon.SC2Replay" );
        final LarvaAggregationResult viaFullPath = run( false, true, new LarvaCancellationToken(), "archon2.SC2Replay" );
        for ( final LarvaAggregationResult result : Arrays.asList( viaPreFilter, viaFullPath ) ) {
            assertEquals( 1, result.getFilterSummary().getCount( LarvaEligibility.FAILED_TO_PARSE ) );
            assertEquals( LarvaSkippedReplay.REASON_FAILED_TO_PARSE, result.getSkipped().get( 0 ).getReason() );
            assertTrue( result.getFilterSummary().toText().endsWith( "failed to parse 1" ) );
        }
    }

    @Test
    public void unexpectedExceptionIsCapturedWithItsMessageAndTheBatchContinues() {
        source.add( "a.SC2Replay", eligible( "a.SC2Replay", 1 ) ).add( "c.SC2Replay", eligible( "c.SC2Replay", 2 ) );
        source.fullFailures.put( "b.SC2Replay", new IllegalArgumentException( "corrupt MPQ" ) );
        source.metadata.put( "b.SC2Replay", eligible( "b.SC2Replay", 3 ) );
        final LarvaAggregationResult result = run( true, true, new LarvaCancellationToken(), "a.SC2Replay", "b.SC2Replay", "c.SC2Replay" );
        assertEquals( 2, result.getFilterSummary().getEligible() );
        assertEquals( 1, result.getFilterSummary().getCount( LarvaEligibility.FAILED_TO_PARSE ) );
        assertEquals( "b.SC2Replay", result.getSkipped().get( 0 ).getFile() );
        assertEquals( LarvaSkippedReplay.REASON_FAILED_TO_PARSE + ": corrupt MPQ", result.getSkipped().get( 0 ).getReason() );
        assertEquals( "c.SC2Replay analyzed after the failure", Arrays.asList( "a.SC2Replay", "b.SC2Replay", "c.SC2Replay" ), source.fullCalls );
    }

    @Test
    public void exceptionWithoutMessageIsListedWithItsClassName() {
        source.metadataFailures.put( "x.SC2Replay", new NullPointerException() );
        final LarvaAggregationResult result = run( true, true, new LarvaCancellationToken(), "x.SC2Replay" );
        assertEquals( LarvaSkippedReplay.REASON_FAILED_TO_PARSE + ": NullPointerException", result.getSkipped().get( 0 ).getReason() );
    }

    @Test
    public void preFilterSkipsTheFullAnalysisOfIneligibleFiles() {
        source.add( "a.SC2Replay", eligible( "a.SC2Replay", 1 ) ).add( "b.SC2Replay", notOneVsOne( "b.SC2Replay" ) );
        final LarvaAggregationResult result = run( true, true, new LarvaCancellationToken(), "a.SC2Replay", "b.SC2Replay" );
        assertEquals( Arrays.asList( "a.SC2Replay" ), source.fullCalls );
        assertEquals( Arrays.asList( "a.SC2Replay", "b.SC2Replay" ), source.metadataCalls );
        assertEquals( 1, result.getFilterSummary().getCount( LarvaEligibility.NOT_1V1 ) );
    }

    @Test
    public void withoutPreFilterEveryFileIsAnalyzedFullyAndGivesTheSameSummary() {
        source.add( "a.SC2Replay", eligible( "a.SC2Replay", 1 ) ).add( "b.SC2Replay", notOneVsOne( "b.SC2Replay" ) );
        final LarvaAggregationResult result = run( false, true, new LarvaCancellationToken(), "a.SC2Replay", "b.SC2Replay" );
        assertEquals( Arrays.asList( "a.SC2Replay", "b.SC2Replay" ), source.fullCalls );
        assertTrue( source.metadataCalls.isEmpty() );
        assertEquals( "selected 2 · eligible 1 · excluded: not 1v1 1, non-ladder 0, < 4 min 0, no Zerg 0, failed to parse 0",
                result.getFilterSummary().toText() );
    }

    @Test
    public void cancelDuringAFileStopsBeforeTheNextOneAndKeepsNoResult() {
        final LarvaCancellationToken token = new LarvaCancellationToken();
        for ( final String name : Arrays.asList( "a.SC2Replay", "b.SC2Replay", "c.SC2Replay" ) )
            source.add( name, eligible( name, 1 ) );
        source.onAnalyze = new Runnable() {
            @Override
            public void run() {
                if ( source.fullCalls.size() == 2 )
                    token.cancel(); // while b is being analyzed
            }
        };
        try {
            run( true, true, token, "a.SC2Replay", "b.SC2Replay", "c.SC2Replay" );
            fail( "expected cancellation" );
        } catch ( final LarvaBatchCancelledException e ) {
            assertEquals( 2, e.getProcessed() );
            assertEquals( 3, e.getTotal() );
            assertEquals( "cancelled after 2/3", e.getMessage() );
        }
        assertEquals( Arrays.asList( "a.SC2Replay", "b.SC2Replay" ), source.fullCalls );
        assertEquals( 2, progress.size() );
    }

    @Test
    public void cancelledBeforeTheStartProcessesNothing() {
        final LarvaCancellationToken token = new LarvaCancellationToken();
        token.cancel();
        try {
            run( true, true, token, "a.SC2Replay" );
            fail( "expected cancellation" );
        } catch ( final LarvaBatchCancelledException e ) {
            assertEquals( 0, e.getProcessed() );
        }
        assertTrue( source.metadataCalls.isEmpty() && progress.isEmpty() );
    }

    @Test
    public void emptySelectionGivesAnEmptyResult() {
        final LarvaAggregationResult result = run( true, true, new LarvaCancellationToken() );
        assertEquals( 0, result.getFilterSummary().getSelected() );
        assertTrue( result.getPlayers().isEmpty() && result.getSkipped().isEmpty() );
    }

    @Test
    public void missingPhaseTableEntriesFromTheAggregatorAreKeptAfterFailedFiles() {
        final LarvaAggregationGameRecord noTable = new LarvaAggregationGameRecord.Builder().replayFile( Paths.get( "/some/folder", "m.SC2Replay" ) )
                .replayDate( new java.util.Date( 0L ) ).oneVsOne( true ).ladder( true ).lengthLoops( 10000 )
                .addZergPlayer( LarvaTestFixtures.player( "Me", LarvaMatchupBucket.ZVT ).missingPhaseTable( true ).build() ).build();
        source.add( "a.SC2Replay", eligible( "a.SC2Replay", 1 ) ).add( "m.SC2Replay", noTable );
        source.metadata.put( "bad.SC2Replay", null );
        final LarvaAggregationResult result = run( true, true, new LarvaCancellationToken(), "bad.SC2Replay", "a.SC2Replay", "m.SC2Replay" );
        assertEquals( "bad.SC2Replay", result.getSkipped().get( 0 ).getFile() );
        assertEquals( LarvaSkippedReplay.REASON_MISSING_PHASE_TABLE, result.getSkipped().get( 1 ).getReason() );
    }
}
