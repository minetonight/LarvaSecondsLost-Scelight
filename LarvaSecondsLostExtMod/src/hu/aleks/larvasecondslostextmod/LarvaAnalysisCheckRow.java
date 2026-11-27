package hu.aleks.larvasecondslostextmod;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * One CSV row of {@link LarvaAnalysisCheckCli}: one Zerg player of one replay. Times are display seconds (as the module shows
 * them), {@code -1} means "not reached".
 */
final class LarvaAnalysisCheckRow {

    private static final int[] PHASE_THRESHOLDS = { LarvaGamePhase.MID.getPromotionWorkerThreshold(),
            LarvaGamePhase.LATE.getPromotionWorkerThreshold(), LarvaGamePhase.END.getPromotionWorkerThreshold() };
    private static final LarvaGamePhase[] PROMOTED_PHASES = { LarvaGamePhase.MID, LarvaGamePhase.LATE, LarvaGamePhase.END };
    private static final LarvaMilestone[] DRONE_MILESTONES = { LarvaMilestone.DRONES_66, LarvaMilestone.DRONES_80 };

    final String file;
    final String matchup;
    final String player;
    private final LarvaAnalysisReport report;

    int lengthSec;
    int waMax;
    int moduleMax;
    int maxAbsDiff;
    int diffSamples;
    int samples;
    final int[] phaseWa = { -1, -1, -1 };
    final int[] phaseModule = { -1, -1, -1 };
    final int[] milestoneWa = { -1, -1 };
    final int[] milestoneModule = { -1, -1 };

    int queens;
    int tumors;
    int injectWindows;
    int redWindows;
    double redSec;
    double redSecTumor;
    double redSecFarTumor;
    int energyBound;
    int missedInjects;
    /** 1 if the module followed the player's queen orders (enough injects had a Spawn Larva command with queens selected). */
    int ordersFollowed;

    LarvaAnalysisCheckRow( final String file, final String matchup, final String player, final LarvaAnalysisReport report ) {
        this.file = file;
        this.matchup = matchup;
        this.player = player;
        this.report = report;
        this.lengthSec = seconds( report.getReplayLengthLoops() );
    }

    /** Compares the module's worker values, phases and milestones with the raw {@code workersActiveCount} samples. */
    void fillDrones( final List< int[] > workerSamples, final List< LarvaPlayerResourceSnapshot > snapshots,
            final LarvaAggregationPlayerRecord record ) {
        final Map< Integer, Integer > moduleByLoop = new HashMap<>();
        if ( snapshots != null )
            for ( final LarvaPlayerResourceSnapshot snapshot : snapshots ) {
                final int workers = snapshot.getWorkersActiveCount() == null ? 0 : snapshot.getWorkersActiveCount().intValue();
                moduleByLoop.put( Integer.valueOf( snapshot.getLoop() ), Integer.valueOf( workers ) );
                moduleMax = Math.max( moduleMax, workers );
            }
        for ( final int[] sample : workerSamples ) {
            samples++;
            waMax = Math.max( waMax, sample[ 1 ] );
            final Integer module = moduleByLoop.get( Integer.valueOf( sample[ 0 ] ) );
            final int diff = Math.abs( ( module == null ? 0 : module.intValue() ) - sample[ 1 ] );
            maxAbsDiff = Math.max( maxAbsDiff, diff );
            if ( diff != 0 )
                diffSamples++;
            for ( int i = 0; i < PHASE_THRESHOLDS.length; i++ )
                if ( phaseWa[ i ] < 0 && sample[ 1 ] > PHASE_THRESHOLDS[ i ] )
                    phaseWa[ i ] = seconds( sample[ 0 ] );
            for ( int i = 0; i < DRONE_MILESTONES.length; i++ )
                if ( milestoneWa[ i ] < 0 && sample[ 1 ] >= DRONE_MILESTONES[ i ].getThreshold() )
                    milestoneWa[ i ] = seconds( sample[ 0 ] );
        }
        fillModulePhases();
        if ( record != null )
            for ( int i = 0; i < DRONE_MILESTONES.length; i++ ) {
                final Integer loop = record.getMilestoneLoops().get( DRONE_MILESTONES[ i ] );
                milestoneModule[ i ] = loop == null ? -1 : seconds( loop.intValue() );
            }
    }

    private void fillModulePhases() {
        final LarvaPlayerPhaseTable table = report.getPlayerPhaseTable( player );
        if ( table == null )
            return;
        for ( int i = 0; i < PROMOTED_PHASES.length; i++ ) {
            final LarvaPhaseInterval interval = table.getPhaseInterval( PROMOTED_PHASES[ i ] );
            if ( interval != null && interval.getStartLoop() < report.getReplayLengthLoops() )
                phaseModule[ i ] = seconds( interval.getStartLoop() );
        }
    }

