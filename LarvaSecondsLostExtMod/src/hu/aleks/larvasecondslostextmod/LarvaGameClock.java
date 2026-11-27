package hu.aleks.larvasecondslostextmod;

/**
 * The one place where game loops become time. The in-game clock of Legacy of the Void replays runs 22.4 game loops per second, the
 * real time of the Faster game speed; the module shows every time and every "per minute" rate with this clock, so its numbers match
 * the replay viewer of StarCraft II (a minute is 60 seconds of the game clock, {@link #LOOPS_PER_MINUTE} loops).
 *
 * <p>Time bases: game loops (what replays record and what the module computes in), the game clock (loops / 22.4, what the module
 * shows) and Normal-speed seconds of 16 loops (the unit of Blizzard's game data, used only to turn data values such as energy
 * regeneration into loops). Seconds and minutes appear only through this class; {@code LarvaTimeBasisGuardTest} fails the build when
 * other code converts loops itself.</p>
 *
 * <p>Scelight converts loops with its own factor: 16 loops per game second times 26/36 for Faster, which is 22.15 loops per second
 * instead of 22.4. Its clock therefore runs about 1.1 % ahead of the game clock (game 7:51 is Scelight 7:56), and with its "use real
 * time" setting off it shows game seconds of 16 loops. The module uses neither.</p>
 *
 * <p>Durations from the game data stay in loops, since a loop is the same at every game speed. Measured on replays: natural larva spawn
 * {@link #LARVA_SPAWN_LOOPS} loops apart, back-to-back injects pop {@link #INJECT_CYCLE_LOOPS} loops apart, a hatchery takes 1600 loops
 * to build (100 Normal-speed seconds, 71.4 s on the game clock).</p>
 */
public final class LarvaGameClock {

    /** Game loops per second of the in-game clock. */
    public static final double LOOPS_PER_SECOND = 22.4d;

    /** Game loops per minute of the in-game clock (60 s), the denominator of every "per minute" rate. */
    public static final double LOOPS_PER_MINUTE = 60.0d * LOOPS_PER_SECOND;

    /**
     * Game loops per Normal-speed second, the unit of the game data (energy regeneration 0.5625 per second, build times). Only for
     * converting game data values; never for showing time.
     */
    public static final int LOOPS_PER_GAME_DATA_SECOND = 16;

    /** Loops between two tracker PlayerStats events: 10 Normal-speed seconds, 7.1 s on the game clock. */
    public static final int PLAYER_STATS_INTERVAL_LOOPS = 160;

    /** Loops between two natural larva of one hatchery: 15 Normal-speed seconds, 10.7 s on the game clock. */
    public static final int LARVA_SPAWN_LOOPS = 240;

    /** Loops between the larva pops of back-to-back injects: 40 Normal-speed seconds plus one loop, 28.6 s on the game clock. */
    public static final int INJECT_CYCLE_LOOPS = 641;

    /** Loops a queen takes to build: 50 Normal-speed seconds, 35.7 s on the game clock. */
    public static final int QUEEN_BUILD_LOOPS = 800;

    /**
     * @param loops game loops, such as a phase's hatchery-eligible loops
     * @return the same duration in game clock minutes, or 0 for no loops
     */
    public static double loopsToMinutes( final long loops ) {
        return loops <= 0L ? 0.0d : loops / LOOPS_PER_MINUTE;
    }

    private LarvaGameClock() {
    }

    /**
     * @param loop game loop
     * @return milliseconds on the game clock, rounded down; 0 for loops before the start
     */
    public static long loopToMs( final int loop ) {
        return loop <= 0 ? 0L : loop * 625L / 14L;
    }

    /**
     * @param seconds seconds on the game clock
     * @return the nearest number of game loops
     */
    public static int secondsToLoops( final double seconds ) {
        return (int) Math.round( seconds * LOOPS_PER_SECOND );
    }

    /**
     * @param loop game loop
     * @return game clock time as {@code m:ss}, or {@code h:mm:ss} from one hour on
     */
    public static String format( final int loop ) {
        return formatMs( loopToMs( loop ), false );
    }

    /**
     * @param loop game loop
     * @return game clock time with tenths of a second, such as {@code 7:51.3}
     */
    public static String formatTenths( final int loop ) {
        return formatMs( loopToMs( loop ), true );
    }

    /**
     * @param ms milliseconds
     * @param includeTenths tells if a tenths digit is appended
     * @return {@code m:ss} or {@code h:mm:ss}, rounded down
     */
    public static String formatMs( final long ms, final boolean includeTenths ) {
        final long safeMs = Math.max( 0L, ms );
        final long totalSeconds = safeMs / 1000L;
        final long hours = totalSeconds / 3600L;
        final long minutes = ( totalSeconds % 3600L ) / 60L;
        final long seconds = totalSeconds % 60L;
        final String suffix = includeTenths ? "." + ( safeMs % 1000L ) / 100L : "";
        if ( hours > 0L )
            return hours + ":" + twoDigits( minutes ) + ":" + twoDigits( seconds ) + suffix;
        return minutes + ":" + twoDigits( seconds ) + suffix;
    }

    private static String twoDigits( final long value ) {
        return value < 10L ? "0" + value : String.valueOf( value );
    }
}
