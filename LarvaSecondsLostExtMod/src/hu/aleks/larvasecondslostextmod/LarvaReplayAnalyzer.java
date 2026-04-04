package hu.aleks.larvasecondslostextmod;

import hu.scelightapi.service.IFactory;
import hu.scelightapi.sc2.balancedata.IBdUtil;
import hu.scelightapi.sc2.balancedata.model.IAbility;
import hu.scelightapi.sc2.balancedata.model.ICommand;
import hu.scelightapi.sc2.balancedata.model.IUnit;
import hu.scelightapi.sc2.rep.model.IEvent;
import hu.scelightapi.sc2.rep.model.IReplay;
import hu.scelightapi.sc2.rep.model.gameevents.IControlGroupUpdateEvent;
import hu.scelightapi.sc2.rep.model.gameevents.IGameEvents;
import hu.scelightapi.sc2.rep.model.gameevents.cmd.ICmdEvent;
import hu.scelightapi.sc2.rep.model.gameevents.selectiondelta.ISelectionDeltaEvent;
import hu.scelightapi.sc2.rep.repproc.IRepProcessor;
import hu.scelightapi.sc2.rep.repproc.ISelectionTracker;
import hu.scelightapi.sc2.rep.repproc.IUser;
import hu.scelightapi.sc2.rep.model.trackerevents.IBaseUnitEvent;
import hu.scelightapi.sc2.rep.model.trackerevents.IPlayerStatsEvent;
import hu.scelightapi.sc2.rep.model.trackerevents.ITrackerEvents;
import hu.scelightapi.sc2.rep.model.trackerevents.IUnitBornEvent;
import hu.scelightapi.sc2.rep.model.trackerevents.IUnitInitEvent;
import hu.scelightapi.util.type.IXString;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.LinkedHashSet;

/**
 * Epic 6 replay analysis foundation that reconstructs per-hatchery larva counts.
 */
public class LarvaReplayAnalyzer {

    /** Queen unit id. */
    private static final String UNIT_QUEEN = "Queen";

    /** Dedicated queen-to-hatchery radius for Story 11.03. */
    private static final double IDLE_INJECT_RADIUS = 12.0d;

    /** Queen starting energy on completion. */
    private static final double QUEEN_STARTING_ENERGY = 25.0d;

    /** Queen SpawnLarva energy cost. */
    private static final double QUEEN_INJECT_ENERGY_COST = 25.0d;

    /** Queen creep tumor energy cost. */
    private static final double QUEEN_CREEP_TUMOR_ENERGY_COST = 25.0d;

    /** Queen transfuse energy cost. */
    private static final double QUEEN_TRANSFUSE_ENERGY_COST = 50.0d;

    /** Queen energy regeneration in visible gameplay seconds. */
    private static final double QUEEN_ENERGY_REGEN_PER_SECOND = 0.5625d;

    /** Conservative SC2 queen max energy cap used for estimates. */
    private static final double QUEEN_MAX_ENERGY = 200.0d;

    /** Conservative Story 11.03 public replay-surface conclusion. */
    private static final String IDLE_INJECT_SIGNAL_CONCLUSION = "Idle-inject windows are derived conservatively from singleton-selected queen command attribution plus a dedicated queen radius. Known queen energy spends such as SpawnLarva, creep tumor, and transfuse delay readiness, and periods without trustworthy queen command or unique nearby-queen proof are suppressed instead of guessed.";

    /** Replay loops per second in SC2 timelines. */
    private static final int REPLAY_LOOPS_PER_SECOND = 16;

    /** Visible inject-active duration in gameplay seconds. */
    private static final int INJECT_ACTIVE_DURATION_SECONDS = 29;

    /** Scelight normal-speed relative value. */
    private static final double NORMAL_GAME_SPEED_RELATIVE = 36.0d;

    /** Scelight Faster-speed relative value used as the fallback default. */
    private static final long DEFAULT_GAME_SPEED_RELATIVE = 26L;

    /** Public replay-surface conclusion for green inject reconstruction. */
    private static final String INJECT_SIGNAL_CONCLUSION = "No direct injected-building state is exposed by the public external-module replay model, so green inject-active windows are inferred from replay-derived hatchery bursts where 3 larva appear within 8 replay loops, then projected backward as 29-second gameplay-time windows converted to replay loops by game speed.";

    /** Evidence label for inferred inject completions. */
    private static final String INFERRED_INJECT_EVIDENCE_KIND = "3 larva within 8 loops";

    /** Field name available on newer UnitBorn tracker events. */
    private static final String F_CREATOR_UNIT_TAG_INDEX = "creatorUnitTagIndex";

    /** Field name available on newer UnitBorn tracker events. */
    private static final String F_CREATOR_UNIT_TAG_RECYCLE = "creatorUnitTagRecycle";

    /** Field name available on newer UnitBorn tracker events. */
    private static final String F_CREATOR_ABILITY_NAME = "creatorAbilityName";

    /** Field name available on generic UnitTypeChange tracker events. */
    private static final String F_UNIT_TYPE_NAME = "unitTypeName";

    /** Field name available on generic UnitOwnerChange tracker events. */
    private static final String F_CONTROL_PLAYER_ID = "controlPlayerId";

    /** Field name available on generic UnitOwnerChange tracker events. */
    private static final String F_UPKEEP_PLAYER_ID = "upkeepPlayerId";

    /** First opening loops used for heuristic calibration. */
    private static final int CALIBRATION_LOOP_LIMIT = 16;

    /** Fallback x offset used if replay opening calibration is not available. */
    private static final double DEFAULT_AVERAGE_DX = 0.0;

    /** Fallback y offset used if replay opening calibration is not available. */
    private static final double DEFAULT_AVERAGE_DY = 6.0;

    /** Fallback assignment radius if replay opening calibration is not available. */
    private static final double DEFAULT_ASSIGNMENT_RADIUS = 10.0;

    /** Hatchery-like Zerg main building unit id. */
    private static final String UNIT_LAIR = "Lair";

    /** Hatchery-like Zerg main building unit id. */
    private static final String UNIT_HIVE = "Hive";

    /** Spawn Larva correlation helper. */
    private final SpawnLarvaCorrelator spawnLarvaCorrelator = new SpawnLarvaCorrelator();

    /** Inferred inject detector used for green inject reconstruction. */
    private final InferredInjectDetector inferredInjectDetector = new InferredInjectDetector();

    /** General Scelight factory used to create selection trackers. */
    private final IFactory factory;

    /**
     * Creates a new replay analyzer.
     *
     * @param factory Scelight factory used to create selection trackers
     */
    public LarvaReplayAnalyzer( final IFactory factory ) {
        this.factory = factory;
    }

    /**
     * Immutable public view of a hatchery state for the assignment heuristic.
     */
    public static class HatcherySnapshot {

        /** Hatchery tag. */
        private final int hatcheryTag;

        /** Player id. */
        private final Integer playerId;

        /** Last known x coordinate. */
        private final Integer x;

        /** Last known y coordinate. */
        private final Integer y;

        /** Tells if the hatchery is alive. */
        private final boolean alive;

        /** Tells if the hatchery is completed. */
        private final boolean completed;

        /**
         * Creates a new hatchery snapshot.
         *
         * @param hatcheryTag hatchery tag
         * @param playerId player id
         * @param x x coordinate
         * @param y y coordinate
         * @param alive tells if the hatchery is alive
         * @param completed tells if the hatchery is completed
         */
        public HatcherySnapshot( final int hatcheryTag, final Integer playerId, final Integer x, final Integer y, final boolean alive,
                final boolean completed ) {
            this.hatcheryTag = hatcheryTag;
            this.playerId = playerId;
            this.x = x;
            this.y = y;
            this.alive = alive;
            this.completed = completed;
        }

        public int getHatcheryTag() {
            return hatcheryTag;
        }

        public Integer getPlayerId() {
            return playerId;
        }

        public Integer getX() {
            return x;
        }

        public Integer getY() {
            return y;
        }

        public boolean isAlive() {
            return alive;
        }

        public boolean isCompleted() {
            return completed;
        }

    }

