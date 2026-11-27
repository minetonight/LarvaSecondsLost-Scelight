package hu.aleks.larvasecondslostextmod;

/** Constants shared by the multi-replay aggregation code (Epic 17). */
final class LarvaAggregationConstants {

    /** Minimum game length in minutes for a game to count (OQ-5). */
    static final int MIN_GAME_LENGTH_MINUTES = 4;

    /** Minimum game length in loops: 240 s on the game clock = 5376 (OQ-5). Eligible iff {@code lengthLoops >= } this value. */
    static final int MIN_GAME_LENGTH_LOOPS = LarvaGameClock.secondsToLoops( MIN_GAME_LENGTH_MINUTES * 60 );

    private LarvaAggregationConstants() {
    }
}
