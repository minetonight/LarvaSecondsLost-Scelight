package hu.aleks.larvasecondslostextmod;

import java.nio.file.Path;

/**
 * Pure helpers for the identity and the label of single-replay sub-pages (OQ-6).
 */
final class LarvaReplayPaths {

    private static final String REPLAY_EXTENSION = ".SC2Replay";

    private LarvaReplayPaths() {
    }

    /**
     * Returns the dedupe key of a replay: absolute and normalized. No manual lower-casing: on Windows
     * {@link Path#equals} is already case-insensitive, on Linux it is not and must not be.
     *
     * @param replayFile replay file; not <code>null</code>
     * @return the absolute, normalized path
     */
    static Path normalize( final Path replayFile ) {
        return replayFile.toAbsolutePath().normalize();
    }

    /**
     * Returns the tree label of a replay: the file name without a trailing {@code .SC2Replay} (case-insensitive).
     * Other extensions stay untouched; a name that is only the extension is kept as is (labels must not be empty).
     *
     * @param replayFile replay file; not <code>null</code>
     * @return the node name
     */
    static String nodeName( final Path replayFile ) {
        final Path fileNamePath = replayFile.getFileName();
        return nodeName( fileNamePath == null ? replayFile.toString() : fileNamePath.toString() );
    }

    /**
     * Returns the tree label for a plain file name, see {@link #nodeName(Path)}.
     *
     * @param fileName file name; not <code>null</code>
     * @return the node name
     */
    static String nodeName( final String fileName ) {
        final int extensionLength = REPLAY_EXTENSION.length();
        final int length = fileName.length();
        if ( length > extensionLength && fileName.regionMatches( true, length - extensionLength, REPLAY_EXTENSION, 0, extensionLength ) )
            return fileName.substring( 0, length - extensionLength );
        return fileName;
    }

}
