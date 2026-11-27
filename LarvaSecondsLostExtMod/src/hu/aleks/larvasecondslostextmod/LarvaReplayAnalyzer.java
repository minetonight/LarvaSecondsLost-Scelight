package hu.aleks.larvasecondslostextmod;

import hu.scelightapi.service.IFactory;
import hu.scelightapi.sc2.balancedata.IBdUtil;
import hu.scelightapi.sc2.rep.model.IEvent;
import hu.scelightapi.sc2.rep.model.IReplay;
import hu.scelightapi.sc2.rep.repproc.IRepProcessor;
import hu.scelightapi.sc2.rep.repproc.IUser;
import hu.scelightapi.sc2.rep.model.trackerevents.IBaseUnitEvent;
import hu.scelightapi.sc2.rep.model.trackerevents.IPlayerStatsEvent;
import hu.scelightapi.sc2.rep.model.trackerevents.ITrackerEvents;
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

    /** Inject-active window per inject, in loops (one inject, about 29 s on the game clock). */
    private static final int INJECT_ACTIVE_DURATION_LOOPS = LarvaGameClock.INJECT_CYCLE_LOOPS;

    /** Loops after Spawning Pool completion before inject uptime becomes eligible: the first queen's build time. */
    private static final int INJECT_ELIGIBILITY_DELAY_LOOPS = LarvaGameClock.QUEEN_BUILD_LOOPS;

    /** Red (missed-inject) loops represented by one missed inject. */
    private static final int MISSED_INJECT_THRESHOLD_LOOPS = LarvaGameClock.INJECT_CYCLE_LOOPS;

    /** Potential injected larva counted for each missed-inject threshold. */
    private static final int MISSED_INJECT_LARVA_PER_THRESHOLD = 3;

    /** Public replay-surface conclusion for green inject reconstruction. */
    private static final String INJECT_SIGNAL_CONCLUSION = "No direct injected-building state is exposed by the public external-module replay model, so green inject-active windows are inferred from replay-derived hatchery bursts where 3 larva appear within 8 replay loops, then projected backward as one-inject windows of " + LarvaGameClock.INJECT_CYCLE_LOOPS + " loops (about 29 s on the game clock).";

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

    /** Zerg tech building used to unlock queen inject availability. */
    private static final String UNIT_SPAWNING_POOL = "SpawningPool";

    /** Spawn Larva correlation helper. */
    private final SpawnLarvaCorrelator spawnLarvaCorrelator = new SpawnLarvaCorrelator();

    /** Inferred inject detector used for green inject reconstruction. */
    private final InferredInjectDetector inferredInjectDetector = new InferredInjectDetector();

    /** Stable 3+ larva window calculator. */
    private final LarvaSaturationWindowCalculator saturationWindowCalculator = new LarvaSaturationWindowCalculator();

    /** Missed-larva threshold marker calculator. */
    private final LarvaMissedLarvaMarkerCalculator missedLarvaMarkerCalculator = new LarvaMissedLarvaMarkerCalculator();

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
        final Map< Integer, LarvaState > larvaByTag = new HashMap<>();
        final Map< String, List< LarvaPlayerResourceSnapshot > > resourceSnapshotsByPlayerName = new LinkedHashMap<>();
        final Map< String, Integer > spawningPoolCompletionLoopByPlayerName = new LinkedHashMap<>();
        final Map< Integer, String > pendingSpawningPoolPlayerNameByTag = new HashMap<>();


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
                        // E19-S03: the same value as Scelight's Workers Active chart (drones at work; not in eggs or in buildings)
                        addResourceSnapshot( resourceSnapshotsByPlayerName, new LarvaPlayerResourceSnapshot( playerName, event.getLoop(),
                            LarvaGameClock.format( event.getLoop() ), playerStatsEvent.getMineralsCurrent(), playerStatsEvent.getGasCurrent(),
                            playerStatsEvent.getFoodUsed(), playerStatsEvent.getFoodMade(), playerStatsEvent.getWorkersActiveCount() ) );
                        break;
                    }
                    case ITrackerEvents.ID_UNIT_INIT :
                    case ITrackerEvents.ID_UNIT_BORN : {
                        final IBaseUnitEvent baseUnitEvent = (IBaseUnitEvent) event;
                        final String unitType = toStringValue( baseUnitEvent.getUnitTypeName() );
                        final Integer unitTag = buildCombinedTagOrNull( baseUnitEvent.getUnitTagIndex(), baseUnitEvent.getUnitTagRecycle() );
                        final String creatorAbilityName = toStringValue( event.get( F_CREATOR_ABILITY_NAME ) );
                        if ( isSpawningPoolLike( unitType ) && unitTag != null ) {
                            final String playerName = resolvePlayerName( repProc,
                                    firstNonNull( baseUnitEvent.getControlPlayerId(), baseUnitEvent.getUpkeepPlayerId() ) );
                            if ( event.getId() == ITrackerEvents.ID_UNIT_INIT ) {
                                pendingSpawningPoolPlayerNameByTag.put( unitTag, playerName );
                            } else if ( event.getId() == ITrackerEvents.ID_UNIT_BORN ) {
                                recordSpawningPoolCompletion( spawningPoolCompletionLoopByPlayerName, playerName, event.getLoop() );
                            }
                        }
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
                            final String pendingSpawningPoolPlayerName = pendingSpawningPoolPlayerNameByTag.remove( hatcheryTag );
                            if ( pendingSpawningPoolPlayerName != null )
                                recordSpawningPoolCompletion( spawningPoolCompletionLoopByPlayerName, pendingSpawningPoolPlayerName, event.getLoop() );
                        }

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

                        pendingSpawningPoolPlayerNameByTag.remove( tag );

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
                        break;
                    }
                    case ITrackerEvents.ID_UNIT_TYPE_CHANGE : {
                        final Integer tag = buildCombinedTagOrNull( event.get( IBaseUnitEvent.F_UNIT_TAG_INDEX ), event.get( IBaseUnitEvent.F_UNIT_TAG_RECYCLE ) );
                        final String unitType = toStringValue( event.get( F_UNIT_TYPE_NAME ) );
                        if ( tag == null )
                            break;

                        if ( !isSpawningPoolLike( unitType ) )
                            pendingSpawningPoolPlayerNameByTag.remove( tag );

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
                        break;
                    }
                    case ITrackerEvents.ID_UNIT_OWNER_CHANGE : {
                        final Integer tag = buildCombinedTagOrNull( event.get( IBaseUnitEvent.F_UNIT_TAG_INDEX ), event.get( IBaseUnitEvent.F_UNIT_TAG_RECYCLE ) );
                        final HatcheryState hatcheryState = tag == null ? null : hatcheryByTag.get( tag );
                        if ( hatcheryState != null ) {
                            hatcheryState.playerId = firstNonNull( (Integer) event.get( F_CONTROL_PLAYER_ID ), (Integer) event.get( F_UPKEEP_PLAYER_ID ) );
                            hatcheryState.playerName = resolvePlayerName( repProc, hatcheryState.playerId );
                        }
                        break;
                    }
                    default :
                        break;
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
                if ( playerCompare != 0 )
                    return playerCompare;

                final int completionCompare = compareCompletionLoops( left.getCompletionLoop(), right.getCompletionLoop() );
                if ( completionCompare != 0 )
                    return completionCompare;

                return compareIgnoreCaseSafe( left.getHatcheryTagText(), right.getHatcheryTagText() );
            }
        } );

        sortResourceSnapshotsByLoop( resourceSnapshotsByPlayerName );

        final int injectActiveDurationLoops = INJECT_ACTIVE_DURATION_LOOPS;
        final List< HatcheryInjectTimeline > injectTimelineList = buildInjectTimelines( repProc, hatcheryByTag, injectActiveDurationLoops,
                replay.getHeader() == null || replay.getHeader().getElapsedGameLoops() == null ? 0 : replay.getHeader().getElapsedGameLoops().intValue() );
        final int replayEndLoop = replay.getHeader() == null || replay.getHeader().getElapsedGameLoops() == null ? 0 : replay.getHeader().getElapsedGameLoops().intValue();
        final LarvaMissedInjectAnalysis.Output missedInjects = LarvaMissedInjectAnalysis.build( repProc, factory, toHatchInfos( hatcheryByTag ),
                injectTimelineList, buildInjectUnlockLoops( spawningPoolCompletionLoopByPlayerName ), replayEndLoop );
        final List< HatcheryIdleInjectTimeline > idleInjectTimelineList = sortIdleInjectTimelines( missedInjects.timelines, hatcheryByTag );
        final Map< String, LarvaPlayerPhaseTable > playerPhaseTableByPlayerName = buildPlayerPhaseTables( repProc, replayEndLoop,
            resourceSnapshotsByPlayerName, spawningPoolCompletionLoopByPlayerName, hatcheryByTag, injectTimelineList, idleInjectTimelineList );

        return new LarvaAnalysisReport( calibration, timelineList, injectTimelineList, INJECT_SIGNAL_CONCLUSION,
            idleInjectTimelineList, LarvaMissedInjectAnalysis.CONCLUSION, LarvaMissedInjectAnalysis.RADIUS,
            missedInjects.queens, missedInjects.queensWithPositions, missedInjects.spendsFromSelection, missedInjects.queenCommands,
            hatcheryByTag.size(), larvaBirthCount, assignedLarvaCount, unassignedLarvaCount,
            ambiguousLarvaCount, noEligibleHatcheryLarvaCount,
            directAssignmentCount, injectCorrelatedAssignmentCount, heuristicAssignmentCount, hatcheryMorphCount,
            trackerEventArray == null ? 0 : trackerEventArray.length, gameEventArray == null ? 0 : gameEventArray.length, fullReplayParseUsed,
            replayEndLoop, resourceSnapshotsByPlayerName, playerPhaseTableByPlayerName );
    }

    /**
     * Hatchery facts for the missed-inject model (E19-S10).
     */
    private List< LarvaMissedInjectAnalysis.HatchInfo > toHatchInfos( final Map< Integer, HatcheryState > hatcheryByTag ) {
        final List< LarvaMissedInjectAnalysis.HatchInfo > hatchInfos = new ArrayList<>();
        for ( final HatcheryState hatcheryState : hatcheryByTag.values() ) {
            if ( hatcheryState == null || hatcheryState.playerId == null || hatcheryState.x == null || hatcheryState.y == null )
                continue;
            hatchInfos.add( new LarvaMissedInjectAnalysis.HatchInfo( hatcheryState.hatcheryTag, hatcheryState.hatcheryTagText,
                    hatcheryState.playerId.intValue(), hatcheryState.playerName, hatcheryState.hatcheryType, hatcheryState.x.intValue(),
                    hatcheryState.y.intValue(), hatcheryState.completed ? hatcheryState.completionLoop : -1, hatcheryState.destroyedLoop ) );
        }
        return hatchInfos;
    }

    /**
     * First loop an inject can be missed, per player: Spawning Pool completion plus the queen build time.
     */
    private Map< String, Integer > buildInjectUnlockLoops( final Map< String, Integer > spawningPoolCompletionLoopByPlayerName ) {
        final Map< String, Integer > unlockLoopByPlayerName = new HashMap<>();
        for ( final Map.Entry< String, Integer > entry : spawningPoolCompletionLoopByPlayerName.entrySet() )
            unlockLoopByPlayerName.put( entry.getKey(), Integer.valueOf( entry.getValue().intValue()
                    + INJECT_ELIGIBILITY_DELAY_LOOPS ) );
        return unlockLoopByPlayerName;
    }

    /**
     * Sorts idle-inject timelines by player, hatchery completion and tag text.
     */
    private List< HatcheryIdleInjectTimeline > sortIdleInjectTimelines( final List< HatcheryIdleInjectTimeline > idleTimelineList,
            final Map< Integer, HatcheryState > hatcheryByTag ) {
        final List< HatcheryIdleInjectTimeline > sorted = new ArrayList<>( idleTimelineList );
        Collections.sort( sorted, new Comparator< HatcheryIdleInjectTimeline >() {
            @Override
            public int compare( final HatcheryIdleInjectTimeline left, final HatcheryIdleInjectTimeline right ) {
                final int playerCompare = compareIgnoreCaseSafe( left.getPlayerName(), right.getPlayerName() );
                if ( playerCompare != 0 )
                    return playerCompare;
                final HatcheryState leftState = hatcheryByTag.get( Integer.valueOf( left.getHatcheryTag() ) );
                final HatcheryState rightState = hatcheryByTag.get( Integer.valueOf( right.getHatcheryTag() ) );
                final int completionCompare = compareCompletionLoops( leftState == null ? -1 : leftState.completionLoop,
                        rightState == null ? -1 : rightState.completionLoop );
                if ( completionCompare != 0 )
                    return completionCompare;
                return compareIgnoreCaseSafe( left.getHatcheryTagText(), right.getHatcheryTagText() );
            }
        } );
        return sorted;
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
                if ( playerCompare != 0 )
                    return playerCompare;

                final int completionCompare = compareCompletionLoops( left.completionLoop, right.completionLoop );
                if ( completionCompare != 0 )
                    return completionCompare;

                return compareIgnoreCaseSafe( left.hatcheryTagText, right.hatcheryTagText );
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
                if ( decision.injectWindow.getEndLoop() <= previousCandidate.endLoop ) {
                    overlapDiscardCount++;
                    diagnosticLineList.add( "Discarded inferred inject evidence at " + decision.evidenceTimeLabel + " because its retroactive 29-second window is fully contained by the earlier kept inferred window ending at "
                            + previousCandidate.evidenceTimeLabel + "." );
                    continue;
                }

                final int overlap = previousCandidate.endLoop - decision.injectWindow.getStartLoop();
                if ( overlap == 1 ) {
                    final int trimmedStartLoop = previousCandidate.endLoop;
                    final long trimmedStartMs = LarvaGameClock.loopToMs( trimmedStartLoop );
                    final String trimmedStartTimeLabel = LarvaGameClock.format( trimmedStartLoop );
                    final HatcheryInjectWindow trimmedWindow = new HatcheryInjectWindow(
                            decision.injectWindow.getEvidenceLoop(), decision.injectWindow.getEvidenceTimeLabel(), decision.injectWindow.getEvidenceKind(),
                            trimmedStartLoop, decision.injectWindow.getEndLoop(),
                            trimmedStartMs, decision.injectWindow.getEndMs(), trimmedStartTimeLabel, decision.injectWindow.getEndTimeLabel(),
                            true, decision.injectWindow.isTrimmedAtEnd(), decision.injectWindow.getDiagnosticNote() + " (trimmed by 1 loop to avoid overlap with the previous window ending at " + previousCandidate.evidenceTimeLabel + ")" );
                    diagnosticLineList.add( decision.diagnosticLine + " (trimmed by 1 loop to avoid overlap with the previous window ending at " + previousCandidate.evidenceTimeLabel + ")" );
                    injectWindowList.add( trimmedWindow );
                    trimmedWindowCount++;
                    previousCandidate = new InjectWindowCandidate( trimmedWindow.getEndLoop(), decision.evidenceTimeLabel, true );
                    continue;
                }

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
        final String evidenceTimeLabel = LarvaGameClock.format( evidenceLoop );
        final String burstStartTimeLabel = LarvaGameClock.format( burstEvidence.getFirstBirthLoop() );

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
            trimReasonList.add( "end trimmed to replay end at " + LarvaGameClock.format( replayEndLoop ) );
        }

        if ( effectiveEndLoop <= effectiveStartLoop ) {
                final String reason = trimReasonList.isEmpty() ? "its retroactive one-inject window collapsed outside the valid hatchery lifetime"
                    : "normalization collapsed the window after " + joinReasons( trimReasonList );
            return new InjectWindowDecision( null, "Discarded inferred inject evidence at " + evidenceTimeLabel + " because " + reason + '.', false,
                    evidenceTimeLabel );
        }

        final String startTimeLabel = LarvaGameClock.format( effectiveStartLoop );
        final String endTimeLabel = LarvaGameClock.format( effectiveEndLoop );
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
                LarvaGameClock.loopToMs( effectiveStartLoop ), LarvaGameClock.loopToMs( effectiveEndLoop ), startTimeLabel, endTimeLabel,
                trimmedAtStart, trimmedAtEnd, diagnosticBuilder.toString() );
        return new InjectWindowDecision( injectWindow, diagnosticBuilder.toString(), trimmedAtStart || trimmedAtEnd, evidenceTimeLabel );
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
     * Builds per-player Epic 12 phase tables.
     */
        private Map< String, LarvaPlayerPhaseTable > buildPlayerPhaseTables( final IRepProcessor repProc, final int replayEndLoop,
            final Map< String, List< LarvaPlayerResourceSnapshot > > resourceSnapshotsByPlayerName,
            final Map< String, Integer > spawningPoolCompletionLoopByPlayerName, final Map< Integer, HatcheryState > hatcheryByTag,
            final List< HatcheryInjectTimeline > injectTimelineList, final List< HatcheryIdleInjectTimeline > idleInjectTimelineList ) {
        final Map< String, LarvaPlayerPhaseTable > playerPhaseTableByPlayerName = new LinkedHashMap<>();
        if ( replayEndLoop <= 0 )
            return playerPhaseTableByPlayerName;

        final Set< String > playerNameSet = new LinkedHashSet<>();
        playerNameSet.addAll( resourceSnapshotsByPlayerName.keySet() );
        for ( final HatcheryState hatcheryState : hatcheryByTag.values() )
            if ( hatcheryState != null && hatcheryState.playerName != null && hatcheryState.playerName.length() > 0 )
                playerNameSet.add( hatcheryState.playerName );

        final Map< Integer, HatcheryInjectTimeline > injectTimelineByTag = indexInjectTimelinesByTag( injectTimelineList );
        final Map< Integer, HatcheryIdleInjectTimeline > idleInjectTimelineByTag = indexIdleInjectTimelinesByTag( idleInjectTimelineList );
        for ( final String playerName : playerNameSet ) {
            if ( playerName == null || playerName.length() == 0 )
                continue;

            final Map< LarvaGamePhase, LarvaPhaseInterval > phaseIntervalMap = LarvaPhaseIntervalResolver.resolve(
                    resourceSnapshotsByPlayerName.get( playerName ), replayEndLoop );
            final Map< LarvaGamePhase, LarvaPhaseStats > phaseStatsMap = buildPhaseStatsMap( repProc, playerName, replayEndLoop,
                    spawningPoolCompletionLoopByPlayerName.get( playerName ), hatcheryByTag, injectTimelineByTag, idleInjectTimelineByTag,
                    phaseIntervalMap );
            playerPhaseTableByPlayerName.put( playerName, new LarvaPlayerPhaseTable( playerName, phaseIntervalMap, phaseStatsMap ) );
        }

        return playerPhaseTableByPlayerName;
    }

    /**
     * Builds per-phase stats for one player.
     */
    private Map< LarvaGamePhase, LarvaPhaseStats > buildPhaseStatsMap( final IRepProcessor repProc, final String playerName, final int replayEndLoop,
            final Integer spawningPoolCompletionLoop, final Map< Integer, HatcheryState > hatcheryByTag,
            final Map< Integer, HatcheryInjectTimeline > injectTimelineByTag,
            final Map< Integer, HatcheryIdleInjectTimeline > idleInjectTimelineByTag,
            final Map< LarvaGamePhase, LarvaPhaseInterval > phaseIntervalMap ) {
        final Map< LarvaGamePhase, LarvaPhaseStats > phaseStatsMap = new LinkedHashMap<>();
        final int injectUnlockLoop = spawningPoolCompletionLoop == null ? -1 : spawningPoolCompletionLoop.intValue()
                + INJECT_ELIGIBILITY_DELAY_LOOPS;

        for ( final LarvaGamePhase phase : LarvaGamePhase.values() ) {
            final LarvaPhaseInterval interval = phaseIntervalMap.get( phase );
            int missedLarvaCount = 0;
            int missedInjectLarvaCount = 0;
            int totalSpawnedLarvaCount = 0;
            long hatchEligibleLoops = 0L;
            long injectEligibleLoops = 0L;
            long injectActiveLoops = 0L;

            for ( final HatcheryState hatcheryState : hatcheryByTag.values() ) {
                if ( hatcheryState == null || !playerName.equals( hatcheryState.playerName ) )
                    continue;

                final int hatchStartLoop = hatcheryState.completionLoop;
                final int hatchEndLoop = hatcheryState.destroyedLoop >= 0 ? Math.min( hatcheryState.destroyedLoop, replayEndLoop ) : replayEndLoop;
                if ( hatchStartLoop < 0 || hatchEndLoop <= hatchStartLoop )
                    continue;

                hatchEligibleLoops += intersectDuration( interval.getStartLoop(), interval.getEndLoop(), hatchStartLoop, hatchEndLoop );

                final HatcheryLarvaTimeline hatcheryTimeline = hatcheryState.toTimeline( repProc );
                final List< Integer > missedLarvaMarkerLoops = buildMissedLarvaMarkerLoops( hatcheryTimeline, replayEndLoop );
                missedLarvaCount += countLoopsInInterval( missedLarvaMarkerLoops, interval.getStartLoop(), interval.getEndLoop() );
                totalSpawnedLarvaCount += countLoopsInInterval( hatcheryState.assignedLarvaBirthLoopList, interval.getStartLoop(), interval.getEndLoop() );

                final HatcheryIdleInjectTimeline idleInjectTimeline = idleInjectTimelineByTag.get( Integer.valueOf( hatcheryState.hatcheryTag ) );
                missedInjectLarvaCount += countLoopsInInterval( buildMissedInjectMarkerLoops( idleInjectTimeline ), interval.getStartLoop(), interval.getEndLoop() )
                        * MISSED_INJECT_LARVA_PER_THRESHOLD;

                if ( injectUnlockLoop >= 0 ) {
                    final int effectiveInjectStartLoop = Math.max( hatchStartLoop, injectUnlockLoop );
                    injectEligibleLoops += intersectDuration( interval.getStartLoop(), interval.getEndLoop(), effectiveInjectStartLoop, hatchEndLoop );

                    final HatcheryInjectTimeline injectTimeline = injectTimelineByTag.get( Integer.valueOf( hatcheryState.hatcheryTag ) );
                    if ( injectTimeline != null )
                        for ( final HatcheryInjectWindow injectWindow : injectTimeline.getInjectWindowList() )
                            injectActiveLoops += intersectDuration( interval.getStartLoop(), interval.getEndLoop(),
                                    Math.max( injectWindow.getStartLoop(), effectiveInjectStartLoop ), Math.min( injectWindow.getEndLoop(), hatchEndLoop ) );
                }
            }

            phaseStatsMap.put( phase, new LarvaPhaseStats( phase, interval, missedLarvaCount, missedInjectLarvaCount,
                    totalSpawnedLarvaCount, hatchEligibleLoops, injectEligibleLoops, injectActiveLoops ) );
        }

        return phaseStatsMap;
    }

    /**
     * Builds chronological missed-larva marker loops for a hatchery.
     */
    private List< Integer > buildMissedLarvaMarkerLoops( final HatcheryLarvaTimeline hatcheryTimeline, final int replayEndLoop ) {
        final List< LarvaSaturationWindow > saturationWindowList = saturationWindowCalculator.buildWindows( hatcheryTimeline, replayEndLoop );
        final List< LarvaTimelineMarker > markerList = missedLarvaMarkerCalculator.buildMarkers( saturationWindowList );
        final List< Integer > markerLoopList = new ArrayList<>( markerList.size() );
        for ( final LarvaTimelineMarker marker : markerList )
            markerLoopList.add( Integer.valueOf( marker.getLoop() ) );
        return markerLoopList;
    }

    /**
     * Builds chronological missed-inject marker loops for one hatchery.
     */
    private List< Integer > buildMissedInjectMarkerLoops( final HatcheryIdleInjectTimeline idleInjectTimeline ) {
        if ( idleInjectTimeline == null || idleInjectTimeline.getIdleWindowList().isEmpty() )
            return Collections.emptyList();

        final List< HatcheryIdleInjectWindow > sortedWindowList = new ArrayList<>( idleInjectTimeline.getIdleWindowList() );
        Collections.sort( sortedWindowList, new Comparator< HatcheryIdleInjectWindow >() {
            @Override
            public int compare( final HatcheryIdleInjectWindow left, final HatcheryIdleInjectWindow right ) {
                if ( left.getStartLoop() != right.getStartLoop() )
                    return left.getStartLoop() < right.getStartLoop() ? -1 : 1;
                return left.getEndLoop() < right.getEndLoop() ? -1 : left.getEndLoop() == right.getEndLoop() ? 0 : 1;
            }
        } );

        final List< Integer > markerLoopList = new ArrayList<>();
        final int thresholdLoops = MISSED_INJECT_THRESHOLD_LOOPS;
        int accumulatedLoops = 0;
        int coveredUntilLoop = Integer.MIN_VALUE;
        for ( final HatcheryIdleInjectWindow idleWindow : sortedWindowList ) {
            if ( idleWindow == null )
                continue;

            final int effectiveStartLoop = Math.max( idleWindow.getStartLoop(), coveredUntilLoop );
            final int effectiveEndLoop = idleWindow.getEndLoop();
            if ( effectiveEndLoop <= effectiveStartLoop )
                continue;

            int loopsRemainingInWindow = effectiveEndLoop - effectiveStartLoop;
            int markerLoop = effectiveStartLoop;
            while ( accumulatedLoops + loopsRemainingInWindow >= thresholdLoops ) {
                final int loopsUntilThreshold = thresholdLoops - accumulatedLoops;
                markerLoop += loopsUntilThreshold;
                markerLoopList.add( Integer.valueOf( markerLoop ) );
                loopsRemainingInWindow -= loopsUntilThreshold;
                accumulatedLoops = 0;
            }

            accumulatedLoops += loopsRemainingInWindow;
            coveredUntilLoop = Math.max( coveredUntilLoop, effectiveEndLoop );
        }

        return markerLoopList;
    }

    /**
     * Counts event loops inside an interval.
     */
    private int countLoopsInInterval( final List< Integer > loopList, final int startLoop, final int endLoop ) {
        if ( loopList == null || loopList.isEmpty() || endLoop <= startLoop )
            return 0;

        int count = 0;
        for ( final Integer loop : loopList )
            if ( loop != null && loop.intValue() >= startLoop && loop.intValue() < endLoop )
                count++;
        return count;
    }

    /**
     * Intersects two half-open replay-loop intervals.
     */
    private int intersectDuration( final int leftStartLoop, final int leftEndLoop, final int rightStartLoop, final int rightEndLoop ) {
        final int effectiveStartLoop = Math.max( leftStartLoop, rightStartLoop );
        final int effectiveEndLoop = Math.min( leftEndLoop, rightEndLoop );
        return Math.max( 0, effectiveEndLoop - effectiveStartLoop );
    }

    /**
     * Indexes inject timelines by hatchery tag.
     */
    private Map< Integer, HatcheryInjectTimeline > indexInjectTimelinesByTag( final List< HatcheryInjectTimeline > injectTimelineList ) {
        final Map< Integer, HatcheryInjectTimeline > injectTimelineByTag = new HashMap<>();
        if ( injectTimelineList == null )
            return injectTimelineByTag;

        for ( final HatcheryInjectTimeline injectTimeline : injectTimelineList )
            if ( injectTimeline != null )
                injectTimelineByTag.put( Integer.valueOf( injectTimeline.getHatcheryTag() ), injectTimeline );

        return injectTimelineByTag;
    }

    /**
     * Indexes idle-inject timelines by hatchery tag.
     */
    private Map< Integer, HatcheryIdleInjectTimeline > indexIdleInjectTimelinesByTag( final List< HatcheryIdleInjectTimeline > idleInjectTimelineList ) {
        final Map< Integer, HatcheryIdleInjectTimeline > idleInjectTimelineByTag = new HashMap<>();
        if ( idleInjectTimelineList == null )
            return idleInjectTimelineByTag;

        for ( final HatcheryIdleInjectTimeline idleInjectTimeline : idleInjectTimelineList )
            if ( idleInjectTimeline != null )
                idleInjectTimelineByTag.put( Integer.valueOf( idleInjectTimeline.getHatcheryTag() ), idleInjectTimeline );

        return idleInjectTimelineByTag;
    }

    /**
     * Records the earliest Spawning Pool completion per player.
     */
    private void recordSpawningPoolCompletion( final Map< String, Integer > spawningPoolCompletionLoopByPlayerName, final String playerName,
            final int loop ) {
        if ( playerName == null || playerName.length() == 0 )
            return;

        final Integer previousLoop = spawningPoolCompletionLoopByPlayerName.get( playerName );
        if ( previousLoop == null || loop < previousLoop.intValue() )
            spawningPoolCompletionLoopByPlayerName.put( playerName, Integer.valueOf( loop ) );
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
     * Sorts hatcheries by completion loop, placing unknown completions last.
     */
    private int compareCompletionLoops( final int leftCompletionLoop, final int rightCompletionLoop ) {
        if ( leftCompletionLoop < 0 && rightCompletionLoop < 0 )
            return 0;
        if ( leftCompletionLoop < 0 )
            return 1;
        if ( rightCompletionLoop < 0 )
            return -1;
        return leftCompletionLoop < rightCompletionLoop ? -1 : leftCompletionLoop == rightCompletionLoop ? 0 : 1;
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
     * Tells if a tracker unit type represents a Spawning Pool.
     */
    private boolean isSpawningPoolLike( final String unitType ) {
        if ( unitType == null || unitType.length() == 0 )
            return false;

        final String normalizedUnitType = unitType.replace( "_", "" ).replace( " ", "" ).toLowerCase();
        return UNIT_SPAWNING_POOL.equals( unitType ) || "spawningpool".equals( normalizedUnitType );
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
                firstLarvaTimeLabel = LarvaGameClock.format( loop );
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
            completionTimeLabel = LarvaGameClock.format( loop );
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
            destroyedTimeLabel = LarvaGameClock.format( loop );
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
            final HatcheryLarvaTimeline.CountPoint point = new HatcheryLarvaTimeline.CountPoint( loop, LarvaGameClock.format( loop ), count );
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