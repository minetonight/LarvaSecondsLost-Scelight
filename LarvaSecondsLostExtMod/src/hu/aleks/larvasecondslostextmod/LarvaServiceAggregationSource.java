package hu.aleks.larvasecondslostextmod;

import java.nio.file.Path;

/** {@link LarvaAggregationSource} on top of one {@link ReplaySummaryService} (not thread-safe: one instance per worker). */
final class LarvaServiceAggregationSource implements LarvaAggregationSource {

    static final String SOURCE_DESCRIPTION = "Multi analysis";

    private final ReplaySummaryService service;

    LarvaServiceAggregationSource( final ReplaySummaryService service ) {
        this.service = service;
    }

    @Override
    public LarvaAggregationGameRecord readMetadata( final Path replayFile ) {
        return service.readMetadataForAggregation( replayFile );
    }

    @Override
    public LarvaAggregationGameRecord analyze( final Path replayFile ) {
        return service.analyzeForAggregation( replayFile, SOURCE_DESCRIPTION );
    }
}
