package hu.aleks.larvasecondslostextmod;

import hu.aleks.larvasecondslostextmod.queens.MissedInjectCalculator;
import hu.aleks.larvasecondslostextmod.queens.MissedInjectInput;
import hu.aleks.larvasecondslostextmod.queens.QueenEvidenceCollector;
import hu.aleks.larvasecondslostextmod.queens.QueenSpendAttributor;
import hu.scelightapi.sc2.rep.repproc.IRepProcessor;
import hu.scelightapi.service.IFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Red (missed-inject) timelines per hatchery from the queen model in {@code queens} (E19-S10), in the output shape the timeline
 * and the tables already use ({@link HatcheryIdleInjectTimeline}).
 */
final class LarvaMissedInjectAnalysis {

    /** Cells within which queen evidence belongs to a hatchery. */
    static final double RADIUS = 12.0d;

    /**
     * How long a queen counts as at the hatchery after evidence there when her next evidence is elsewhere or missing (OQ-5), in
     * loops. Equal to the inject duration, so a queen that injects and leaves adds no red.
     */
    static final int HOLD_LOOPS = LarvaGameClock.INJECT_CYCLE_LOOPS;

    /**
     * Evidence only: how long a queen counts as at the hatchery after evidence there when her next evidence is at the same hatchery,
     * in game clock seconds. A longer gap without evidence may be a queen that walked away and came back; it adds at most about 2
     * missed injects. When the player's queen orders are followed there is no limit: she stays until an order sends her away.
     */
    static final int BRIDGE_LIMIT_SECONDS = 90;

    /**
     * Queen orders are followed for a player when at least this share of the player's injects has a Spawn Larva command with queens
     * selected (E19-S11). Below it (computer players, replays whose selections cannot be rebuilt) presence uses evidence only.
     */
    static final double ORDER_COVERAGE_MIN = 0.5d;

    /**
     * Energy a queen spends this soon (loops) is not spare for an inject; spends further ahead do not count (E19-S02: a queen
     * hoarding energy at a skipped hatchery before a creep burst). Equal to the inject duration.
     */
    static final int SPARE_HORIZON_LOOPS = LarvaGameClock.INJECT_CYCLE_LOOPS;

    /** Queen walking speed off creep (cells per real second at Faster), for arrival estimates: slow on purpose, arrivals count late. */
    static final double QUEEN_CELLS_PER_SECOND = 1.31d;

    /** How long before an inject window a Spawn Larva command may be, in game clock seconds (walking or a queued inject). */
    static final int INJECT_COMMAND_LOOKBACK_SECONDS = 40;

    /** Rounding slack between a Spawn Larva command and the inject window start, in loops. */
    static final int INJECT_COMMAND_LOOKAHEAD_LOOPS = 32;

    /** Answer for the diagnostics: how red windows are made. */
    static final String CONCLUSION = "A missed inject needs a queen within " + (int) RADIUS + " cells of the hatchery with 25 spare energy while the "
            + "hatchery is not injected. Presence comes from queen evidence (birth, injects, creep tumors, damaged-unit positions, death) and, "
            + "when the player's queen commands can be followed, from her orders: she stays until an order sends her away. Without orders "
            + "she stays one inject (29 seconds) after evidence (up to " + BRIDGE_LIMIT_SECONDS + " seconds when the next evidence is at "
            + "the same hatchery). Energy comes from a per-queen model (25 at birth, regeneration, 200 cap, attributed injects, tumors and "
            + "transfusions). Energy a queen spends on tumors or transfusions within the next inject (29 seconds) is not spare.";

    /** Hatchery facts from the analyzer. */
    static final class HatchInfo {
        final int tag;
        final String tagText;
        final int playerId;
        final String playerName;
        final String type;
        final int x;
        final int y;
        final int completedLoop;
        final int destroyedLoop;

        HatchInfo( final int tag, final String tagText, final int playerId, final String playerName, final String type, final int x, final int y,
                final int completedLoop, final int destroyedLoop ) {
            this.tag = tag;
            this.tagText = tagText;
            this.playerId = playerId;
            this.playerName = playerName;
            this.type = type;
            this.x = x;
            this.y = y;
            this.completedLoop = completedLoop;
            this.destroyedLoop = destroyedLoop;
        }
    }

