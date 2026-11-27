package hu.aleks.larvasecondslostextmod;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import org.junit.Test;

/** The headless dump must not depend on the machine: folders, locale, time zone (E17-S12, criterion 4). */
public class LarvaMultiAnalysisDumpFormatterTest {

    private static LarvaAggregationResult sampleResult( final String folder ) {
        final LarvaFakeAggregationSource source = new LarvaFakeAggregationSource();
        final List< Path > files = new ArrayList<>();
        for ( int i = 1; i <= 3; i++ ) {
            final String name = "g" + i + ".SC2Replay";
            final Path path = Paths.get( folder, name );
            files.add( path );
            final LarvaAggregationPlayerRecord.Builder player = LarvaTestFixtures.withStats(
                    LarvaTestFixtures.player( "Me", i == 3 ? LarvaMatchupBucket.ZVP : LarvaMatchupBucket.ZVT ),
                    LarvaTestFixtures.stats( LarvaGamePhase.EARLY, 2 * i, i, 30 * i, 20000L * i, 15000L * i, 9000L * i ) );
            LarvaTestFixtures.withEnd( player, LarvaGamePhase.EARLY, 240000L + i * 1500L );
            LarvaTestFixtures.withMilestone( player, LarvaMilestone.DRONES_66, 6720 + i * 160 );
            source.add( name, new LarvaAggregationGameRecord.Builder().replayFile( path ).replayDate( new Date( 1700000000000L + i * 86400000L ) )
                    .oneVsOne( true ).ladder( true ).lengthLoops( 10000 ).addZergPlayer( player.build() ).build() );
        }
        files.add( Paths.get( folder, "broken.SC2Replay" ) );
        return new LarvaMultiAnalysisPipeline( new StdErrLogger( false, false ), source, true ).run( files, true, new LarvaProgressSink() {
            @Override
            public void onProgress( final int k, final int n, final String fileName, final int failedCount ) {
            }
        }, new LarvaCancellationToken() );
    }

    @Test
    public void dumpHasFileNamesOnlyAndTheFixedSections() {
        final String dump = new LarvaMultiAnalysisDumpFormatter().format( sampleResult( "/abs/secret/folder" ) );
        assertFalse( dump, dump.contains( "/abs" ) || dump.contains( "secret" ) );
        assertTrue( dump, dump.startsWith( "== Multi analysis dump ==\nfilter: selected 4 · eligible 3 · excluded: not 1v1 0, non-ladder 0, < 4 min 0, no Zerg 0, failed to parse 1\n" ) );
        assertTrue( dump, dump.contains( "distinct eligible games: 3\n" ) );
        assertTrue( dump, dump.contains( "date range (UTC): 2023-11-15 22:13:20 .. 2023-11-17 22:13:20\n" ) );
        assertTrue( dump, dump.contains( "  broken.SC2Replay | " + LarvaSkippedReplay.REASON_FAILED_TO_PARSE + "\n" ) );
        assertTrue( dump, dump.contains( "player 1: name=Me key=Me games=3" ) );
        // E20-S04/S06: median loop of 6880, 7040, 7200 (ZvT, ZvT, ZvP) with its game clock time and the ranking of the bucket
        assertTrue( dump, dump.contains( "    milestone 66 drones: 5:14 (7040 loops) (3/3) rank=top 5% pro\n" ) );
        assertTrue( dump, dump.contains( "    milestone 80 drones: n/a (0/3) rank=n/a\n" ) );
        assertTrue( dump, dump.contains( "    phase EARLY: missed=12 missedInject=6 spawned=180 hatchEligibleLoops=120000 injectEligibleLoops=90000 injectActiveLoops=54000 |" ) );
        assertTrue( dump, dump.contains( "    phase END: no stats | missed/h/min=n/a" ) );
        assertTrue( dump, dump.contains( "  bucket ZVZ games=0\n" ) );
        assertTrue( dump, dump.endsWith( "\n" ) && !dump.contains( "\r" ) );
    }

    @Test
    public void dumpIsIdenticalForTwoRunsAndIndependentOfFolderLocaleAndTimeZone() {
        final String first = new LarvaMultiAnalysisDumpFormatter().format( sampleResult( "/one/folder" ) );
        final Locale oldLocale = Locale.getDefault();
        final TimeZone oldZone = TimeZone.getDefault();
        try {
            Locale.setDefault( new Locale( "bg", "BG" ) );
            TimeZone.setDefault( TimeZone.getTimeZone( "Pacific/Auckland" ) );
            final String other = new LarvaMultiAnalysisDumpFormatter().format( sampleResult( "/other/place" ) );
            assertEquals( first, other );
            Locale.setDefault( Locale.GERMANY );
            assertEquals( first, new LarvaMultiAnalysisDumpFormatter().format( sampleResult( "/one/folder" ) ) );
        } finally {
            Locale.setDefault( oldLocale );
            TimeZone.setDefault( oldZone );
        }
    }

    @Test
    public void emptyResultStillHasAllHeaderLines() {
        final LarvaAggregationResult empty = new LarvaAggregator().aggregate( Collections.< LarvaAggregationGameRecord > emptyList(),
                Collections.< LarvaTrackedPlayer > emptyList(), new LarvaFilterSummary.Builder().build() );
        final String dump = new LarvaMultiAnalysisDumpFormatter().format( empty );
        assertTrue( dump, dump.contains( "date range (UTC): none .. none\n" ) );
        assertTrue( dump, dump.contains( "tracked players: 0\n" ) );
    }
}
