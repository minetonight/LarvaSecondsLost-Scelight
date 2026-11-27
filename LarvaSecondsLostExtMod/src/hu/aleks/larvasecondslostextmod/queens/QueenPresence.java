package hu.aleks.larvasecondslostextmod.queens;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Where one queen was, as far as the replay shows it (E19-S08, S11). Tracker events give no positions for a queen that walks around,
 * so presence comes from facts: evidence points (birth, injects, creep tumors, damaged-unit positions, death) and, when the player's
 * commands can be followed, the queen's own commands (moves, and the walk to an inject or tumor she was ordered to cast). A fact is
 * near every hatchery standing within the radius, so a queen between a main and a macro hatchery is at both.
 *
 * <p>Evidence only (commands not followed), for hatchery H:</p>
 * <ul>
 * <li>from a point near H to the next point near H she stays for the gap, but at most the bridge limit;</li>
 * <li>if the next point is somewhere else, or there is none, she stays at most the hold time.</li>
 * </ul>
 * <p>Following commands, a queen stays where she is until a command sends her somewhere else:</p>
 * <ul>
 * <li>from a point near H she stays until her next fact; a move or cast order away from H ends presence at the order (a queued
 * order when the previous order is done), a move to a point near H keeps her there;</li>
 * <li>a move to a point near H from elsewhere starts presence at the estimated arrival (straight line from where she is on her
 * current walk, queen speed off creep);
 * a cast order does not, she casts when she gets there and the cast is evidence;</li>
 * <li>if the next point is somewhere else without any order sending her away, she left unseen: she stays at most the hold time
 * after the last fact that kept her near H (the point, or a later order within the radius);</li>
 * <li>an order queued behind a cast order (shift-click after an inject or tumor order) starts when the cast is seen;</li>
 * <li>a stop on the way leaves her somewhere unknown, until the next fact.</li>
 * </ul>
 * Death ends presence in both modes. Following commands needs the commands: after the last game event (Scelight stops decoding
 * some replays early) the evidence-only limits apply again, counted from her last fact, but never ending before that loop.
 */
public final class QueenPresence {

    /** No home hatchery: the point is away from every hatchery. */
    public static final int AWAY = -1;

    /** What a fact is. */
    public enum Kind {
        /** Evidence of where she was at the loop. */
        EVIDENCE,
        /** An order with a target: she walks there. */
        MOVE,
        /** Stop or hold position: she stays wherever she is. */
        STOP
    }

    /** One fact about where the queen was or went. */
    public static final class Point {

        final int loop;
        final double x;
        final double y;
        final int homeHatchTag;
        final int[] nearHatchTags;
        final String label;
        final boolean death;
        final Kind kind;
        final boolean queued;
        final boolean cast;

        /**
         * An evidence point near its home hatchery only.
         *
         * @param loop loop of the evidence
         * @param x tracker-scale x
         * @param y tracker-scale y
         * @param homeHatchTag hatchery the point belongs to, or {@link QueenPresence#AWAY}
         * @param label what the evidence is ("born", "inject", "tumor", "seen", "died")
         * @param death true for the death point
         */
        public Point( final int loop, final double x, final double y, final int homeHatchTag, final String label, final boolean death ) {
            this( loop, x, y, homeHatchTag, homeHatchTag == AWAY ? new int[ 0 ] : new int[] { homeHatchTag }, label, death, Kind.EVIDENCE, false,
                    false );
        }

        Point( final int loop, final double x, final double y, final int homeHatchTag, final int[] nearHatchTags, final String label,
                final boolean death, final Kind kind, final boolean queued, final boolean cast ) {
            this.loop = loop;
            this.x = x;
            this.y = y;
            this.homeHatchTag = homeHatchTag;
            this.nearHatchTags = nearHatchTags == null ? new int[ 0 ] : Arrays.copyOf( nearHatchTags, nearHatchTags.length );
            this.label = label;
            this.death = death;
            this.kind = kind;
            this.queued = queued;
            this.cast = cast;
        }

        /**
         * @param loop loop
         * @param x x
         * @param y y
         * @param homeHatchTag nearest hatchery within the radius, or {@link QueenPresence#AWAY}
         * @param nearHatchTags all hatcheries within the radius
         * @param label what the evidence is
         * @param death true for the death point
         * @return an evidence point
         */
        public static Point evidence( final int loop, final double x, final double y, final int homeHatchTag, final int[] nearHatchTags,
                final String label, final boolean death ) {
            return new Point( loop, x, y, homeHatchTag, nearHatchTags, label, death, Kind.EVIDENCE, false, false );
        }

