package hu.aleks.larvasecondslostextmod.queens;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Red (missed-inject) windows per hatchery (E19-S10). A loop is red when all of these hold:
 * <ul>
 * <li>the hatchery stands (completed, not destroyed) and inject is unlocked;</li>
 * <li>the hatchery is not injected;</li>
 * <li>some queen of the player is at the hatchery ({@link QueenPresence}: within the radius, following her orders when the player's
 * commands can be followed) with 25 spare energy for it ({@link QueenEnergyTimeline#spareIntervals(int)}).</li>
 * </ul>
 * Each window says which queen and which evidence made it.
 */
public final class MissedInjectCalculator {

    /** Formats a loop as game time for the diagnostics. */
    public interface TimeFormatter {
        String format( int loop );
    }

    /** Tuning values. */
    public static final class Settings {

        final int holdLoops;
        final int bridgeLimitLoops;
        final int injectUnlockLoop;
        final int replayEndLoop;
        final TimeFormatter timeFormatter;
        final boolean followCommands;
        final double cellsPerLoop;
        final int spareHorizonLoops;
        final int commandsEndLoop;

        /**
         * Evidence only (orders ignored), no spare-energy horizon.
         *
         * @param holdLoops how long a queen counts as present after a point when her next point is elsewhere or missing
         * @param bridgeLimitLoops how long a queen counts as present after a point when her next point is at the same hatchery
         * @param injectUnlockLoop first loop an inject can count as missed (pool done + 36 s), or -1 if unknown
         * @param replayEndLoop replay end loop
         * @param timeFormatter formats loops for diagnostics
         */
        public Settings( final int holdLoops, final int bridgeLimitLoops, final int injectUnlockLoop, final int replayEndLoop,
                final TimeFormatter timeFormatter ) {
            this( holdLoops, bridgeLimitLoops, injectUnlockLoop, replayEndLoop, timeFormatter, false, 1.0d, Integer.MAX_VALUE, Integer.MAX_VALUE );
        }

        /**
         * @param holdLoops how long a queen counts as present after a point when her next point is elsewhere without an order
         * @param bridgeLimitLoops evidence only: how long a queen counts as present after a point when her next point is at the same hatchery
         * @param injectUnlockLoop first loop an inject can count as missed (pool done + 36 s), or -1 if unknown
         * @param replayEndLoop replay end loop
         * @param timeFormatter formats loops for diagnostics
         * @param followCommands true to use the queens' orders (the player's commands can be followed)
         * @param cellsPerLoop queen walking speed for arrival estimates
         * @param spareHorizonLoops only spends this close ahead make energy not spare ({@link QueenEnergyTimeline#spareIntervals(int, int)})
         * @param commandsEndLoop last loop with decoded commands ({@link Integer#MAX_VALUE} if unknown)
         */
        public Settings( final int holdLoops, final int bridgeLimitLoops, final int injectUnlockLoop, final int replayEndLoop,
                final TimeFormatter timeFormatter, final boolean followCommands, final double cellsPerLoop, final int spareHorizonLoops,
                final int commandsEndLoop ) {
            this.holdLoops = holdLoops;
            this.bridgeLimitLoops = bridgeLimitLoops;
            this.injectUnlockLoop = injectUnlockLoop;
            this.replayEndLoop = replayEndLoop;
            this.timeFormatter = timeFormatter;
            this.followCommands = followCommands;
            this.cellsPerLoop = cellsPerLoop;
            this.spareHorizonLoops = spareHorizonLoops;
            this.commandsEndLoop = commandsEndLoop;
        }
    }

    /** One red window of a hatchery. */
    public static final class RedWindow {

        final int start;
        int end;
        final Set< String > queenTagTexts = new LinkedHashSet<>();
        final Set< String > notes = new LinkedHashSet<>();
        double minStartEnergy = Double.MAX_VALUE;
        double maxStartEnergy;
        double minEndEnergy = Double.MAX_VALUE;
        double maxEndEnergy;

        RedWindow( final int start, final int end ) {
            this.start = start;
            this.end = end;
        }

        public int getStart() {
            return start;
        }

        public int getEnd() {
            return end;
        }

        public List< String > getQueenTagTexts() {
            return new ArrayList<>( queenTagTexts );
        }

        /** @return why the window is red, one line per queen piece */
        public String getNote() {
            return join( notes, "; " );
        }

        public double getMinStartEnergy() {
            return minStartEnergy;
        }

        public double getMaxStartEnergy() {
            return maxStartEnergy;
        }

        public double getMinEndEnergy() {
            return minEndEnergy;
        }

        public double getMaxEndEnergy() {
            return maxEndEnergy;
        }
    }

    /** Red windows and diagnostics of one hatchery. */
    public static final class HatchResult {

        final List< RedWindow > windows = new ArrayList<>();
        final List< String > diagnostics = new ArrayList<>();
        int evidencePoints;
        int injects;
        int injectsWithCommand;
        int ambiguousSpends;

        public List< RedWindow > getWindows() {
            return Collections.unmodifiableList( windows );
        }

        public List< String > getDiagnostics() {
            return Collections.unmodifiableList( diagnostics );
        }

        public int getEvidencePoints() {
            return evidencePoints;
        }

        public int getInjects() {
            return injects;
        }

        public int getInjectsWithCommand() {
            return injectsWithCommand;
        }

        public int getAmbiguousSpends() {
            return ambiguousSpends;
        }
    }

    /** A red piece from one queen at one hatchery, before the hatchery's pieces are merged into windows. */
    private static final class Piece {

        final int start;
        final int end;
        final QueenSpendAttributor.TrackedQueen queen;
        final String note;
        final int hatchTag;
        /** Distance from where she was (the fact that started her presence) to the hatchery. */
        final double distance;

        Piece( final int start, final int end, final QueenSpendAttributor.TrackedQueen queen, final String note, final int hatchTag,
                final double distance ) {
            this.start = start;
            this.end = end;
            this.queen = queen;
            this.note = note;
            this.hatchTag = hatchTag;
            this.distance = distance;
        }

        Piece part( final int partStart, final int partEnd ) {
            return new Piece( partStart, partEnd, queen, note, hatchTag, distance );
        }
    }

    private MissedInjectCalculator() {
    }

    /**
     * @param input the player's evidence (hatcheries)
     * @param attribution queens with attributed spends and points
     * @param settings tuning values
     * @return hatchery tag to result, for every completed hatchery, in input order
     */
    public static Map< Integer, HatchResult > compute( final MissedInjectInput input, final QueenSpendAttributor.Result attribution,
            final Settings settings ) {
        final Map< Integer, HatchResult > results = new LinkedHashMap<>();
        final Map< Integer, List< QueenSpendAttributor.TrackedQueen > > queensByHatch = new LinkedHashMap<>();
        final List< Piece > pieces = new ArrayList<>();
        for ( final MissedInjectInput.Hatch hatch : input.hatches ) {
            if ( hatch.completedLoop < 0 )
                continue;
            final HatchResult result = new HatchResult();
            queensByHatch.put( Integer.valueOf( hatch.tag ), collectHatch( result, hatch, attribution, settings, pieces ) );
            results.put( Integer.valueOf( hatch.tag ), result );
        }
        final List< Piece > kept = oneHatchAtATime( pieces );
        for ( final MissedInjectInput.Hatch hatch : input.hatches ) {
            final HatchResult result = results.get( Integer.valueOf( hatch.tag ) );
            if ( result == null )
                continue;
            final List< Piece > hatchPieces = new ArrayList<>();
            for ( final Piece piece : kept )
                if ( piece.hatchTag == hatch.tag )
                    hatchPieces.add( piece );
            mergeInto( result.windows, hatchPieces );
            describe( result, hatch, queensByHatch.get( Integer.valueOf( hatch.tag ) ), settings );
        }
        return results;
    }

    /** Counters and red pieces of one hatchery; @return the queens with evidence or orders there */
    private static List< QueenSpendAttributor.TrackedQueen > collectHatch( final HatchResult result, final MissedInjectInput.Hatch hatch,
            final QueenSpendAttributor.Result attribution, final Settings settings, final List< Piece > pieces ) {
        final int[] counters = attribution.countersOf( hatch.tag );
        result.injects = counters[ 0 ];
        result.injectsWithCommand = counters[ 1 ];
        result.ambiguousSpends = counters[ 2 ];
        final List< int[] > eligible = eligibleLoops( hatch, settings );
        final List< QueenSpendAttributor.TrackedQueen > queensWithEvidence = new ArrayList<>();
        for ( final QueenSpendAttributor.TrackedQueen queen : attribution.queens ) {
            final int points = countPointsAt( queen, hatch.tag );
            if ( points == 0 && !( settings.followCommands && hasOrderAt( queen, hatch.tag ) ) )
                continue;
            result.evidencePoints += points;
            queensWithEvidence.add( queen );
            addPieces( pieces, queen, hatch, eligible, settings );
        }
        return queensWithEvidence;
    }

    /** Where one queen's pieces at different hatcheries overlap, keeps the piece of the hatchery nearest to her. */
    private static List< Piece > oneHatchAtATime( final List< Piece > pieces ) {
        final Map< QueenSpendAttributor.TrackedQueen, List< Piece > > byQueen = new LinkedHashMap<>();
        for ( final Piece piece : pieces ) {
            List< Piece > list = byQueen.get( piece.queen );
            if ( list == null ) {
                list = new ArrayList<>();
                byQueen.put( piece.queen, list );
            }
            list.add( piece );
        }
        final List< Piece > kept = new ArrayList<>();
        for ( final List< Piece > queenPieces : byQueen.values() ) {
            final Set< Integer > bounds = new java.util.TreeSet<>();
            for ( final Piece piece : queenPieces ) {
                bounds.add( Integer.valueOf( piece.start ) );
                bounds.add( Integer.valueOf( piece.end ) );
            }
            Integer previous = null;
            for ( final Integer bound : bounds ) {
                if ( previous != null ) {
                    final Piece winner = nearestCovering( queenPieces, previous.intValue(), bound.intValue() );
                    if ( winner != null )
                        kept.add( winner.part( previous.intValue(), bound.intValue() ) );
                }
                previous = bound;
            }
        }
        return kept;
    }

    private static Piece nearestCovering( final List< Piece > pieces, final int start, final int end ) {
        Piece best = null;
        for ( final Piece piece : pieces ) {
            if ( piece.start > start || piece.end < end )
                continue;
            if ( best == null || piece.distance < best.distance - QueenEnergy.EPSILON
                    || Math.abs( piece.distance - best.distance ) <= QueenEnergy.EPSILON && piece.hatchTag < best.hatchTag )
                best = piece;
        }
        return best;
    }

    /** Standing, unlocked and not injected. */
    private static List< int[] > eligibleLoops( final MissedInjectInput.Hatch hatch, final Settings settings ) {
        final int start = Math.max( hatch.completedLoop, settings.injectUnlockLoop );
        final int end = hatch.diedLoop >= 0 ? Math.min( hatch.diedLoop, settings.replayEndLoop ) : settings.replayEndLoop;
        final List< int[] > standing = new ArrayList<>();
        standing.add( new int[] { start, end } );
        return LoopIntervals.subtract( standing, hatch.injectedLoops );
    }

    private static int countPointsAt( final QueenSpendAttributor.TrackedQueen queen, final int hatchTag ) {
        int count = 0;
        for ( final QueenPresence.Point point : queen.points )
            if ( point.isNear( hatchTag ) )
                count++;
        return count;
    }

    /** @return true if one of her orders targets the hatchery (she may idle there after walking in) */
    private static boolean hasOrderAt( final QueenSpendAttributor.TrackedQueen queen, final int hatchTag ) {
        for ( final QueenPresence.Point order : queen.orders )
            if ( order.isNear( hatchTag ) )
                return true;
        return false;
    }

    private static void addPieces( final List< Piece > pieces, final QueenSpendAttributor.TrackedQueen queen, final MissedInjectInput.Hatch hatch,
            final List< int[] > eligible, final Settings settings ) {
        final QueenPresence presence = new QueenPresence( queen.getFacts(), queen.energy.getEndLoop(),
                new QueenPresence.Settings( settings.holdLoops, settings.bridgeLimitLoops, settings.followCommands, settings.cellsPerLoop,
                        settings.commandsEndLoop ) );
        final List< int[] > spare = queen.energy.spareIntervals( hatch.tag, settings.spareHorizonLoops );
        for ( final QueenPresence.Interval interval : presence.intervalsAt( hatch.tag ) ) {
            final List< int[] > present = new ArrayList<>();
            present.add( new int[] { interval.start, interval.end } );
            final List< int[] > red = LoopIntervals.intersect( LoopIntervals.intersect( present, spare ), eligible );
            if ( red.isEmpty() )
                continue;
            final String note = "queen " + queen.queen.tagText + " " + describePresence( interval, hatch, settings ) + ", 25+ spare energy";
            final double distance = Double.isNaN( interval.from.x ) ? 0.0d : Math.hypot( interval.from.x - hatch.x, interval.from.y - hatch.y );
            for ( final int[] part : red )
                pieces.add( new Piece( part[ 0 ], part[ 1 ], queen, note, hatch.tag, distance ) );
        }
    }

    private static String describePresence( final QueenPresence.Interval interval, final MissedInjectInput.Hatch hatch, final Settings settings ) {
        final TimeFormatter time = settings.timeFormatter;
        final String from = interval.fromArrival
                ? "at the hatchery from arrival " + time.format( interval.start ) + " (estimated, " + interval.from.label + " " + time.format( interval.from.loop ) + ")"
                : "at the hatchery from " + interval.from.label + " " + time.format( interval.from.loop );
        switch ( interval.endReason ) {
            case MOVED :
                return from + " until " + interval.to.label + " " + Math.round( Math.hypot( interval.to.x - hatch.x, interval.to.y - hatch.y ) )
                        + " cells away " + time.format( interval.end );
            case BRIDGE :
                return from + " to " + interval.to.label + " " + time.format( interval.to.loop );
            case BRIDGE_LIMIT :
                return from + " (next evidence " + interval.to.label + " " + time.format( interval.to.loop ) + ", counted until "
                        + time.format( interval.end ) + ")";
            case NEXT_AWAY :
                return from + " until " + interval.to.label + " " + Math.round( Math.hypot( interval.to.x - hatch.x, interval.to.y - hatch.y ) )
                        + " cells away " + time.format( interval.to.loop );
            case HOLD :
                return from + " (no later evidence, held until " + time.format( interval.end ) + ")";
            case LIFE_END :
            default :
                return from + " until " + ( interval.to != null && interval.to.death ? "death " : "the end " ) + time.format( interval.end );
        }
    }

    private static void mergeInto( final List< RedWindow > windows, final List< Piece > pieces ) {
        Collections.sort( pieces, new Comparator< Piece >() {
            @Override
            public int compare( final Piece a, final Piece b ) {
                return a.start != b.start ? ( a.start < b.start ? -1 : 1 ) : ( a.end < b.end ? -1 : a.end == b.end ? 0 : 1 );
            }
        } );
        RedWindow current = null;
        for ( final Piece piece : pieces ) {
            if ( current == null || piece.start > current.end ) {
                current = new RedWindow( piece.start, piece.end );
                windows.add( current );
            }
            current.end = Math.max( current.end, piece.end );
            current.queenTagTexts.add( piece.queen.queen.tagText );
            current.notes.add( piece.note );
        }
        for ( final RedWindow window : windows )
            fillEnergies( window, pieces );
    }

    private static void fillEnergies( final RedWindow window, final List< Piece > pieces ) {
        for ( final Piece piece : pieces ) {
            if ( piece.end <= window.start || piece.start >= window.end )
                continue;
            final double startEnergy = piece.queen.energy.energyAt( piece.start );
            final double endEnergy = piece.queen.energy.energyAt( Math.max( piece.start, piece.end - 1 ) );
            window.minStartEnergy = Math.min( window.minStartEnergy, startEnergy );
            window.maxStartEnergy = Math.max( window.maxStartEnergy, startEnergy );
            window.minEndEnergy = Math.min( window.minEndEnergy, endEnergy );
            window.maxEndEnergy = Math.max( window.maxEndEnergy, endEnergy );
        }
    }

    private static void describe( final HatchResult result, final MissedInjectInput.Hatch hatch,
            final List< QueenSpendAttributor.TrackedQueen > queensWithEvidence, final Settings settings ) {
        if ( queensWithEvidence.isEmpty() ) {
            result.diagnostics.add( "No queen evidence at this hatchery (birth, inject, tumor or position within the radius), so no missed injects." );
            return;
        }
        final Set< String > tagTexts = new LinkedHashSet<>();
        for ( final QueenSpendAttributor.TrackedQueen queen : queensWithEvidence )
            tagTexts.add( queen.queen.tagText );
        result.diagnostics.add( "Queen evidence at this hatchery: " + result.evidencePoints + " points from queens " + join( tagTexts, ", " )
                + "; injects " + result.injects + " (" + result.injectsWithCommand + " with a Spawn Larva command), ambiguous spends "
                + result.ambiguousSpends + "; " + ( settings.followCommands ? "queen orders followed." : "queen orders not followed (evidence only)." ) );
        for ( final RedWindow window : result.windows )
            result.diagnostics.add( "Red " + settings.timeFormatter.format( window.start ) + "-" + settings.timeFormatter.format( window.end ) + ": "
                    + window.getNote() + "." );
        for ( final QueenSpendAttributor.TrackedQueen queen : queensWithEvidence )
            result.diagnostics.add( "Queen " + queen.queen.tagText + " evidence: " + describeEvidence( queen, hatch, settings ) + "." );
    }

    /**
     * The queen's points (and orders, when followed) as "label time place (energy)", with place "here", "other hatchery" or "away";
     * a stop has no place.
     */
    private static String describeEvidence( final QueenSpendAttributor.TrackedQueen queen, final MissedInjectInput.Hatch hatch,
            final Settings settings ) {
        final List< QueenPresence.Point > facts = settings.followCommands ? queen.getFacts() : new ArrayList<>( queen.points );
        Collections.sort( facts, new Comparator< QueenPresence.Point >() {
            @Override
            public int compare( final QueenPresence.Point a, final QueenPresence.Point b ) {
                return a.loop != b.loop ? ( a.loop < b.loop ? -1 : 1 ) : a.kind.ordinal() - b.kind.ordinal();
            }
        } );
        final List< String > parts = new ArrayList<>();
        for ( final QueenPresence.Point point : facts ) {
            final String place = point.kind == QueenPresence.Kind.STOP ? ""
                    : point.isNear( hatch.tag ) ? " here" : point.homeHatchTag == QueenPresence.AWAY ? " away" : " other hatchery";
            parts.add( point.label + " " + settings.timeFormatter.format( point.loop ) + place + " ("
                    + Math.round( queen.energy.energyAt( point.loop ) ) + ")" );
        }
        return join( parts, ", " );
    }

    static String join( final Iterable< String > values, final String separator ) {
        final StringBuilder builder = new StringBuilder();
        for ( final String value : values ) {
            if ( builder.length() > 0 )
                builder.append( separator );
            builder.append( value );
        }
        return builder.toString();
    }
}