    /** Timelines and replay-wide queen counters. */
    static final class Output {
        final List< HatcheryIdleInjectTimeline > timelines = new ArrayList<>();
        int queens;
        int queensWithPositions;
        int spendsFromSelection;
        int queenCommands;
        int queenOrders;
        /** Player names whose queen orders are followed. */
        final List< String > playersWithOrdersFollowed = new ArrayList<>();
    }

    private LarvaMissedInjectAnalysis() {
    }

    /**
     * @param repProc replay processor
     * @param factory Scelight factory (selection trackers), may be null
     * @param hatches hatcheries of all players
     * @param injectTimelines inject windows per hatchery
     * @param injectUnlockLoopByPlayerName first loop an inject can be missed, per player (absent: no pool)
     * @param replayEndLoop replay end loop
     * @return timelines (one per completed hatchery, unsorted) and counters
     */
    static Output build( final IRepProcessor repProc, final IFactory factory, final List< HatchInfo > hatches,
            final List< HatcheryInjectTimeline > injectTimelines, final Map< String, Integer > injectUnlockLoopByPlayerName,
            final int replayEndLoop ) {
        final Output output = new Output();
        final Map< Integer, QueenEvidenceCollector.PlayerEvidence > evidenceByPlayerId = QueenEvidenceCollector.collect( repProc, factory );
        for ( final QueenEvidenceCollector.PlayerEvidence evidence : evidenceByPlayerId.values() ) {
            output.queens += evidence.getQueenCount();
            output.queenCommands += evidence.getQueenCommandCount();
            output.queenOrders += evidence.getMoveCount();
        }
        final Map< Integer, List< int[] > > injectWindowsByTag = injectWindowsByTag( injectTimelines );
        for ( final Map.Entry< Integer, List< HatchInfo > > entry : hatchesByPlayerId( hatches ).entrySet() ) {
            final QueenEvidenceCollector.PlayerEvidence evidence = evidenceByPlayerId.get( entry.getKey() );
            final MissedInjectInput input = evidence == null ? new MissedInjectInput() : evidence.toInput();
            for ( final HatchInfo hatch : entry.getValue() ) {
                final List< int[] > windows = injectWindowsByTag.get( Integer.valueOf( hatch.tag ) );
                input.addHatch( new MissedInjectInput.Hatch( hatch.tag, hatch.x, hatch.y, hatch.completedLoop, hatch.destroyedLoop,
                        windows == null ? Collections.< int[] >emptyList() : windows ) );
            }
            final QueenSpendAttributor.Result attribution = QueenSpendAttributor.attribute( input, new QueenSpendAttributor.Settings( RADIUS,
                    replayEndLoop, LarvaGameClock.secondsToLoops( INJECT_COMMAND_LOOKBACK_SECONDS ), INJECT_COMMAND_LOOKAHEAD_LOOPS ) );
            output.spendsFromSelection += attribution.getFromSelection();
            for ( final QueenSpendAttributor.TrackedQueen queen : attribution.getQueens() )
                if ( hasPosition( queen ) )
                    output.queensWithPositions++;
            final String playerName = entry.getValue().get( 0 ).playerName;
            final Integer unlockLoop = injectUnlockLoopByPlayerName.get( playerName );
            final boolean followOrders = followsOrders( attribution );
            if ( followOrders )
                output.playersWithOrdersFollowed.add( playerName );
            final Map< Integer, MissedInjectCalculator.HatchResult > results = MissedInjectCalculator.compute( input, attribution,
                    new MissedInjectCalculator.Settings( HOLD_LOOPS,
                            LarvaGameClock.secondsToLoops( BRIDGE_LIMIT_SECONDS ), unlockLoop == null ? -1 : unlockLoop.intValue(),
                            replayEndLoop, timeFormatter(), followOrders, QUEEN_CELLS_PER_SECOND / LarvaGameClock.LOOPS_PER_SECOND,
                            SPARE_HORIZON_LOOPS,
                            commandsEndLoop( evidence, replayEndLoop - HOLD_LOOPS ) ) );
            for ( final HatchInfo hatch : entry.getValue() ) {
                final MissedInjectCalculator.HatchResult result = results.get( Integer.valueOf( hatch.tag ) );
                if ( result != null )
                    output.timelines.add( toTimeline( hatch, result ) );
            }
        }
        return output;
    }