        /**
         * @param loop command loop
         * @param x target x
         * @param y target y
         * @param homeHatchTag nearest hatchery to the target within the radius, or {@link QueenPresence#AWAY}
         * @param nearHatchTags all hatcheries within the radius of the target
         * @param label what the order is ("move", "inject order", "tumor order")
         * @param queued true for a queued (shift) order
         * @param cast true for an inject or tumor order: presence starts at the cast, not at the arrival
         * @return an order
         */
        public static Point move( final int loop, final double x, final double y, final int homeHatchTag, final int[] nearHatchTags,
                final String label, final boolean queued, final boolean cast ) {
            return new Point( loop, x, y, homeHatchTag, nearHatchTags, label, false, Kind.MOVE, queued, cast );
        }

        /** @return a stop or hold-position order */
        public static Point stop( final int loop, final String label ) {
            return new Point( loop, Double.NaN, Double.NaN, AWAY, new int[ 0 ], label, false, Kind.STOP, false, false );
        }

        public int getLoop() {
            return loop;
        }

        public int getHomeHatchTag() {
            return homeHatchTag;
        }

        public String getLabel() {
            return label;
        }

        public double getX() {
            return x;
        }

        public double getY() {
            return y;
        }

        public Kind getKind() {
            return kind;
        }

        public boolean isQueued() {
            return queued;
        }

        /** @return true if the point (or the order's target) is within the radius of the hatchery */
        public boolean isNear( final int hatchTag ) {
            for ( final int tag : nearHatchTags )
                if ( tag == hatchTag )
                    return true;
            return false;
        }
    }

    /** Why a presence interval ends. */
    public enum End {
        /** The next point is near the same hatchery. */
        BRIDGE,
        /** The next point is near the same hatchery, but later than the bridge limit. */
        BRIDGE_LIMIT,
        /** The next point is away from the hatchery. */
        NEXT_AWAY,
        /** No evidence for the hold time. */
        HOLD,
        /** An order sent her away from the hatchery. */
        MOVED,
        /** The queen died, or the replay ended. */
        LIFE_END
    }

    /** One interval where the queen is at a hatchery, with the facts that made it. */
    public static final class Interval {

        final int start;
        final int end;
        final Point from;
        final Point to;
        final End endReason;
        final boolean fromArrival;

        Interval( final int start, final int end, final Point from, final Point to, final End endReason, final boolean fromArrival ) {
            this.start = start;
            this.end = end;
            this.from = from;
            this.to = to;
            this.endReason = endReason;
            this.fromArrival = fromArrival;
        }

        public int getStart() {
            return start;
        }

        public int getEnd() {
            return end;
        }

        /** @return the fact that starts the interval: a point near the hatchery, or the move she arrived with */
        public Point getFrom() {
            return from;
        }

        /** @return the fact that ends it, or null */
        public Point getTo() {
            return to;
        }

        public End getEndReason() {
            return endReason;
        }

        /** @return true if the interval starts at an estimated arrival after a move */
        public boolean isFromArrival() {
            return fromArrival;
        }
    }

    /** Tuning values. */
    public static final class Settings {

        final int holdLoops;
        final int bridgeLimitLoops;
        final boolean followCommands;
        final double cellsPerLoop;
        final int commandsEndLoop;

        /**
         * Commands followed up to the end, or not at all.
         *
         * @param holdLoops how long she counts as present after a point when the next point is elsewhere (or, evidence only, missing)
         * @param bridgeLimitLoops evidence only: how long she counts as present after a point when the next point is near the same hatchery
         * @param followCommands true to use her orders (the player's commands can be followed)
         * @param cellsPerLoop walking speed for arrival estimates
         */
        public Settings( final int holdLoops, final int bridgeLimitLoops, final boolean followCommands, final double cellsPerLoop ) {
            this( holdLoops, bridgeLimitLoops, followCommands, cellsPerLoop, Integer.MAX_VALUE );
        }

        /**
         * @param holdLoops how long she counts as present after a point when the next point is elsewhere (or, evidence only, missing)
         * @param bridgeLimitLoops evidence only: how long she counts as present after a point when the next point is near the same hatchery
         * @param followCommands true to use her orders (the player's commands can be followed)
         * @param cellsPerLoop walking speed for arrival estimates
         * @param commandsEndLoop last loop with decoded commands; later, absent orders prove nothing
         */
        public Settings( final int holdLoops, final int bridgeLimitLoops, final boolean followCommands, final double cellsPerLoop,
                final int commandsEndLoop ) {
            this.holdLoops = holdLoops;
            this.bridgeLimitLoops = Math.max( holdLoops, bridgeLimitLoops );
            this.followCommands = followCommands;
            this.cellsPerLoop = cellsPerLoop > 0 ? cellsPerLoop : QueenEnergy.EPSILON;
            this.commandsEndLoop = commandsEndLoop;
        }
    }