    /**
     * Analyzes a replay and reconstructs per-hatchery larva count timelines.
     *
     * @param repProc replay processor to analyze
     * @return larva analysis report
     */
    public LarvaAnalysisReport analyze( final IRepProcessor repProc, final boolean fullReplayParseUsed ) {
        final IReplay replay = repProc.getReplay();
        final IEvent[] trackerEventArray = replay.getTrackerEvents() == null ? null : replay.getTrackerEvents().getEvents();
        final IEvent[] gameEventArray = replay.getGameEvents() == null ? null : replay.getGameEvents().getEvents();
        final Map< Integer, List< Integer > > injectLoopsByTag = spawnLarvaCorrelator.collectByTargetTag( gameEventArray );
        final LarvaHeuristicCalibration calibration = calibrate( trackerEventArray );
        final LarvaAssignmentHeuristic assignmentHeuristic = new LarvaAssignmentHeuristic( calibration );

        final Map< Integer, HatcheryState > hatcheryByTag = new LinkedHashMap<>();
        final Map< Integer, QueenState > queenByTag = new LinkedHashMap<>();
        final Map< Integer, LarvaState > larvaByTag = new HashMap<>();
        final Map< String, List< LarvaPlayerResourceSnapshot > > resourceSnapshotsByPlayerName = new LinkedHashMap<>();

        int larvaBirthCount = 0;
        int assignedLarvaCount = 0;
        int unassignedLarvaCount = 0;
        int ambiguousLarvaCount = 0;
        int noEligibleHatcheryLarvaCount = 0;
        int directAssignmentCount = 0;
        int injectCorrelatedAssignmentCount = 0;
        int heuristicAssignmentCount = 0;
        int hatcheryMorphCount = 0;

        if ( trackerEventArray != null ) {
            for ( final IEvent event : trackerEventArray ) {
                if ( event == null )
                    continue;

                switch ( event.getId() ) {
                    case ITrackerEvents.ID_PLAYER_STATS : {
                        final IPlayerStatsEvent playerStatsEvent = (IPlayerStatsEvent) event;
                        final Integer playerId = playerStatsEvent.getPlayerId();
                        final String playerName = resolvePlayerName( repProc, playerId );
                        addResourceSnapshot( resourceSnapshotsByPlayerName, new LarvaPlayerResourceSnapshot( playerName, event.getLoop(),
                            repProc.formatLoopTime( event.getLoop() ), playerStatsEvent.getMineralsCurrent(), playerStatsEvent.getGasCurrent(),
                            playerStatsEvent.getFoodUsed(), playerStatsEvent.getFoodMade() ) );
                        break;
                    }
                    case ITrackerEvents.ID_UNIT_INIT :
                    case ITrackerEvents.ID_UNIT_BORN : {
                        final IBaseUnitEvent baseUnitEvent = (IBaseUnitEvent) event;
                        final String unitType = toStringValue( baseUnitEvent.getUnitTypeName() );
                        if ( isHatcheryLike( unitType ) ) {
                            final HatcheryState hatcheryState = getOrCreateHatcheryState( repProc, hatcheryByTag, buildCombinedTag( baseUnitEvent.getUnitTagIndex(),
                                    baseUnitEvent.getUnitTagRecycle() ) );
                            hatcheryState.playerId = firstNonNull( baseUnitEvent.getControlPlayerId(), baseUnitEvent.getUpkeepPlayerId() );
                            hatcheryState.playerName = resolvePlayerName( repProc, hatcheryState.playerId );
                            hatcheryState.hatcheryType = unitType;
                            hatcheryState.x = baseUnitEvent.getXCoord();
                            hatcheryState.y = baseUnitEvent.getYCoord();
                            hatcheryState.alive = true;
                            hatcheryState.completed = event.getId() == ITrackerEvents.ID_UNIT_BORN;
                            if ( hatcheryState.completed )
                                hatcheryState.recordCompletion( event.getLoop(), repProc );
                            hatcheryState.ensurePoint( event.getLoop(), repProc );
                        } else if ( IBdUtil.UNIT_LARVA.equals( unitType ) ) {
                            larvaBirthCount++;
                            final int larvaTag = buildCombinedTag( baseUnitEvent.getUnitTagIndex(), baseUnitEvent.getUnitTagRecycle() );
                            final Integer creatorTag = buildCombinedTagOrNull( event.get( F_CREATOR_UNIT_TAG_INDEX ), event.get( F_CREATOR_UNIT_TAG_RECYCLE ) );
                            final String creatorAbilityName = toStringValue( event.get( F_CREATOR_ABILITY_NAME ) );
                            final LarvaAssignmentHeuristic.AssignmentResult assignment = assignmentHeuristic.assignLarva( event.getLoop(),
                                    firstNonNull( baseUnitEvent.getControlPlayerId(), baseUnitEvent.getUpkeepPlayerId() ), baseUnitEvent.getXCoord(),
                                    baseUnitEvent.getYCoord(), creatorTag, creatorAbilityName, snapshotHatcheries( hatcheryByTag.values() ), injectLoopsByTag );
                            if ( assignment.isAssigned() ) {
                                final LarvaState larvaState = new LarvaState( larvaTag, assignment.getHatcheryTag(), assignment.getConfidence() );
                                larvaByTag.put( Integer.valueOf( larvaTag ), larvaState );
                                final HatcheryState hatcheryState = hatcheryByTag.get( assignment.getHatcheryTag() );
                                if ( hatcheryState != null ) {
                                    hatcheryState.addLarva( event.getLoop(), repProc, assignment.getConfidence() );
                                    assignedLarvaCount++;
                                    switch ( assignment.getConfidence() ) {
                                        case DIRECT :
                                            directAssignmentCount++;
                                            break;
                                        case INJECT_CORRELATED :
                                            injectCorrelatedAssignmentCount++;
                                            break;
                                        case HEURISTIC :
                                            heuristicAssignmentCount++;
                                            break;
                                        case UNASSIGNED :
                                        default :
                                            break;
                                    }
                                } else {
                                    unassignedLarvaCount++;
                                }
                            } else {
                                unassignedLarvaCount++;
                                if ( assignment.isAmbiguous() )
                                    ambiguousLarvaCount++;
                                else if ( assignment.isNoEligibleHatchery() )
                                    noEligibleHatcheryLarvaCount++;
                            }
                        }
                        break;
                    }
                    case ITrackerEvents.ID_UNIT_DONE : {
                        final Integer hatcheryTag = buildCombinedTagOrNull( event.get( IBaseUnitEvent.F_UNIT_TAG_INDEX ), event.get( IBaseUnitEvent.F_UNIT_TAG_RECYCLE ) );
                        if ( hatcheryTag != null ) {
                            final HatcheryState hatcheryState = hatcheryByTag.get( hatcheryTag );
                            if ( hatcheryState != null ) {
                                hatcheryState.completed = true;
                                hatcheryState.recordCompletion( event.getLoop(), repProc );
                                hatcheryState.ensurePoint( event.getLoop(), repProc );
                            }
                        }
                        break;
                    }
                    case ITrackerEvents.ID_UNIT_DIED : {
                        final Integer tag = buildCombinedTagOrNull( event.get( IBaseUnitEvent.F_UNIT_TAG_INDEX ), event.get( IBaseUnitEvent.F_UNIT_TAG_RECYCLE ) );
                        if ( tag == null )
                            break;

                        final LarvaState larvaState = larvaByTag.remove( tag );
                        if ( larvaState != null ) {
                            final HatcheryState hatcheryState = hatcheryByTag.get( larvaState.hatcheryTag );
                            if ( hatcheryState != null )
                                hatcheryState.removeLarva( event.getLoop(), repProc );
                        }

                        final HatcheryState hatcheryState = hatcheryByTag.get( tag );
                        if ( hatcheryState != null ) {
                            hatcheryState.alive = false;
                            hatcheryState.recordDestroyed( event.getLoop(), repProc );
                        }

                        final QueenState queenState = queenByTag.get( tag );
                        if ( queenState != null ) {
                            queenState.alive = false;
                            queenState.recordDestroyed( event.getLoop(), repProc );
                        }
                        break;
                    }
                    case ITrackerEvents.ID_UNIT_TYPE_CHANGE : {
                        final Integer tag = buildCombinedTagOrNull( event.get( IBaseUnitEvent.F_UNIT_TAG_INDEX ), event.get( IBaseUnitEvent.F_UNIT_TAG_RECYCLE ) );
                        final String unitType = toStringValue( event.get( F_UNIT_TYPE_NAME ) );
                        if ( tag == null )
                            break;

                        final HatcheryState hatcheryState = hatcheryByTag.get( tag );
                        if ( hatcheryState != null && isHatcheryLike( unitType ) ) {
                            if ( !unitType.equals( hatcheryState.hatcheryType ) )
                                hatcheryMorphCount++;
                            hatcheryState.hatcheryType = unitType;
                            hatcheryState.alive = true;
                            hatcheryState.completed = true;
                            hatcheryState.recordCompletion( event.getLoop(), repProc );
                            hatcheryState.ensurePoint( event.getLoop(), repProc );
                        }

                        final LarvaState larvaState = larvaByTag.get( tag );
                        if ( larvaState != null && !IBdUtil.UNIT_LARVA.equals( unitType ) ) {
                            larvaByTag.remove( tag );
                            final HatcheryState larvaHatcheryState = hatcheryByTag.get( larvaState.hatcheryTag );
                            if ( larvaHatcheryState != null )
                                larvaHatcheryState.removeLarva( event.getLoop(), repProc );
                        }

                        final QueenState queenState = queenByTag.get( tag );
                        if ( queenState != null ) {
                            if ( UNIT_QUEEN.equals( unitType ) ) {
                                queenState.alive = true;
                                queenState.completed = true;
                                queenState.recordCompletion( event.getLoop(), repProc );
                            } else {
                                queenState.alive = false;
                                queenState.recordDestroyed( event.getLoop(), repProc );
                            }
                        }
                        break;
                    }
                    case ITrackerEvents.ID_UNIT_OWNER_CHANGE : {
                        final Integer tag = buildCombinedTagOrNull( event.get( IBaseUnitEvent.F_UNIT_TAG_INDEX ), event.get( IBaseUnitEvent.F_UNIT_TAG_RECYCLE ) );
                        final HatcheryState hatcheryState = tag == null ? null : hatcheryByTag.get( tag );
                        if ( hatcheryState != null ) {
                            hatcheryState.playerId = firstNonNull( (Integer) event.get( F_CONTROL_PLAYER_ID ), (Integer) event.get( F_UPKEEP_PLAYER_ID ) );
                            hatcheryState.playerName = resolvePlayerName( repProc, hatcheryState.playerId );
                        }

                        final QueenState queenState = tag == null ? null : queenByTag.get( tag );
                        if ( queenState != null ) {
                            queenState.playerId = firstNonNull( (Integer) event.get( F_CONTROL_PLAYER_ID ), (Integer) event.get( F_UPKEEP_PLAYER_ID ) );
                            queenState.playerName = resolvePlayerName( repProc, queenState.playerId );
                        }
                        break;
                    }
                    default :
                        break;
                }

                if ( event instanceof IBaseUnitEvent ) {
                    final IBaseUnitEvent baseUnitEvent = (IBaseUnitEvent) event;
                    final String unitType = toStringValue( baseUnitEvent.getUnitTypeName() );
                    if ( UNIT_QUEEN.equals( unitType ) || queenByTag.containsKey( buildCombinedTagOrNull( baseUnitEvent.getUnitTagIndex(), baseUnitEvent.getUnitTagRecycle() ) ) ) {
                        updateQueenState( repProc, queenByTag, event, baseUnitEvent, unitType );
                    }
                }
            }
        }

        final List< HatcheryLarvaTimeline > timelineList = new ArrayList<>();
        for ( final HatcheryState hatcheryState : hatcheryByTag.values() ) {
            if ( hatcheryState.countPointList.isEmpty() )
                continue;
            timelineList.add( hatcheryState.toTimeline( repProc ) );
        }

        Collections.sort( timelineList, new Comparator< HatcheryLarvaTimeline >() {
            @Override
            public int compare( final HatcheryLarvaTimeline left, final HatcheryLarvaTimeline right ) {
                final int playerCompare = compareIgnoreCaseSafe( left.getPlayerName(), right.getPlayerName() );
                return playerCompare != 0 ? playerCompare : compareIgnoreCaseSafe( left.getHatcheryTagText(), right.getHatcheryTagText() );
            }
        } );

        sortResourceSnapshotsByLoop( resourceSnapshotsByPlayerName );

        final int injectActiveDurationLoops = resolveInjectActiveDurationLoops( repProc );
        final List< HatcheryInjectTimeline > injectTimelineList = buildInjectTimelines( repProc, hatcheryByTag, injectActiveDurationLoops,
                replay.getHeader() == null || replay.getHeader().getElapsedGameLoops() == null ? 0 : replay.getHeader().getElapsedGameLoops().intValue() );
        final Map< Integer, List< QueenCommandEvidence > > queenCommandEvidenceByTag = collectQueenCommandEvidence( repProc, gameEventArray, queenByTag );
        final List< HatcheryIdleInjectTimeline > idleInjectTimelineList = buildIdleInjectTimelines( repProc, hatcheryByTag, queenByTag,
                injectTimelineList, queenCommandEvidenceByTag,
                replay.getHeader() == null || replay.getHeader().getElapsedGameLoops() == null ? 0 : replay.getHeader().getElapsedGameLoops().intValue() );

        return new LarvaAnalysisReport( calibration, timelineList, injectTimelineList, INJECT_SIGNAL_CONCLUSION,
            idleInjectTimelineList, IDLE_INJECT_SIGNAL_CONCLUSION, IDLE_INJECT_RADIUS,
            hatcheryByTag.size(), larvaBirthCount, assignedLarvaCount, unassignedLarvaCount,
            ambiguousLarvaCount, noEligibleHatcheryLarvaCount,
            directAssignmentCount, injectCorrelatedAssignmentCount, heuristicAssignmentCount, hatcheryMorphCount,
            trackerEventArray == null ? 0 : trackerEventArray.length, gameEventArray == null ? 0 : gameEventArray.length, fullReplayParseUsed,
            repProc.isRealTime(), repProc.getConverterGameSpeed() == null ? 36L : repProc.getConverterGameSpeed().getRelativeSpeed(),
            replay.getHeader() == null || replay.getHeader().getElapsedGameLoops() == null ? 0 : replay.getHeader().getElapsedGameLoops().intValue(),
            resourceSnapshotsByPlayerName );
    }

    /**
     * Updates mutable queen lifecycle state from a tracker event.
     */
    private void updateQueenState( final IRepProcessor repProc, final Map< Integer, QueenState > queenByTag, final IEvent event,
            final IBaseUnitEvent baseUnitEvent, final String unitType ) {
        final Integer queenTag = buildCombinedTagOrNull( baseUnitEvent.getUnitTagIndex(), baseUnitEvent.getUnitTagRecycle() );
        if ( queenTag == null )
            return;

        final QueenState queenState = getOrCreateQueenState( repProc, queenByTag, queenTag.intValue() );
        queenState.playerId = firstNonNull( baseUnitEvent.getControlPlayerId(), baseUnitEvent.getUpkeepPlayerId() );
        queenState.playerName = resolvePlayerName( repProc, queenState.playerId );
        queenState.x = baseUnitEvent.getXCoord();
        queenState.y = baseUnitEvent.getYCoord();
        queenState.lastKnownPositionLoop = event.getLoop();
        queenState.unitType = unitType == null || unitType.length() == 0 ? queenState.unitType : unitType;

        switch ( event.getId() ) {
            case ITrackerEvents.ID_UNIT_INIT :
                queenState.alive = true;
                break;
            case ITrackerEvents.ID_UNIT_BORN :
            case ITrackerEvents.ID_UNIT_DONE :
                queenState.alive = true;
                queenState.completed = true;
                queenState.recordCompletion( event.getLoop(), repProc );
                break;
            default :
                break;
        }
    }

