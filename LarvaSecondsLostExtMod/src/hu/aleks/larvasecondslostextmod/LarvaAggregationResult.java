package hu.aleks.larvasecondslostextmod;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Immutable result of a multi-replay aggregation (E17-S11). Produced by {@link LarvaAggregator}. */
public final class LarvaAggregationResult {

    private final List< PlayerResult > players;

    private final int distinctGameCount;

    private final Date startDate;

    private final Date endDate;

    private final LarvaFilterSummary filterSummary;

    private final List< LarvaSkippedReplay > skipped;

    LarvaAggregationResult( final List< PlayerResult > players, final int distinctGameCount, final Date startDate, final Date endDate,
            final LarvaFilterSummary filterSummary, final List< LarvaSkippedReplay > skipped ) {
        this.players = Collections.unmodifiableList( new ArrayList<>( players ) );
        this.distinctGameCount = distinctGameCount;
        this.startDate = startDate == null ? null : new Date( startDate.getTime() );
        this.endDate = endDate == null ? null : new Date( endDate.getTime() );
        this.filterSummary = filterSummary;
        this.skipped = Collections.unmodifiableList( new ArrayList<>( skipped ) );
    }

    /** @return per-player results in tracked order (#1 first) */
    public List< PlayerResult > getPlayers() {
        return players;
    }

    /** @return header n: distinct eligible games that contributed at least one tracked player-game */
    public int getDistinctGameCount() {
        return distinctGameCount;
    }

    /** @return earliest replay date of the eligible games, or null (AC-7.2) */
    public Date getStartDate() {
        return startDate == null ? null : new Date( startDate.getTime() );
    }

    /** @return latest replay date of the eligible games, or null (AC-7.2) */
    public Date getEndDate() {
        return endDate == null ? null : new Date( endDate.getTime() );
    }

    public LarvaFilterSummary getFilterSummary() {
        return filterSummary;
    }

    /** @return unmodifiable skipped list (file, reason) */
    public List< LarvaSkippedReplay > getSkipped() {
        return skipped;
    }

    /** Immutable per-player section: counts, phase cells and milestone cells per bucket. */
    public static final class PlayerResult {

        private final LarvaTrackedPlayer player;

        private final Map< LarvaMatchupBucket, Integer > gameCounts;

        private final Map< LarvaMatchupBucket, Map< LarvaGamePhase, LarvaAggregatedPhaseCell > > phaseCells;

        private final Map< LarvaMatchupBucket, Map< LarvaMilestone, LarvaAggregatedMilestoneCell > > milestoneCells;

        PlayerResult( final LarvaTrackedPlayer player, final Map< LarvaMatchupBucket, Integer > gameCounts,
                final Map< LarvaMatchupBucket, Map< LarvaGamePhase, LarvaAggregatedPhaseCell > > phaseCells,
                final Map< LarvaMatchupBucket, Map< LarvaMilestone, LarvaAggregatedMilestoneCell > > milestoneCells ) {
            this.player = player;
            this.gameCounts = Collections.unmodifiableMap( new EnumMap<>( gameCounts ) );
            this.phaseCells = Collections.unmodifiableMap( new EnumMap<>( phaseCells ) );
            this.milestoneCells = Collections.unmodifiableMap( new EnumMap<>( milestoneCells ) );
        }

        public LarvaTrackedPlayer getPlayer() {
            return player;
        }

        /** @return player-games in the bucket (0 for an empty matchup, OQ-4) */
        public int getGameCount( final LarvaMatchupBucket bucket ) {
            final Integer count = gameCounts.get( bucket );
            return count == null ? 0 : count.intValue();
        }

        /** @return the cell; never null, but its values are null when there is no data */
        public LarvaAggregatedPhaseCell getPhaseCell( final LarvaMatchupBucket bucket, final LarvaGamePhase phase ) {
            return phaseCells.get( bucket ).get( phase );
        }

        /** @return the cell; never null, but its mean is null when no game reached the milestone */
        public LarvaAggregatedMilestoneCell getMilestoneCell( final LarvaMatchupBucket bucket, final LarvaMilestone milestone ) {
            return milestoneCells.get( bucket ).get( milestone );
        }
    }
}
