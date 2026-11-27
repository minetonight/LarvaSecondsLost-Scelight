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

    /** Tiers of one metric and phase, weakest first (the shared tier logic is {@link LarvaBenchmarkTiers}, E20-S05). */
    static final class Bands extends LarvaBenchmarkTiers {

        Bands( final String[] labels, final double[] starts ) {
            super( labels, starts );
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
        return rankSpawnedLarva( phase, value ).getText();
    }

    /** @return the spawned-larva ranking with its league label marked when inside the bands (E20-S12) */
    LarvaRanking rankSpawnedLarva( final LarvaGamePhase phase, final Double value ) {
        return rank( phase == null ? null : BANDS.get( SPAWNED + '.' + phase.name() ), value, true );
    }

    /**
     * Formats the larva-missed ranking for one phase.
     *
     * @param phase phase to rate
     * @param value missed larva per hatch per minute
     * @return compact ranking label
     */
    public String formatMissedLarvaRanking( final LarvaGamePhase phase, final Double value ) {
        return rankMissedLarva( phase, value ).getText();
    }

    /** @return the larva-missed ranking with its league label marked when inside the bands (E20-S12) */
    LarvaRanking rankMissedLarva( final LarvaGamePhase phase, final Double value ) {
        return rank( phase == null ? null : BANDS.get( MISSED + '.' + phase.name() ), value, false );
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
            final LarvaBenchmarkTiers tiers = LarvaBenchmarkTiers.parse( key, properties.getProperty( key ), higherIsBetter );
            bands.put( key, new Bands( tiers.labels, tiers.starts ) );
        }
        return Collections.unmodifiableMap( bands );
    }

    /**
     * Formats one phase benchmark rating: {@code top p% <label>}; a value worse than the weakest tier reads {@code top 100%} of it.
     */
    String formatRanking( final Bands bands, final Double value, final boolean higherIsBetter ) {
        return rank( bands, value, higherIsBetter ).getText();
    }

    /** @return as {@link #formatRanking(Bands, Double, boolean)}, with the league label marked when the value is inside the bands */
    LarvaRanking rank( final Bands bands, final Double value, final boolean higherIsBetter ) {
        if ( bands == null || value == null )
            return LarvaRanking.plain( NOT_AVAILABLE );
        return bands.rankTopPercent( value.doubleValue(), higherIsBetter );
    }

}
