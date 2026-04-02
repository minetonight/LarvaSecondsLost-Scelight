package hu.aleks.larvasecondslostextmod;

/**
 * Small overlay decoration rendered on a timeline row without a tooltip.
 */
public class LarvaTimelineDecoration {

    /** Decoration kind. */
    public static enum Kind {
        /** One- or two-larva rhythm dot column. */
        LARVA_DOT_COLUMN,
        /** Small cumulative larva-sitting label inside a 3+ window. */
        ACCUMULATION_LABEL
    }

    /** Decoration kind. */
    private final Kind kind;

    /** Replay time in milliseconds. */
    private final long timeMs;

    /** Larva count represented by the decoration, or {@code 0} if not applicable. */
    private final int larvaCount;

    /** Visible text label, or <code>null</code> if not applicable. */
    private final String label;

    /**
     * Creates a new decoration.
     *
     * @param kind decoration kind
     * @param timeMs replay time in milliseconds
     * @param larvaCount larva count represented by the decoration
     * @param label visible text label
     */
    public LarvaTimelineDecoration( final Kind kind, final long timeMs, final int larvaCount, final String label ) {
        this.kind = kind;
        this.timeMs = timeMs;
        this.larvaCount = larvaCount;
        this.label = label;
    }

    public Kind getKind() {
        return kind;
    }

    public long getTimeMs() {
        return timeMs;
    }

    public int getLarvaCount() {
        return larvaCount;
    }

    public String getLabel() {
        return label;
    }

}
