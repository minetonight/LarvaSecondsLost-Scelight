package hu.aleks.larvasecondslostextmod;

/**
 * Monotonic gameplay phases used for Epic 12 phase statistics.
 */
public enum LarvaGamePhase {

    /** Opening phase before any worker-threshold promotion is confirmed. */
    EARLY( "Early", -1 ),

    /** Phase confirmed after staying above 36 workers for the dwell threshold. */
    MID( "Mid", 36 ),

    /** Phase confirmed after staying above 66 workers for the dwell threshold. */
    LATE( "Late", 66 ),

    /** Phase confirmed after staying above 89 workers for the dwell threshold. */
    END( "End", 89 );

    /** Stable display label. */
    private final String displayLabel;

    /** Worker threshold that promotes into this phase, or -1 for the starting phase. */
    private final int promotionWorkerThreshold;

    /**
     * Creates a new gameplay phase.
     *
     * @param displayLabel stable display label
     * @param promotionWorkerThreshold worker threshold that promotes into this phase
     */
    private LarvaGamePhase( final String displayLabel, final int promotionWorkerThreshold ) {
        this.displayLabel = displayLabel;
        this.promotionWorkerThreshold = promotionWorkerThreshold;
    }

    public String getDisplayLabel() {
        return displayLabel;
    }

    public int getPromotionWorkerThreshold() {
        return promotionWorkerThreshold;
    }

    /**
     * Returns the next monotonic phase after this one.
     *
     * @return next phase, or <code>null</code> if this is the final phase
     */
    public LarvaGamePhase next() {
        switch ( this ) {
            case EARLY :
                return MID;
            case MID :
                return LATE;
            case LATE :
                return END;
            case END :
            default :
                return null;
        }
    }

}
