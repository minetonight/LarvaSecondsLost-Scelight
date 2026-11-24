package hu.aleks.larvasecondslostextmod;

/**
 * Table buckets of the multi-replay analysis (E17-S11, FR-12). {@link #ALL} pools every game; the other three are the
 * matchups of a Zerg player, decided by the opponent's race (D-7).
 *
 * <p>Declaration order is the display order (OQ-3).</p>
 */
public enum LarvaMatchupBucket {

    /** Every eligible player-game of the tracked player. */
    ALL( "All" ),

    /** Zerg vs. Protoss. */
    ZVP( "ZvP" ),

    /** Zerg vs. Terran. */
    ZVT( "ZvT" ),

    /** Zerg vs. Zerg. */
    ZVZ( "ZvZ" );

    private final String label;

    private LarvaMatchupBucket( final String label ) {
        this.label = label;
    }

    /** @return display label such as {@code ZvP} */
    public String getLabel() {
        return label;
    }
}
