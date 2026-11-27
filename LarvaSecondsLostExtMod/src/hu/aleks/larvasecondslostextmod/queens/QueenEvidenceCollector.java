package hu.aleks.larvasecondslostextmod.queens;

import hu.scelightapi.sc2.rep.model.IEvent;
import hu.scelightapi.sc2.rep.model.gameevents.IGameEvents;
import hu.scelightapi.sc2.rep.model.trackerevents.ITrackerEvents;
import hu.scelightapi.sc2.rep.repproc.IRepProcessor;
import hu.scelightapi.sc2.rep.repproc.IUser;
import hu.scelightapi.service.IFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads queen evidence from a replay, per player (E19-S07, S11): queen lives, queen creep tumors with positions (tracker
 * {@code UnitInit CreepTumorQueen}), queen positions of damaged queens (tracker {@code UnitPositions}), and the commands given with
 * queens selected ({@link ReplayCommandStream}): Spawn Larva, tumor and transfusion commands with the queens of the selection, and
 * the queens' orders (moves, single-queen cast orders, stops). Hatcheries and their inject windows come from the analyzer.
 */
public final class QueenEvidenceCollector {

    /** One player's queen evidence and counters. */
    public static final class PlayerEvidence {

        final List< MissedInjectInput.Queen > queens = new ArrayList<>();
        final List< MissedInjectInput.InjectCommand > injectCommands = new ArrayList<>();
        final List< QueenSpend > tumors = new ArrayList<>();
        final List< QueenSpend > transfusions = new ArrayList<>();
        final List< MissedInjectInput.Position > positions = new ArrayList<>();
        final List< QueenEvidenceDecoding.TumorCommand > tumorCommands = new ArrayList<>();
        final List< MissedInjectInput.Move > moves = new ArrayList<>();
        int queenCommands;
        int commandsEndLoop = -1;
        int tumorsWithCommand;

        /** @return a model input with this evidence; the caller adds the hatcheries */
        public MissedInjectInput toInput() {
            final MissedInjectInput input = new MissedInjectInput();
            for ( final MissedInjectInput.Queen queen : queens )
                input.addQueen( queen );
            for ( final MissedInjectInput.InjectCommand command : injectCommands )
                input.addInjectCommand( command );
            for ( final QueenSpend tumor : tumors )
                input.addTumor( tumor );
            for ( final QueenSpend transfusion : transfusions )
                input.addTransfusion( transfusion );
            for ( final MissedInjectInput.Position position : positions )
                input.addPosition( position );
            for ( final MissedInjectInput.Move move : moves )
                input.addMove( move );
            return input;
        }

        public int getQueenCount() {
            return queens.size();
        }

        public int getTumorCount() {
            return tumors.size();
        }

        public int getPositionCount() {
            return positions.size();
        }

        /** @return commands given while at least one of the player's queens was selected */
        public int getQueenCommandCount() {
            return queenCommands;
        }

        public int getTumorsWithCommand() {
            return tumorsWithCommand;
        }

        /**
         * @return loop of the replay's last decoded game event (Scelight stops decoding some replays early: after it, missing orders
         *         prove nothing), or -1 without game events
         */
        public int getCommandsEndLoop() {
            return commandsEndLoop;
        }

        /** @return queen orders (moves, single-queen cast orders, stops) */
        public int getMoveCount() {
            return moves.size();
        }
    }

    /** A queen being built from tracker events. */
    private static final class QueenRecord {
        final int tag;
        final int playerId;
        final int bornLoop;
        final double bornX;
        final double bornY;
        int diedLoop = -1;
        double diedX;
        double diedY;

        QueenRecord( final int tag, final int playerId, final int bornLoop, final double bornX, final double bornY ) {
            this.tag = tag;
            this.playerId = playerId;
            this.bornLoop = bornLoop;
            this.bornX = bornX;
            this.bornY = bornY;
        }
    }

