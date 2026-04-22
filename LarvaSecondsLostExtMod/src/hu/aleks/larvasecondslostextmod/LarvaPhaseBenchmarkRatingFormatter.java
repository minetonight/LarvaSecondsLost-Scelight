package hu.aleks.larvasecondslostextmod;

import java.util.EnumMap;
import java.util.Map;

/**
 * Formats player-facing benchmark rankings for phase-table metrics.
 */
public class LarvaPhaseBenchmarkRatingFormatter {

    /** Stable no-data label used by the phase table. */
    private static final String NOT_AVAILABLE = "n/a";

    /** Compact tier labels used by the phase table UI. */
    private static final String[] TIER_LABELS = new String[] { "gold", "plat", "dia", "masters", "pro" };

    /** Spawned-larva benchmark anchors keyed by phase. Higher is better. */
    private static final Map< LarvaGamePhase, double[] > SPAWNED_LARVA_BENCHMARK_MAP = new EnumMap< LarvaGamePhase, double[] >( LarvaGamePhase.class );

    /** Missed-larva benchmark anchors keyed by phase. Lower is better. */
    private static final Map< LarvaGamePhase, double[] > MISSED_LARVA_BENCHMARK_MAP = new EnumMap< LarvaGamePhase, double[] >( LarvaGamePhase.class );

    static {
        SPAWNED_LARVA_BENCHMARK_MAP.put( LarvaGamePhase.MID, new double[]  { 3.859d, 4.378d, 5.096d, 5.730d, 5.964d } );
        SPAWNED_LARVA_BENCHMARK_MAP.put( LarvaGamePhase.LATE, new double[] { 2.909d, 3.519d, 4.028d, 4.439d, 4.823d } );

        MISSED_LARVA_BENCHMARK_MAP.put( LarvaGamePhase.EARLY, new double[] { 1.2315d, 0.9565d, 0.708d, 0.525d, 0.347d } );
        MISSED_LARVA_BENCHMARK_MAP.put( LarvaGamePhase.MID, new double[]   { 2.3865d, 2.1475d, 1.724d, 1.266d, 0.859d } );
        MISSED_LARVA_BENCHMARK_MAP.put( LarvaGamePhase.LATE, new double[]  { 2.7305d, 2.4135d, 2.031d, 1.701d, 1.243d } );
    }

    /**
     * Formats the spawned-larva ranking for one phase.
     *
     * @param phase phase to rate
     * @param value spawned larva per hatch per minute
     * @return compact ranking label
     */
    public String formatSpawnedLarvaRanking( final LarvaGamePhase phase, final Double value ) {
        return formatRanking( phase, value, SPAWNED_LARVA_BENCHMARK_MAP, true );
    }

    /**
     * Formats the larva-missed ranking for one phase.
     *
     * @param phase phase to rate
     * @param value missed larva per hatch per minute
     * @return compact ranking label
     */
    public String formatMissedLarvaRanking( final LarvaGamePhase phase, final Double value ) {
        return formatRanking( phase, value, MISSED_LARVA_BENCHMARK_MAP, false );
    }

    /**
     * Formats one phase benchmark rating.
     */
    private String formatRanking( final LarvaGamePhase phase, final Double value, final Map< LarvaGamePhase, double[] > benchmarkMap,
            final boolean higherIsBetter ) {
        if ( phase == null || value == null )
            return NOT_AVAILABLE;

        final double[] benchmarkArray = benchmarkMap.get( phase );
        if ( benchmarkArray == null || benchmarkArray.length != TIER_LABELS.length )
            return NOT_AVAILABLE;

        final double numericValue = value.doubleValue();
        final int tierIndex = resolveTierIndex( numericValue, benchmarkArray, higherIsBetter );
        final int topPercent = resolveTopPercent( numericValue, benchmarkArray, higherIsBetter, tierIndex );
        return "top " + topPercent + "% " + TIER_LABELS[ tierIndex ];
    }

    /**
     * Resolves the current tier anchor index for a metric.
     */
    private int resolveTierIndex( final double value, final double[] benchmarkArray, final boolean higherIsBetter ) {
        if ( higherIsBetter ) {
            if ( value < benchmarkArray[ 0 ] )
                return 0;
            for ( int i = 0; i < benchmarkArray.length - 1; i++ )
                if ( value < benchmarkArray[ i + 1 ] )
                    return i;
        } else {
            if ( value > benchmarkArray[ 0 ] )
                return 0;
            for ( int i = 0; i < benchmarkArray.length - 1; i++ )
                if ( value > benchmarkArray[ i + 1 ] )
                    return i;
        }

        return benchmarkArray.length - 1;
    }

    /**
     * Resolves the top-percent string payload inside the current tier.
     */
    private int resolveTopPercent( final double value, final double[] benchmarkArray, final boolean higherIsBetter, final int tierIndex ) {
        final double tierStart = benchmarkArray[ tierIndex ];
        final double referenceSpan;
        final double progress;

        if ( tierIndex < benchmarkArray.length - 1 ) {
            final double tierEnd = benchmarkArray[ tierIndex + 1 ];
            referenceSpan = Math.abs( tierEnd - tierStart );
        } else {
            final double previousTierStart = benchmarkArray[ tierIndex - 1 ];
            referenceSpan = Math.abs( tierStart - previousTierStart );
        }
        progress = higherIsBetter ? safeRatio( value - tierStart, referenceSpan ) : safeRatio( tierStart - value, referenceSpan );

        final double clampedProgress = Math.max( 0.0d, Math.min( 1.0d, progress ) );
        final double rawPercent = 100.0d - clampedProgress * 95.0d;
        final int roundedPercent = (int) ( Math.round( rawPercent / 5.0d ) * 5L );
        return Math.max( 5, Math.min( 100, roundedPercent ) );
    }

    /**
     * Returns a ratio guarded against a zero denominator.
     */
    private double safeRatio( final double numerator, final double denominator ) {
        return denominator <= 0.0d ? 0.0d : numerator / denominator;
    }

}