    /**
     * Collects conservative singleton-selected queen command evidence.
     */
    private Map< Integer, List< QueenCommandEvidence > > collectQueenCommandEvidence( final IRepProcessor repProc,
            final IEvent[] gameEventArray, final Map< Integer, QueenState > queenByTag ) {
        final Map< Integer, List< QueenCommandEvidence > > evidenceByQueenTag = new LinkedHashMap<>();
        if ( gameEventArray == null || factory == null )
            return evidenceByQueenTag;

        final Map< Integer, ISelectionTracker > selectionTrackerByUserId = new HashMap<>();
        for ( final IEvent event : gameEventArray ) {
            if ( event == null )
                continue;

            final ISelectionTracker selectionTracker = getOrCreateSelectionTracker( selectionTrackerByUserId, event.getUserId() );
            switch ( event.getId() ) {
                case IGameEvents.ID_SELECTION_DELTA :
                    if ( selectionTracker != null && event instanceof ISelectionDeltaEvent )
                        selectionTracker.processSelectionDelta( (ISelectionDeltaEvent) event );
                    break;
                case IGameEvents.ID_CONTROL_GROUP_UPDATE :
                    if ( selectionTracker != null && event instanceof IControlGroupUpdateEvent )
                        selectionTracker.processControlGroupUpdate( (IControlGroupUpdateEvent) event );
                    break;
                case IGameEvents.ID_CMD :
                    if ( selectionTracker != null && event instanceof ICmdEvent )
                        recordQueenCommandEvidence( repProc, evidenceByQueenTag, queenByTag, selectionTracker, (ICmdEvent) event );
                    break;
                default :
                    break;
            }
        }

        return evidenceByQueenTag;
    }

    /**
     * Creates one command-evidence record if the active selection proves a singleton queen caster.
     */
    private void recordQueenCommandEvidence( final IRepProcessor repProc, final Map< Integer, List< QueenCommandEvidence > > evidenceByQueenTag,
            final Map< Integer, QueenState > queenByTag, final ISelectionTracker selectionTracker, final ICmdEvent cmdEvent ) {
        final Integer queenTag = resolveSingletonSelectedQueenTag( repProc, selectionTracker );
        if ( queenTag == null )
            return;

        final QueenState queenState = queenByTag.get( queenTag );
        if ( queenState == null || !queenState.alive || !queenState.completed )
            return;

        final ICommand command = cmdEvent.getCommand();
        final String abilityId = command == null ? null : command.getAbilId();
        final Integer targetHatcheryTag = cmdEvent.getTargetUnit() == null ? null : cmdEvent.getTargetUnit().getTag();
        final boolean inject = IAbility.ID_SPAWN_LARVA.equals( abilityId ) && targetHatcheryTag != null;
        final int energyCost = resolveQueenEnergyCost( command );

        List< QueenCommandEvidence > evidenceList = evidenceByQueenTag.get( queenTag );
        if ( evidenceList == null ) {
            evidenceList = new ArrayList<>();
            evidenceByQueenTag.put( queenTag, evidenceList );
        }

        evidenceList.add( new QueenCommandEvidence( queenTag.intValue(), cmdEvent.getLoop(), repProc.formatLoopTime( cmdEvent.getLoop() ),
        inject ? targetHatcheryTag.intValue() : -1, abilityId, inject, energyCost ) );
    }

    /**
     * Resolves a singleton queen from the active player selection.
     */
    private Integer resolveSingletonSelectedQueenTag( final IRepProcessor repProc, final ISelectionTracker selectionTracker ) {
        if ( selectionTracker == null || selectionTracker.getActiveSelection() == null || selectionTracker.getActiveSelection().size() != 1 )
            return null;

        final Integer[] selectedUnit = selectionTracker.getActiveSelection().get( 0 );
        if ( selectedUnit == null || selectedUnit.length < 2 || selectedUnit[ 0 ] == null || selectedUnit[ 1 ] == null )
            return null;

        return isQueenLink( repProc, selectedUnit[ 0 ] ) ? selectedUnit[ 1 ] : null;
    }

    /**
     * Tells if a unit link resolves to a queen.
     */
    private boolean isQueenLink( final IRepProcessor repProc, final Integer unitLink ) {
        if ( repProc == null || repProc.getReplay() == null || repProc.getReplay().getBalanceData() == null || unitLink == null )
            return false;

        final IUnit unit = repProc.getReplay().getBalanceData().getUnit( unitLink );
        return unit != null && UNIT_QUEEN.equals( unit.getId() );
    }

    /**
     * Returns an existing selection tracker or creates a new one for a user id.
     */
    private ISelectionTracker getOrCreateSelectionTracker( final Map< Integer, ISelectionTracker > selectionTrackerByUserId, final int userId ) {
        if ( selectionTrackerByUserId == null || factory == null || userId < 0 )
            return null;

        final Integer key = Integer.valueOf( userId );
        ISelectionTracker selectionTracker = selectionTrackerByUserId.get( key );
        if ( selectionTracker == null ) {
            selectionTracker = factory.newSelectionTracker();
            selectionTrackerByUserId.put( key, selectionTracker );
        }
        return selectionTracker;
    }

    /**
     * Builds per-hatchery idle-inject timelines from conservative queen command attribution.
     */
    private List< HatcheryIdleInjectTimeline > buildIdleInjectTimelines( final IRepProcessor repProc, final Map< Integer, HatcheryState > hatcheryByTag,
            final Map< Integer, QueenState > queenByTag, final List< HatcheryInjectTimeline > injectTimelineList,
            final Map< Integer, List< QueenCommandEvidence > > queenCommandEvidenceByTag, final int replayEndLoop ) {
        if ( hatcheryByTag == null || hatcheryByTag.isEmpty() )
            return Collections.emptyList();

        final Map< Integer, HatcheryInjectTimeline > injectTimelineByHatchTag = new HashMap<>();
        if ( injectTimelineList != null )
            for ( final HatcheryInjectTimeline injectTimeline : injectTimelineList )
                if ( injectTimeline != null )
                    injectTimelineByHatchTag.put( Integer.valueOf( injectTimeline.getHatcheryTag() ), injectTimeline );

        final Map< Integer, List< CandidateIdleWindow > > candidateWindowsByHatchTag = new HashMap<>();
        final Map< Integer, List< String > > diagnosticsByHatchTag = new HashMap<>();
        final Map< Integer, Integer > attributedQueenCommandCountByHatchTag = new HashMap<>();
        final Map< Integer, Integer > attributedInjectCommandCountByHatchTag = new HashMap<>();
        final Map< Integer, Integer > uncertaintyDiscardCountByHatchTag = new HashMap<>();

        for ( final QueenState queenState : queenByTag.values() ) {
            if ( queenState == null || !queenState.completed || queenState.playerId == null )
                continue;

            final List< QueenCommandEvidence > evidenceList = queenCommandEvidenceByTag.get( Integer.valueOf( queenState.queenTag ) );
            if ( evidenceList != null && !evidenceList.isEmpty() ) {
                Collections.sort( evidenceList, new Comparator< QueenCommandEvidence >() {
                    @Override
                    public int compare( final QueenCommandEvidence left, final QueenCommandEvidence right ) {
                        return left.loop < right.loop ? -1 : left.loop == right.loop ? 0 : 1;
                    }
                } );
                buildQueenIdleCandidatesFromCommands( repProc, queenState, evidenceList, hatcheryByTag, injectTimelineByHatchTag, replayEndLoop,
                        candidateWindowsByHatchTag, diagnosticsByHatchTag, attributedQueenCommandCountByHatchTag,
                        attributedInjectCommandCountByHatchTag, uncertaintyDiscardCountByHatchTag );
            } else {
                trySeedInitialIdleCandidateFromCompletion( repProc, queenState, hatcheryByTag, injectTimelineByHatchTag, replayEndLoop,
                        candidateWindowsByHatchTag, diagnosticsByHatchTag, uncertaintyDiscardCountByHatchTag );
            }
        }

        final List< HatcheryIdleInjectTimeline > idleTimelineList = new ArrayList<>();
        for ( final HatcheryState hatcheryState : hatcheryByTag.values() ) {
            if ( hatcheryState == null || !hatcheryState.completed || hatcheryState.completionLoop < 0 )
                continue;

            final List< CandidateIdleWindow > candidateWindowList = candidateWindowsByHatchTag.get( Integer.valueOf( hatcheryState.hatcheryTag ) );
            final List< HatcheryIdleInjectWindow > idleWindowList = mergeIdleCandidates( repProc, candidateWindowList );
            final List< String > diagnosticLineList = diagnosticsByHatchTag.get( Integer.valueOf( hatcheryState.hatcheryTag ) );
            idleTimelineList.add( new HatcheryIdleInjectTimeline( hatcheryState.hatcheryTag, hatcheryState.hatcheryTagText,
                    hatcheryState.playerName, hatcheryState.hatcheryType, IDLE_INJECT_RADIUS,
                    intValue( attributedQueenCommandCountByHatchTag.get( Integer.valueOf( hatcheryState.hatcheryTag ) ) ),
                    intValue( attributedInjectCommandCountByHatchTag.get( Integer.valueOf( hatcheryState.hatcheryTag ) ) ),
                    intValue( uncertaintyDiscardCountByHatchTag.get( Integer.valueOf( hatcheryState.hatcheryTag ) ) ),
                    idleWindowList, diagnosticLineList == null ? Collections.< String >emptyList() : diagnosticLineList ) );
        }

        Collections.sort( idleTimelineList, new Comparator< HatcheryIdleInjectTimeline >() {
            @Override
            public int compare( final HatcheryIdleInjectTimeline left, final HatcheryIdleInjectTimeline right ) {
                final int playerCompare = compareIgnoreCaseSafe( left.getPlayerName(), right.getPlayerName() );
                return playerCompare != 0 ? playerCompare : compareIgnoreCaseSafe( left.getHatcheryTagText(), right.getHatcheryTagText() );
            }
        } );
        return idleTimelineList;
    }

