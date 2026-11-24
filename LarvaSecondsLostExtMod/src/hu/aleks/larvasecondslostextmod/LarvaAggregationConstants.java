package hu.aleks.larvasecondslostextmod;

/** Constants shared by the multi-replay aggregation code (Epic 17). */
final class LarvaAggregationConstants {

    /** Game loops per second at the "Faster" game speed. */
    static final double LOOPS_PER_SECOND = 22.4;

    /** Minimum game length in minutes for a game to count (OQ-5). */
    static final int MIN_GAME_LENGTH_MINUTES = 4;

    /** Minimum game length in loops: 22.4 loops/s x 240 s = 5376 (OQ-5). Eligible iff {@code lengthLoops >= } this value. */
    static final int MIN_GAME_LENGTH_LOOPS = 5376;

    private LarvaAggregationConstants() {
    }
}
