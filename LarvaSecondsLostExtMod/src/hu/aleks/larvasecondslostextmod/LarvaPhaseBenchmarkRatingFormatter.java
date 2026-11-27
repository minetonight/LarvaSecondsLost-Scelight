package hu.aleks.larvasecondslostextmod;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Formats player-facing benchmark rankings for phase-table metrics.
 *
 * <p>The tiers come from {@value #BANDS_RESOURCE}, written by {@code benchmark-data/tools/benchmark_bands.py} ({@code ant
 * BENCHMARK_BANDS}): per metric and phase, league labels with the value where each tier starts, weakest tier first. Replays are put
 * into leagues by MMR with the MMR-Ranges table; a tier starts halfway between its league median and the median of the league below.
 * A phase without bands shows {@code n/a}.</p>
 */
public class LarvaPhaseBenchmarkRatingFormatter {

    /** Band file next to this class, generated; see {@code benchmark-data/exports/benchmark-bands.md}. */
    static final String BANDS_RESOURCE = "larva-benchmark-bands.properties";

    /** Stable no-data label used by the phase table. */
    private static final String NOT_AVAILABLE = "n/a";

    /** Band key prefix of larva generated per hatchery per minute (higher is better). */
    static final String SPAWNED = "spawned";

    /** Band key prefix of larva missed per hatchery per minute (lower is better). */
    static final String MISSED = "missed";

    /** Bands by key {@code <metric>.<PHASE>}. */
    private static final Map< String, Bands > BANDS = loadBands();

    /** Tiers of one metric and phase, weakest first. */
    static final class Bands {

        final String[] labels;

        final double[] starts;

        Bands( final String[] labels, final double[] starts ) {
            this.labels = labels;
            this.starts = starts;
        }
    }

    /**
     * Formats the spawned-larva ranking for one phase.
     *
     * @param phase phase to rate
     * @param value spawned larva per hatch per minute
     * @return compact ranking label
     */
    public String formatSpawnedLarvaRanking( final LarvaGamePhase phase, final Double value ) {
        return formatRanking( phase == null ? null : BANDS.get( SPAWNED + '.' + phase.name() ), value, true );
    }

    /**
     * Formats the larva-missed ranking for one phase.
     *
     * @param phase phase to rate
     * @param value missed larva per hatch per minute
     * @return compact ranking label
     */
    public String formatMissedLarvaRanking( final LarvaGamePhase phase, final Double value ) {
        return formatRanking( phase == null ? null : BANDS.get( MISSED + '.' + phase.name() ), value, false );
    }

    /** @return true if the packaged band file was read and holds at least one band */
    static boolean hasBands() {
        return !BANDS.isEmpty();
    }

    /**
     * Reads the band file. A missing or malformed file leaves the rankings at {@code n/a} and is reported on stderr (a unit test
     * guards the packaged file).
     */
    private static Map< String, Bands > loadBands() {
        try ( final InputStream in = LarvaPhaseBenchmarkRatingFormatter.class.getResourceAsStream( BANDS_RESOURCE ) ) {
            if ( in == null )
                throw new IOException( BANDS_RESOURCE + " is missing from the module" );
            final Properties properties = new Properties();
            properties.load( in );
            return parse( properties );
        } catch ( final IOException | RuntimeException e ) {
            System.err.println( "Larva benchmark bands not available, rankings show n/a: " + e.getMessage() );
            return Collections.emptyMap();
        }
    }

    /**
     * @param properties entries {@code <metric>.<PHASE>=<label>:<start>,...}, weakest tier first
     * @return bands by key
     * @throws IllegalArgumentException if an entry is malformed, has fewer than two tiers or its starts are out of order
     */
    static Map< String, Bands > parse( final Properties properties ) {
        final Map< String, Bands > bands = new HashMap<>();
        for ( final String key : properties.stringPropertyNames() ) {
            final boolean higherIsBetter = key.startsWith( SPAWNED + '.' );
            if ( !higherIsBetter && !key.startsWith( MISSED + '.' ) )
                throw new IllegalArgumentException( "Unknown band metric: " + key );
            LarvaGamePhase.valueOf( key.substring( key.indexOf( '.' ) + 1 ) );
            final String[] tiers = properties.getProperty( key ).split( "," );
            if ( tiers.length < 2 )
                throw new IllegalArgumentException( key + " needs at least two tiers" );
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
            bands.put( key, new Bands( labels, starts ) );
        }
        return Collections.unmodifiableMap( bands );
    }

    /**
     * Formats one phase benchmark rating.
     */
    String formatRanking( final Bands bands, final Double value, final boolean higherIsBetter ) {
        if ( bands == null || value == null )
            return NOT_AVAILABLE;

        final double numericValue = value.doubleValue();
        final int tierIndex = resolveTierIndex( numericValue, bands.starts, higherIsBetter );
        final int topPercent = resolveTopPercent( numericValue, bands.starts, higherIsBetter, tierIndex );
        return "top " + topPercent + "% " + bands.labels[ tierIndex ];
    }

    /**
     * Resolves the current tier index for a metric; a value below the weakest tier counts as that tier.
     */
    private int resolveTierIndex( final double value, final double[] benchmarkArray, final boolean higherIsBetter ) {
        for ( int i = 0; i < benchmarkArray.length - 1; i++ )
            if ( higherIsBetter ? value < benchmarkArray[ i + 1 ] : value > benchmarkArray[ i + 1 ] )
                return i;
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
