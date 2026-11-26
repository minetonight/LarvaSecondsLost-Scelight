package hu.aleks.larvasecondslostextmod.update;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;

import hu.scelightapibase.service.log.ILogger;

/**
 * Plain HTTP(S) GET with timeouts and a neutral User-Agent (E18-S08).
 *
 * <p>Sends no user data: only the module name and version are in the User-Agent. A timestamp query parameter is added so
 * GitHub Pages' cache (about 10 minutes) cannot hide a just-published module.xml. HTTPS connections trust the operating
 * system's root certificates too ({@link UpdateTrust}), so an old bundled Java can still verify the site.</p>
 */
public final class HttpReleaseDownloader implements ReleaseDownloader {

    private final String userAgent;
    private final ILogger logger;

    /**
     * @param runningVersion the running module version, shown in the User-Agent
     * @param logger logs where the trusted certificates come from
     */
    public HttpReleaseDownloader( final ModuleVersion runningVersion, final ILogger logger ) {
        this.userAgent = "LarvaSecondsLost/" + runningVersion;
        this.logger = logger;
    }

    @Override
    public InputStream open( final String url ) throws IOException {
        final URLConnection connection = new URL( withTimestamp( url ) ).openConnection();
        connection.setConnectTimeout( UpdateConfig.CONNECT_TIMEOUT_MS );
        connection.setReadTimeout( UpdateConfig.READ_TIMEOUT_MS );
        connection.setUseCaches( false );
        connection.setRequestProperty( "User-Agent", userAgent );
        if ( connection instanceof HttpsURLConnection ) {
            final SSLSocketFactory factory = UpdateTrust.socketFactory( logger );
            if ( factory != null )
                ( (HttpsURLConnection) connection ).setSSLSocketFactory( factory );
        }
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