    /**
     * @param completeFromLoop game events that reach this loop count as complete
     * @return last loop with decoded commands, {@link Integer#MAX_VALUE} if complete; a replay whose game events end early cannot
     *         show that a queen stayed
     */
    private static int commandsEndLoop( final QueenEvidenceCollector.PlayerEvidence evidence, final int completeFromLoop ) {
        final int end = evidence == null ? -1 : evidence.getCommandsEndLoop();
        return end < 0 || end >= completeFromLoop ? Integer.MAX_VALUE : end;
    }

    /** @return true if enough injects have a Spawn Larva command with queens selected to trust the player's queen selections */
    static boolean followsOrders( final QueenSpendAttributor.Result attribution ) {
        final int injects = attribution.getInjects();
        return injects > 0 && attribution.getInjectsWithCommand() >= ORDER_COVERAGE_MIN * injects;
    }

    private static boolean hasPosition( final QueenSpendAttributor.TrackedQueen queen ) {
        for ( final hu.aleks.larvasecondslostextmod.queens.QueenPresence.Point point : queen.getPoints() )
            if ( "seen".equals( point.getLabel() ) )
                return true;
        return false;
    }

    private static HatcheryIdleInjectTimeline toTimeline( final HatchInfo hatch,
            final MissedInjectCalculator.HatchResult result ) {
        final List< HatcheryIdleInjectWindow > windows = new ArrayList<>();
        for ( final MissedInjectCalculator.RedWindow red : result.getWindows() )
            windows.add( new HatcheryIdleInjectWindow( red.getStart(), red.getEnd(), LarvaGameClock.loopToMs( red.getStart() ),
                    LarvaGameClock.loopToMs( red.getEnd() ), LarvaGameClock.format( red.getStart() ), LarvaGameClock.format( red.getEnd() ),
                    red.getQueenTagTexts().size(), red.getMinStartEnergy(), red.getMaxStartEnergy(), red.getMinEndEnergy(), red.getMaxEndEnergy(),
                    join( red.getQueenTagTexts() ), red.getNote() ) );
        return new HatcheryIdleInjectTimeline( hatch.tag, hatch.tagText, hatch.playerName, hatch.type, RADIUS, result.getEvidencePoints(),
                result.getInjectsWithCommand(), result.getAmbiguousSpends(), windows, result.getDiagnostics() );
    }

    private static Map< Integer, List< HatchInfo > > hatchesByPlayerId( final List< HatchInfo > hatches ) {
        final Map< Integer, List< HatchInfo > > byPlayerId = new LinkedHashMap<>();
        for ( final HatchInfo hatch : hatches ) {
            List< HatchInfo > list = byPlayerId.get( Integer.valueOf( hatch.playerId ) );
            if ( list == null ) {
                list = new ArrayList<>();
                byPlayerId.put( Integer.valueOf( hatch.playerId ), list );
            }
            list.add( hatch );
        }
        return byPlayerId;
    }

    private static Map< Integer, List< int[] > > injectWindowsByTag( final List< HatcheryInjectTimeline > injectTimelines ) {
        final Map< Integer, List< int[] > > byTag = new HashMap<>();
        for ( final HatcheryInjectTimeline timeline : injectTimelines ) {
            final List< int[] > windows = new ArrayList<>();
            for ( final HatcheryInjectWindow window : timeline.getInjectWindowList() )
                windows.add( new int[] { window.getStartLoop(), window.getEndLoop() } );
            byTag.put( Integer.valueOf( timeline.getHatcheryTag() ), windows );
        }
        return byTag;
    }

    private static MissedInjectCalculator.TimeFormatter timeFormatter() {
        return new MissedInjectCalculator.TimeFormatter() {
            @Override
            public String format( final int loop ) {
                return LarvaGameClock.format( loop );
            }
        };
    }

    private static String join( final List< String > values ) {
        final StringBuilder builder = new StringBuilder();
        for ( final String value : values ) {
            if ( builder.length() > 0 )
                builder.append( ", " );
            builder.append( value );
        }
        return builder.toString();
    }
}
