package hu.aleks.larvasecondslostextmod.queens;

import java.util.Arrays;

/**
 * One queen energy spend seen in the replay (E19-S07): an inject on a hatchery, a creep tumor, or a transfusion command. Before
 * attribution it carries the candidate queens from the command selection (empty when unknown).
 */
public final class QueenSpend {

    /** What the energy was spent on. */
    public enum Kind {

        /** Spawn Larva on a hatchery. */
        INJECT( "inject", QueenEnergy.INJECT_COST ),

        /** Queen creep tumor. */
        TUMOR( "tumor", QueenEnergy.TUMOR_COST ),

        /** Transfusion (a command: it may not have been cast). */
        TRANSFUSE( "transfusion", QueenEnergy.TRANSFUSE_COST );

        private final String label;
        private final double cost;

        Kind( final String label, final double cost ) {
            this.label = label;
            this.cost = cost;
        }

        public String getLabel() {
            return label;
        }

        public double getCost() {
            return cost;
        }
    }

    private final int loop;
    private final Kind kind;
    private final int hatchTag;
    private final double x;
    private final double y;
    private final int[] candidateQueenTags;
    private final int commandLoop;

    /**
     * @param loop loop of the spend
     * @param kind what the energy was spent on
     * @param hatchTag injected hatchery tag, or -1
     * @param x tracker-scale x, or NaN if unknown
     * @param y tracker-scale y, or NaN if unknown
     * @param candidateQueenTags queens in the command selection, empty if unknown
     */
    public QueenSpend( final int loop, final Kind kind, final int hatchTag, final double x, final double y, final int[] candidateQueenTags ) {
        this( loop, kind, hatchTag, x, y, candidateQueenTags, -1 );
    }

    /**
     * @param loop loop of the spend
     * @param kind what the energy was spent on
     * @param hatchTag injected hatchery tag, or -1
     * @param x tracker-scale x, or NaN if unknown
     * @param y tracker-scale y, or NaN if unknown
     * @param candidateQueenTags queens in the command selection, empty if unknown
     * @param commandLoop loop of the matched command (the queen walked from there), or -1
     */
    public QueenSpend( final int loop, final Kind kind, final int hatchTag, final double x, final double y, final int[] candidateQueenTags,
            final int commandLoop ) {
        this.commandLoop = commandLoop;
        this.loop = loop;
        this.kind = kind;
        this.hatchTag = hatchTag;
        this.x = x;
        this.y = y;
        this.candidateQueenTags = candidateQueenTags == null ? new int[ 0 ] : Arrays.copyOf( candidateQueenTags, candidateQueenTags.length );
    }

    public int getLoop() {
        return loop;
    }

    public Kind getKind() {
        return kind;
    }

    public double getCost() {
        return kind.getCost();
    }

    public int getHatchTag() {
        return hatchTag;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public boolean hasPosition() {
        return !Double.isNaN( x ) && !Double.isNaN( y );
    }

    public int[] getCandidateQueenTags() {
        return Arrays.copyOf( candidateQueenTags, candidateQueenTags.length );
    }

    /** @return loop of the matched command, or -1 */
    public int getCommandLoop() {
        return commandLoop;
    }

    /** @return true if this spend is an inject on the hatchery */
    public boolean isInjectOn( final int hatcheryTag ) {
        return kind == Kind.INJECT && hatchTag == hatcheryTag;
    }
}