    private final IRepProcessor repProc;
    private final Map< Integer, PlayerEvidence > evidenceByPlayerId = new LinkedHashMap<>();
    private final Map< Integer, QueenRecord > queenByTag = new LinkedHashMap<>();
    private final Map< Integer, QueenRecord > aliveQueenByUnitIndex = new HashMap<>();
    private final List< int[] > pendingTumors = new ArrayList<>();
    /** {loop, tag} of every unit death, for control groups. */
    private final List< int[] > deaths = new ArrayList<>();

    private QueenEvidenceCollector( final IRepProcessor repProc ) {
        this.repProc = repProc;
    }

    /**
     * @param repProc replay processor with tracker and game events
     * @param factory Scelight factory; without it commands are skipped (kept for callers, the selection is tracked here)
     * @return player id to evidence, for every player with a queen or a queen tumor
     */
    public static Map< Integer, PlayerEvidence > collect( final IRepProcessor repProc, final IFactory factory ) {
        final QueenEvidenceCollector collector = new QueenEvidenceCollector( repProc );
        final IEvent[] tracker = repProc.getReplay().getTrackerEvents() == null ? null : repProc.getReplay().getTrackerEvents().getEvents();
        final IEvent[] game = repProc.getReplay().getGameEvents() == null ? null : repProc.getReplay().getGameEvents().getEvents();
        if ( tracker != null )
            for ( final IEvent event : tracker )
                if ( event != null )
                    collector.onTrackerEvent( event );
        collector.finishQueens();
        if ( game != null && factory != null )
            collector.readCommands( game );
        collector.finishTumors();
        final int commandsEndLoop = lastLoopOf( game );
        for ( final PlayerEvidence evidence : collector.evidenceByPlayerId.values() )
            evidence.commandsEndLoop = commandsEndLoop;
        return collector.evidenceByPlayerId;
    }

    /**
     * @return loop of the last command, selection, control group or camera event: the end of what players did that the replay shows
     *         (a leave event may follow a decoding stop)
     */
    private static int lastLoopOf( final IEvent[] events ) {
        if ( events != null )
            for ( int i = events.length - 1; i >= 0; i-- )
                if ( events[ i ] != null && isPlayerAction( events[ i ] ) )
                    return events[ i ].getLoop();
        return -1;
    }

    private static boolean isPlayerAction( final IEvent event ) {
        final int id = event.getId();
        if ( id == IGameEvents.ID_CMD || id == IGameEvents.ID_SELECTION_DELTA || id == IGameEvents.ID_CONTROL_GROUP_UPDATE )
            return true;
        final String name = event.getName();
        return "CameraUpdate".equals( name ) || ReplayCommandStream.UPDATE_TARGET_POINT.equals( name ) || ReplayCommandStream.UPDATE_TARGET_UNIT.equals( name );
    }

    private void onTrackerEvent( final IEvent event ) {
        switch ( event.getId() ) {
            case ITrackerEvents.ID_UNIT_BORN :
            case ITrackerEvents.ID_UNIT_INIT :
                onUnitStart( event );
                break;
            case ITrackerEvents.ID_UNIT_DIED :
                onUnitDied( event );
                break;
            case ITrackerEvents.ID_UNIT_POSITIONS :
                onUnitPositions( event );
                break;
            default :
                break;
        }
    }

    private void onUnitStart( final IEvent event ) {
        final String type = QueenEvidenceDecoding.normalize( text( event.get( "unitTypeName" ) ) );
        final Integer tag = tagOf( event );
        final Integer playerId = firstNonNull( (Integer) event.get( "controlPlayerId" ), (Integer) event.get( "upkeepPlayerId" ) );
        final Integer x = (Integer) event.get( "x" );
        final Integer y = (Integer) event.get( "y" );
        if ( tag == null || playerId == null || x == null || y == null )
            return;
        if ( event.getId() == ITrackerEvents.ID_UNIT_BORN && type.contains( "queen" ) ) {
            final QueenRecord queen = new QueenRecord( tag.intValue(), playerId.intValue(), event.getLoop(), x.intValue(), y.intValue() );
            queenByTag.put( tag, queen );
            aliveQueenByUnitIndex.put( Integer.valueOf( QueenEvidenceDecoding.unitIndexOf( tag.intValue() ) ), queen );
        } else if ( event.getId() == ITrackerEvents.ID_UNIT_INIT && "creeptumorqueen".equals( type ) )
            pendingTumors.add( new int[] { playerId.intValue(), event.getLoop(), x.intValue(), y.intValue() } );
    }

