package hu.aleks.larvasecondslostextmod;

/**
 * Macro milestones of the multi-replay analysis (E17-S08, FR-13). Declaration order is the display order.
 */
public enum LarvaMilestone {

    /** First time the player has 100 supply used. */
    SUPPLY_100( "100 supply", Kind.SUPPLY, 100 ),

    /** First time the player has 66 alive drones. */
    DRONES_66( "66 drones", Kind.DRONES, 66 ),

    /** First time the player has 80 alive drones. */
    DRONES_80( "80 drones", Kind.DRONES, 80 ),

    /** First time the player has 200 supply used. */
    SUPPLY_200( "200 supply", Kind.SUPPLY, 200 );

    /** What a milestone counts. */
    public enum Kind {
        /** Supply used, from tracker {@code foodUsed} (fixed-point x4096, OQ-11). */
        SUPPLY,
        /** Alive drones. */
        DRONES
    }

    /** Display label. */
    private final String label;

    /** Counted quantity. */
    private final Kind kind;

    /** Threshold, reached when the count is greater than or equal to it (D-4). */
    private final int threshold;

    private LarvaMilestone( final String label, final Kind kind, final int threshold ) {
        this.label = label;
        this.kind = kind;
        this.threshold = threshold;
    }

    public String getLabel() {
        return label;
    }

    public Kind getKind() {
        return kind;
    }

    public int getThreshold() {
        return threshold;
    }
}
