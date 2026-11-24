package hu.aleks.larvasecondslostextmod;

import hu.scelightapi.sc2.rep.model.details.IRace;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * Immutable per-player slice of a {@link LarvaAggregationGameRecord} (E17-S09, FR-9..FR-11).
 *
 * <p>Holds only plain values and the existing immutable {@link LarvaPhaseStats} objects. It keeps no reference to the
 * analysis report, the replay processor or the timeline model, so those can be garbage collected right after extraction.
 * Display times are converted to milliseconds at build time (C-9).</p>
 */
public final class LarvaAggregationPlayerRecord {

    private final String identityKey;

    private final String toonString;

    private final String name;

    private final IRace race;

    private final IRace opponentRace;

    private final LarvaMatchupBucket matchup;

    private final Map< LarvaGamePhase, LarvaPhaseStats > phaseStats;

    private final Map< LarvaGamePhase, Integer > phaseEndLoops;

    private final Map< LarvaGamePhase, Long > phaseEndDisplayMs;

    private final Map< LarvaMilestone, Long > milestoneDisplayMs;

    private final boolean missingPhaseTable;

    private LarvaAggregationPlayerRecord( final Builder b ) {
        identityKey = b.identityKey;
        toonString = b.toonString;
        name = b.name;
        race = b.race;
        opponentRace = b.opponentRace;
        matchup = b.matchup;
        phaseStats = Collections.unmodifiableMap( copy( LarvaGamePhase.class, b.phaseStats ) );
        phaseEndLoops = Collections.unmodifiableMap( copy( LarvaGamePhase.class, b.phaseEndLoops ) );
        phaseEndDisplayMs = Collections.unmodifiableMap( copy( LarvaGamePhase.class, b.phaseEndDisplayMs ) );
        milestoneDisplayMs = Collections.unmodifiableMap( copy( LarvaMilestone.class, b.milestoneDisplayMs ) );
        missingPhaseTable = b.missingPhaseTable;
    }

    private static < K extends Enum< K >, V > EnumMap< K, V > copy( final Class< K > keyType, final Map< K, V > source ) {
        final EnumMap< K, V > copy = new EnumMap<>( keyType );
        copy.putAll( source );
        return copy;
    }

    /** @return toon-based identity key (D-5, C-10), never <code>null</code> */
    public String getIdentityKey() {
        return identityKey;
    }

    /** @return display form of the toon (may be <code>null</code>) */
    public String getToonString() {
        return toonString;
    }

    /** @return player name in this replay */
    public String getName() {
        return name;
    }

    /** @return player race (Zerg for every record built from a replay) */
    public IRace getRace() {
        return race;
    }

    /** @return race of the other player in a 1v1, otherwise <code>null</code> (D-7) */
    public IRace getOpponentRace() {
        return opponentRace;
    }

    /**
     * @return matchup derived from the opponent race at extraction time (E17-S11): {@code ZVP}, {@code ZVT} or {@code ZVZ};
     *         <code>null</code> if unknown (counts in the All bucket only)
     */
    public LarvaMatchupBucket getMatchup() {
        return matchup;
    }

    /** @return unmodifiable raw phase stats per phase present in the phase table (empty if the table is missing) */
    public Map< LarvaGamePhase, LarvaPhaseStats > getPhaseStats() {
        return phaseStats;
    }

    /** @return unmodifiable loop at which each phase ended (CR-1.1); a phase that was not reached (missing interval or duration 0) is absent (D-9) */
    public Map< LarvaGamePhase, Integer > getPhaseEndLoops() {
        return phaseEndLoops;
    }

    /** @return unmodifiable game time in display milliseconds at which each phase ended (D-11, CR-1.1); same keys as the loop map */
    public Map< LarvaGamePhase, Long > getPhaseEndDisplayMs() {
        return phaseEndDisplayMs;
    }

    /** @return unmodifiable milestone time in display milliseconds; absent = not reached (D-10) */
    public Map< LarvaMilestone, Long > getMilestoneDisplayMs() {
        return milestoneDisplayMs;
    }

    /** @return <code>true</code> if the analysis produced no phase table for this player (OQ-15) */
    public boolean isMissingPhaseTable() {
        return missingPhaseTable;
    }

    /** Mutable builder (Java 7 style). Inputs are copied on {@link #build()}. */
    public static final class Builder {

        private String identityKey;

        private String toonString;

        private String name;

        private IRace race;

        private IRace opponentRace;

        private LarvaMatchupBucket matchup;

        private final Map< LarvaGamePhase, LarvaPhaseStats > phaseStats = new EnumMap<>( LarvaGamePhase.class );

        private final Map< LarvaGamePhase, Integer > phaseEndLoops = new EnumMap<>( LarvaGamePhase.class );

        private final Map< LarvaGamePhase, Long > phaseEndDisplayMs = new EnumMap<>( LarvaGamePhase.class );

        private final Map< LarvaMilestone, Long > milestoneDisplayMs = new EnumMap<>( LarvaMilestone.class );

        private boolean missingPhaseTable;

        public Builder identityKey( final String identityKey ) {
            this.identityKey = identityKey;
            return this;
        }

        public Builder toonString( final String toonString ) {
            this.toonString = toonString;
            return this;
        }

        public Builder name( final String name ) {
            this.name = name;
            return this;
        }

        public Builder race( final IRace race ) {
            this.race = race;
            return this;
        }

        public Builder opponentRace( final IRace opponentRace ) {
            this.opponentRace = opponentRace;
            return this;
        }

        /**
         * @param matchup
         *            {@code ZVP}, {@code ZVT}, {@code ZVZ} or null for unknown; {@code ALL} is rejected
         * @return this builder
         */
        public Builder matchup( final LarvaMatchupBucket matchup ) {
            if ( matchup == LarvaMatchupBucket.ALL )
                throw new IllegalArgumentException( "ALL is not a matchup." );
            this.matchup = matchup;
            return this;
        }

        public Builder phaseStats( final Map< LarvaGamePhase, LarvaPhaseStats > phaseStats ) {
            this.phaseStats.clear();
            if ( phaseStats != null )
                this.phaseStats.putAll( phaseStats );
            return this;
        }

        public Builder phaseEndLoops( final Map< LarvaGamePhase, Integer > phaseEndLoops ) {
            this.phaseEndLoops.clear();
            if ( phaseEndLoops != null )
                this.phaseEndLoops.putAll( phaseEndLoops );
            return this;
        }

        public Builder phaseEndDisplayMs( final Map< LarvaGamePhase, Long > phaseEndDisplayMs ) {
            this.phaseEndDisplayMs.clear();
            if ( phaseEndDisplayMs != null )
                this.phaseEndDisplayMs.putAll( phaseEndDisplayMs );
            return this;
        }

        public Builder milestoneDisplayMs( final Map< LarvaMilestone, Long > milestoneDisplayMs ) {
            this.milestoneDisplayMs.clear();
            if ( milestoneDisplayMs != null )
                this.milestoneDisplayMs.putAll( milestoneDisplayMs );
            return this;
        }

        public Builder missingPhaseTable( final boolean missingPhaseTable ) {
            this.missingPhaseTable = missingPhaseTable;
            return this;
        }

        /**
         * @return the immutable record
         * @throws IllegalStateException if identity key or name is missing
         */
        public LarvaAggregationPlayerRecord build() {
            if ( identityKey == null || identityKey.length() == 0 )
                throw new IllegalStateException( "Identity key is required." );
            if ( name == null )
                throw new IllegalStateException( "Player name is required." );
            return new LarvaAggregationPlayerRecord( this );
        }
    }
}
