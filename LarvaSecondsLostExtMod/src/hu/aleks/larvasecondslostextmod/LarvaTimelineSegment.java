package hu.aleks.larvasecondslostextmod;

/**
 * One normalized timeline segment rendered by the module-owned Larva preview timeline.
 */
public class LarvaTimelineSegment {

    /** Kind of segment to render. */
    public static enum Kind {
        /** Stable replay-derived interval where a hatchery had 3 or more larva. */
        SATURATION_WINDOW,
        /** Stable replay-derived interval where a hatchery was actively injected. */
        INJECT_WINDOW,
        /** Interval-style placeholder segment derived from replay timing data. */
        PREVIEW_INTERVAL,
        /** Narrow marker-style placeholder segment derived from a single replay timing point. */
        PREVIEW_MARKER
    }

    /** Segment start in milliseconds. */
    private final long startMs;

    /** Segment end in milliseconds. */
    private final long endMs;

    /** User-facing segment label. */
    private final String label;

    /** Semantic segment kind. */
    private final Kind kind;

    /** Optional tooltip text shown when the segment is hovered. */
    private final String tooltipText;

    /**
     * Creates a new timeline segment.
     *
     * @param startMs segment start in milliseconds
     * @param endMs segment end in milliseconds
     * @param label segment label
     * @param kind semantic segment kind
     */
    public LarvaTimelineSegment( final long startMs, final long endMs, final String label, final Kind kind ) {
        this( startMs, endMs, label, kind, null );
    }

    /**
     * Creates a new timeline segment.
     *
     * @param startMs segment start in milliseconds
     * @param endMs segment end in milliseconds
     * @param label segment label
     * @param kind semantic segment kind
     * @param tooltipText optional tooltip text shown when the segment is hovered
     */
    public LarvaTimelineSegment( final long startMs, final long endMs, final String label, final Kind kind, final String tooltipText ) {
        this.startMs = startMs;
        this.endMs = endMs;
        this.label = label;
        this.kind = kind;
        this.tooltipText = tooltipText;
    }

    public long getStartMs() {
        return startMs;
    }

    public long getEndMs() {
        return endMs;
    }

    public String getLabel() {
        return label;
    }

    public Kind getKind() {
        return kind;
    }

    public String getTooltipText() {
        return tooltipText;
    }

}