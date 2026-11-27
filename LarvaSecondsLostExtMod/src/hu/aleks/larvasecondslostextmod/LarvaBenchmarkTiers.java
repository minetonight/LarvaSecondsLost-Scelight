package hu.aleks.larvasecondslostextmod;

/**
 * League tiers of one benchmark band, weakest tier first, and the "top p%" rule shared by the larva rankings and the milestone
 * rankings (E20-S05). Immutable.
 *
 * <p>A band entry is {@code <label>:<tier start>,...}, written by the band tools in {@code benchmark-data/tools}. A tier starts
 * halfway between its league median and the median of the league below; the weakest tier half a step below its own median. The
 * direction (higher or lower is better) belongs to the metric, so every method takes it.</p>
 *
 * <p>A label may name several leagues ({@code GM/pro}): the band tool merges a league whose median is too close to the league above
 * into that tier. A milestone band may have a single tier (E20-S10); it has no scale for "top p%", only its start.</p>
 */
class LarvaBenchmarkTiers {

    /** League labels, weakest first. */
    final String[] labels;

    /** Tier starts, same order as {@link #labels}. */
    final double[] starts;

    LarvaBenchmarkTiers( final String[] labels, final double[] starts ) {
        this.labels = labels.clone();
        this.starts = starts.clone();
    }

    /**
     * @param key band key, only for the error messages
     * @param value {@code <label>:<start>,...}, weakest tier first
     * @param higherIsBetter direction of the metric
     * @return the tiers
     * @throws IllegalArgumentException if the entry is malformed, has fewer than two tiers or does not improve tier by tier
     */
    static LarvaBenchmarkTiers parse( final String key, final String value, final boolean higherIsBetter ) {
        return parse( key, value, higherIsBetter, 2 );
    }

    /**
     * @param minTiers fewest tiers the band may have: 2 for a "top p%" scale, 1 where a single tier reads as a pace
     * @see #parse(String, String, boolean)
     */
    static LarvaBenchmarkTiers parse( final String key, final String value, final boolean higherIsBetter, final int minTiers ) {
        final String[] tiers = value.split( "," );
        if ( tiers.length < minTiers )
            throw new IllegalArgumentException( key + " needs at least " + minTiers + ( minTiers == 1 ? " tier" : " tiers" ) );
        final String[] labels = new String[ tiers.length ];
        final double[] starts = new double[ tiers.length ];
        for ( int i = 0; i < tiers.length; i++ ) {
            final String[] parts = tiers[ i ].trim().split( ":" );
            if ( parts.length != 2 || parts[ 0 ].isEmpty() )
                throw new IllegalArgumentException( key + ": tier '" + tiers[ i ] + "' is not <label>:<start>" );
            labels[ i ] = parts[ 0 ];
            starts[ i ] = Double.parseDouble( parts[ 1 ] );
            if ( i > 0 && ( higherIsBetter ? starts[ i ] <= starts[ i - 1 ] : starts[ i ] >= starts[ i - 1 ] ) )
                throw new IllegalArgumentException( key + ": tier " + labels[ i ] + " does not improve on " + labels[ i - 1 ] );
        }
        return new LarvaBenchmarkTiers( labels, starts );
    }

    /** @return label of the weakest tier */
    String weakestLabel() {
        return labels[ 0 ];
    }

    /** @return where the weakest tier starts */
    double weakestStart() {
        return starts[ 0 ];
    }

    /** @return true if there are two tiers or more, so "top p%" has a scale */
    boolean hasScale() {
        return starts.length >= 2;
    }

    /** @return true if the value is worse than where the weakest tier starts */
    boolean isWorseThanWeakest( final double value, final boolean higherIsBetter ) {
        return higherIsBetter ? value < starts[ 0 ] : value > starts[ 0 ];
    }

    /**
     * @return {@code top <p>% <label>}; a value worse than the weakest tier counts as that tier (top 100%)
     * @throws IllegalStateException for a single tier (see {@link #hasScale()})
     */
    String formatTopPercent( final double value, final boolean higherIsBetter ) {
        return rankTopPercent( value, higherIsBetter ).getText();
    }

    /**
     * @return {@code top <p>% <label>} with the label marked when the value is inside the tiers (E20-S12); a value worse than the
     *         weakest tier reads {@code top 100%} of it, without a marked label
     * @throws IllegalStateException for a single tier (see {@link #hasScale()})
     */
    LarvaRanking rankTopPercent( final double value, final boolean higherIsBetter ) {
        if ( !hasScale() )
            throw new IllegalStateException( "A single tier has no top p% scale" );
        final int tierIndex = resolveTierIndex( value, higherIsBetter );
        final String prefix = "top " + resolveTopPercent( value, higherIsBetter, tierIndex ) + "% ";
        if ( isWorseThanWeakest( value, higherIsBetter ) )
            return LarvaRanking.plain( prefix + labels[ tierIndex ] );
        return LarvaRanking.withLabel( prefix, labels[ tierIndex ], "" );
    }

    /** Resolves the tier of a value; a value worse than the weakest tier counts as that tier. */
    private int resolveTierIndex( final double value, final boolean higherIsBetter ) {
        for ( int i = 0; i < starts.length - 1; i++ )
            if ( higherIsBetter ? value < starts[ i + 1 ] : value > starts[ i + 1 ] )
                return i;
        return starts.length - 1;
    }

    /**
     * Position inside the tier as "top p%": 100 at the tier start, 5 at the next tier start, in steps of 5. The best tier uses the
     * width of the tier below it.
     */
    private int resolveTopPercent( final double value, final boolean higherIsBetter, final int tierIndex ) {
        final double tierStart = starts[ tierIndex ];
        final double referenceSpan;
        if ( tierIndex < starts.length - 1 )
            referenceSpan = Math.abs( starts[ tierIndex + 1 ] - tierStart );
        else
            referenceSpan = Math.abs( tierStart - starts[ tierIndex - 1 ] );
        final double progress = higherIsBetter ? safeRatio( value - tierStart, referenceSpan ) : safeRatio( tierStart - value, referenceSpan );

        final double clampedProgress = Math.max( 0.0d, Math.min( 1.0d, progress ) );
        final double rawPercent = 100.0d - clampedProgress * 95.0d;
        final int roundedPercent = (int) ( Math.round( rawPercent / 5.0d ) * 5L );
        return Math.max( 5, Math.min( 100, roundedPercent ) );
    }

    private static double safeRatio( final double numerator, final double denominator ) {
        return denominator <= 0.0d ? 0.0d : numerator / denominator;
    }
}
