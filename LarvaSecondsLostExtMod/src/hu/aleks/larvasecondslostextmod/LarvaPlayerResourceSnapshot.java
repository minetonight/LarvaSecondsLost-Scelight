package hu.aleks.larvasecondslostextmod;

/**
 * Immutable player resource snapshot extracted from replay tracker events.
 */
public class LarvaPlayerResourceSnapshot {

    /** Player name owning the snapshot. */
    private final String playerName;

    /** Replay loop of the snapshot. */
    private final int loop;

    /** Formatted replay time. */
    private final String timeLabel;

    /** Minerals available at the snapshot, if known. */
    private final Integer mineralsCurrent;

    /** Gas available at the snapshot, if known. */
    private final Integer gasCurrent;

    /** Food used at the snapshot, in fixed-point tracker units. */
    private final Integer foodUsed;

    /** Food made at the snapshot, in fixed-point tracker units. */
    private final Integer foodMade;

    /**
     * Creates a new player resource snapshot.
     *
     * @param playerName player name owning the snapshot
     * @param loop replay loop of the snapshot
     * @param timeLabel formatted replay time
     * @param mineralsCurrent minerals available at the snapshot
     * @param gasCurrent gas available at the snapshot
     */
    public LarvaPlayerResourceSnapshot( final String playerName, final int loop, final String timeLabel, final Integer mineralsCurrent,
            final Integer gasCurrent, final Integer foodUsed, final Integer foodMade ) {
        this.playerName = playerName;
        this.loop = loop;
        this.timeLabel = timeLabel;
        this.mineralsCurrent = mineralsCurrent;
        this.gasCurrent = gasCurrent;
        this.foodUsed = foodUsed;
        this.foodMade = foodMade;
    }

    public String getPlayerName() {
        return playerName;
    }

    public int getLoop() {
        return loop;
    }

    public String getTimeLabel() {
        return timeLabel;
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

}