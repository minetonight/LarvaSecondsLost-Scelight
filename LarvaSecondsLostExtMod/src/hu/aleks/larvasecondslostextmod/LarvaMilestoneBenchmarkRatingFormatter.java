package hu.aleks.larvasecondslostextmod;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Formats the league ranking of a milestone time (E20-S05), for the multi page (the player's median per matchup) and the
 * single-replay page (one game).
 *
 * <p>The tiers come from {@value #BANDS_RESOURCE}, written by {@code benchmark-data/tools/milestone_bands.py} ({@code ant
 * MILESTONE_BANDS}): per milestone and matchup bucket, league labels with the loop where each tier starts, weakest tier first. An
 * earlier loop is better. A tier starts halfway between its league median and the median of the league below, over the games that
 * reached the milestone. A label names every league of its tier ({@code plat/dia}: plat reaches it as early as dia, E20-S10).</p>
 *
 * <p>Texts: {@code top p% <label>} inside the tiers; {@code <label> pace m:ss} at or before the start of a band with a single tier
 * (only one league has enough games, E20-S10); {@code aim for m:ss} for a time later than the weakest tier, the time where it starts
 * (E20-S12; OQ-5: no percentage there, it would read like praise); {@code n/a} without a band or without a time. Inside the bands the
 * league label is marked, so the tables paint it bold.</p>
 */
class LarvaMilestoneBenchmarkRatingFormatter {

    /** Band file next to this class, generated; see {@code benchmark-data/exports/milestone-bands.md}. */
    static final String BANDS_RESOURCE = "larva-milestone-bands.properties";

    /** Prefix of the text for a time later than the weakest tier, before the time where that tier starts. */
    static final String AIM_FOR = "aim for ";

    /** Between label and time of a single-tier band: {@code GM/pro pace 6:47}. */
    static final String PACE = " pace ";

    /** Bands by key {@code <LarvaMilestone>.<LarvaMatchupBucket>}. */
    private static final Map< String, LarvaBenchmarkTiers > BANDS = loadBands();

    /**
     * @param milestone milestone
     * @param bucket matchup bucket whose band applies
     * @param loop first loop at or above the milestone (one game) or the player's median of it; null if not reached
     * @return ranking text, never null
     */
    String format( final LarvaMilestone milestone, final LarvaMatchupBucket bucket, final Integer loop ) {
        return rank( milestone, bucket, loop ).getText();
    }

    /** @return as {@link #format(LarvaMilestone, LarvaMatchupBucket, Integer)}, with the league label marked inside the bands */
    LarvaRanking rank( final LarvaMilestone milestone, final LarvaMatchupBucket bucket, final Integer loop ) {
        if ( milestone == null || bucket == null )
            return LarvaRanking.NOT_AVAILABLE;
        return rankLoop( BANDS.get( key( milestone, bucket ) ), loop );
    }

    /** @return the ranking text of a loop against the tiers (lower is better) */
    static String formatRanking( final LarvaBenchmarkTiers tiers, final Integer loop ) {
        return rankLoop( tiers, loop ).getText();
    }

    /** @return the ranking of a loop against the tiers (lower is better), the league label marked inside the bands */
    static LarvaRanking rankLoop( final LarvaBenchmarkTiers tiers, final Integer loop ) {
        if ( tiers == null || loop == null )
            return LarvaRanking.NOT_AVAILABLE;
        final double value = loop.doubleValue();
        final String weakestStart = LarvaGameClock.format( (int) Math.round( tiers.weakestStart() ) );
        if ( tiers.isWorseThanWeakest( value, false ) )
            return LarvaRanking.plain( AIM_FOR + weakestStart );
        if ( !tiers.hasScale() )
            return LarvaRanking.withLabel( "", tiers.weakestLabel(), PACE + weakestStart );
        return tiers.rankTopPercent( value, false );
    }

    /** @return true if the packaged band file was read and holds at least one band */
    static boolean hasBands() {
        return !BANDS.isEmpty();
    }

    static String key( final LarvaMilestone milestone, final LarvaMatchupBucket bucket ) {
        return milestone.name() + '.' + bucket.name();
    }

    /** Reads the band file. A missing or malformed file leaves every ranking at {@code n/a} and is reported once on stderr. */
    private static Map< String, LarvaBenchmarkTiers > loadBands() {
        try ( final InputStream in = LarvaMilestoneBenchmarkRatingFormatter.class.getResourceAsStream( BANDS_RESOURCE ) ) {
            if ( in == null )
                throw new IOException( BANDS_RESOURCE + " is missing from the module" );
            final Properties properties = new Properties();
            properties.load( in );
            return parse( properties );
        } catch ( final IOException | RuntimeException e ) {
            System.err.println( "Larva milestone bands not available, milestone rankings show n/a: " + e.getMessage() );
            return Collections.emptyMap();
        }
    }

    /**
     * @param properties entries {@code <LarvaMilestone>.<LarvaMatchupBucket>=<label>:<start loop>,...}, weakest tier first; one tier
     *            is a pace
     * @return bands by key
     * @throws IllegalArgumentException if a key names no milestone or bucket, or an entry is malformed or out of order
     */
    static Map< String, LarvaBenchmarkTiers > parse( final Properties properties ) {
        final Map< String, LarvaBenchmarkTiers > bands = new HashMap<>();
        for ( final String key : properties.stringPropertyNames() ) {
            final int dot = key.indexOf( '.' );
            if ( dot <= 0 )
                throw new IllegalArgumentException( "Milestone band key is not <milestone>.<bucket>: " + key );
            LarvaMilestone.valueOf( key.substring( 0, dot ) );
            LarvaMatchupBucket.valueOf( key.substring( dot + 1 ) );
            bands.put( key, LarvaBenchmarkTiers.parse( key, properties.getProperty( key ), false, 1 ) );
        }
        return Collections.unmodifiableMap( bands );
    }
}