    /** Red (missed-inject) windows of the module against queen tumors and the energy bound. */
    void fillQueens( final LarvaAnalysisCheckCli.Facts facts, final int playerId ) {
        final List< int[] > tumorList = facts.of( facts.tumorsByPlayerId, playerId );
        tumors = tumorList.size();
        final List< int[] > queenList = facts.of( facts.queensByPlayerId, playerId );
        queens = queenList.size();
        for ( final HatcheryInjectTimeline timeline : report.getInjectTimelineList() )
            if ( player.equals( timeline.getPlayerName() ) )
                injectWindows += timeline.getInjectWindowList().size();
        for ( final HatcheryIdleInjectTimeline timeline : report.getIdleInjectTimelineList() )
            if ( player.equals( timeline.getPlayerName() ) ) {
                addRedWindows( timeline, tumorList, facts.hatcheryXyByTag.get( Integer.valueOf( timeline.getHatcheryTag() ) ) );
                for ( final String line : timeline.getDiagnosticLineList() )
                    if ( line.endsWith( "queen orders followed." ) )
                        ordersFollowed = 1;
            }
        energyBound = energyBound( queenList, facts.lastLoop );
        final LarvaPlayerPhaseTable table = report.getPlayerPhaseTable( player );
        if ( table != null )
            for ( final LarvaPhaseStats stats : table.getPhaseStatsMap().values() )
                missedInjects += stats.getMissedInjectLarvaCount() / 3;
    }

    private void addRedWindows( final HatcheryIdleInjectTimeline timeline, final List< int[] > tumorList, final int[] hatchXy ) {
        for ( final HatcheryIdleInjectWindow window : timeline.getIdleWindowList() ) {
            redWindows++;
            final double sec = ( report.loopToTimeMs( window.getEndLoop() ) - report.loopToTimeMs( window.getStartLoop() ) ) / 1000.0d;
            redSec += sec;
            boolean tumorDuring = false;
            boolean farTumorDuring = false;
            for ( final int[] tumor : tumorList ) {
                if ( tumor[ 0 ] < window.getStartLoop() - LarvaAnalysisCheckCli.TUMOR_LOOKBACK_LOOPS || tumor[ 0 ] > window.getEndLoop() )
                    continue;
                tumorDuring = true;
                if ( hatchXy != null && Math.hypot( tumor[ 1 ] - hatchXy[ 0 ], tumor[ 2 ] - hatchXy[ 1 ] ) >= LarvaAnalysisCheckCli.FAR_TUMOR_CELLS )
                    farTumorDuring = true;
            }
            if ( tumorDuring )
                redSecTumor += sec;
            if ( farTumorDuring )
                redSecFarTumor += sec;
        }
    }

    /** Most injects the player could have missed: all queen energy minus injects and tumors, in 25-energy units (no cap). */
    private int energyBound( final List< int[] > queenList, final int lastLoop ) {
        double energy = 0.0d;
        for ( final int[] queen : queenList )
            energy += 25.0d + ( ( queen[ 1 ] < 0 ? lastLoop : queen[ 1 ] ) - queen[ 0 ] ) * LarvaAnalysisCheckCli.QUEEN_ENERGY_PER_LOOP;
        energy -= 25.0d * ( injectWindows + tumors );
        return (int) Math.max( 0.0d, Math.floor( energy / 25.0d ) );
    }

    private int seconds( final int loop ) {
        return (int) ( report.loopToTimeMs( loop ) / 1000L );
    }

    String toCsv() {
        final StringBuilder b = new StringBuilder();
        b.append( '"' ).append( file.replace( "\"", "'" ) ).append( "\"," ).append( matchup ).append( ",\"" )
                .append( player.replace( "\"", "'" ) ).append( "\"," );
        append( b, lengthSec, waMax, moduleMax, maxAbsDiff, diffSamples, samples );
        append( b, phaseWa[ 0 ], phaseWa[ 1 ], phaseWa[ 2 ], phaseModule[ 0 ], phaseModule[ 1 ], phaseModule[ 2 ] );
        append( b, milestoneWa[ 0 ], milestoneWa[ 1 ], milestoneModule[ 0 ], milestoneModule[ 1 ] );
        append( b, queens, tumors, injectWindows, redWindows );
        b.append( LarvaAnalysisCheckCli.fmt( redSec ) ).append( ',' ).append( LarvaAnalysisCheckCli.fmt( redSecTumor ) ).append( ',' )
                .append( LarvaAnalysisCheckCli.fmt( redSecFarTumor ) ).append( ',' ).append( energyBound ).append( ',' ).append( missedInjects ).append( ',' ).append( ordersFollowed );
        return b.toString();
    }

    private static void append( final StringBuilder b, final int... values ) {
        for ( final int value : values )
            b.append( value ).append( ',' );
    }
}
