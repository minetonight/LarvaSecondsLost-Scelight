package hu.aleks.larvasecondslostextmod.queens;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Gives every queen energy spend of one player to one queen, in loop order, and collects each queen's evidence points and orders
 * (E19-S07 to S09, S11).
 *
 * <ul>
 * <li>Injects come from the hatchery's inject windows (3 larva at once). A Spawn Larva command on the same hatchery shortly before
 * the window supplies the queens of the selection; for a queued inject (command while the hatchery was still injected) the
 * command loop is the cast loop.</li>
 * <li>Candidates are the living queens of the command selection, or all living queens when the selection is unknown.</li>
 * <li>The chosen queen can afford the spend (otherwise the next candidate is tried), then can have walked there in time (creep speed
 * from her last evidence or her last order), then is the nearest by last known position (orders count; within
 * {@link #DISTANCE_TIE_CELLS} is a tie), then has her next certain fact (an order given to her, a position, her death) near the
 * spend, then has more energy, then has the lower tag. The look-ahead settles queens that stand together, for example after a
 * group move. A transfusion command that no candidate can afford is dropped: it was not cast.</li>
 * <li>Orders (moves, stops) apply to every selected queen and move her last known position. The caster of an inject or tumor
 * ordered with several queens selected gets the order too: she walked from the command.</li>
 * </ul>
 */
public final class QueenSpendAttributor {

    /** Fastest a queen walks (on creep, with margin), cells per loop: 4.5 cells per game clock second. */
    static final double MAX_CELLS_PER_LOOP = 4.5d / hu.aleks.larvasecondslostextmod.LarvaGameClock.LOOPS_PER_SECOND;

    /** How far from the spend position a queen can cast it (inject: hatchery center to the queen's edge). */
    static final double CAST_REACH_CELLS = 4.0d;

    /** Last known positions this close count as equally near. */
    static final double DISTANCE_TIE_CELLS = 3.0d;

    /** A next certain fact this close to the spend says the queen stayed there. */
    static final double LOOK_AHEAD_NEAR_CELLS = 12.0d;

    /** Tuning values. */
    public static final class Settings {

        final double radius;
        final int replayEndLoop;
        final int injectCommandLookbackLoops;
        final int injectCommandLookaheadLoops;

        /**
         * @param radius cells within which a point belongs to a hatchery
         * @param replayEndLoop replay end loop
         * @param injectCommandLookbackLoops how long before an inject window a Spawn Larva command may be
         * @param injectCommandLookaheadLoops how long after the window start a Spawn Larva command may be (rounding)
         */
        public Settings( final double radius, final int replayEndLoop, final int injectCommandLookbackLoops, final int injectCommandLookaheadLoops ) {
            this.radius = radius;
            this.replayEndLoop = replayEndLoop;
            this.injectCommandLookbackLoops = injectCommandLookbackLoops;
            this.injectCommandLookaheadLoops = injectCommandLookaheadLoops;
        }
    }

    /** One queen with her energy and evidence after attribution. */
    public static final class TrackedQueen {

        final MissedInjectInput.Queen queen;
        final QueenEnergyTimeline energy;
        final List< QueenPresence.Point > points = new ArrayList<>();
        final List< QueenPresence.Point > orders = new ArrayList<>();
        /** Facts that name her (orders given to her selection, positions, birth, death): {loop, x, y}, in loop order. */
        final List< double[] > certainFacts = new ArrayList<>();
        private double lastX;
        private double lastY;
        private double evidenceX;
        private double evidenceY;
        private int evidenceLoop;
        private double orderX = Double.NaN;
        private double orderY = Double.NaN;
        private int orderLoop = -1;

        TrackedQueen( final MissedInjectInput.Queen queen, final int replayEndLoop ) {
            this.queen = queen;
            this.energy = new QueenEnergyTimeline( queen.bornLoop, queen.diedLoop >= 0 ? queen.diedLoop : replayEndLoop );
            this.lastX = queen.bornX;
            this.lastY = queen.bornY;
            this.evidenceX = queen.bornX;
            this.evidenceY = queen.bornY;
            this.evidenceLoop = queen.bornLoop;
        }

        public MissedInjectInput.Queen getQueen() {
            return queen;
        }

        public QueenEnergyTimeline getEnergy() {
            return energy;
        }

        public List< QueenPresence.Point > getPoints() {
            return Collections.unmodifiableList( points );
        }

        /** @return her orders (moves, cast orders, stops) */
        public List< QueenPresence.Point > getOrders() {
            return Collections.unmodifiableList( orders );
        }

        /** @return evidence points and orders */
        public List< QueenPresence.Point > getFacts() {
            final List< QueenPresence.Point > facts = new ArrayList<>( points );
            facts.addAll( orders );
            return facts;
        }

        void addPoint( final QueenPresence.Point point ) {
            points.add( point );
            lastX = point.x;
            lastY = point.y;
            evidenceX = point.x;
            evidenceY = point.y;
            evidenceLoop = point.loop;
            orderLoop = -1;
        }

        void addOrder( final QueenPresence.Point order ) {
            orders.add( order );
            if ( !Double.isNaN( order.x ) ) {
                lastX = order.x;
                lastY = order.y;
                orderX = order.x;
                orderY = order.y;
                orderLoop = order.loop;
            }
        }

        /** @return true if she can have walked from her last evidence, or from her last order's target, to cast the spend in time */
        boolean canReach( final QueenSpend spend ) {
            if ( !spend.hasPosition() )
                return true;
            if ( reachable( evidenceX, evidenceY, evidenceLoop, spend ) )
                return true;
            return orderLoop >= 0 && reachable( orderX, orderY, orderLoop, spend );
        }

        private static boolean reachable( final double x, final double y, final int loop, final QueenSpend spend ) {
            final double distance = Math.hypot( spend.getX() - x, spend.getY() - y ) - CAST_REACH_CELLS;
            return distance <= MAX_CELLS_PER_LOOP * Math.max( 0, spend.getLoop() - loop );
        }

        /** @return distance from the spend to her next certain fact after it, or NaN if there is none */
        double nextCertainDistance( final QueenSpend spend ) {
            if ( !spend.hasPosition() )
                return Double.NaN;
            for ( final double[] fact : certainFacts )
                if ( fact[ 0 ] > spend.getLoop() )
                    return Math.hypot( fact[ 1 ] - spend.getX(), fact[ 2 ] - spend.getY() );
            return Double.NaN;
        }

        double distanceTo( final QueenSpend spend ) {
            return spend.hasPosition() ? Math.hypot( spend.getX() - lastX, spend.getY() - lastY ) : Double.MAX_VALUE;
        }
    }

    /** Attribution result and counters. */
    public static final class Result {

        final List< TrackedQueen > queens = new ArrayList<>();
        final Map< Integer, int[] > countersByHatchTag = new HashMap<>();
        int spends;
        int fromSelection;
        int ambiguous;
        int unattributed;
        int droppedTransfusions;

        public List< TrackedQueen > getQueens() {
            return Collections.unmodifiableList( queens );
        }

        /** @return [injects, injects with a matched command, ambiguous spends] of the hatchery */
        public int[] countersOf( final int hatchTag ) {
            final int[] counters = countersByHatchTag.get( Integer.valueOf( hatchTag ) );
            return counters == null ? new int[ 3 ] : counters.clone();
        }

        public int getSpends() {
            return spends;
        }

        public int getFromSelection() {
            return fromSelection;
        }

        public int getAmbiguous() {
            return ambiguous;
        }

        public int getUnattributed() {
            return unattributed;
        }

        public int getDroppedTransfusions() {
            return droppedTransfusions;
        }

        /** @return inject windows of all hatcheries */
        public int getInjects() {
            int total = 0;
            for ( final int[] counters : countersByHatchTag.values() )
                total += counters[ 0 ];
            return total;
        }

        /** @return inject windows with a matched Spawn Larva command (queens selected) */
        public int getInjectsWithCommand() {
            int total = 0;
            for ( final int[] counters : countersByHatchTag.values() )
                total += counters[ 1 ];
            return total;
        }

        int[] counters( final int hatchTag ) {
            int[] counters = countersByHatchTag.get( Integer.valueOf( hatchTag ) );
            if ( counters == null ) {
                counters = new int[ 3 ];
                countersByHatchTag.put( Integer.valueOf( hatchTag ), counters );
            }
            return counters;
        }
    }

    private final MissedInjectInput input;
    private final Settings settings;
    private final Result result = new Result();
    private final Map< Integer, TrackedQueen > queenByTag = new HashMap<>();

    private QueenSpendAttributor( final MissedInjectInput input, final Settings settings ) {
        this.input = input;
        this.settings = settings;
    }

    /**
     * @param input one player's evidence
     * @param settings tuning values
     * @return queens with attributed spends and evidence points
     */
    public static Result attribute( final MissedInjectInput input, final Settings settings ) {
        return new QueenSpendAttributor( input, settings ).run();
    }

    private Result run() {
        for ( final MissedInjectInput.Queen queen : input.queens ) {
            final TrackedQueen tracked = new TrackedQueen( queen, settings.replayEndLoop );
            tracked.addPoint( evidence( queen.bornLoop, queen.bornX, queen.bornY, "born", false ) );
            result.queens.add( tracked );
            queenByTag.put( Integer.valueOf( queen.tag ), tracked );
        }
        collectCertainFacts();
        for ( final Object event : chronologicalEvents() ) {
            if ( event instanceof MissedInjectInput.Position )
                addPosition( (MissedInjectInput.Position) event );
            else if ( event instanceof MissedInjectInput.Move )
                addMove( (MissedInjectInput.Move) event );
            else
                attributeSpend( (QueenSpend) event );
        }
        for ( final TrackedQueen tracked : result.queens ) {
            final MissedInjectInput.Queen queen = tracked.queen;
            if ( queen.diedLoop >= 0 )
                tracked.addPoint( evidence( queen.diedLoop, queen.diedX, queen.diedY, "died", true ) );
        }
        return result;
    }

    /** Orders given to a queen's selection, her positions and her death name her: they guide the look-ahead. */
    private void collectCertainFacts() {
        final List< Object > events = new ArrayList<>();
        events.addAll( input.positions );
        events.addAll( input.moves );
        Collections.sort( events, new Comparator< Object >() {
            @Override
            public int compare( final Object a, final Object b ) {
                final int loopA = loopOf( a );
                final int loopB = loopOf( b );
                return loopA < loopB ? -1 : loopA == loopB ? 0 : 1;
            }
        } );
        for ( final Object event : events ) {
            if ( event instanceof MissedInjectInput.Position ) {
                final MissedInjectInput.Position position = (MissedInjectInput.Position) event;
                addCertainFact( position.queenTag, position.loop, position.x, position.y );
            } else {
                final MissedInjectInput.Move move = (MissedInjectInput.Move) event;
                if ( !move.stop )
                    for ( final int tag : move.queenTags )
                        addCertainFact( tag, move.loop, move.x, move.y );
            }
        }
        for ( final TrackedQueen tracked : result.queens )
            if ( tracked.queen.diedLoop >= 0 )
                tracked.certainFacts.add( new double[] { tracked.queen.diedLoop, tracked.queen.diedX, tracked.queen.diedY } );
    }

    private void addCertainFact( final int queenTag, final int loop, final double x, final double y ) {
        final TrackedQueen tracked = queenByTag.get( Integer.valueOf( queenTag ) );
        if ( tracked != null )
            tracked.certainFacts.add( new double[] { loop, x, y } );
    }

    /** Positions, orders and spends in loop order; at the same loop positions first, then orders, then spends in input order. */
    private List< Object > chronologicalEvents() {
        final List< Object > events = new ArrayList<>();
        events.addAll( input.positions );
        events.addAll( input.moves );
        events.addAll( injectSpends() );
        events.addAll( input.tumors );
        events.addAll( input.transfusions );
        Collections.sort( events, new Comparator< Object >() {
            @Override
            public int compare( final Object a, final Object b ) {
                final int loopA = loopOf( a );
                final int loopB = loopOf( b );
                if ( loopA != loopB )
                    return loopA < loopB ? -1 : 1;
                return rankOf( a ) - rankOf( b );
            }
        } );
        return events;
    }

    private static int rankOf( final Object event ) {
        return event instanceof MissedInjectInput.Position ? 0 : event instanceof MissedInjectInput.Move ? 1 : 2;
    }

    private static int loopOf( final Object event ) {
        if ( event instanceof MissedInjectInput.Position )
            return ( (MissedInjectInput.Position) event ).loop;
        if ( event instanceof MissedInjectInput.Move )
            return ( (MissedInjectInput.Move) event ).loop;
        return ( (QueenSpend) event ).getLoop();
    }

    /** One inject spend per inject window, with the matching Spawn Larva command if there is one. */
    private List< QueenSpend > injectSpends() {
        final List< QueenSpend > spends = new ArrayList<>();
        final Set< MissedInjectInput.InjectCommand > used = new HashSet<>();
        for ( final MissedInjectInput.Hatch hatch : input.hatches ) {
            for ( final int[] window : hatch.injectWindows ) {
                final MissedInjectInput.InjectCommand command = matchCommand( hatch, window[ 0 ], used );
                final boolean queued = command != null && command.loop < window[ 0 ] && hatch.isInjectActiveAt( command.loop );
                final int castLoop = queued ? command.loop : window[ 0 ];
                if ( command != null )
                    result.counters( hatch.tag )[ 1 ]++;
                result.counters( hatch.tag )[ 0 ]++;
                spends.add( new QueenSpend( castLoop, QueenSpend.Kind.INJECT, hatch.tag, hatch.x, hatch.y, command == null ? null : command.queenTags,
                        command == null ? -1 : command.loop ) );
            }
        }
        return spends;
    }

    private MissedInjectInput.InjectCommand matchCommand( final MissedInjectInput.Hatch hatch, final int windowStart,
            final Set< MissedInjectInput.InjectCommand > used ) {
        MissedInjectInput.InjectCommand best = null;
        for ( final MissedInjectInput.InjectCommand command : input.injectCommands ) {
            if ( command.hatchTag != hatch.tag || used.contains( command ) || command.loop < windowStart - settings.injectCommandLookbackLoops
                    || command.loop > windowStart + settings.injectCommandLookaheadLoops )
                continue;
            if ( best == null || command.loop > best.loop )
                best = command;
        }
        if ( best != null )
            used.add( best );
        return best;
    }

    private void addPosition( final MissedInjectInput.Position position ) {
        final TrackedQueen tracked = queenByTag.get( Integer.valueOf( position.queenTag ) );
        if ( tracked == null || !tracked.energy.isAliveAt( position.loop ) )
            return;
        tracked.addPoint( evidence( position.loop, position.x, position.y, "seen", false ) );
    }

    /** An order applies to every selected queen that is alive. */
    private void addMove( final MissedInjectInput.Move move ) {
        for ( final int tag : move.queenTags ) {
            final TrackedQueen tracked = queenByTag.get( Integer.valueOf( tag ) );
            if ( tracked == null || !tracked.energy.isAliveAt( move.loop ) )
                continue;
            tracked.addOrder( move.stop ? QueenPresence.Point.stop( move.loop, move.label )
                    : QueenPresence.Point.move( move.loop, move.x, move.y, homeOf( move.x, move.y, move.loop ), nearOf( move.x, move.y, move.loop ),
                            move.label, move.queued, move.cast ) );
        }
    }

    private void attributeSpend( final QueenSpend spend ) {
        result.spends++;
        final List< TrackedQueen > pool = candidatesOf( spend );
        if ( pool.isEmpty() ) {
            result.unattributed++;
            return;
        }
        final TrackedQueen queen = choose( pool, spend );
        if ( spend.getKind() == QueenSpend.Kind.TRANSFUSE && !queen.energy.canAfford( spend.getLoop(), spend.getCost() ) ) {
            result.droppedTransfusions++;
            return;
        }
        final int home = spend.getKind() == QueenSpend.Kind.INJECT ? spend.getHatchTag()
                : spend.hasPosition() ? homeOf( spend.getX(), spend.getY(), spend.getLoop() ) : QueenPresence.AWAY;
        if ( pool.size() > 1 ) {
            result.ambiguous++;
            if ( home != QueenPresence.AWAY )
                result.counters( home )[ 2 ]++;
        }
        queen.energy.addSpend( spend );
        if ( spend.getKind() == QueenSpend.Kind.TRANSFUSE || !spend.hasPosition() )
            return;
        if ( spend.getCandidateQueenTags().length > 1 && spend.getCommandLoop() >= 0 && spend.getCommandLoop() < spend.getLoop() )
            queen.addOrder( QueenPresence.Point.move( spend.getCommandLoop(), spend.getX(), spend.getY(), home,
                    nearOf( spend.getX(), spend.getY(), spend.getCommandLoop() ), spend.getKind().getLabel() + " order", false, true ) );
        queen.addPoint( QueenPresence.Point.evidence( spend.getLoop(), spend.getX(), spend.getY(), home, nearOf( spend.getX(), spend.getY(), spend.getLoop() ),
                spend.getKind().getLabel(), false ) );
    }

    private QueenPresence.Point evidence( final int loop, final double x, final double y, final String label, final boolean death ) {
        return QueenPresence.Point.evidence( loop, x, y, homeOf( x, y, loop ), nearOf( x, y, loop ), label, death );
    }

    /** Living queens of the selection, or all living queens if the selection names none of them. */
    private List< TrackedQueen > candidatesOf( final QueenSpend spend ) {
        final List< TrackedQueen > alive = new ArrayList<>();
        final List< TrackedQueen > selected = new ArrayList<>();
        final int[] candidateTags = spend.getCandidateQueenTags();
        for ( final TrackedQueen tracked : result.queens ) {
            if ( !tracked.energy.isAliveAt( spend.getLoop() ) )
                continue;
            alive.add( tracked );
            for ( final int tag : candidateTags )
                if ( tag == tracked.queen.tag )
                    selected.add( tracked );
        }
        if ( !selected.isEmpty() )
            result.fromSelection++;
        return selected.isEmpty() ? alive : selected;
    }

    private static TrackedQueen choose( final List< TrackedQueen > pool, final QueenSpend spend ) {
        TrackedQueen best = null;
        for ( final TrackedQueen candidate : pool )
            if ( best == null || better( candidate, best, spend ) )
                best = candidate;
        return best;
    }

    private static boolean better( final TrackedQueen a, final TrackedQueen b, final QueenSpend spend ) {
        final boolean affordA = a.energy.canAfford( spend.getLoop(), spend.getCost() );
        final boolean affordB = b.energy.canAfford( spend.getLoop(), spend.getCost() );
        if ( affordA != affordB )
            return affordA;
        final boolean reachA = a.canReach( spend );
        final boolean reachB = b.canReach( spend );
        if ( reachA != reachB )
            return reachA;
        final double distanceA = a.distanceTo( spend );
        final double distanceB = b.distanceTo( spend );
        if ( Math.abs( distanceA - distanceB ) > DISTANCE_TIE_CELLS )
            return distanceA < distanceB;
        final int aheadA = lookAheadRank( a, spend );
        final int aheadB = lookAheadRank( b, spend );
        if ( aheadA != aheadB )
            return aheadA < aheadB;
        if ( Math.abs( distanceA - distanceB ) > QueenEnergy.EPSILON )
            return distanceA < distanceB;
        final double energyA = a.energy.energyNow( spend.getLoop() );
        final double energyB = b.energy.energyNow( spend.getLoop() );
        if ( Math.abs( energyA - energyB ) > QueenEnergy.EPSILON )
            return energyA > energyB;
        return a.queen.tag < b.queen.tag;
    }

    /** @return 0 if her next certain fact is near the spend, 1 if she has none, 2 if it is far */
    private static int lookAheadRank( final TrackedQueen queen, final QueenSpend spend ) {
        final double distance = queen.nextCertainDistance( spend );
        return Double.isNaN( distance ) ? 1 : distance <= LOOK_AHEAD_NEAR_CELLS ? 0 : 2;
    }

    /**
     * @return every hatchery within the radius that is ever completed, standing or not at the loop: a queen idling at a main is within
     *         reach of a macro hatchery that finishes later (the calculator only counts loops when the hatchery stands)
     */
    int[] nearOf( final double x, final double y, final int loop ) {
        final List< Integer > near = new ArrayList<>();
        for ( final MissedInjectInput.Hatch hatch : input.hatches )
            if ( hatch.completedLoop >= 0 && Math.hypot( hatch.x - x, hatch.y - y ) <= settings.radius )
                near.add( Integer.valueOf( hatch.tag ) );
        final int[] tags = new int[ near.size() ];
        for ( int i = 0; i < tags.length; i++ )
            tags[ i ] = near.get( i ).intValue();
        return tags;
    }

    /** @return the nearest hatchery standing at the loop within the radius, or {@link QueenPresence#AWAY} */
    int homeOf( final double x, final double y, final int loop ) {
        int home = QueenPresence.AWAY;
        double bestDistance = Double.MAX_VALUE;
        for ( final MissedInjectInput.Hatch hatch : input.hatches ) {
            if ( !hatch.isStandingAt( loop ) )
                continue;
            final double distance = Math.hypot( hatch.x - x, hatch.y - y );
            if ( distance <= settings.radius && ( distance < bestDistance - QueenEnergy.EPSILON
                    || Math.abs( distance - bestDistance ) <= QueenEnergy.EPSILON && hatch.tag < home ) ) {
                home = hatch.tag;
                bestDistance = distance;
            }
        }
        return home;
    }
}