    /**
     * Builds conservative idle-window candidates from confidently attributed queen commands.
     */
    private void buildQueenIdleCandidatesFromCommands( final IRepProcessor repProc, final QueenState queenState,
            final List< QueenCommandEvidence > evidenceList, final Map< Integer, HatcheryState > hatcheryByTag,
            final Map< Integer, HatcheryInjectTimeline > injectTimelineByHatchTag, final int replayEndLoop,
            final Map< Integer, List< CandidateIdleWindow > > candidateWindowsByHatchTag, final Map< Integer, List< String > > diagnosticsByHatchTag,
            final Map< Integer, Integer > attributedQueenCommandCountByHatchTag, final Map< Integer, Integer > attributedInjectCommandCountByHatchTag,
            final Map< Integer, Integer > uncertaintyDiscardCountByHatchTag ) {
        int boundHatchTag = -1;
        int boundStartLoop = -1;
        int nextReadyLoop = Integer.MAX_VALUE;

        for ( final QueenCommandEvidence evidence : evidenceList ) {
            if ( boundHatchTag >= 0 && boundStartLoop >= 0 ) {
                addQueenIdleCandidatesForBoundInterval( repProc, queenState, boundHatchTag, boundStartLoop, evidence.loop, nextReadyLoop,
                        hatcheryByTag, injectTimelineByHatchTag, candidateWindowsByHatchTag, diagnosticsByHatchTag );
                addCount( attributedQueenCommandCountByHatchTag, boundHatchTag, 1 );
            }

            if ( evidence.inject && evidence.targetHatcheryTag >= 0 && hatcheryByTag.containsKey( Integer.valueOf( evidence.targetHatcheryTag ) ) ) {
                boundHatchTag = evidence.targetHatcheryTag;
                boundStartLoop = evidence.loop;
                nextReadyLoop = evidence.loop + resolveQueenReadyDelayLoops( repProc, evidence.energyCost );
                addCount( attributedInjectCommandCountByHatchTag, boundHatchTag, 1 );
                addDiagnostic( diagnosticsByHatchTag, boundHatchTag, "Queen " + queenState.queenTagText + " bound by singleton SpawnLarva at "
                        + evidence.timeLabel + "; next 25 energy reaches at " + repProc.formatLoopTime( nextReadyLoop ) + '.' );
            } else if ( evidence.energyCost > 0 && boundHatchTag >= 0 ) {
                final int spentReadyLoop = evidence.loop + resolveQueenReadyDelayLoops( repProc, evidence.energyCost );
                addDiagnostic( diagnosticsByHatchTag, boundHatchTag, "Queen " + queenState.queenTagText + " spent " + evidence.energyCost
                        + " energy on " + safeAbilityId( evidence.abilityId ) + " at " + evidence.timeLabel
                        + "; inject proof reset conservatively to avoid false missed-inject windows. Earliest re-ready time would be "
                        + repProc.formatLoopTime( spentReadyLoop ) + '.' );
                boundHatchTag = -1;
                boundStartLoop = -1;
                nextReadyLoop = Integer.MAX_VALUE;
            } else if ( boundHatchTag >= 0 ) {
                addDiagnostic( diagnosticsByHatchTag, boundHatchTag, "Queen " + queenState.queenTagText + " proof stopped at " + evidence.timeLabel
                        + " due to singleton-selected non-inject command " + safeAbilityId( evidence.abilityId ) + '.' );
                addCount( uncertaintyDiscardCountByHatchTag, boundHatchTag, 1 );
                boundHatchTag = -1;
                boundStartLoop = -1;
                nextReadyLoop = Integer.MAX_VALUE;
            }
        }

        if ( boundHatchTag >= 0 && boundStartLoop >= 0 ) {
            final int intervalEndLoop = queenState.destroyedLoop >= 0 ? queenState.destroyedLoop : replayEndLoop;
            addQueenIdleCandidatesForBoundInterval( repProc, queenState, boundHatchTag, boundStartLoop, intervalEndLoop, nextReadyLoop,
                    hatcheryByTag, injectTimelineByHatchTag, candidateWindowsByHatchTag, diagnosticsByHatchTag );
            addCount( attributedQueenCommandCountByHatchTag, boundHatchTag, 1 );
            if ( queenState.destroyedLoop >= 0 )
                addDiagnostic( diagnosticsByHatchTag, boundHatchTag, "Queen " + queenState.queenTagText + " proof ended at "
                        + queenState.destroyedTimeLabel + " because the queen died." );
        }
    }

    /**
     * Tries to seed an initial conservative idle candidate from a queen completion moment.
     */
    private void trySeedInitialIdleCandidateFromCompletion( final IRepProcessor repProc, final QueenState queenState,
            final Map< Integer, HatcheryState > hatcheryByTag, final Map< Integer, HatcheryInjectTimeline > injectTimelineByHatchTag,
            final int replayEndLoop, final Map< Integer, List< CandidateIdleWindow > > candidateWindowsByHatchTag,
            final Map< Integer, List< String > > diagnosticsByHatchTag,
            final Map< Integer, Integer > uncertaintyDiscardCountByHatchTag ) {
        if ( queenState.completionLoop < 0 || queenState.x == null || queenState.y == null )
            return;

        final List< HatcheryState > nearbyHatcheryList = new ArrayList<>();
        for ( final HatcheryState hatcheryState : hatcheryByTag.values() ) {
            if ( hatcheryState == null || !hatcheryState.completed || !hatcheryState.alive || hatcheryState.playerId == null
                    || !hatcheryState.playerId.equals( queenState.playerId ) || hatcheryState.completionLoop < 0
                    || hatcheryState.completionLoop > queenState.completionLoop || hatcheryState.x == null || hatcheryState.y == null )
                continue;

            final double distance = distance( queenState.x.intValue(), queenState.y.intValue(), hatcheryState.x.intValue(), hatcheryState.y.intValue() );
            if ( distance <= IDLE_INJECT_RADIUS )
                nearbyHatcheryList.add( hatcheryState );
        }

        if ( nearbyHatcheryList.isEmpty() )
            return;

        if ( nearbyHatcheryList.size() > 1 ) {
            for ( final HatcheryState hatcheryState : nearbyHatcheryList ) {
                addDiagnostic( diagnosticsByHatchTag, hatcheryState.hatcheryTag, "Queen " + queenState.queenTagText + " completed at "
                        + queenState.completionTimeLabel + " within the dedicated radius of multiple hatcheries; initial idle proof suppressed." );
                addCount( uncertaintyDiscardCountByHatchTag, hatcheryState.hatcheryTag, 1 );
            }
            return;
        }

        final HatcheryState hatcheryState = nearbyHatcheryList.get( 0 );
        final int intervalEndLoop = queenState.destroyedLoop >= 0 ? queenState.destroyedLoop : replayEndLoop;
        addDiagnostic( diagnosticsByHatchTag, hatcheryState.hatcheryTag, "Queen " + queenState.queenTagText + " completed at "
                + queenState.completionTimeLabel + " within radius " + formatRadius() + " of this hatchery; conservative initial eligibility seeded." );
        addQueenIdleCandidatesForBoundInterval( repProc, queenState, hatcheryState.hatcheryTag, queenState.completionLoop, intervalEndLoop,
                queenState.completionLoop, hatcheryByTag, injectTimelineByHatchTag, candidateWindowsByHatchTag, diagnosticsByHatchTag );
    }

    /**
     * Adds non-injected red-lane candidates for one queen-bound interval.
     */
    private void addQueenIdleCandidatesForBoundInterval( final IRepProcessor repProc, final QueenState queenState, final int hatcheryTag,
            final int intervalStartLoop, final int intervalEndLoop, final int nextReadyLoop, final Map< Integer, HatcheryState > hatcheryByTag,
            final Map< Integer, HatcheryInjectTimeline > injectTimelineByHatchTag,
            final Map< Integer, List< CandidateIdleWindow > > candidateWindowsByHatchTag,
            final Map< Integer, List< String > > diagnosticsByHatchTag ) {
        final HatcheryState hatcheryState = hatcheryByTag.get( Integer.valueOf( hatcheryTag ) );
        if ( hatcheryState == null || !hatcheryState.completed || hatcheryState.completionLoop < 0 )
            return;

        final int effectiveStartLoop = Math.max( Math.max( intervalStartLoop, nextReadyLoop ), hatcheryState.completionLoop );
        final int effectiveEndLoop = hatcheryState.destroyedLoop >= 0 ? Math.min( intervalEndLoop, hatcheryState.destroyedLoop ) : intervalEndLoop;
        if ( effectiveEndLoop <= effectiveStartLoop ) {
            addDiagnostic( diagnosticsByHatchTag, hatcheryTag, "Queen " + queenState.queenTagText + " never reached a qualifying non-injected 25-energy interval." );
            return;
        }

        final HatcheryInjectTimeline injectTimeline = injectTimelineByHatchTag.get( Integer.valueOf( hatcheryTag ) );
        int cursor = effectiveStartLoop;
        if ( injectTimeline != null ) {
            for ( final HatcheryInjectWindow injectWindow : injectTimeline.getInjectWindowList() ) {
                if ( injectWindow == null || injectWindow.getEndLoop() <= cursor )
                    continue;
                if ( injectWindow.getStartLoop() >= effectiveEndLoop )
                    break;

                if ( injectWindow.getStartLoop() > cursor )
                        addCandidateIdleWindow( repProc, hatcheryTag, cursor, Math.min( effectiveEndLoop, injectWindow.getStartLoop() ), queenState, nextReadyLoop,
                            candidateWindowsByHatchTag, diagnosticsByHatchTag );
                cursor = Math.max( cursor, injectWindow.getEndLoop() );
            }
        }

        if ( cursor < effectiveEndLoop )
                    addCandidateIdleWindow( repProc, hatcheryTag, cursor, effectiveEndLoop, queenState, nextReadyLoop, candidateWindowsByHatchTag, diagnosticsByHatchTag );
    }

    /**
     * Adds one candidate idle window if it has positive width.
     */
    private void addCandidateIdleWindow( final IRepProcessor repProc, final int hatcheryTag, final int startLoop, final int endLoop,
            final QueenState queenState, final int nextReadyLoop, final Map< Integer, List< CandidateIdleWindow > > candidateWindowsByHatchTag,
            final Map< Integer, List< String > > diagnosticsByHatchTag ) {
        if ( endLoop <= startLoop )
            return;

        List< CandidateIdleWindow > candidateWindowList = candidateWindowsByHatchTag.get( Integer.valueOf( hatcheryTag ) );
        if ( candidateWindowList == null ) {
            candidateWindowList = new ArrayList<>();
            candidateWindowsByHatchTag.put( Integer.valueOf( hatcheryTag ), candidateWindowList );
        }

        final String queenSummary = queenState.queenTagText;
        final String diagnosticNote = "Qualified by queen " + queenState.queenTagText + " with conservative singleton-command attribution.";
    candidateWindowList.add( new CandidateIdleWindow( startLoop, endLoop,
        estimateQueenEnergyAtLoop( repProc, nextReadyLoop, startLoop ), estimateQueenEnergyAtLoop( repProc, nextReadyLoop, endLoop ),
        queenSummary, diagnosticNote ) );
        addDiagnostic( diagnosticsByHatchTag, hatcheryTag, "Kept idle-inject candidate " + repProc.formatLoopTime( startLoop ) + '-'
                + repProc.formatLoopTime( endLoop ) + " from queen " + queenState.queenTagText + '.' );
    }

