package hu.aleks.larvasecondslostextmod;

/** Corpus totals of {@link LarvaAnalysisCheckCli} (E19-S00), printed after the CSV. */
final class LarvaAnalysisCheckSummary {

    /**
     * The module counts one missed inject per 29 s of red, but a queen needs 31.7 s (712 loops) to regenerate 25 energy, so even a
     * consistent model may exceed the energy bound by this factor (plus one for rounding) over long red stretches.
     */
    static final double COUNTING_RATIO = 712.0d / 642.0d;

    private int players;
    private int errors;
    private int diffAny;
    private int diff10;
    private int diff20;
    private final int[] wrongPhase = new int[ 3 ];
    private final int[] wrongMilestone = new int[ 2 ];
    private int aboveBound;
    private int aboveBoundExcess;
    private int aboveTolerance;
    private int missedInjects;
    private int ordersFollowed;
    private double redSec;
    private double redSecTumor;
    private double redSecFarTumor;

    void add( final LarvaAnalysisCheckRow row ) {
        players++;
        if ( row.maxAbsDiff > 0 )
            diffAny++;
        if ( row.maxAbsDiff >= 10 )
            diff10++;
        if ( row.maxAbsDiff >= 20 )
            diff20++;
        for ( int i = 0; i < wrongPhase.length; i++ )
            if ( ( row.phaseWa[ i ] < 0 ) != ( row.phaseModule[ i ] < 0 ) )
                wrongPhase[ i ]++;
        for ( int i = 0; i < wrongMilestone.length; i++ )
            if ( ( row.milestoneWa[ i ] < 0 ) != ( row.milestoneModule[ i ] < 0 ) )
                wrongMilestone[ i ]++;
        if ( row.missedInjects > row.energyBound ) {
            aboveBound++;
            aboveBoundExcess += row.missedInjects - row.energyBound;
        }
        if ( row.missedInjects > allowedMissedInjects( row.energyBound ) )
            aboveTolerance++;
        missedInjects += row.missedInjects;
        ordersFollowed += row.ordersFollowed;
        redSec += row.redSec;
        redSecTumor += row.redSecTumor;
        redSecFarTumor += row.redSecFarTumor;
    }

    /** @return the most missed injects the module may report for an energy bound, see {@link #COUNTING_RATIO} */
    static int allowedMissedInjects( final int energyBound ) {
        return (int) Math.floor( energyBound * COUNTING_RATIO ) + 1;
    }

    void addError() {
        errors++;
    }

    String format() {
        return "# Zerg players: " + players + ", replays with errors: " + errors + '\n'
                + "# drones: module differs from workersActiveCount for " + diffAny + " players (>= 10: " + diff10 + ", >= 20: " + diff20 + ")\n"
                + "# wrong phase reached (module vs workersActiveCount): Mid " + wrongPhase[ 0 ] + ", Late " + wrongPhase[ 1 ] + ", End "
                + wrongPhase[ 2 ] + '\n'
                + "# wrong drone milestone reached: 66 " + wrongMilestone[ 0 ] + ", 80 " + wrongMilestone[ 1 ] + '\n'
                + "# missed injects: " + missedInjects + " in total; players above the energy bound: " + aboveBound + " (" + aboveBoundExcess
                + " injects above it), above it beyond the 29 s / 31.7 s counting ratio: " + aboveTolerance + "\n"
                + "# queen orders followed for " + ordersFollowed + " of " + players + " players\n"
                + "# red seconds: " + LarvaAnalysisCheckCli.fmt( redSec ) + ", overlapping a queen tumor: " + LarvaAnalysisCheckCli.fmt( redSecTumor )
                + ", overlapping a tumor >= 12 cells from the hatchery: " + LarvaAnalysisCheckCli.fmt( redSecFarTumor );
    }
}