    private void onUnitDied( final IEvent event ) {
        final Integer tag = tagOf( event );
        if ( tag != null )
            deaths.add( new int[] { event.getLoop(), tag.intValue() } );
        final QueenRecord queen = tag == null ? null : queenByTag.get( tag );
        if ( queen == null || queen.diedLoop >= 0 )
            return;
        queen.diedLoop = event.getLoop();
        final Integer x = (Integer) event.get( "x" );
        final Integer y = (Integer) event.get( "y" );
        queen.diedX = x == null ? queen.bornX : x.intValue();
        queen.diedY = y == null ? queen.bornY : y.intValue();
        aliveQueenByUnitIndex.remove( Integer.valueOf( QueenEvidenceDecoding.unitIndexOf( tag.intValue() ) ) );
    }

    private void onUnitPositions( final IEvent event ) {
        final Object first = event.get( "firstUnitIndex" );
        final Object items = event.get( "items" );
        if ( !( first instanceof Integer ) || !( items instanceof Object[] ) )
            return;
        for ( final int[] position : QueenEvidenceDecoding.decodeUnitPositions( ( (Integer) first ).intValue(), (Object[]) items ) ) {
            final QueenRecord queen = aliveQueenByUnitIndex.get( Integer.valueOf( position[ 0 ] ) );
            if ( queen != null )
                evidenceOf( queen.playerId ).positions.add( new MissedInjectInput.Position( queen.tag, event.getLoop(), position[ 1 ], position[ 2 ] ) );
        }
    }

    private void finishQueens() {
        for ( final QueenRecord queen : queenByTag.values() ) {
            final String tagText = repProc.getTagTransformation() == null ? String.valueOf( queen.tag )
                    : repProc.getTagTransformation().tagToString( queen.tag );
            evidenceOf( queen.playerId ).queens.add( new MissedInjectInput.Queen( queen.tag, tagText, queen.bornLoop, queen.bornX, queen.bornY,
                    queen.diedLoop, queen.diedX, queen.diedY ) );
        }
    }

    private void readCommands( final IEvent[] game ) {
        ReplayCommandStream.read( game, deaths, new ReplayCommandStream.Listener() {
            @Override
            public void onCommand( final ReplayCommandStream.Command command ) {
                QueenEvidenceCollector.this.onCommand( command );
            }
        } );
    }

    private void onCommand( final ReplayCommandStream.Command command ) {
        final Integer playerId = playerIdOfUser( command.getUserId() );
        if ( playerId == null )
            return;
        final int[] queenTags = selectedQueenTags( command.getSelection(), playerId.intValue(), command.getLoop() );
        if ( queenTags.length == 0 )
            return;
        final PlayerEvidence evidence = evidenceOf( playerId.intValue() );
        evidence.queenCommands++;
        final String abilityId = command.getAbilityId();
        final String commandId = command.getCommandId();
        final boolean single = queenTags.length == 1;
        if ( QueenEvidenceDecoding.isInjectCommand( abilityId, commandId ) ) {
            if ( command.getTargetTag() == null )
                return;
            evidence.injectCommands.add( new MissedInjectInput.InjectCommand( command.getLoop(), command.getTargetTag().intValue(), queenTags ) );
            if ( single && command.hasTarget() )
                evidence.moves.add( order( command, queenTags, true, "inject order" ) );
        } else if ( QueenEvidenceDecoding.isQueenTumorCommand( abilityId, commandId ) ) {
            if ( !command.hasTarget() )
                return;
            evidence.tumorCommands.add( new QueenEvidenceDecoding.TumorCommand( command.getLoop(), command.getX(), command.getY(), queenTags ) );
            if ( single )
                evidence.moves.add( order( command, queenTags, true, "tumor order" ) );
        } else if ( QueenEvidenceDecoding.isTransfusionCommand( abilityId, commandId ) )
            evidence.transfusions.add( new QueenSpend( command.getLoop(), QueenSpend.Kind.TRANSFUSE, -1, command.getX(), command.getY(), queenTags ) );
        else if ( command.hasTarget() )
            evidence.moves.add( order( command, queenTags, false, "move" ) );
        else if ( QueenEvidenceDecoding.isStopCommand( abilityId, commandId ) )
            evidence.moves.add( new MissedInjectInput.Move( command.getLoop(), queenTags, Double.NaN, Double.NaN, false, false, true, "stop" ) );
    }