    /**
     * Merges overlapping candidate idle windows into normalized hatchery windows.
     */
    private List< HatcheryIdleInjectWindow > mergeIdleCandidates( final IRepProcessor repProc, final List< CandidateIdleWindow > candidateWindowList ) {
        if ( candidateWindowList == null || candidateWindowList.isEmpty() )
            return Collections.emptyList();

        final List< CandidateIdleWindow > sortedCandidateList = new ArrayList<>( candidateWindowList );
        Collections.sort( sortedCandidateList, new Comparator< CandidateIdleWindow >() {
            @Override
            public int compare( final CandidateIdleWindow left, final CandidateIdleWindow right ) {
                if ( left.startLoop != right.startLoop )
                    return left.startLoop < right.startLoop ? -1 : 1;
                return left.endLoop < right.endLoop ? -1 : left.endLoop == right.endLoop ? 0 : 1;
            }
        } );

        final List< HatcheryIdleInjectWindow > mergedWindowList = new ArrayList<>();
        int mergedStartLoop = -1;
        int mergedEndLoop = -1;
        double mergedMinStartEnergy = 0.0d;
        double mergedMaxStartEnergy = 0.0d;
        double mergedMinEndEnergy = 0.0d;
        double mergedMaxEndEnergy = 0.0d;
        final Set< String > mergedQueenSummarySet = new LinkedHashSet<>();
        final List< String > mergedDiagnosticList = new ArrayList<>();

        for ( final CandidateIdleWindow candidateWindow : sortedCandidateList ) {
            if ( mergedStartLoop < 0 ) {
                mergedStartLoop = candidateWindow.startLoop;
                mergedEndLoop = candidateWindow.endLoop;
                mergedMinStartEnergy = candidateWindow.estimatedStartEnergy;
                mergedMaxStartEnergy = candidateWindow.estimatedStartEnergy;
                mergedMinEndEnergy = candidateWindow.estimatedEndEnergy;
                mergedMaxEndEnergy = candidateWindow.estimatedEndEnergy;
                mergedQueenSummarySet.add( candidateWindow.queenSummary );
                mergedDiagnosticList.add( candidateWindow.diagnosticNote );
                continue;
            }

            if ( candidateWindow.startLoop <= mergedEndLoop ) {
                if ( candidateWindow.endLoop > mergedEndLoop )
                    mergedEndLoop = candidateWindow.endLoop;
                mergedMinStartEnergy = Math.min( mergedMinStartEnergy, candidateWindow.estimatedStartEnergy );
                mergedMaxStartEnergy = Math.max( mergedMaxStartEnergy, candidateWindow.estimatedStartEnergy );
                mergedMinEndEnergy = Math.min( mergedMinEndEnergy, candidateWindow.estimatedEndEnergy );
                mergedMaxEndEnergy = Math.max( mergedMaxEndEnergy, candidateWindow.estimatedEndEnergy );
                mergedQueenSummarySet.add( candidateWindow.queenSummary );
                mergedDiagnosticList.add( candidateWindow.diagnosticNote );
                continue;
            }

            mergedWindowList.add( buildMergedIdleWindow( repProc, mergedStartLoop, mergedEndLoop, mergedMinStartEnergy, mergedMaxStartEnergy,
                    mergedMinEndEnergy, mergedMaxEndEnergy, mergedQueenSummarySet, mergedDiagnosticList ) );
            mergedStartLoop = candidateWindow.startLoop;
            mergedEndLoop = candidateWindow.endLoop;
            mergedMinStartEnergy = candidateWindow.estimatedStartEnergy;
            mergedMaxStartEnergy = candidateWindow.estimatedStartEnergy;
            mergedMinEndEnergy = candidateWindow.estimatedEndEnergy;
            mergedMaxEndEnergy = candidateWindow.estimatedEndEnergy;
            mergedQueenSummarySet.clear();
            mergedDiagnosticList.clear();
            mergedQueenSummarySet.add( candidateWindow.queenSummary );
            mergedDiagnosticList.add( candidateWindow.diagnosticNote );
        }

        if ( mergedStartLoop >= 0 )
            mergedWindowList.add( buildMergedIdleWindow( repProc, mergedStartLoop, mergedEndLoop, mergedMinStartEnergy, mergedMaxStartEnergy,
                    mergedMinEndEnergy, mergedMaxEndEnergy, mergedQueenSummarySet, mergedDiagnosticList ) );

        return mergedWindowList;
    }

    /**
     * Creates one immutable merged idle window.
     */
        private HatcheryIdleInjectWindow buildMergedIdleWindow( final IRepProcessor repProc, final int startLoop, final int endLoop,
            final double minEstimatedStartEnergy, final double maxEstimatedStartEnergy, final double minEstimatedEndEnergy,
            final double maxEstimatedEndEnergy, final Set< String > queenSummarySet, final List< String > diagnosticList ) {
        final String queenSummary = joinStrings( queenSummarySet );
        final long startMs = toTimelineMs( repProc, startLoop );
        final long endMs = toTimelineMs( repProc, endLoop );
        final String startTimeLabel = repProc == null ? null : repProc.formatLoopTime( startLoop );
        final String endTimeLabel = repProc == null ? null : repProc.formatLoopTime( endLoop );
        return new HatcheryIdleInjectWindow( startLoop, endLoop, startMs, endMs, startTimeLabel, endTimeLabel,
            queenSummarySet.size(), minEstimatedStartEnergy, maxEstimatedStartEnergy, minEstimatedEndEnergy, maxEstimatedEndEnergy,
            queenSummary, joinStrings( diagnosticList ) );
    }

    /**
     * Estimates queen energy at the specified loop using the conservative ready-loop baseline.
     */
    private double estimateQueenEnergyAtLoop( final IRepProcessor repProc, final int nextReadyLoop, final int targetLoop ) {
        if ( targetLoop <= nextReadyLoop )
            return QUEEN_INJECT_ENERGY_COST;

        final long gameSpeedRelative = repProc == null || repProc.getConverterGameSpeed() == null ? DEFAULT_GAME_SPEED_RELATIVE
                : repProc.getConverterGameSpeed().getRelativeSpeed();
        final double gameplaySeconds = ( targetLoop - nextReadyLoop ) / ( REPLAY_LOOPS_PER_SECOND * ( NORMAL_GAME_SPEED_RELATIVE / gameSpeedRelative ) );
        return Math.min( QUEEN_MAX_ENERGY, QUEEN_INJECT_ENERGY_COST + gameplaySeconds * QUEEN_ENERGY_REGEN_PER_SECOND );
    }

    /**
     * Converts raw loops to game-time milliseconds.
     */
    private long toTimelineMs( final IRepProcessor repProc, final int loop ) {
        if ( loop <= 0 )
            return 0L;

        long gameMs = loop * 1000L / REPLAY_LOOPS_PER_SECOND;
        if ( repProc != null && repProc.isRealTime() )
            gameMs = repProc.convertToRealTime( gameMs );
        return gameMs;
    }

    /**
     * Resolves the replay-loop delay for a queen to regenerate 25 energy.
     */
    private int resolveQueenReadyDelayLoops( final IRepProcessor repProc, final double energyCost ) {
        final long gameSpeedRelative = repProc == null || repProc.getConverterGameSpeed() == null ? DEFAULT_GAME_SPEED_RELATIVE
                : repProc.getConverterGameSpeed().getRelativeSpeed();
        final double seconds = energyCost / QUEEN_ENERGY_REGEN_PER_SECOND;
        return (int) Math.ceil( seconds * REPLAY_LOOPS_PER_SECOND * ( NORMAL_GAME_SPEED_RELATIVE / gameSpeedRelative ) );
    }

    /**
     * Resolves the energy cost of a confidently singleton-attributed queen command.
     */
    private int resolveQueenEnergyCost( final ICommand command ) {
        if ( command == null )
            return 0;

        final String abilityId = normalizeCommandText( command.getAbilId() );
        final String commandId = normalizeCommandText( command.getId() );
        final String commandText = normalizeCommandText( command.getText() );

        if ( containsAny( abilityId, commandId, commandText, "spawnlarva", "injectlarva" ) )
            return (int) QUEEN_INJECT_ENERGY_COST;

        if ( containsAny( abilityId, commandId, commandText, "transfusion", "transfuse" ) )
            return (int) QUEEN_TRANSFUSE_ENERGY_COST;

        if ( containsAny( abilityId, commandId, commandText, "creeptumor", "buildcreeptumor", "tumor" ) )
            return (int) QUEEN_CREEP_TUMOR_ENERGY_COST;

        return 0;
    }

    /**
     * Lower-cases command text for ability matching.
     */
    private String normalizeCommandText( final String value ) {
        return value == null ? "" : value.toLowerCase();
    }

    /**
     * Tells if any haystack contains any of the needles.
     */
    private boolean containsAny( final String haystack1, final String haystack2, final String haystack3, final String needle1, final String needle2 ) {
        return containsAny( haystack1, needle1, needle2 ) || containsAny( haystack2, needle1, needle2 ) || containsAny( haystack3, needle1, needle2 );
    }

    /**
     * Tells if any haystack contains any of the three needles.
     */
    private boolean containsAny( final String haystack1, final String haystack2, final String haystack3, final String needle1, final String needle2,
            final String needle3 ) {
        return containsAny( haystack1, needle1, needle2, needle3 ) || containsAny( haystack2, needle1, needle2, needle3 )
                || containsAny( haystack3, needle1, needle2, needle3 );
    }

    /**
     * Tells if the haystack contains any of the needles.
     */
    private boolean containsAny( final String haystack, final String needle1, final String needle2 ) {
        if ( haystack == null || haystack.length() == 0 )
            return false;
        return haystack.indexOf( needle1 ) >= 0 || haystack.indexOf( needle2 ) >= 0;
    }

    /**
     * Tells if the haystack contains any of the three needles.
     */
    private boolean containsAny( final String haystack, final String needle1, final String needle2, final String needle3 ) {
        if ( haystack == null || haystack.length() == 0 )
            return false;
        return haystack.indexOf( needle1 ) >= 0 || haystack.indexOf( needle2 ) >= 0 || haystack.indexOf( needle3 ) >= 0;
    }

    /**
     * Returns the distance between two tracker points.
     */
    private double distance( final int x1, final int y1, final int x2, final int y2 ) {
        final double dx = x1 - x2;
        final double dy = y1 - y2;
        return Math.sqrt( dx * dx + dy * dy );
    }

    /**
     * Adds one deterministic diagnostic line per hatchery.
     */
    private void addDiagnostic( final Map< Integer, List< String > > diagnosticsByHatchTag, final int hatcheryTag, final String line ) {
        if ( diagnosticsByHatchTag == null || line == null )
            return;

        List< String > diagnosticLineList = diagnosticsByHatchTag.get( Integer.valueOf( hatcheryTag ) );
        if ( diagnosticLineList == null ) {
            diagnosticLineList = new ArrayList<>();
            diagnosticsByHatchTag.put( Integer.valueOf( hatcheryTag ), diagnosticLineList );
        }
        diagnosticLineList.add( line );
    }

    /**
     * Adds a small integer count in a map.
     */
    private void addCount( final Map< Integer, Integer > countMap, final int key, final int delta ) {
        if ( countMap == null )
            return;
        final Integer previous = countMap.get( Integer.valueOf( key ) );
        countMap.put( Integer.valueOf( key ), Integer.valueOf( ( previous == null ? 0 : previous.intValue() ) + delta ) );
    }

    /**
     * Null-safe integer value.
     */
    private int intValue( final Integer value ) {
        return value == null ? 0 : value.intValue();
    }

    /**
     * Returns safe command text for diagnostics.
     */
    private String safeAbilityId( final String abilityId ) {
        return abilityId == null || abilityId.length() == 0 ? "unknown" : abilityId;
    }

    /**
     * Joins deterministic string collections.
     */
    private String joinStrings( final Iterable< String > values ) {
        if ( values == null )
            return "";

        final StringBuilder builder = new StringBuilder();
        for ( final String value : values ) {
            if ( value == null || value.length() == 0 )
                continue;
            if ( builder.length() > 0 )
                builder.append( ", " );
            builder.append( value );
        }
        return builder.toString();
    }

    /**
     * Formats the dedicated idle radius.
     */
    private String formatRadius() {
        return String.valueOf( (long) IDLE_INJECT_RADIUS );
    }

    /**
     * Builds per-hatchery inject timelines from SpawnLarva command targets.
     *
     * <p>The public external-module replay model exposes command targets and unit lifecycle events,
    * but it does not expose a direct building-side injected-state flag or tracker buff surface.
    * The supported workaround is to infer inject completion from replay-derived larva bursts.</p>
     *
     * @param repProc replay processor
     * @param hatcheryByTag tracked hatcheries by tag
     * @param injectActiveDurationLoops normalized inject-active duration in replay loops
     * @param replayEndLoop replay end loop from the replay header
     * @return sorted immutable inject timelines
     */
    private List< HatcheryInjectTimeline > buildInjectTimelines( final IRepProcessor repProc, final Map< Integer, HatcheryState > hatcheryByTag,
             final int injectActiveDurationLoops, final int replayEndLoop ) {
        if ( hatcheryByTag == null || hatcheryByTag.isEmpty() )
            return Collections.emptyList();

        final List< HatcheryState > hatcheryStateList = new ArrayList<>( hatcheryByTag.values() );
        Collections.sort( hatcheryStateList, new Comparator< HatcheryState >() {
            @Override
            public int compare( final HatcheryState left, final HatcheryState right ) {
                final int playerCompare = compareIgnoreCaseSafe( left.playerName, right.playerName );
                return playerCompare != 0 ? playerCompare : compareIgnoreCaseSafe( left.hatcheryTagText, right.hatcheryTagText );
            }
        } );

        final List< HatcheryInjectTimeline > injectTimelineList = new ArrayList<>( hatcheryStateList.size() );
        for ( final HatcheryState hatcheryState : hatcheryStateList ) {
            if ( hatcheryState.assignedLarvaBirthLoopList.isEmpty() && hatcheryState.countPointList.isEmpty() )
                continue;
            injectTimelineList.add( buildInjectTimeline( repProc, hatcheryState, injectActiveDurationLoops, replayEndLoop ) );
        }

        return injectTimelineList;
    }

