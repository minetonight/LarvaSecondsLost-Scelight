package hu.aleks.larvasecondslostextmod;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Picks the tracked players from the eligible games (FR-11, D-5, D-6). Pure and stateless. Scelight's favored-player setting
 * is deliberately not used (OQ-2).
 *
 * <p>Rules: count Zerg player-games per account identity; rank by count desc, then most recent game desc, then name
 * (case-insensitive). The first is always tracked, the second only with at least {@link #MIN_SECOND_GAMES} games and at
 * least {@link #MIN_SECOND_PERCENT} % of the eligible games. At most {@link #MAX_TRACKED} players.</p>
 */
public final class LarvaTrackedPlayerSelector {

    static final int MAX_TRACKED = 2;

    static final int MIN_SECOND_GAMES = 3;

    static final int MIN_SECOND_PERCENT = 20;

    private static final int PERCENT = 100;

    /**
     * @param eligible
     *            the eligible game records
     * @return tracked players, best first (empty if no Zerg player-game could be counted)
     */
    public List< LarvaTrackedPlayer > select( final List< LarvaAggregationGameRecord > eligible ) {
        final List< LarvaTrackedPlayer > ranked = rank( eligible );
        final List< LarvaTrackedPlayer > tracked = new ArrayList<>();
        for ( final LarvaTrackedPlayer candidate : ranked ) {
            if ( tracked.size() >= MAX_TRACKED )
                break;
            if ( tracked.isEmpty() || qualifiesAsSecond( candidate, eligible.size() ) )
                tracked.add( candidate );
        }
        return tracked;
    }

    /**
     * Lists the player-games left out of the counts because their phase table is missing (OQ-15).
     *
     * @param eligible
     *            the eligible game records
     * @return entries like {@code file.SC2Replay (name)}, in record order
     */
    public List< String > listMissingPhaseTable( final List< LarvaAggregationGameRecord > eligible ) {
        final List< String > result = new ArrayList<>();
        for ( final LarvaAggregationGameRecord game : eligible )
            for ( final LarvaAggregationPlayerRecord player : game.getZergPlayers() )
                if ( player.isMissingPhaseTable() )
                    result.add( game.getReplayFile().getFileName() + " (" + player.getName() + ")" );
        return result;
    }

    private static boolean qualifiesAsSecond( final LarvaTrackedPlayer candidate, final int eligibleGames ) {
        return candidate.getGameCount() >= MIN_SECOND_GAMES
                && (long) candidate.getGameCount() * PERCENT >= (long) MIN_SECOND_PERCENT * eligibleGames;
    }

    private static List< LarvaTrackedPlayer > rank( final List< LarvaAggregationGameRecord > eligible ) {
        final Map< String, Accumulator > byIdentity = new LinkedHashMap<>();
        for ( final LarvaAggregationGameRecord game : eligible )
            for ( final LarvaAggregationPlayerRecord player : game.getZergPlayers() ) {
                if ( player.isMissingPhaseTable() )
                    continue;
                Accumulator acc = byIdentity.get( player.getIdentityKey() );
                if ( acc == null )
                    byIdentity.put( player.getIdentityKey(), acc = new Accumulator( player.getIdentityKey() ) );
                acc.add( game.getReplayDate(), player.getName() );
            }
        final List< LarvaTrackedPlayer > players = new ArrayList<>();
        for ( final Accumulator acc : byIdentity.values() )
            players.add( acc.toPlayer() );
        Collections.sort( players, RANKING );
        return players;
    }

    private static final Comparator< LarvaTrackedPlayer > RANKING = new Comparator< LarvaTrackedPlayer >() {
        @Override
        public int compare( final LarvaTrackedPlayer a, final LarvaTrackedPlayer b ) {
            if ( a.getGameCount() != b.getGameCount() )
                return a.getGameCount() > b.getGameCount() ? -1 : 1;
            final int byDate = compareDatesDesc( a.getLastGameDate(), b.getLastGameDate() );
            if ( byDate != 0 )
                return byDate;
            final String nameA = a.getDisplayName() == null ? "" : a.getDisplayName();
            final String nameB = b.getDisplayName() == null ? "" : b.getDisplayName();
            final int byName = nameA.compareToIgnoreCase( nameB );
            return byName != 0 ? byName : a.getIdentityKey().compareTo( b.getIdentityKey() );
        }
    };

    /** Newest first; a missing date counts as the oldest. */
    private static int compareDatesDesc( final Date a, final Date b ) {
        if ( a == null || b == null )
            return a == b ? 0 : ( a == null ? 1 : -1 );
        return b.compareTo( a );
    }

    /** Collects the games of one identity; the name follows the most recent game. */
    private static final class Accumulator {

        private final String identityKey;

        private int count;

        private Date lastDate;

        private String lastName;

        private boolean seen;

        Accumulator( final String identityKey ) {
            this.identityKey = identityKey;
        }

        void add( final Date date, final String name ) {
            count++;
            if ( !seen || compareDatesDesc( date, lastDate ) < 0 ) {
                lastDate = date;
                lastName = name;
                seen = true;
            }
        }

        LarvaTrackedPlayer toPlayer() {
            return new LarvaTrackedPlayer( identityKey, lastName, count, lastDate );
        }
    }
}