    private static MissedInjectInput.Move order( final ReplayCommandStream.Command command, final int[] queenTags, final boolean cast,
            final String label ) {
        return new MissedInjectInput.Move( command.getLoop(), queenTags, command.getX(), command.getY(), command.isQueued(), cast, false, label );
    }

    /** Tumors with the queens of their command; a tumor without a matching command has no candidates. */
    private void finishTumors() {
        for ( final int[] tumor : pendingTumors ) {
            final PlayerEvidence evidence = evidenceOf( tumor[ 0 ] );
            final QueenEvidenceDecoding.TumorCommand command = QueenEvidenceDecoding.matchTumor( evidence.tumorCommands, tumor[ 1 ], tumor[ 2 ], tumor[ 3 ] );
            if ( command != null )
                evidence.tumorsWithCommand++;
            evidence.tumors.add( new QueenSpend( tumor[ 1 ], QueenSpend.Kind.TUMOR, -1, tumor[ 2 ], tumor[ 3 ], command == null ? null : command.queenTags,
                    command == null ? -1 : command.loop ) );
        }
    }

    /** @return the player's queens in the selection that are alive at the loop */
    private int[] selectedQueenTags( final int[] selection, final int playerId, final int loop ) {
        final List< Integer > tags = new ArrayList<>();
        for ( final int tag : selection ) {
            final QueenRecord queen = queenByTag.get( Integer.valueOf( tag ) );
            if ( queen != null && queen.playerId == playerId && queen.bornLoop <= loop && ( queen.diedLoop < 0 || loop < queen.diedLoop ) )
                tags.add( Integer.valueOf( tag ) );
        }
        final int[] result = new int[ tags.size() ];
        for ( int i = 0; i < result.length; i++ )
            result[ i ] = tags.get( i ).intValue();
        return result;
    }

    private Integer playerIdOfUser( final int userId ) {
        final IUser[] usersByUserId = repProc.getUsersByUserId();
        final IUser[] usersByPlayerId = repProc.getUsersByPlayerId();
        if ( usersByUserId == null || usersByPlayerId == null || userId < 0 || userId >= usersByUserId.length || usersByUserId[ userId ] == null )
            return null;
        for ( int playerId = 0; playerId < usersByPlayerId.length; playerId++ )
            if ( usersByPlayerId[ playerId ] == usersByUserId[ userId ] )
                return Integer.valueOf( playerId );
        return null;
    }

    private PlayerEvidence evidenceOf( final int playerId ) {
        PlayerEvidence evidence = evidenceByPlayerId.get( Integer.valueOf( playerId ) );
        if ( evidence == null ) {
            evidence = new PlayerEvidence();
            evidenceByPlayerId.put( Integer.valueOf( playerId ), evidence );
        }
        return evidence;
    }

    private static Integer tagOf( final IEvent event ) {
        final Object index = event.get( "unitTagIndex" );
        final Object recycle = event.get( "unitTagRecycle" );
        return index instanceof Integer && recycle instanceof Integer
                ? Integer.valueOf( QueenEvidenceDecoding.tagOf( ( (Integer) index ).intValue(), ( (Integer) recycle ).intValue() ) ) : null;
    }

    private static String text( final Object value ) {
        return value == null ? "" : value.toString();
    }

    private static Integer firstNonNull( final Integer primary, final Integer fallback ) {
        return primary != null ? primary : fallback;
    }
}