    /**
     * Builds one normalized inject timeline for a hatchery.
     *
     * @param repProc replay processor
     * @param hatcheryState hatchery state
     * @param injectActiveDurationLoops normalized inject-active duration in replay loops
     * @param replayEndLoop replay end loop
     * @return immutable inject timeline
     */
    private HatcheryInjectTimeline buildInjectTimeline( final IRepProcessor repProc, final HatcheryState hatcheryState,
             final int injectActiveDurationLoops, final int replayEndLoop ) {
        final List< InferredInjectDetector.BurstEvidence > inferredEvidenceList = inferredInjectDetector.detect( hatcheryState.assignedLarvaBirthLoopList );
        final List< HatcheryInjectWindow > injectWindowList = new ArrayList<>();
        final List< String > diagnosticLineList = new ArrayList<>();
        int overlapDiscardCount = 0;
        int boundsDiscardCount = 0;
        int trimmedWindowCount = 0;
        InjectWindowCandidate previousCandidate = null;

        if ( inferredEvidenceList.isEmpty() )
            diagnosticLineList.add( "No inferred inject evidence was detected because no 3-larva burst landed within 8 replay loops for this hatchery." );

        for ( final InferredInjectDetector.BurstEvidence burstEvidence : inferredEvidenceList ) {
            if ( burstEvidence == null )
                continue;

            final InjectWindowDecision decision = buildInjectWindowDecision( repProc, hatcheryState, burstEvidence, injectActiveDurationLoops, replayEndLoop );
            if ( decision.injectWindow != null && previousCandidate != null && decision.injectWindow.getStartLoop() < previousCandidate.endLoop ) {
                overlapDiscardCount++;
                diagnosticLineList.add( "Discarded inferred inject evidence at " + decision.evidenceTimeLabel + " because its retroactive 29-second window overlaps the earlier kept inferred window ending at "
                        + previousCandidate.evidenceTimeLabel + "." );
                continue;
            }

            diagnosticLineList.add( decision.diagnosticLine );
            if ( decision.injectWindow == null ) {
                boundsDiscardCount++;
                continue;
            }

            injectWindowList.add( decision.injectWindow );
            if ( decision.trimmed )
                trimmedWindowCount++;
            previousCandidate = new InjectWindowCandidate( decision.injectWindow.getEndLoop(), decision.evidenceTimeLabel, decision.trimmed );
        }

        return new HatcheryInjectTimeline( hatcheryState.hatcheryTag, hatcheryState.hatcheryTagText,
                hatcheryState.playerName == null ? hatcheryState.resolveFallbackPlayerName( repProc, hatcheryState.playerId ) : hatcheryState.playerName,
                hatcheryState.hatcheryType, hatcheryState.completed, hatcheryState.completionLoop, hatcheryState.completionTimeLabel,
                hatcheryState.destroyedLoop, hatcheryState.destroyedTimeLabel, inferredEvidenceList.size(), overlapDiscardCount, boundsDiscardCount,
                trimmedWindowCount, injectWindowList, diagnosticLineList );
    }

    /**
    * Builds one inject-window decision from inferred burst evidence.
     *
     * @param repProc replay processor
     * @param hatcheryState hatchery state
     * @param burstEvidence inferred burst evidence that proves inject completion
     * @param injectActiveDurationLoops normalized inject-active duration in replay loops
     * @param replayEndLoop replay end loop
     * @return normalized decision containing either a kept window or a discard explanation
     */
    private InjectWindowDecision buildInjectWindowDecision( final IRepProcessor repProc, final HatcheryState hatcheryState,
            final InferredInjectDetector.BurstEvidence burstEvidence, final int injectActiveDurationLoops, final int replayEndLoop ) {
        final int evidenceLoop = burstEvidence.getEvidenceLoop();
        final String evidenceTimeLabel = repProc.formatLoopTime( evidenceLoop );
        final String burstStartTimeLabel = repProc.formatLoopTime( burstEvidence.getFirstBirthLoop() );

        if ( !hatcheryState.completed || hatcheryState.completionLoop < 0 )
            return new InjectWindowDecision( null,
                    "Discarded inferred inject evidence at " + evidenceTimeLabel + " because the hatchery never reached a confirmed completion loop.", false,
                    evidenceTimeLabel );

        final int rawWindowStartLoop = evidenceLoop - injectActiveDurationLoops;
        int effectiveStartLoop = rawWindowStartLoop;
        int effectiveEndLoop = evidenceLoop;
        boolean trimmedAtStart = false;
        boolean trimmedAtEnd = false;
        final List< String > trimReasonList = new ArrayList<>( 2 );

        if ( effectiveStartLoop < 0 ) {
            effectiveStartLoop = 0;
            trimmedAtStart = true;
            trimReasonList.add( "start trimmed to replay origin" );
        }

        if ( effectiveStartLoop < hatcheryState.completionLoop ) {
            effectiveStartLoop = hatcheryState.completionLoop;
            trimmedAtStart = true;
            trimReasonList.add( "start trimmed to completion at " + safeTimeLabel( hatcheryState.completionTimeLabel ) );
        }

        if ( hatcheryState.destroyedLoop >= 0 && effectiveEndLoop > hatcheryState.destroyedLoop ) {
            effectiveEndLoop = hatcheryState.destroyedLoop;
            trimmedAtEnd = true;
            trimReasonList.add( "end trimmed to destruction at " + safeTimeLabel( hatcheryState.destroyedTimeLabel ) );
        }

        if ( replayEndLoop > 0 && effectiveEndLoop > replayEndLoop ) {
            effectiveEndLoop = replayEndLoop;
            trimmedAtEnd = true;
            trimReasonList.add( "end trimmed to replay end at " + repProc.formatLoopTime( replayEndLoop ) );
        }

        if ( effectiveEndLoop <= effectiveStartLoop ) {
                final String reason = trimReasonList.isEmpty() ? "its retroactive " + INJECT_ACTIVE_DURATION_SECONDS + "-second window collapsed outside the valid hatchery lifetime"
                    : "normalization collapsed the window after " + joinReasons( trimReasonList );
            return new InjectWindowDecision( null, "Discarded inferred inject evidence at " + evidenceTimeLabel + " because " + reason + '.', false,
                    evidenceTimeLabel );
        }

        final String startTimeLabel = repProc.formatLoopTime( effectiveStartLoop );
        final String endTimeLabel = repProc.formatLoopTime( effectiveEndLoop );
        final StringBuilder diagnosticBuilder = new StringBuilder();
        diagnosticBuilder.append( "Kept inferred inject evidence at " ).append( evidenceTimeLabel )
                .append( " from larva burst " ).append( burstStartTimeLabel ).append( '-' ).append( evidenceTimeLabel )
                .append( " (" ).append( burstEvidence.getBirthCount() ).append( " larva within " ).append( burstEvidence.getBurstSpanLoops() ).append( " loops) as inject-active window " )
                .append( startTimeLabel ).append( "-" ).append( endTimeLabel );
        if ( trimReasonList.isEmpty() )
            diagnosticBuilder.append( " using replay-derived triple-larva burst inference." );
        else
            diagnosticBuilder.append( " with " ).append( joinReasons( trimReasonList ) ).append( '.' );

        final HatcheryInjectWindow injectWindow = new HatcheryInjectWindow( evidenceLoop, evidenceTimeLabel, INFERRED_INJECT_EVIDENCE_KIND,
                effectiveStartLoop, effectiveEndLoop,
                repProc.loopToTime( effectiveStartLoop ), repProc.loopToTime( effectiveEndLoop ), startTimeLabel, endTimeLabel,
                trimmedAtStart, trimmedAtEnd, diagnosticBuilder.toString() );
        return new InjectWindowDecision( injectWindow, diagnosticBuilder.toString(), trimmedAtStart || trimmedAtEnd, evidenceTimeLabel );
    }

    /**
     * Resolves the inject-active window length in replay loops for the replay game speed.
     *
     * @param repProc replay processor
     * @return inject-active window duration in replay loops
     */
    private int resolveInjectActiveDurationLoops( final IRepProcessor repProc ) {
        final long gameSpeedRelative = repProc == null || repProc.getConverterGameSpeed() == null ? DEFAULT_GAME_SPEED_RELATIVE : repProc.getConverterGameSpeed().getRelativeSpeed();
        final double effectiveGameSpeedRelative = gameSpeedRelative <= 0L ? DEFAULT_GAME_SPEED_RELATIVE : gameSpeedRelative;
        return (int) ( INJECT_ACTIVE_DURATION_SECONDS * REPLAY_LOOPS_PER_SECOND * ( NORMAL_GAME_SPEED_RELATIVE / effectiveGameSpeedRelative ) );
    }

    /**
     * Joins normalization reasons into readable diagnostics.
     *
     * @param trimReasonList reasons to join
     * @return joined readable text
     */
    private String joinReasons( final List< String > trimReasonList ) {
        if ( trimReasonList == null || trimReasonList.isEmpty() )
            return "no additional normalization";
        if ( trimReasonList.size() == 1 )
            return trimReasonList.get( 0 );

        final StringBuilder builder = new StringBuilder();
        for ( int i = 0; i < trimReasonList.size(); i++ ) {
            if ( i > 0 )
                builder.append( i == trimReasonList.size() - 1 ? " and " : ", " );
            builder.append( trimReasonList.get( i ) );
        }
        return builder.toString();
    }

    /**
     * Returns a readable time label even if the source value is absent.
     *
     * @param timeLabel source time label
     * @return readable label
     */
    private String safeTimeLabel( final String timeLabel ) {
        return timeLabel == null || timeLabel.length() == 0 ? "n/a" : timeLabel;
    }

    /**
     * Adds one player resource snapshot to the target map.
     *
     * @param snapshotMap target snapshot map
     * @param snapshot snapshot to add
     */
    private void addResourceSnapshot( final Map< String, List< LarvaPlayerResourceSnapshot > > snapshotMap, final LarvaPlayerResourceSnapshot snapshot ) {
        if ( snapshot == null || snapshot.getPlayerName() == null || snapshot.getPlayerName().length() == 0 )
            return;

        List< LarvaPlayerResourceSnapshot > snapshotList = snapshotMap.get( snapshot.getPlayerName() );
        if ( snapshotList == null ) {
            snapshotList = new ArrayList<>();
            snapshotMap.put( snapshot.getPlayerName(), snapshotList );
        }

        snapshotList.add( snapshot );
    }

