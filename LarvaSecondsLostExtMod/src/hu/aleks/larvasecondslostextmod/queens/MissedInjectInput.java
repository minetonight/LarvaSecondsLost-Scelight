package hu.aleks.larvasecondslostextmod.queens;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Everything the missed-inject model needs about one player (E19-S07), in tracker coordinates and game loops. Built from the
 * replay by {@code QueenEvidenceCollector} and the analyzer, or by hand in tests.
 */
public final class MissedInjectInput {

    /** A queen's life. */
    public static final class Queen {

        final int tag;
        final String tagText;
        final int bornLoop;
        final double bornX;
        final double bornY;
        final int diedLoop;
        final double diedX;
        final double diedY;

        /**
         * @param tag unit tag
         * @param tagText tag as Scelight shows it
         * @param bornLoop birth loop
         * @param bornX birth x
         * @param bornY birth y
         * @param diedLoop death loop, or -1 if alive at the end
         * @param diedX death x (ignored if alive)
         * @param diedY death y (ignored if alive)
         */
        public Queen( final int tag, final String tagText, final int bornLoop, final double bornX, final double bornY, final int diedLoop,
                final double diedX, final double diedY ) {
            this.tag = tag;
            this.tagText = tagText;
            this.bornLoop = bornLoop;
            this.bornX = bornX;
            this.bornY = bornY;
            this.diedLoop = diedLoop;
            this.diedX = diedX;
            this.diedY = diedY;
        }

        public int getTag() {
            return tag;
        }

        public String getTagText() {
            return tagText;
        }
    }

    /** A hatchery (or lair, hive) of the player. */
    public static final class Hatch {

        final int tag;
        final double x;
        final double y;
        final int completedLoop;
        final int diedLoop;
        /** One window per inject, sorted; queued injects are back to back. */
        final List< int[] > injectWindows;
        /** Loops the hatchery is injected (the windows merged). */
        final List< int[] > injectedLoops;

        /**
         * @param tag unit tag
         * @param x x
         * @param y y
         * @param completedLoop completion loop, or -1 if never completed
         * @param diedLoop death loop, or -1 if alive at the end
         * @param injectWindows inject-active windows {@code [start, end)}
         */
        public Hatch( final int tag, final double x, final double y, final int completedLoop, final int diedLoop, final List< int[] > injectWindows ) {
            this.tag = tag;
            this.x = x;
            this.y = y;
            this.completedLoop = completedLoop;
            this.diedLoop = diedLoop;
            this.injectWindows = new ArrayList<>();
            if ( injectWindows != null )
                for ( final int[] window : injectWindows )
                    if ( window[ 1 ] > window[ 0 ] )
                        this.injectWindows.add( new int[] { window[ 0 ], window[ 1 ] } );
            Collections.sort( this.injectWindows, new java.util.Comparator< int[] >() {
                @Override
                public int compare( final int[] a, final int[] b ) {
                    return a[ 0 ] < b[ 0 ] ? -1 : a[ 0 ] == b[ 0 ] ? 0 : 1;
                }
            } );
            this.injectedLoops = LoopIntervals.normalize( this.injectWindows );
        }

        public int getTag() {
            return tag;
        }

        /** @return true if completed and not destroyed at the loop */
        boolean isStandingAt( final int loop ) {
            return completedLoop >= 0 && completedLoop <= loop && ( diedLoop < 0 || loop < diedLoop );
        }

        /** @return true if an inject window covers the loop */
        boolean isInjectActiveAt( final int loop ) {
            for ( final int[] window : injectedLoops )
                if ( loop >= window[ 0 ] && loop < window[ 1 ] )
                    return true;
            return false;
        }
    }

    /** A Spawn Larva command on a hatchery, with the queens of the selection. */
    public static final class InjectCommand {

        final int loop;
        final int hatchTag;
        final int[] queenTags;

        public InjectCommand( final int loop, final int hatchTag, final int[] queenTags ) {
            this.loop = loop;
            this.hatchTag = hatchTag;
            this.queenTags = queenTags == null ? new int[ 0 ] : Arrays.copyOf( queenTags, queenTags.length );
        }
    }

    /**
     * An order given to selected queens (E19-S11): a move (any targeted command that is not a spell, for every selected queen), a cast
     * order of a single selected queen (inject or tumor: she walks to the target), or a stop.
     */
    public static final class Move {

        final int loop;
        final int[] queenTags;
        final double x;
        final double y;
        final boolean queued;
        final boolean cast;
        final boolean stop;
        final String label;

        /**
         * @param loop command loop
         * @param queenTags queens it applies to
         * @param x target x (NaN for a stop)
         * @param y target y (NaN for a stop)
         * @param queued true for a queued (shift) order
         * @param cast true for an inject or tumor order
         * @param stop true for stop or hold position
         * @param label what the order is
         */
        public Move( final int loop, final int[] queenTags, final double x, final double y, final boolean queued, final boolean cast,
                final boolean stop, final String label ) {
            this.loop = loop;
            this.queenTags = queenTags == null ? new int[ 0 ] : Arrays.copyOf( queenTags, queenTags.length );
            this.x = x;
            this.y = y;
            this.queued = queued;
            this.cast = cast;
            this.stop = stop;
            this.label = label;
        }

        public int getLoop() {
            return loop;
        }
    }

    /** A queen position from a tracker {@code UnitPositions} event (damaged units only). */
    public static final class Position {

        final int queenTag;
        final int loop;
        final double x;
        final double y;

        public Position( final int queenTag, final int loop, final double x, final double y ) {
            this.queenTag = queenTag;
            this.loop = loop;
            this.x = x;
            this.y = y;
        }
    }

    final List< Queen > queens = new ArrayList<>();
    final List< Hatch > hatches = new ArrayList<>();
    final List< InjectCommand > injectCommands = new ArrayList<>();
    final List< QueenSpend > tumors = new ArrayList<>();
    final List< QueenSpend > transfusions = new ArrayList<>();
    final List< Position > positions = new ArrayList<>();
    final List< Move > moves = new ArrayList<>();

    public MissedInjectInput addQueen( final Queen queen ) {
        queens.add( queen );
        return this;
    }

    public MissedInjectInput addHatch( final Hatch hatch ) {
        hatches.add( hatch );
        return this;
    }

    public MissedInjectInput addInjectCommand( final InjectCommand command ) {
        injectCommands.add( command );
        return this;
    }

    /** @param tumor a queen creep tumor ({@link QueenSpend.Kind#TUMOR}) */
    public MissedInjectInput addTumor( final QueenSpend tumor ) {
        tumors.add( tumor );
        return this;
    }

    /** @param transfusion a transfusion command ({@link QueenSpend.Kind#TRANSFUSE}) */
    public MissedInjectInput addTransfusion( final QueenSpend transfusion ) {
        transfusions.add( transfusion );
        return this;
    }

    public MissedInjectInput addPosition( final Position position ) {
        positions.add( position );
        return this;
    }

    public MissedInjectInput addMove( final Move move ) {
        moves.add( move );
        return this;
    }

    public List< Move > getMoves() {
        return Collections.unmodifiableList( moves );
    }

    public List< Hatch > getHatches() {
        return Collections.unmodifiableList( hatches );
    }

    public List< Queen > getQueens() {
        return Collections.unmodifiableList( queens );
    }
}
