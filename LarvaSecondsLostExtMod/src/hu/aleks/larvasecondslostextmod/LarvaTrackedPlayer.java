package hu.aleks.larvasecondslostextmod;

import java.util.Date;

/** Immutable tracked player of a multi-replay analysis (E17-S10, D-5, D-6). */
public final class LarvaTrackedPlayer {

    private final String identityKey;

    private final String displayName;

    private final int gameCount;

    private final Date lastGameDate;

    /**
     * @param identityKey
     *            account identity key (S09), never null
     * @param displayName
     *            name in the most recent eligible game
     * @param gameCount
     *            number of eligible Zerg player-games
     * @param lastGameDate
     *            date of the most recent counted game, may be null
     */
    LarvaTrackedPlayer( final String identityKey, final String displayName, final int gameCount, final Date lastGameDate ) {
        if ( identityKey == null )
            throw new IllegalArgumentException( "Identity key is required." );
        this.identityKey = identityKey;
        this.displayName = displayName;
        this.gameCount = gameCount;
        this.lastGameDate = lastGameDate == null ? null : new Date( lastGameDate.getTime() );
    }

    /** @return account identity key */
    public String getIdentityKey() {
        return identityKey;
    }

    /** @return name used in the most recent eligible game (D-5) */
    public String getDisplayName() {
        return displayName;
    }

    /** @return number of eligible Zerg games of this account */
    public int getGameCount() {
        return gameCount;
    }

    /** @return copy of the date of the most recent counted game, or null */
    public Date getLastGameDate() {
        return lastGameDate == null ? null : new Date( lastGameDate.getTime() );
    }
}