    /**
     * Derives a calibrated larva offset from the replay opening when possible.
     *
     * @param trackerEventArray tracker event array
     * @return derived calibration
     */
    private LarvaHeuristicCalibration calibrate( final IEvent[] trackerEventArray ) {
        final Map< Integer, List< EarlyUnitSample > > hatcherySamplesByPlayer = new HashMap<>();
        final Map< Integer, List< EarlyUnitSample > > larvaSamplesByPlayer = new HashMap<>();

        if ( trackerEventArray != null ) {
            for ( final IEvent event : trackerEventArray ) {
                if ( event == null || event.getLoop() > CALIBRATION_LOOP_LIMIT )
                    continue;
                if ( event.getId() != ITrackerEvents.ID_UNIT_BORN && event.getId() != ITrackerEvents.ID_UNIT_INIT )
                    continue;

                final IBaseUnitEvent baseUnitEvent = (IBaseUnitEvent) event;
                final String unitType = toStringValue( baseUnitEvent.getUnitTypeName() );
                final Integer playerId = firstNonNull( baseUnitEvent.getControlPlayerId(), baseUnitEvent.getUpkeepPlayerId() );
                if ( playerId == null || baseUnitEvent.getXCoord() == null || baseUnitEvent.getYCoord() == null )
                    continue;

                if ( isHatcheryLike( unitType ) )
                    addSample( hatcherySamplesByPlayer, playerId, new EarlyUnitSample( baseUnitEvent.getXCoord(), baseUnitEvent.getYCoord() ) );
                else if ( IBdUtil.UNIT_LARVA.equals( unitType ) )
                    addSample( larvaSamplesByPlayer, playerId, new EarlyUnitSample( baseUnitEvent.getXCoord(), baseUnitEvent.getYCoord() ) );
            }
        }

        double dxSum = 0.0;
        double dySum = 0.0;
        double maxDistance = 0.0;
        int sampleCount = 0;
        int playerOpenings = 0;

        for ( final Map.Entry< Integer, List< EarlyUnitSample > > entry : larvaSamplesByPlayer.entrySet() ) {
            final List< EarlyUnitSample > hatcherySamples = hatcherySamplesByPlayer.get( entry.getKey() );
            if ( hatcherySamples == null || hatcherySamples.size() != 1 || entry.getValue().size() < 3 )
                continue;

            final EarlyUnitSample hatcherySample = hatcherySamples.get( 0 );
            playerOpenings++;
            for ( final EarlyUnitSample larvaSample : entry.getValue() ) {
                final double dx = larvaSample.x.intValue() - hatcherySample.x.intValue();
                final double dy = larvaSample.y.intValue() - hatcherySample.y.intValue();
                final double distance = Math.sqrt( dx * dx + dy * dy );
                dxSum += dx;
                dySum += dy;
                if ( distance > maxDistance )
                    maxDistance = distance;
                sampleCount++;
            }
        }

        if ( sampleCount > 0 ) {
            final double averageDx = dxSum / sampleCount;
            final double averageDy = dySum / sampleCount;
            final double recommendedDistance = Math.max( DEFAULT_ASSIGNMENT_RADIUS, maxDistance + 2.0 );
            return new LarvaHeuristicCalibration( true, sampleCount, averageDx, averageDy, maxDistance, recommendedDistance,
                    "Derived from " + sampleCount + " opening larva samples across " + playerOpenings
                            + " Zerg opening(s) that started with one hatchery and at least three larva." );
        }

        return new LarvaHeuristicCalibration( false, 0, DEFAULT_AVERAGE_DX, DEFAULT_AVERAGE_DY, DEFAULT_ASSIGNMENT_RADIUS,
                DEFAULT_ASSIGNMENT_RADIUS, "Replay opening calibration was unavailable, so Epic 6 fell back to documented default offset/radius values." );
    }

    /**
     * Adds one early sample to a player-indexed map.
     *
     * @param sampleMap sample map
     * @param playerId player id
     * @param sample sample to add
     */
    private void addSample( final Map< Integer, List< EarlyUnitSample > > sampleMap, final Integer playerId, final EarlyUnitSample sample ) {
        List< EarlyUnitSample > sampleList = sampleMap.get( playerId );
        if ( sampleList == null ) {
            sampleList = new ArrayList<>();
            sampleMap.put( playerId, sampleList );
        }

        sampleList.add( sample );
    }

    /**
     * Converts internal hatchery states to immutable snapshots for the assignment heuristic.
     *
     * @param hatcheryStates hatchery states to snapshot
     * @return immutable hatchery snapshots
     */
    private Collection< HatcherySnapshot > snapshotHatcheries( final Collection< HatcheryState > hatcheryStates ) {
        final List< HatcherySnapshot > snapshotList = new ArrayList<>( hatcheryStates.size() );
        for ( final HatcheryState hatcheryState : hatcheryStates )
            snapshotList.add( new HatcherySnapshot( hatcheryState.hatcheryTag, hatcheryState.playerId, hatcheryState.x, hatcheryState.y,
                    hatcheryState.alive, hatcheryState.completed ) );
        return snapshotList;
    }

    /**
     * Returns an existing hatchery state or creates a new one.
     *
     * @param repProc replay processor
     * @param hatcheryByTag hatchery state map
     * @param hatcheryTag hatchery tag
     * @return hatchery state
     */
    private HatcheryState getOrCreateHatcheryState( final IRepProcessor repProc, final Map< Integer, HatcheryState > hatcheryByTag, final int hatcheryTag ) {
        HatcheryState hatcheryState = hatcheryByTag.get( Integer.valueOf( hatcheryTag ) );
        if ( hatcheryState == null ) {
            hatcheryState = new HatcheryState( hatcheryTag, repProc.getTagTransformation().tagToString( hatcheryTag ) );
            hatcheryByTag.put( Integer.valueOf( hatcheryTag ), hatcheryState );
        }

        return hatcheryState;
    }

    /**
     * Resolves a player name for a player id.
     *
     * @param repProc replay processor
     * @param playerId player id
     * @return player name
     */
    private String resolvePlayerName( final IRepProcessor repProc, final Integer playerId ) {
        if ( playerId == null )
            return "Unknown player";

        final IUser[] usersByPlayerId = repProc.getUsersByPlayerId();
        if ( usersByPlayerId == null )
            return "Player " + playerId;
        if ( playerId.intValue() < 0 || playerId.intValue() >= usersByPlayerId.length || usersByPlayerId[ playerId.intValue() ] == null )
            return "Player " + playerId;

        return usersByPlayerId[ playerId.intValue() ].getName();
    }

    /**
     * Sorts resource snapshots per player so sparse or slightly out-of-order tracker samples remain
     * deterministic for later hover lookup and validation output.
     *
     * @param resourceSnapshotsByPlayerName snapshots to sort in place
     */
    private void sortResourceSnapshotsByLoop( final Map< String, List< LarvaPlayerResourceSnapshot > > resourceSnapshotsByPlayerName ) {
        for ( final List< LarvaPlayerResourceSnapshot > snapshotList : resourceSnapshotsByPlayerName.values() ) {
            Collections.sort( snapshotList, new Comparator< LarvaPlayerResourceSnapshot >() {
                @Override
                public int compare( final LarvaPlayerResourceSnapshot left, final LarvaPlayerResourceSnapshot right ) {
                    return left.getLoop() < right.getLoop() ? -1 : left.getLoop() == right.getLoop() ? 0 : 1;
                }
            } );
        }
    }

    /**
     * Null-safe text comparison for deterministic sorting.
     *
     * @param left left text
     * @param right right text
     * @return comparator result
     */
    private int compareIgnoreCaseSafe( final String left, final String right ) {
        final String safeLeft = left == null ? "" : left;
        final String safeRight = right == null ? "" : right;
        return safeLeft.compareToIgnoreCase( safeRight );
    }

    /**
     * Tells if the specified unit type is part of the Zerg main-building lineage.
     *
     * @param unitType unit type to test
     * @return true if the type is Hatchery, Lair, or Hive; false otherwise
     */
    private boolean isHatcheryLike( final String unitType ) {
        return IBdUtil.UNIT_HATCHERY.equals( unitType ) || UNIT_LAIR.equals( unitType ) || UNIT_HIVE.equals( unitType );
    }

    /**
     * Builds a combined unit tag from tracker unit tag index and recycle values.
     *
     * @param unitTagIndex unit tag index
     * @param unitTagRecycle unit tag recycle
     * @return combined tag
     */
    private int buildCombinedTag( final Integer unitTagIndex, final Integer unitTagRecycle ) {
        return unitTagIndex.intValue() << 18 | unitTagRecycle.intValue();
    }

    /**
     * Builds a combined unit tag if both parts are available.
     *
     * @param unitTagIndex unit tag index object
     * @param unitTagRecycle unit tag recycle object
     * @return combined tag or <code>null</code> if parts are missing
     */
    private Integer buildCombinedTagOrNull( final Object unitTagIndex, final Object unitTagRecycle ) {
        return unitTagIndex instanceof Integer && unitTagRecycle instanceof Integer ? Integer.valueOf( buildCombinedTag( (Integer) unitTagIndex, (Integer) unitTagRecycle ) )
                : null;
    }

    /**
     * Returns an existing queen state or creates a new one.
     */
    private QueenState getOrCreateQueenState( final IRepProcessor repProc, final Map< Integer, QueenState > queenByTag, final int queenTag ) {
        final Integer key = Integer.valueOf( queenTag );
        QueenState queenState = queenByTag.get( key );
        if ( queenState == null ) {
            queenState = new QueenState( queenTag, repProc == null || repProc.getTagTransformation() == null ? String.valueOf( queenTag )
                    : repProc.getTagTransformation().tagToString( queenTag ) );
            queenByTag.put( key, queenState );
        }
        return queenState;
    }

    /**
     * Returns the first non-null integer.
     *
     * @param primary primary value
     * @param fallback fallback value
     * @return first non-null integer or <code>null</code>
     */
    private Integer firstNonNull( final Integer primary, final Integer fallback ) {
        return primary != null ? primary : fallback;
    }

    /**
     * Converts a raw tracker field value to text.
     *
     * @param value raw value
     * @return textual representation or empty string
     */
    private String toStringValue( final Object value ) {
        if ( value == null )
            return "";
        return value instanceof IXString ? ( (IXString) value ).toString() : value.toString();
    }

    /** Opening calibration sample. */
    private static class EarlyUnitSample {

        /** Sample x coordinate. */
        private final Integer x;

        /** Sample y coordinate. */
        private final Integer y;

        /**
         * Creates a new early unit sample.
         *
         * @param x x coordinate
         * @param y y coordinate
         */
        private EarlyUnitSample( final Integer x, final Integer y ) {
            this.x = x;
            this.y = y;
        }

    }

    /** One normalized inject-window decision. */
    private static class InjectWindowDecision {

        /** Kept inject window, or <code>null</code> if discarded. */
        private final HatcheryInjectWindow injectWindow;

        /** Deterministic explanation of the decision. */
        private final String diagnosticLine;

        /** Tells if the kept window was trimmed. */
        private final boolean trimmed;

        /** Time label of the evidence point that produced this decision. */
        private final String evidenceTimeLabel;

        /**
         * Creates a new inject-window decision.
         *
         * @param injectWindow kept inject window, or <code>null</code> if discarded
         * @param diagnosticLine deterministic explanation of the decision
         * @param trimmed tells if the kept window was trimmed
         * @param evidenceTimeLabel time label of the evidence point that produced this decision
         */
        private InjectWindowDecision( final HatcheryInjectWindow injectWindow, final String diagnosticLine, final boolean trimmed,
                final String evidenceTimeLabel ) {
            this.injectWindow = injectWindow;
            this.diagnosticLine = diagnosticLine;
            this.trimmed = trimmed;
            this.evidenceTimeLabel = evidenceTimeLabel;
        }

    }

    /** Raw overlap tracking state for the previously kept inject window. */
    private static class InjectWindowCandidate {

        /** Effective end loop of the previous kept window. */
        private final int endLoop;

        /** Previous evidence time label. */
        private final String evidenceTimeLabel;

        /** Tells if the previous kept window was trimmed. */
        private final boolean trimmed;

        /**
         * Creates a new overlap-tracking candidate.
         *
         * @param endLoop effective end loop of the previous window
         * @param evidenceTimeLabel previous evidence time label
         * @param trimmed tells if the previous kept window was trimmed
         */
        private InjectWindowCandidate( final int endLoop, final String evidenceTimeLabel, final boolean trimmed ) {
            this.endLoop = endLoop;
            this.evidenceTimeLabel = evidenceTimeLabel;
            this.trimmed = trimmed;
        }

    }

    /** One conservatively attributed queen command. */
    private static class QueenCommandEvidence {

        /** Queen tag. */
        private final int queenTag;

        /** Command loop. */
        private final int loop;

        /** Formatted command time. */
        private final String timeLabel;

        /** Inject target hatchery tag, or -1 if not an inject. */
        private final int targetHatcheryTag;

        /** Ability id. */
        private final String abilityId;

        /** Tells if this was a SpawnLarva command. */
        private final boolean inject;

