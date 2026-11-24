package hu.aleks.larvasecondslostextmod;

import static org.junit.Assert.assertEquals;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.Test;

/** Eligibility filter and summary (E17-S10, AC 1-4). Synthetic records only; no Scelight runtime needed. */
public class LarvaEligibilityFilterTest {

    private static final int LONG = 10000;

    private final LarvaEligibilityFilter filter = new LarvaEligibilityFilter();

    static LarvaAggregationPlayerRecord player( final String key, final String name ) {
        return new LarvaAggregationPlayerRecord.Builder().identityKey( key ).name( name ).build();
    }

    static LarvaAggregationGameRecord.Builder game( final boolean oneVsOne, final boolean ladder, final int loops ) {
        return new LarvaAggregationGameRecord.Builder().replayFile( Paths.get( "g.SC2Replay" ) ).replayDate( new Date( 1000L ) )
                .oneVsOne( oneVsOne ).ladder( ladder ).lengthLoops( loops ).fullParse( true );
    }

    private static LarvaAggregationGameRecord zerg( final boolean ladder, final int loops ) {
        return game( true, ladder, loops ).addZergPlayer( player( "k", "A" ) ).build();
    }

    private LarvaFilterSummary summarize( final List< LarvaAggregationGameRecord > records, final boolean ladderOnly ) {
        final LarvaFilterSummary.Builder summary = new LarvaFilterSummary.Builder();
        for ( final LarvaAggregationGameRecord r : records )
            summary.add( filter.evaluate( r, ladderOnly ) );
        return summary.build();
    }

    @Test
    public void ac1_ladderOnlyTogglesPrivateGames() {
        final List< LarvaAggregationGameRecord > records = new ArrayList<>();
        for ( int i = 0; i < 3; i++ )
            records.add( zerg( true, LONG ) );
        for ( int i = 0; i < 2; i++ )
            records.add( zerg( false, LONG ) );
        records.add( zerg( true, 4000 ) );
        assertEquals( 3, summarize( records, true ).getEligible() );
        assertEquals( 5, summarize( records, false ).getEligible() );
    }

    @Test
    public void ac2_lengthBoundary() {
        assertEquals( LarvaEligibility.TOO_SHORT, filter.evaluate( zerg( true, 5375 ), false ) );
        assertEquals( LarvaEligibility.ELIGIBLE, filter.evaluate( zerg( true, 5376 ), false ) );
        assertEquals( 5376, LarvaAggregationConstants.MIN_GAME_LENGTH_LOOPS );
    }

    @Test
    public void ac3_nullOrUnknownModeIsNonLadder() {
        // A record built without a game mode has ladder == false.
        final LarvaAggregationGameRecord noMode = new LarvaAggregationGameRecord.Builder().replayFile( Paths.get( "g.SC2Replay" ) )
                .oneVsOne( true ).lengthLoops( LONG ).addZergPlayer( player( "k", "A" ) ).build();
        assertEquals( LarvaEligibility.NON_LADDER, filter.evaluate( noMode, true ) );
        assertEquals( LarvaEligibility.ELIGIBLE, filter.evaluate( noMode, false ) );
    }

    @Test
    public void ac4_failedFileIsCountedAndBatchContinues() {
        final List< LarvaAggregationGameRecord > records = new ArrayList<>();
        records.add( zerg( true, LONG ) );
        records.add( null );
        records.add( zerg( true, LONG ) );
        records.add( game( false, true, LONG ).addZergPlayer( player( "k", "A" ) ).build() );
        records.add( game( true, true, LONG ).build() );
        records.add( zerg( true, 100 ) );
        records.add( zerg( false, LONG ) );
        final LarvaFilterSummary s = summarize( records, true );
        assertEquals( 7, s.getSelected() );
        assertEquals( 2, s.getEligible() );
        assertEquals( 1, s.getCount( LarvaEligibility.FAILED_TO_PARSE ) );
        assertEquals( 1, s.getCount( LarvaEligibility.NOT_1V1 ) );
        assertEquals( 1, s.getCount( LarvaEligibility.NON_LADDER ) );
        assertEquals( 1, s.getCount( LarvaEligibility.TOO_SHORT ) );
        assertEquals( 1, s.getCount( LarvaEligibility.NO_ZERG ) );
        assertEquals( s.getSelected(), s.getEligible() + s.getExcludedTotal() );
        assertEquals( "selected 7 · eligible 2 · excluded: not 1v1 1, non-ladder 1, < 4 min 1, no Zerg 1, failed to parse 1",
                s.toText() );
        assertEquals( s.toText(), s.toString() );
    }

    @Test
    public void reasonOrderIsFixed() {
        // Not 1v1 AND too short AND no Zerg -> NOT_1V1 wins.
        assertEquals( LarvaEligibility.NOT_1V1, filter.evaluate( game( false, false, 10 ).build(), true ) );
        // Non-ladder AND too short AND no Zerg -> NON_LADDER wins.
        assertEquals( LarvaEligibility.NON_LADDER, filter.evaluate( game( true, false, 10 ).build(), true ) );
        // Too short AND no Zerg -> TOO_SHORT wins.
        assertEquals( LarvaEligibility.TOO_SHORT, filter.evaluate( game( true, true, 10 ).build(), true ) );
        assertEquals( LarvaEligibility.NO_ZERG, filter.evaluate( game( true, true, LONG ).build(), true ) );
    }

    @Test
    public void emptySummaryIsAllZero() {
        final LarvaFilterSummary s = new LarvaFilterSummary.Builder().build();
        assertEquals( 0, s.getSelected() );
        assertEquals( "selected 0 · eligible 0 · excluded: not 1v1 0, non-ladder 0, < 4 min 0, no Zerg 0, failed to parse 0",
                s.toText() );
    }

    @Test( expected = IllegalArgumentException.class )
    public void summaryRejectsNullResult() {
        new LarvaFilterSummary.Builder().add( null );
    }

    @Test
    public void labelsAreDefined() {
        for ( final LarvaEligibility e : LarvaEligibility.values() )
            assertEquals( true, e.getLabel().length() > 0 );
    }
}