    private static final Comparator< Point > ORDER = new Comparator< Point >() {
        @Override
        public int compare( final Point a, final Point b ) {
            if ( a.loop != b.loop )
                return a.loop < b.loop ? -1 : 1;
            return a.kind.ordinal() - b.kind.ordinal();
        }
    };

    private final List< Point > facts;
    private final int lifeEndLoop;
    private final Settings settings;

    /**
     * Evidence only, as before commands were followed.
     *
     * @param points evidence points in loop order
     * @param lifeEndLoop death loop, or the replay end
     * @param holdLoops how long she counts as present after a point when the next point is elsewhere or missing
     * @param bridgeLimitLoops how long she counts as present after a point when the next point is at the same hatchery
     */
    public QueenPresence( final List< Point > points, final int lifeEndLoop, final int holdLoops, final int bridgeLimitLoops ) {
        this( points, lifeEndLoop, new Settings( holdLoops, bridgeLimitLoops, false, 1.0d ) );
    }

    /**
     * @param facts evidence points and orders, any order
     * @param lifeEndLoop death loop, or the replay end
     * @param settings tuning values
     */
    public QueenPresence( final List< Point > facts, final int lifeEndLoop, final Settings settings ) {
        this.facts = new ArrayList<>( facts );
        Collections.sort( this.facts, ORDER );
        this.lifeEndLoop = lifeEndLoop;
        this.settings = settings;
    }

    /** @return true if any evidence point is near the hatchery */
    public boolean hasPointAt( final int hatchTag ) {
        for ( final Point point : facts )
            if ( point.kind == Kind.EVIDENCE && point.isNear( hatchTag ) )
                return true;
        return false;
    }

    /** @return the presence intervals at the hatchery in loop order */
    public List< Interval > intervalsAt( final int hatchTag ) {
        return new Walk( hatchTag ).run();
    }

    /** One pass over the facts for one hatchery. */
    private final class Walk {

        final int hatchTag;
        final List< Interval > intervals = new ArrayList<>();
        /** The fact she is near the hatchery since, or null. */
        Point anchor;
        int anchorLoop;
        boolean anchorArrival;
        /** The last loop a fact kept her near the hatchery (the anchor, or an order within the radius). */
        int nearSinceLoop;
        /** The order she is walking with, or null; where and when the walk started. */
        Point transit;
        int transitArrival;
        int transitDeparture;
        double transitFromX;
        double transitFromY;
        double lastX = Double.NaN;
        double lastY = Double.NaN;
        /** A cast order not seen cast yet, and the orders queued behind it. */
        Point castPending;
        final List< Point > queuedBehindCast = new ArrayList<>();

        Walk( final int hatchTag ) {
            this.hatchTag = hatchTag;
        }

        List< Interval > run() {
            for ( final Point fact : facts ) {
                if ( fact.kind == Kind.EVIDENCE ) {
                    if ( onEvidence( fact ) )
                        return intervals;
                } else if ( settings.followCommands ) {
                    if ( fact.kind == Kind.MOVE )
                        onOrder( fact );
                    else
                        onStop( fact );
                }
            }
            settleTransit( lifeEndLoop );
            if ( anchor != null )
                closeAtEnd();
            return intervals;
        }

        /** @return true if the walk ends (death) */
        private boolean onEvidence( final Point point ) {
            settleTransit( point.loop );
            transit = null;
            if ( anchor != null ) {
                if ( point.isNear( hatchTag ) )
                    closeBridge( point );
                else
                    closeHold( point );
            }
            if ( point.death )
                return true;
            setAnchor( point.isNear( hatchTag ) ? point : null, point.loop, false );
            lastX = point.x;
            lastY = point.y;
            castPending = null;
            if ( !queuedBehindCast.isEmpty() ) {
                final List< Point > queued = new ArrayList<>( queuedBehindCast );
                queuedBehindCast.clear();
                for ( final Point order : queued )
                    onOrder( order, point.loop );
            }
            return false;
        }

        private void onOrder( final Point order ) {
            if ( !order.queued ) {
                castPending = null;
                queuedBehindCast.clear();
            } else if ( castPending != null ) {
                queuedBehindCast.add( order );
                return;
            }
            onMove( order, order.loop );
        }