        /** Known energy cost of the command, or 0 if not a tracked queen energy spend. */
        private final int energyCost;

        private QueenCommandEvidence( final int queenTag, final int loop, final String timeLabel, final int targetHatcheryTag,
                final String abilityId, final boolean inject, final int energyCost ) {
            this.queenTag = queenTag;
            this.loop = loop;
            this.timeLabel = timeLabel;
            this.targetHatcheryTag = targetHatcheryTag;
            this.abilityId = abilityId;
            this.inject = inject;
            this.energyCost = energyCost;
        }

    }

    /** One candidate idle-inject interval before hatchery-level merging. */
    private static class CandidateIdleWindow {

        /** Start loop. */
        private final int startLoop;

        /** End loop. */
        private final int endLoop;

        /** Estimated queen energy at candidate start. */
        private final double estimatedStartEnergy;

        /** Estimated queen energy at candidate end. */
        private final double estimatedEndEnergy;

        /** Queen summary text. */
        private final String queenSummary;

        /** Diagnostic note. */
        private final String diagnosticNote;

        private CandidateIdleWindow( final int startLoop, final int endLoop, final double estimatedStartEnergy,
                final double estimatedEndEnergy, final String queenSummary, final String diagnosticNote ) {
            this.startLoop = startLoop;
            this.endLoop = endLoop;
            this.estimatedStartEnergy = estimatedStartEnergy;
            this.estimatedEndEnergy = estimatedEndEnergy;
            this.queenSummary = queenSummary;
            this.diagnosticNote = diagnosticNote;
        }

    }

    /** Mutable hatchery state during analysis. */
    private static class HatcheryState {

        /** Hatchery tag. */
        private final int hatcheryTag;

        /** Formatted hatchery tag text. */
        private final String hatcheryTagText;

        /** Player id. */
        private Integer playerId;

        /** Player name. */
        private String playerName = "Unknown player";

        /** Hatchery type. */
        private String hatcheryType = IBdUtil.UNIT_HATCHERY;

        /** Last known x coordinate. */
        private Integer x;

        /** Last known y coordinate. */
        private Integer y;

        /** Tells if the hatchery is alive. */
        private boolean alive = true;

        /** Tells if the hatchery is completed. */
        private boolean completed;

        /** Completion loop, or <code>-1</code> if unknown. */
        private int completionLoop = -1;

        /** Completion time label. */
        private String completionTimeLabel;

        /** First loop where at least one larva was assigned. */
        private int firstLarvaLoop = -1;

        /** First larva time label. */
        private String firstLarvaTimeLabel;

        /** Destroyed loop, or <code>-1</code> if the hatchery survived. */
        private int destroyedLoop = -1;

        /** Destroyed time label. */
        private String destroyedTimeLabel;

        /** Current larva count. */
        private int larvaCount;

        /** Max larva count reached. */
        private int maxLarvaCount;

        /** Direct creator-based assignments. */
        private int directAssignmentCount;

        /** Inject-correlated assignments. */
        private int injectCorrelatedAssignmentCount;

        /** Pure heuristic assignments. */
        private int heuristicAssignmentCount;

        /** Count timeline points. */
        private final List< HatcheryLarvaTimeline.CountPoint > countPointList = new ArrayList<>();

        /** Assigned larva birth loops used to infer green inject windows. */
        private final List< Integer > assignedLarvaBirthLoopList = new ArrayList<>();

        /**
         * Creates a new hatchery state.
         *
         * @param hatcheryTag hatchery tag
         * @param hatcheryTagText formatted hatchery tag text
         */
        private HatcheryState( final int hatcheryTag, final String hatcheryTagText ) {
            this.hatcheryTag = hatcheryTag;
            this.hatcheryTagText = hatcheryTagText;
        }

        /**
         * Ensures a point exists for the current count at the specified loop.
         *
         * @param loop loop to ensure a point for
         * @param repProc replay processor
         */
        private void ensurePoint( final int loop, final IRepProcessor repProc ) {
            addOrReplacePoint( loop, repProc, larvaCount );
        }

        /**
         * Adds one larva to the count timeline.
         *
         * @param loop loop of the increase
         * @param repProc replay processor
         * @param confidence assignment confidence
         */
        private void addLarva( final int loop, final IRepProcessor repProc, final LarvaAssignmentHeuristic.Confidence confidence ) {
            larvaCount++;
            assignedLarvaBirthLoopList.add( Integer.valueOf( loop ) );
            if ( firstLarvaLoop < 0 ) {
                firstLarvaLoop = loop;
                firstLarvaTimeLabel = repProc.formatLoopTime( loop );
            }
            if ( larvaCount > maxLarvaCount )
                maxLarvaCount = larvaCount;
            switch ( confidence ) {
                case DIRECT :
                    directAssignmentCount++;
                    break;
                case INJECT_CORRELATED :
                    injectCorrelatedAssignmentCount++;
                    break;
                case HEURISTIC :
                    heuristicAssignmentCount++;
                    break;
                case UNASSIGNED :
                default :
                    break;
            }
            addOrReplacePoint( loop, repProc, larvaCount );
        }

        /**
         * Records the first known completion moment if it has not been captured yet.
         *
         * @param loop completion loop
         * @param repProc replay processor
         */
        private void recordCompletion( final int loop, final IRepProcessor repProc ) {
            if ( completionLoop >= 0 )
                return;

            completionLoop = loop;
            completionTimeLabel = repProc.formatLoopTime( loop );
        }

        /**
         * Records the first known destroy moment if it has not been captured yet.
         *
         * @param loop destroyed loop
         * @param repProc replay processor
         */
        private void recordDestroyed( final int loop, final IRepProcessor repProc ) {
            if ( destroyedLoop >= 0 )
                return;

            destroyedLoop = loop;
            destroyedTimeLabel = repProc.formatLoopTime( loop );
        }

        /**
         * Removes one larva from the count timeline.
         *
         * @param loop loop of the decrease
         * @param repProc replay processor
         */
        private void removeLarva( final int loop, final IRepProcessor repProc ) {
            if ( larvaCount > 0 )
                larvaCount--;
            addOrReplacePoint( loop, repProc, larvaCount );
        }

        /**
         * Adds a new point or replaces the latest point if it is on the same loop.
         *
         * @param loop loop to record
         * @param repProc replay processor
         * @param count larva count at the loop
         */
        private void addOrReplacePoint( final int loop, final IRepProcessor repProc, final int count ) {
            final HatcheryLarvaTimeline.CountPoint point = new HatcheryLarvaTimeline.CountPoint( loop, repProc.formatLoopTime( loop ), count );
            if ( !countPointList.isEmpty() && countPointList.get( countPointList.size() - 1 ).getLoop() == loop )
                countPointList.set( countPointList.size() - 1, point );
            else
                countPointList.add( point );
        }

        /**
         * Converts the mutable state to an immutable timeline.
         *
         * @param repProc replay processor
         * @return immutable timeline
         */
        private HatcheryLarvaTimeline toTimeline( final IRepProcessor repProc ) {
            return new HatcheryLarvaTimeline( hatcheryTag, hatcheryTagText, playerName == null ? resolveFallbackPlayerName( repProc, playerId ) : playerName,
                    hatcheryType, completed, completionLoop, completionTimeLabel, firstLarvaLoop, firstLarvaTimeLabel, destroyedLoop, destroyedTimeLabel,
                    normalizePointList(), maxLarvaCount, directAssignmentCount, injectCorrelatedAssignmentCount, heuristicAssignmentCount );
        }

        /**
         * Produces a normalized count-point list so downstream window calculations do not depend on
         * tracker event ordering quirks.
         *
         * @return normalized point list
         */
        private List< HatcheryLarvaTimeline.CountPoint > normalizePointList() {
            if ( countPointList.isEmpty() )
                return countPointList;

            final List< HatcheryLarvaTimeline.CountPoint > normalizedPointList = new ArrayList<>( countPointList );
            Collections.sort( normalizedPointList, new Comparator< HatcheryLarvaTimeline.CountPoint >() {
                @Override
                public int compare( final HatcheryLarvaTimeline.CountPoint left, final HatcheryLarvaTimeline.CountPoint right ) {
                    return left.getLoop() < right.getLoop() ? -1 : left.getLoop() == right.getLoop() ? 0 : 1;
                }
            } );

            final List< HatcheryLarvaTimeline.CountPoint > deduplicatedPointList = new ArrayList<>( normalizedPointList.size() );
            for ( final HatcheryLarvaTimeline.CountPoint point : normalizedPointList ) {
                if ( !deduplicatedPointList.isEmpty() && deduplicatedPointList.get( deduplicatedPointList.size() - 1 ).getLoop() == point.getLoop() )
                    deduplicatedPointList.set( deduplicatedPointList.size() - 1, point );
                else
                    deduplicatedPointList.add( point );
            }

            return deduplicatedPointList;
        }

        /**
         * Resolves a fallback player name if needed.
         *
         * @param repProc replay processor
         * @param playerId player id
         * @return player name
         */
        private String resolveFallbackPlayerName( final IRepProcessor repProc, final Integer playerId ) {
            if ( playerId == null )
                return "Unknown player";
            final IUser[] usersByPlayerId = repProc.getUsersByPlayerId();
            if ( usersByPlayerId == null )
                return "Player " + playerId;
            return playerId.intValue() >= 0 && playerId.intValue() < usersByPlayerId.length && usersByPlayerId[ playerId.intValue() ] != null
                    ? usersByPlayerId[ playerId.intValue() ].getName()
                    : "Player " + playerId;
        }

    }

    /** Mutable queen state during analysis. */
    private static class QueenState {

        /** Queen tag. */
        private final int queenTag;

        /** Formatted queen tag text. */
        private final String queenTagText;

        /** Player id. */
        private Integer playerId;

        /** Player name. */
        private String playerName = "Unknown player";

        /** Unit type currently represented by the tag. */
        private String unitType = UNIT_QUEEN;

        /** Last known x coordinate. */
        private Integer x;

        /** Last known y coordinate. */
        private Integer y;

        /** Last loop with a trusted sparse tracker position. */
        private int lastKnownPositionLoop = -1;

        /** Tells if the queen is alive. */
        private boolean alive = true;

        /** Tells if the queen is completed. */
        private boolean completed;

        /** Completion loop. */
        private int completionLoop = -1;

        /** Completion time label. */
        private String completionTimeLabel;

        /** Destroyed loop. */
        private int destroyedLoop = -1;

        /** Destroyed time label. */
        private String destroyedTimeLabel;

        private QueenState( final int queenTag, final String queenTagText ) {
            this.queenTag = queenTag;
            this.queenTagText = queenTagText;
        }

        private void recordCompletion( final int loop, final IRepProcessor repProc ) {
            if ( completionLoop >= 0 )
                return;
            completionLoop = loop;
            completionTimeLabel = repProc == null ? null : repProc.formatLoopTime( loop );
        }

        private void recordDestroyed( final int loop, final IRepProcessor repProc ) {
            if ( destroyedLoop >= 0 )
                return;
            destroyedLoop = loop;
            destroyedTimeLabel = repProc == null ? null : repProc.formatLoopTime( loop );
        }

    }

    /** Mutable larva state during analysis. */
    private static class LarvaState {

        /** Larva tag. */
        private final int larvaTag;

        /** Assigned hatchery tag. */
        private final int hatcheryTag;

        /** Assignment confidence. */
        private final LarvaAssignmentHeuristic.Confidence confidence;

        /**
         * Creates a new larva state.
         *
         * @param larvaTag larva tag
         * @param hatcheryTag assigned hatchery tag
         * @param confidence assignment confidence
         */
        private LarvaState( final int larvaTag, final int hatcheryTag, final LarvaAssignmentHeuristic.Confidence confidence ) {
            this.larvaTag = larvaTag;
            this.hatcheryTag = hatcheryTag;
            this.confidence = confidence;
        }

    }

}