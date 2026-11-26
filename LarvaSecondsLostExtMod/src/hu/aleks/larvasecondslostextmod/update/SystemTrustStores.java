package hu.aleks.larvasecondslostextmod.update;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;

import hu.scelightapibase.service.log.ILogger;

/**
 * Reads the root certificates the operating system trusts (E18 bug fix: old bundled Java).
 *
 * <p>Scelight ships Java 1.8.0_60, whose own trust store predates ISRG Root X1 (added in 8u141), so it cannot verify
 * GitHub Pages' Let's Encrypt certificate. The operating system's store is kept current by the OS, so trusting it too
 * makes the update work on that Java. Sources: Windows "Windows-ROOT"; macOS Keychain plus <code>/etc/ssl/cert.pem</code>;
 * Linux/BSD the distribution's PEM bundle. A source that is missing or unreadable is logged and skipped.</p>
 */
final class SystemTrustStores {

    /** Keystore types of the OS store: Windows, macOS system roots (Java 23+), macOS user keychain. */
    private static final String WINDOWS_ROOT = "Windows-ROOT";
    private static final String[] MAC_KEYSTORES = { "KeychainStore-ROOT", "KeychainStore" };

    /** Where Linux distributions (Debian/Ubuntu, Fedora/RHEL, openSUSE, Alpine) and macOS keep their PEM bundle. */
    static final String[] PEM_BUNDLES = { "/etc/ssl/certs/ca-certificates.crt", "/etc/pki/ca-trust/extracted/pem/tls-ca-bundle.pem",
            "/etc/pki/tls/certs/ca-bundle.crt", "/etc/ssl/ca-bundle.pem", "/etc/ssl/cert.pem" };

    private final ILogger logger;

    SystemTrustStores( final ILogger logger ) {
        this.logger = logger;
    }

    /**
     * @param osName value of the <code>os.name</code> system property
     * @return the OS-trusted root certificates; empty if none could be read
     */
    List< X509Certificate > load( final String osName ) {
        final List< X509Certificate > certs = new ArrayList<>();
        final String os = osName == null ? "" : osName.toLowerCase( Locale.ROOT );
        if ( os.startsWith( "windows" ) ) {
            addKeyStore( WINDOWS_ROOT, certs );
            return certs;
        }
        if ( os.startsWith( "mac" ) ) {
            for ( final String type : MAC_KEYSTORES )
                addKeyStore( type, certs );
        }
        for ( final String bundle : PEM_BUNDLES ) {
            final Path path = Paths.get( bundle );
            if ( Files.isRegularFile( path ) && addPemBundle( path, certs ) )
                break; // The bundles are alternative locations of the same list; the first readable one is enough.
        }
        return certs;
    }

    private void addKeyStore( final String type, final List< X509Certificate > certs ) {
        try {
            final KeyStore store = KeyStore.getInstance( type );
            store.load( null, null );
            final int before = certs.size();
            certs.addAll( certificatesOf( store ) );
            logger.debug( "[larva-update] trust store " + type + ": " + ( certs.size() - before ) + " certificates" );
        } catch ( final GeneralSecurityException | IOException e ) {
            // Expected on some systems (e.g. KeychainStore-ROOT before Java 23); the other sources still apply.
            logger.debug( "[larva-update] trust store " + type + " not available: " + e );
        }
    }

    /** @return true if the bundle was read */
    boolean addPemBundle( final Path path, final List< X509Certificate > certs ) {
        try ( final InputStream in = Files.newInputStream( path ) ) {
            final int before = certs.size();
            certs.addAll( x509Only( CertificateFactory.getInstance( "X.509" ).generateCertificates( in ) ) );
            logger.debug( "[larva-update] trust store " + path + ": " + ( certs.size() - before ) + " certificates" );
            return true;
        } catch ( final GeneralSecurityException | IOException e ) {
            logger.warning( "[larva-update] cannot read trust store " + path + ": " + e );
            return false;
        }
    }

    static List< X509Certificate > certificatesOf( final KeyStore store ) throws GeneralSecurityException {
        final List< Certificate > all = new ArrayList<>();
        for ( final Enumeration< String > aliases = store.aliases(); aliases.hasMoreElements(); ) {
            final Certificate cert = store.getCertificate( aliases.nextElement() );
            if ( cert != null )
                all.add( cert );
        }
        return x509Only( all );
    }

    private static List< X509Certificate > x509Only( final Collection< ? extends Certificate > certs ) {
        final List< X509Certificate > result = new ArrayList<>();
        for ( final Certificate cert : certs )
            if ( cert instanceof X509Certificate )
                result.add( (X509Certificate) cert );
        return result;
    }

}
