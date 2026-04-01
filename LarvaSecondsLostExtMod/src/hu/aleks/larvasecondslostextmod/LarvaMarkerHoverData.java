package hu.aleks.larvasecondslostextmod;

/**
 * Immutable hover metadata attached to a normalized missed-larva marker.
 */
public class LarvaMarkerHoverData {

    /** Player name owning the marker. */
    private final String playerName;

    /** Replay loop of the resource snapshot. */
    private final int snapshotLoop;

    /** Formatted replay time of the resource snapshot. */
    private final String snapshotTimeLabel;

    /** Minerals available at the snapshot, if known. */
    private final Integer mineralsCurrent;

    /** Gas available at the snapshot, if known. */
    private final Integer gasCurrent;

    /** Food used at the snapshot, in fixed-point tracker units. */
    private final Integer foodUsed;

    /** Food made at the snapshot, in fixed-point tracker units. */
    private final Integer foodMade;

    /** Tells if the snapshot comes from shortly after the hovered time. */
    private final boolean futureSnapshot;

    /**
     * Creates new marker hover metadata.
     *
     * @param playerName player name owning the marker
     * @param snapshotLoop replay loop of the snapshot
     * @param snapshotTimeLabel formatted replay time of the snapshot
     * @param mineralsCurrent minerals available at the snapshot
     * @param gasCurrent gas available at the snapshot
     */
    public LarvaMarkerHoverData( final String playerName, final int snapshotLoop, final String snapshotTimeLabel, final Integer mineralsCurrent,
            final Integer gasCurrent, final Integer foodUsed, final Integer foodMade ) {
        this( playerName, snapshotLoop, snapshotTimeLabel, mineralsCurrent, gasCurrent, foodUsed, foodMade, false );
    }

    /**
     * Creates new marker hover metadata.
     *
     * @param playerName player name owning the marker
     * @param snapshotLoop replay loop of the snapshot
     * @param snapshotTimeLabel formatted replay time of the snapshot
     * @param mineralsCurrent minerals available at the snapshot
     * @param gasCurrent gas available at the snapshot
     * @param foodUsed supply used at the snapshot
     * @param foodMade supply cap at the snapshot
     * @param futureSnapshot tells if the snapshot comes from shortly after the hovered time
     */
    public LarvaMarkerHoverData( final String playerName, final int snapshotLoop, final String snapshotTimeLabel, final Integer mineralsCurrent,
            final Integer gasCurrent, final Integer foodUsed, final Integer foodMade, final boolean futureSnapshot ) {
        this.playerName = playerName;
        this.snapshotLoop = snapshotLoop;
        this.snapshotTimeLabel = snapshotTimeLabel;
        this.mineralsCurrent = mineralsCurrent;
        this.gasCurrent = gasCurrent;
        this.foodUsed = foodUsed;
        this.foodMade = foodMade;
        this.futureSnapshot = futureSnapshot;
    }

    public String getPlayerName() {
        return playerName;
    }

    public int getSnapshotLoop() {
        return snapshotLoop;
    }

    public String getSnapshotTimeLabel() {
        return snapshotTimeLabel;
    }

    public Integer getMineralsCurrent() {
        return mineralsCurrent;
    }

    public Integer getGasCurrent() {
        return gasCurrent;
    }

    public Integer getFoodUsed() {
        return foodUsed;
    }

    public Integer getFoodMade() {
        return foodMade;
    }

    public boolean isFutureSnapshot() {
        return futureSnapshot;
    }

}