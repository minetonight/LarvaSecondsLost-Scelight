package hu.aleks.larvasecondslostextmod.update;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import hu.scelightapibase.service.log.ILogger;

/**
 * The TLS socket factory of the update connections: trusts what Java trusts <em>and</em> what the operating system trusts
 * (see {@link SystemTrustStores}). It is used only for the update's own connections; Scelight's global TLS settings are
 * not changed.
 */
final class UpdateTrust {

    private static SSLSocketFactory cached;
    private static boolean built;

    private UpdateTrust() {
    }

    /**
     * Built once and reused. If it cannot be built the reason is logged and <code>null</code> is returned, so the caller
     * keeps Java's default.
     *
     * @return the combined socket factory, or <code>null</code> to use Java's default
     */
    static synchronized SSLSocketFactory socketFactory( final ILogger logger ) {
        if ( !built ) {
            built = true;
            try {
                cached = build( new SystemTrustStores( logger ).load( System.getProperty( "os.name" ) ) );
            } catch ( final GeneralSecurityException | IOException e ) {
                logger.warning( "[larva-update] cannot add the system trust store, using Java's own: " + e, e );
            }
        }
        return cached;
    }

    /**
     * @param extraRoots root certificates trusted in addition to Java's own
     * @return a socket factory trusting Java's roots and <code>extraRoots</code>
     */
    static SSLSocketFactory build( final Collection< X509Certificate > extraRoots ) throws GeneralSecurityException, IOException {
        final Set< X509Certificate > roots = new LinkedHashSet<>( Arrays.asList( javaDefaultRoots() ) );
        roots.addAll( extraRoots );
        final KeyStore store = KeyStore.getInstance( KeyStore.getDefaultType() );
        store.load( null, null );
        int index = 0;
        for ( final X509Certificate root : roots )
            store.setCertificateEntry( "root-" + index++, root );
        final TrustManagerFactory factory = TrustManagerFactory.getInstance( TrustManagerFactory.getDefaultAlgorithm() );
        factory.init( store );
        final SSLContext context = SSLContext.getInstance( "TLS" );
        context.init( null, factory.getTrustManagers(), null );
        return context.getSocketFactory();
    }

    /** @return the roots of Java's own trust store (cacerts, or the javax.net.ssl.trustStore override) */
    static X509Certificate[] javaDefaultRoots() throws GeneralSecurityException {
        final TrustManagerFactory factory = TrustManagerFactory.getInstance( TrustManagerFactory.getDefaultAlgorithm() );
        factory.init( (KeyStore) null );
        for ( final TrustManager manager : factory.getTrustManagers() )
            if ( manager instanceof X509TrustManager )
                return ( (X509TrustManager) manager ).getAcceptedIssuers();
        return new X509Certificate[ 0 ];
    }

}
