package hu.aleks.larvasecondslostextmod.update;

import java.io.IOException;
import java.io.InputStream;

/**
 * Opens a download stream. Separated from the logic so tests can serve files without a network.
 */
public interface ReleaseDownloader {

    /**
     * @param url the URL to download
     * @return the content stream; the caller closes it
     * @throws IOException if the URL cannot be opened or answers with an HTTP error
     */
    InputStream open( String url ) throws IOException;

}
