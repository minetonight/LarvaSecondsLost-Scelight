package hu.aleks.larvasecondslostextmod;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.junit.Test;

/** Tracked-player selection (E17-S10, AC 5-10). Synthetic records only. */
public class LarvaTrackedPlayerSelectorTest {

    private static final long DAY = 86400000L;

    private final LarvaTrackedPlayerSelector selector = new LarvaTrackedPlayerSelector();

    private static LarvaAggregationGameRecord game( final long day, final String key, final String name ) {
        return gameWith( day, new LarvaAggregationPlayerRecord.Builder().identityKey( key ).name( name ).build() );
    }

    private static LarvaAggregationGameRecord gameWith( final long day, final LarvaAggregationPlayerRecord... players ) {
        final LarvaAggregationGameRecord.Builder b = new LarvaAggregationGameRecord.Builder()
                .replayFile( Paths.get( "r" + day + ".SC2Replay" ) ).replayDate( new Date( day * DAY ) ).oneVsOne( true ).ladder( true )
                .lengthLoops( 10000 ).fullParse( true );
        for ( final LarvaAggregationPlayerRecord p : players )
            b.addZergPlayer( p );
        return b.build();
    }

    private static void addGames( final List< LarvaAggregationGameRecord > list, final int n, final long firstDay, final String key,
            final String name ) {
        for ( int i = 0; i < n; i++ )
            list.add( game( firstDay + i, key, name ) );
    }

    @Test
    public void ac5_onlyTheMainPlayerIsTrackedAgainstOneOffOpponents() {
        final List< LarvaAggregationGameRecord > games = new ArrayList<>();
        addGames( games, 40, 100, "A", "Scorpion" );
        // Opponents (also Zerg in ZvZ), at most twice each.
        for ( int i = 0; i < 20; i++ )
            games.add( gameWith( 200 + i, new LarvaAggregationPlayerRecord.Builder().identityKey( "A" ).name( "Scorpion" ).build(),
                    new LarvaAggregationPlayerRecord.Builder().identityKey( "opp" + ( i / 2 ) ).name( "Opp" + ( i / 2 ) ).build() ) );
        final List< LarvaTrackedPlayer > tracked = selector.select( games );
        assertEquals( 1, tracked.size() );
        assertEquals( "Scorpion", tracked.get( 0 ).getDisplayName() );
        assertEquals( 60, tracked.get( 0 ).getGameCount() );
    }

    @Test
    public void ac6_twoMainAccountsAreBothTracked() {
        final List< LarvaAggregationGameRecord > games = new ArrayList<>();
        addGames( games, 15, 100, "B", "Other" );
        addGames( games, 20, 200, "A", "Main" );
        final List< LarvaTrackedPlayer > tracked = selector.select( games );
        assertEquals( 2, tracked.size() );
        assertEquals( "Main", tracked.get( 0 ).getDisplayName() );
        assertEquals( "Other", tracked.get( 1 ).getDisplayName() );
    }

    @Test
    public void ac7_nonZergGamesOfAnAccountAreNotCounted() {
        // Records list only Zerg players, so A's 5 Terran games contribute no player entry.
        final List< LarvaAggregationGameRecord > games = new ArrayList<>();
        addGames( games, 10, 100, "A", "Main" );
        for ( int i = 0; i < 5; i++ )
            games.add( gameWith( 300 + i, new LarvaAggregationPlayerRecord.Builder().identityKey( "z" ).name( "Z" ).build() ) );
        final List< LarvaTrackedPlayer > tracked = selector.select( games );
        assertEquals( 10, tracked.get( 0 ).getGameCount() );
    }

    @Test
    public void ac8_equalCountsRecencyThenNameDecide() {
        List< LarvaAggregationGameRecord > games = new ArrayList<>();
        addGames( games, 5, 100, "A", "Alpha" );
        addGames( games, 5, 200, "B", "Bravo" );
        assertEquals( "Bravo", selector.select( games ).get( 0 ).getDisplayName() );

        games = new ArrayList<>();
        addGames( games, 5, 100, "A", "zulu" );
        addGames( games, 5, 100, "B", "Alpha" );
        assertEquals( "Alpha", selector.select( games ).get( 0 ).getDisplayName() );
        assertEquals( "zulu", selector.select( games ).get( 1 ).getDisplayName() );
    }

