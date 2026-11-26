package hu.aleks.larvasecondslostextmod.update;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

/** Reading OS certificate bundles and combining them with Java's own roots (E18 bug fix: old bundled Java). */
public class UpdateTrustTest {

    private static X509Certificate[] someJavaRoots() throws Exception {
        final X509Certificate[] roots = UpdateTrust.javaDefaultRoots();
        assertTrue( "test JVM has no trusted roots", roots.length >= 2 );
        return Arrays.copyOf( roots, 2 );
    }

    private static String pem( final X509Certificate cert ) throws Exception {
        return "-----BEGIN CERTIFICATE-----\n" + Base64.getMimeEncoder().encodeToString( cert.getEncoded() ) + "\n-----END CERTIFICATE-----\n";
    }

    @Test
    public void readsEveryCertificateOfAPemBundle() throws Exception {
        final X509Certificate[] roots = someJavaRoots();
        final Path bundle = Files.createTempFile( "larva-bundle", ".pem" );
        try {
            Files.write( bundle, ( "# comment lines are allowed\n" + pem( roots[ 0 ] ) + pem( roots[ 1 ] ) ).getBytes( StandardCharsets.US_ASCII ) );
            final List< X509Certificate > certs = new ArrayList<>();
            assertTrue( new SystemTrustStores( UpdateTestFixtures.LOGGER ).addPemBundle( bundle, certs ) );
            assertEquals( Arrays.asList( roots ), certs );
        } finally {
            Files.deleteIfExists( bundle );
        }
    }

    @Test
    public void unreadableBundleIsSkipped() throws Exception {
        final Path bundle = Files.createTempFile( "larva-bundle", ".pem" );
        try {
            Files.write( bundle, "not a certificate".getBytes( StandardCharsets.US_ASCII ) );
            final List< X509Certificate > certs = new ArrayList<>();
            new SystemTrustStores( UpdateTestFixtures.LOGGER ).addPemBundle( bundle, certs );
            assertTrue( certs.isEmpty() );
        } finally {
            Files.deleteIfExists( bundle );
        }
    }

    @Test
    public void missingOsStoreGivesNoCertificatesAndNoError() {
        // A made-up OS reads only the PEM bundle paths; on a machine without them the list is just empty.
        assertNotNull( new SystemTrustStores( UpdateTestFixtures.LOGGER ).load( "NoSuchOS" ) );
    }

    @Test
    public void certificatesOfKeyStoreSkipsNothingTrusted() throws Exception {
        final X509Certificate[] roots = someJavaRoots();
        final KeyStore store = KeyStore.getInstance( KeyStore.getDefaultType() );
        store.load( null, null );
        store.setCertificateEntry( "a", roots[ 0 ] );
        store.setCertificateEntry( "b", roots[ 1 ] );
        final List< X509Certificate > certs = SystemTrustStores.certificatesOf( store );
        assertEquals( 2, certs.size() );
        assertTrue( certs.containsAll( Arrays.asList( roots ) ) );
    }

    @Test
    public void buildsFactoryWithAndWithoutExtraRoots() throws Exception {
        assertNotNull( UpdateTrust.build( Collections.< X509Certificate > emptyList() ) );
        assertNotNull( UpdateTrust.build( Arrays.asList( someJavaRoots() ) ) );
        assertFalse( UpdateTrust.build( Arrays.asList( someJavaRoots() ) ).getDefaultCipherSuites().length == 0 );
    }

}
