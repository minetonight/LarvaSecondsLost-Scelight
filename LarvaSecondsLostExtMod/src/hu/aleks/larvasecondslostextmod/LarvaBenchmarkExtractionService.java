package hu.aleks.larvasecondslostextmod;

import java.nio.file.Path;

/**
 * Headless extraction seam for Epic 13 benchmark replay exports.
 */
public class LarvaBenchmarkExtractionService {

    /** Underlying replay-analysis service. */
    private final ReplaySummaryService replaySummaryService;

    /** Deterministic formatter for validation snapshots. */
    private final LarvaBenchmarkRecordFormatter recordFormatter = new LarvaBenchmarkRecordFormatter();

    /**
     * Creates a new benchmark extraction service.
     *
     * @param replaySummaryService shared replay-analysis service
     */
    public LarvaBenchmarkExtractionService( final ReplaySummaryService replaySummaryService ) {
        if ( replaySummaryService == null )
            throw new IllegalArgumentException( "Replay summary service is required." );
        this.replaySummaryService = replaySummaryService;
    }

    /**
     * Extracts a headless benchmark replay record.
     *
     * @param replayFile replay file to analyze
     * @param sourceDescription short description of how the replay was selected
     * @return benchmark replay record
     */
    public LarvaBenchmarkReplayRecord extract( final Path replayFile, final String sourceDescription ) {
        return replaySummaryService.analyzeBenchmarkReplay( replayFile, sourceDescription );
    }

    /**
     * Formats a replay record into deterministic validation text.
     *
     * @param replayRecord replay record to format
     * @return deterministic validation text
     */
    public String format( final LarvaBenchmarkReplayRecord replayRecord ) {
        return recordFormatter.format( replayRecord );
    }

}
