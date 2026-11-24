package hu.aleks.larvasecondslostextmod;

import hu.scelightapi.sc2.rep.model.attributesevents.IGameMode;
import hu.scelightapi.sc2.rep.repproc.IFormat;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Immutable per-replay aggregation record (E17-S09, FR-9, FR-10, D-8).
 *
 * <p>Small enough to keep hundreds in memory; it keeps no reference to the analysis report, replay processor or timeline
 * model. Filtering (S10) and aggregation (S11) work on this class only. The record does not decide eligibility: the
 * length is stored as loops and compared against the minimum game length by S10.</p>
 */
public final class LarvaAggregationGameRecord {

    private final Path replayFile;

    private final Date replayDate;

    private final IGameMode gameMode;

    private final IFormat format;

    private final boolean oneVsOne;

    private final boolean ladder;

    private final int lengthLoops;

    private final boolean fullParse;

    private final List< LarvaAggregationPlayerRecord > zergPlayers;

    private final List< String > diagnosticFlags;

    private LarvaAggregationGameRecord( final Builder b ) {
        replayFile = b.replayFile;
        replayDate = b.replayDate == null ? null : new Date( b.replayDate.getTime() );
        gameMode = b.gameMode;
        format = b.format;
        oneVsOne = b.oneVsOne;
        ladder = b.ladder;
        lengthLoops = b.lengthLoops;
        fullParse = b.fullParse;
        zergPlayers = Collections.unmodifiableList( new ArrayList<>( b.zergPlayers ) );
        diagnosticFlags = Collections.unmodifiableList( new ArrayList<>( b.diagnosticFlags ) );
    }

    /** @return replay file */
    public Path getReplayFile() {
        return replayFile;
    }

    /** @return copy of the in-game replay end time from the replay details (FR-9), or <code>null</code> */
    public Date getReplayDate() {
        return replayDate == null ? null : new Date( replayDate.getTime() );
    }

    /** @return game mode; <code>null</code> is treated as non-ladder */
    public IGameMode getGameMode() {
        return gameMode;
    }

    /** @return replay format (1v1, 2v2, ...) */
    public IFormat getFormat() {
        return format;
    }

    /**
     * @return <code>true</code> if the replay format is 1v1. Stored as a plain flag (set where the SDK format is read,
     *         see {@link LarvaGameKinds}) so that filtering does not need the Scelight runtime.
     */
    public boolean isOneVsOne() {
        return oneVsOne;
    }

    /** @return <code>true</code> if the game mode is a ladder (AMM) game; <code>false</code> for null or unknown modes */
    public boolean isLadder() {
        return ladder;
    }

    /** @return game length in loops (compare with the 4 minute minimum in S10) */
    public int getLengthLoops() {
        return lengthLoops;
    }

    /** @return <code>true</code> if a full analysis ran; <code>false</code> for a metadata-only record */
    public boolean isFullParse() {
        return fullParse;
    }

    /** @return unmodifiable Zerg players in replay order (empty if none, or for metadata-only records without Zerg) */
    public List< LarvaAggregationPlayerRecord > getZergPlayers() {
        return zergPlayers;
    }

    /** @return unmodifiable replay-level diagnostic flags */
    public List< String > getDiagnosticFlags() {
        return diagnosticFlags;
    }

    /** Mutable builder (Java 7 style). Inputs are copied on {@link #build()}. */
    public static final class Builder {

        private Path replayFile;

        private Date replayDate;

        private IGameMode gameMode;

        private IFormat format;

        private boolean oneVsOne;

        private boolean ladder;

        private int lengthLoops;

        private boolean fullParse;

        private final List< LarvaAggregationPlayerRecord > zergPlayers = new ArrayList<>();

        private final List< String > diagnosticFlags = new ArrayList<>();

        public Builder replayFile( final Path replayFile ) {
            this.replayFile = replayFile;
            return this;
        }

        public Builder replayDate( final Date replayDate ) {
            this.replayDate = replayDate;
            return this;
        }

        public Builder gameMode( final IGameMode gameMode ) {
            this.gameMode = gameMode;
            return this;
        }

        public Builder format( final IFormat format ) {
            this.format = format;
            return this;
        }

        public Builder oneVsOne( final boolean oneVsOne ) {
            this.oneVsOne = oneVsOne;
            return this;
        }

        public Builder ladder( final boolean ladder ) {
            this.ladder = ladder;
            return this;
        }

        public Builder lengthLoops( final int lengthLoops ) {
            this.lengthLoops = lengthLoops;
            return this;
        }

        public Builder fullParse( final boolean fullParse ) {
            this.fullParse = fullParse;
            return this;
        }

        public Builder addZergPlayer( final LarvaAggregationPlayerRecord player ) {
            if ( player == null )
                throw new IllegalArgumentException( "Player record is required." );
            zergPlayers.add( player );
            return this;
        }

        public Builder diagnosticFlags( final List< String > flags ) {
            diagnosticFlags.clear();
            if ( flags != null )
                diagnosticFlags.addAll( flags );
            return this;
        }

        /**
         * @return the immutable record
         * @throws IllegalStateException if the replay file is missing
         */
        public LarvaAggregationGameRecord build() {
            if ( replayFile == null )
                throw new IllegalStateException( "Replay file is required." );
            return new LarvaAggregationGameRecord( this );
        }
    }
}
