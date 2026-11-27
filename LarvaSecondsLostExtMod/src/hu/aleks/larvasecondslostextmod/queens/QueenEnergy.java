package hu.aleks.larvasecondslostextmod.queens;

/**
 * Queen energy rules from the game data (E19-S09). Loops are game loops: 16 per game second, 22.4 per real second at Faster.
 */
public final class QueenEnergy {

    /** Energy of a new queen. */
    public static final double START = 25.0d;

    /** Most energy a queen can hold. */
    public static final double CAP = 200.0d;

    /** 0.5625 per Normal-speed second of the game data (0.7875 per game clock second): 25 energy take about 711 loops (31.7 s). */
    public static final double PER_LOOP = 0.5625d / hu.aleks.larvasecondslostextmod.LarvaGameClock.LOOPS_PER_GAME_DATA_SECOND;

    /** Spawn Larva (inject). */
    public static final double INJECT_COST = 25.0d;

    /** Queen creep tumor. */
    public static final double TUMOR_COST = 25.0d;

    /** Transfusion. */
    public static final double TRANSFUSE_COST = 50.0d;

    /** Tolerance for energy comparisons. */
    static final double EPSILON = 1e-6d;

    private QueenEnergy() {
    }

    /** @return the loops a queen needs to regenerate the energy (rounded up) */
    public static int loopsToRegenerate( final double energy ) {
        return energy <= 0.0d ? 0 : (int) Math.ceil( energy / PER_LOOP - EPSILON );
    }
}
