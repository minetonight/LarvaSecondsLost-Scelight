package hu.aleks.larvasecondslostextmod;

/**
 * A benchmark ranking text and where its league label is (E20-S12): {@code top 50% plat}, {@code GM/pro pace 6:47}. The label is
 * only marked when the value is inside the league bands, so the tables can paint it bold; {@code aim for 9:01}, {@code n/a} and the
 * larva {@code top 100% silver} of a value worse than the weakest band have none. Immutable.
 */
final class LarvaRanking {

    /** No ranking: {@link LarvaStatFormat#NOT_AVAILABLE}, no label. */
    static final LarvaRanking NOT_AVAILABLE = plain( LarvaStatFormat.NOT_AVAILABLE );

    private final String text;

    /** Start of the label in {@link #text}, or -1. */
    private final int labelStart;

    /** End (exclusive) of the label in {@link #text}, or -1. */
    private final int labelEnd;

    private LarvaRanking( final String text, final int labelStart, final int labelEnd ) {
        this.text = text;
        this.labelStart = labelStart;
        this.labelEnd = labelEnd;
    }

    /** @return a ranking text without a marked label */
    static LarvaRanking plain( final String text ) {
        if ( text == null )
            throw new IllegalArgumentException( "text is required" );
        return new LarvaRanking( text, -1, -1 );
    }

    /** @return {@code prefix + label + suffix} with the label marked */
    static LarvaRanking withLabel( final String prefix, final String label, final String suffix ) {
        if ( prefix == null || label == null || label.isEmpty() || suffix == null )
            throw new IllegalArgumentException( "prefix, a non-empty label and suffix are required" );
        return new LarvaRanking( prefix + label + suffix, prefix.length(), prefix.length() + label.length() );
    }

    String getText() {
        return text;
    }

    /** @return true if the text has a league label inside the bands */
    boolean hasLabel() {
        return labelStart >= 0;
    }

    /** @return start of the label, or -1 */
    int getLabelStart() {
        return labelStart;
    }

    /** @return end (exclusive) of the label, or -1 */
    int getLabelEnd() {
        return labelEnd;
    }

    @Override
    public String toString() {
        return text;
    }
}