        /** An order queued behind a cast order runs from the cast: {@code loop} is when she takes it up. */
        private void onOrder( final Point order, final int loop ) {
            if ( castPending != null ) {
                queuedBehindCast.add( order );
                return;
            }
            onMove( order, loop );
        }

        private void onMove( final Point move, final int loop ) {
            if ( move.cast )
                castPending = move;
            int departure = loop;
            double fromX = lastX;
            double fromY = lastY;
            if ( transit != null && transitArrival > loop ) {
                if ( move.queued ) {
                    departure = transitArrival;
                    fromX = transit.x;
                    fromY = transit.y;
                } else {
                    final double done = (double) ( loop - transitDeparture ) / Math.max( 1, transitArrival - transitDeparture );
                    fromX = transitFromX + ( transit.x - transitFromX ) * done;
                    fromY = transitFromY + ( transit.y - transitFromY ) * done;
                }
            }
            settleTransit( departure );
            lastX = move.x;
            lastY = move.y;
            if ( anchor != null ) {
                if ( move.isNear( hatchTag ) ) {
                    nearSinceLoop = Math.max( nearSinceLoop, loop );
                    return;
                }
                add( anchorLoop, departure, anchor, move, End.MOVED );
                setAnchor( null, departure, false );
            }
            final double distance = Double.isNaN( fromX ) ? 0.0d : Math.hypot( move.x - fromX, move.y - fromY );
            transit = move;
            transitDeparture = departure;
            transitFromX = Double.isNaN( fromX ) ? move.x : fromX;
            transitFromY = Double.isNaN( fromY ) ? move.y : fromY;
            transitArrival = departure + (int) Math.ceil( distance / settings.cellsPerLoop );
        }

        private void onStop( final Point stop ) {
            if ( transit != null && transitArrival > stop.loop ) {
                transit = null;
                lastX = Double.NaN;
                lastY = Double.NaN;
                return;
            }
            settleTransit( stop.loop );
        }

        /** A walk that has arrived by the loop makes her present near its target, unless it was a cast order. */
        private void settleTransit( final int loop ) {
            if ( transit == null || transitArrival > loop )
                return;
            if ( anchor == null && !transit.cast && transit.isNear( hatchTag ) )
                setAnchor( transit, transitArrival, true );
            transit = null;
        }

        private void closeBridge( final Point next ) {
            final int limit = trustedEnd( anchorLoop + settings.bridgeLimitLoops );
            if ( next.loop > limit )
                add( anchorLoop, limit, anchor, next, End.BRIDGE_LIMIT );
            else
                add( anchorLoop, next.loop, anchor, next, End.BRIDGE );
        }

        /**
         * @param evidenceOnlyEnd where presence ends by the evidence-only limits
         * @return where it ends: no limit while commands are followed, the evidence-only limit after the last decoded command (but not
         *         before it)
         */
        private int trustedEnd( final int evidenceOnlyEnd ) {
            if ( !settings.followCommands )
                return evidenceOnlyEnd;
            return settings.commandsEndLoop == Integer.MAX_VALUE ? Integer.MAX_VALUE : Math.max( settings.commandsEndLoop, evidenceOnlyEnd );
        }

        private void closeHold( final Point next ) {
            final int holdEnd = nearSinceLoop + settings.holdLoops;
            if ( next.loop <= holdEnd && next.loop <= lifeEndLoop )
                add( anchorLoop, next.loop, anchor, next, next.death ? End.LIFE_END : End.NEXT_AWAY );
            else if ( lifeEndLoop <= holdEnd )
                add( anchorLoop, lifeEndLoop, anchor, null, End.LIFE_END );
            else
                add( anchorLoop, holdEnd, anchor, null, End.HOLD );
        }

        private void closeAtEnd() {
            final int limit = trustedEnd( nearSinceLoop + settings.holdLoops );
            if ( lifeEndLoop <= limit )
                add( anchorLoop, lifeEndLoop, anchor, null, End.LIFE_END );
            else
                add( anchorLoop, limit, anchor, null, End.HOLD );
        }

        private void setAnchor( final Point point, final int loop, final boolean arrival ) {
            anchor = point;
            anchorLoop = loop;
            anchorArrival = arrival;
            nearSinceLoop = loop;
        }

        private void add( final int start, final int end, final Point from, final Point to, final End reason ) {
            final int clippedEnd = Math.min( end, lifeEndLoop );
            if ( clippedEnd > start )
                intervals.add( new Interval( start, clippedEnd, from, to, reason, anchorArrival ) );
        }
    }
}
