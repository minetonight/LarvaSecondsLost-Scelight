package hu.aleks.larvasecondslostextmod;

/**
 * One normalized marker rendered on a hatchery row of the module-owned Larva timeline.
 */
public class LarvaTimelineMarker {

    /** Semantic marker kind. */
    public static enum Kind {
        /** One missed-larva threshold marker at 11 seconds of accumulated 3+ larva saturation. */
        MISSED_LARVA
    }

    /** Marker loop. */
    private final int loop;

    /** Marker time in milliseconds. */
    private final long timeMs;

    /** Marker label. */
    private final String label;

    /** Marker kind. */
    private final Kind kind;

    /**
     * Creates a new timeline marker.
     *
     * @param loop marker loop
     * @param timeMs marker time in milliseconds
     * @param label marker label
     * @param kind marker kind
     */
    public LarvaTimelineMarker( final int loop, final long timeMs, final String label, final Kind kind ) {
        this.loop = loop;
        this.timeMs = timeMs;
        this.label = label;
        this.kind = kind;
    }

    public int getLoop() {
        return loop;
    }

    public long getTimeMs() {
        return timeMs;
    }

    public String getLabel() {
        return label;
    }

    public Kind getKind() {
        return kind;
    }

}