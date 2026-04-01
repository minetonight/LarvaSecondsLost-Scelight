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

    /** Optional hover metadata for the marker. */
    private final LarvaMarkerHoverData hoverData;

    /** Optional tooltip text shown when the marker is hovered. */
    private final String tooltipText;

    /**
     * Creates a new timeline marker.
     *
     * @param loop marker loop
     * @param timeMs marker time in milliseconds
     * @param label marker label
     * @param kind marker kind
     */
    public LarvaTimelineMarker( final int loop, final long timeMs, final String label, final Kind kind ) {
        this( loop, timeMs, label, kind, null, null );
    }

    /**
     * Creates a new timeline marker.
     *
     * @param loop marker loop
     * @param timeMs marker time in milliseconds
     * @param label marker label
     * @param kind marker kind
     * @param hoverData optional hover metadata
     */
    public LarvaTimelineMarker( final int loop, final long timeMs, final String label, final Kind kind, final LarvaMarkerHoverData hoverData ) {
        this( loop, timeMs, label, kind, hoverData, null );
    }

    /**
     * Creates a new timeline marker.
     *
     * @param loop marker loop
     * @param timeMs marker time in milliseconds
     * @param label marker label
     * @param kind marker kind
     * @param hoverData optional hover metadata
     * @param tooltipText optional tooltip text shown when the marker is hovered
     */
    public LarvaTimelineMarker( final int loop, final long timeMs, final String label, final Kind kind, final LarvaMarkerHoverData hoverData,
            final String tooltipText ) {
        this.loop = loop;
        this.timeMs = timeMs;
        this.label = label;
        this.kind = kind;
        this.hoverData = hoverData;
        this.tooltipText = tooltipText;
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

    public LarvaMarkerHoverData getHoverData() {
        return hoverData;
    }

    public String getTooltipText() {
        return tooltipText;
    }

}