    @Test
    public void ac9_secondPlayerNeedsTwentyPercent() {
        List< LarvaAggregationGameRecord > games = new ArrayList<>();
        addGames( games, 17, 100, "A", "Main" );
        addGames( games, 3, 200, "B", "Other" );
        assertEquals( 1, selector.select( games ).size() );

        games = new ArrayList<>();
        addGames( games, 16, 100, "A", "Main" );
        addGames( games, 4, 200, "B", "Other" );
        assertEquals( 2, selector.select( games ).size() );
    }

    @Test
    public void secondPlayerNeedsThreeGamesEvenAbovePercent() {
        final List< LarvaAggregationGameRecord > games = new ArrayList<>();
        addGames( games, 4, 100, "A", "Main" );
        addGames( games, 2, 200, "B", "Other" ); // 33 % but only 2 games
        assertEquals( 1, selector.select( games ).size() );
    }

    @Test
    public void atMostTwoPlayersAreTracked() {
        final List< LarvaAggregationGameRecord > games = new ArrayList<>();
        addGames( games, 5, 100, "A", "A" );
        addGames( games, 5, 200, "B", "B" );
        addGames( games, 5, 300, "C", "C" );
        assertEquals( 2, selector.select( games ).size() );
    }

    @Test
    public void ac10_latestNameIsDisplayedForOneAccount() {
        final List< LarvaAggregationGameRecord > games = new ArrayList<>();
        addGames( games, 4, 100, "A", "OldName" );
        games.add( game( 500, "A", "NewName" ) );
        // Newest game listed first: order of the list must not matter.
        Collections.reverse( games );
        final List< LarvaTrackedPlayer > tracked = selector.select( games );
        assertEquals( 1, tracked.size() );
        assertEquals( "NewName", tracked.get( 0 ).getDisplayName() );
        assertEquals( 5, tracked.get( 0 ).getGameCount() );
        assertEquals( new Date( 500 * DAY ), tracked.get( 0 ).getLastGameDate() );
    }

    @Test
    public void missingPhaseTablePlayerGamesAreNotCountedButListed() {
        final List< LarvaAggregationGameRecord > games = new ArrayList<>();
        addGames( games, 3, 100, "A", "Main" );
        games.add( gameWith( 400, new LarvaAggregationPlayerRecord.Builder().identityKey( "A" ).name( "Main" )
                .missingPhaseTable( true ).build() ) );
        assertEquals( 3, selector.select( games ).get( 0 ).getGameCount() );
        final List< String > missing = selector.listMissingPhaseTable( games );
        assertEquals( 1, missing.size() );
        assertEquals( "r400.SC2Replay (Main)", missing.get( 0 ) );
    }

    @Test
    public void emptyInputGivesNoTrackedPlayers() {
        assertTrue( selector.select( new ArrayList< LarvaAggregationGameRecord >() ).isEmpty() );
        assertTrue( selector.listMissingPhaseTable( new ArrayList< LarvaAggregationGameRecord >() ).isEmpty() );
    }

    @Test
    public void undatedGamesRankAsOldest() {
        final List< LarvaAggregationGameRecord > games = new ArrayList<>();
        games.add( new LarvaAggregationGameRecord.Builder().replayFile( Paths.get( "u.SC2Replay" ) ).oneVsOne( true )
                .lengthLoops( 10000 ).addZergPlayer( new LarvaAggregationPlayerRecord.Builder().identityKey( "U" ).name( "Undated" ).build() )
                .build() );
        games.add( game( 5, "D", "Dated" ) );
        final List< LarvaTrackedPlayer > tracked = selector.select( games );
        assertEquals( "Dated", tracked.get( 0 ).getDisplayName() );
        assertEquals( 1, tracked.size() ); // the undated one has 1 game < 3, so it is not a second tracked player
    }
}
