package hu.aleks.larvasecondslostextmod;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Scriptable {@link LarvaAggregationSource} for pipeline and runner tests. Not a test class itself. */
final class LarvaFakeAggregationSource implements LarvaAggregationSource {

    final Map< String, LarvaAggregationGameRecord > metadata = new HashMap<>();
    final Map< String, LarvaAggregationGameRecord > full = new HashMap<>();
    final Map< String, RuntimeException > metadataFailures = new HashMap<>();
    final Map< String, RuntimeException > fullFailures = new HashMap<>();
    final List< String > metadataCalls = new ArrayList<>();
    final List< String > fullCalls = new ArrayList<>();

    /** Called at the start of every {@link #analyze}, e.g. to cancel a token or block. */
    Runnable onAnalyze;

    /** Registers the same record for the cheap and the full path. */
    LarvaFakeAggregationSource add( final String name, final LarvaAggregationGameRecord record ) {
        metadata.put( name, record );
        full.put( name, record );
        return this;
    }

    @Override
    public LarvaAggregationGameRecord readMetadata( final Path replayFile ) {
        final String name = replayFile.getFileName().toString();
        metadataCalls.add( name );
        if ( metadataFailures.containsKey( name ) )
            throw metadataFailures.get( name );
        return metadata.get( name );
    }

    @Override
    public LarvaAggregationGameRecord analyze( final Path replayFile ) {
        final String name = replayFile.getFileName().toString();
        fullCalls.add( name );
        if ( onAnalyze != null )
            onAnalyze.run();
        if ( fullFailures.containsKey( name ) )
            throw fullFailures.get( name );
        return full.get( name );
    }
}
