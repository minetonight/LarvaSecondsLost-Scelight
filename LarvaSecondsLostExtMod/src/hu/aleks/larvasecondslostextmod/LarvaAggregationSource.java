package hu.aleks.larvasecondslostextmod;

import java.nio.file.Path;

/**
 * Where the batch pipeline gets per-replay records from (E17-S12 seam). Production: {@link LarvaServiceAggregationSource};
 * unit tests use fakes. Implementations are used by one worker thread only.
 */
interface LarvaAggregationSource {

    /**
     * Cheap metadata record for the pre-filter (S02).
     *
     * @param replayFile replay file
     * @return metadata-only record, or <code>null</code> if the replay cannot be parsed
     * @throws RuntimeException for any per-file failure
     */
    LarvaAggregationGameRecord readMetadata( Path replayFile );

    /**
     * Full analysis, reduced to the compact record.
     *
     * @param replayFile replay file
     * @return the record, or <code>null</code> if the replay cannot be parsed
     * @throws RuntimeException for any per-file failure
     */
    LarvaAggregationGameRecord analyze( Path replayFile );
}
