package hu.aleks.larvasecondslostextmod.update;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

/**
 * Plain HTTP(S) GET with timeouts and a neutral User-Agent (E18-S08).
 *
 * <p>Sends no user data: only the module name and version are in the User-Agent. A timestamp query parameter is added so
 * GitHub Pages' cache (about 10 minutes) cannot hide a just-published module.xml.</p>
 */
public final class HttpReleaseDownloader implements ReleaseDownloader {

    private final String userAgent;

    /**
     * @param runningVersion the running module version, shown in the User-Agent
     */
    public HttpReleaseDownloader( final ModuleVersion runningVersion ) {
        this.userAgent = "LarvaSecondsLost/" + runningVersion;
    }

    @Override
    public InputStream open( final String url ) throws IOException {
        final URLConnection connection = new URL( withTimestamp( url ) ).openConnection();
        connection.setConnectTimeout( UpdateConfig.CONNECT_TIMEOUT_MS );
        connection.setReadTimeout( UpdateConfig.READ_TIMEOUT_MS );
        connection.setUseCaches( false );
        connection.setRequestProperty( "User-Agent", userAgent );
        if ( connection instanceof HttpURLConnection ) {
            final HttpURLConnection http = (HttpURLConnection) connection;
            http.setInstanceFollowRedirects( true );
            final int status = http.getResponseCode();
            if ( status != HttpURLConnection.HTTP_OK ) {
                http.disconnect();
                throw new IOException( "HTTP " + status + " from " + url );
            }
        }
        return connection.getInputStream();
    }

    static String withTimestamp( final String url ) {
        return url + ( url.indexOf( '?' ) < 0 ? '?' : '&' ) + "t=" + System.currentTimeMillis();
    }

}
