package hu.aleks.larvasecondslostextmod;

import java.util.EnumMap;
import java.util.Map;

/**
 * Immutable counts of the eligibility filter result (FR-8, AC-8.2).
 *
 * <p>Invariant: {@code selected == eligible + sum(excluded)}, because every selected file is counted exactly once.</p>
 */
public final class LarvaFilterSummary {

    private final Map< LarvaEligibility, Integer > counts;

    private LarvaFilterSummary( final Map< LarvaEligibility, Integer > counts ) {
        this.counts = new EnumMap<>( counts );
    }

    /** @return number of selected files */
    public int getSelected() {
        int sum = 0;
        for ( final int count : counts.values() )
            sum += count;
        return sum;
    }

    /** @return number of eligible files */
    public int getEligible() {
        return getCount( LarvaEligibility.ELIGIBLE );
    }

    /** @return number of excluded files (all reasons) */
    public int getExcludedTotal() {
        return getSelected() - getEligible();
    }

    /**
     * @param reason
     *            the eligibility result
     * @return number of files with that result
     */
    public int getCount( final LarvaEligibility reason ) {
        final Integer count = counts.get( reason );
        return count == null ? 0 : count.intValue();
    }

    /** @return text like {@code selected 6 · eligible 3 · excluded: not 1v1 0, non-ladder 2, < 4 min 1, no Zerg 0, failed to parse 0} */
    public String toText() {
        return "selected " + getSelected() + " · eligible " + getEligible() + " · excluded: not 1v1 "
                + getCount( LarvaEligibility.NOT_1V1 ) + ", non-ladder " + getCount( LarvaEligibility.NON_LADDER ) + ", < 4 min "
                + getCount( LarvaEligibility.TOO_SHORT ) + ", no Zerg " + getCount( LarvaEligibility.NO_ZERG ) + ", failed to parse "
                + getCount( LarvaEligibility.FAILED_TO_PARSE );
    }

    @Override
    public String toString() {
        return toText();
    }

    /** Mutable collector; call {@link #add(LarvaEligibility)} once per selected file. */
    public static final class Builder {

        private final Map< LarvaEligibility, Integer > counts = new EnumMap<>( LarvaEligibility.class );

        /**
         * @param result
         *            the result of one file
         * @return this builder
         */
        public Builder add( final LarvaEligibility result ) {
            if ( result == null )
                throw new IllegalArgumentException( "Eligibility result is required." );
            final Integer old = counts.get( result );
            counts.put( result, old == null ? 1 : old + 1 );
            return this;
        }

        /** @return the immutable summary */
        public LarvaFilterSummary build() {
            return new LarvaFilterSummary( counts );
        }
    }
}
