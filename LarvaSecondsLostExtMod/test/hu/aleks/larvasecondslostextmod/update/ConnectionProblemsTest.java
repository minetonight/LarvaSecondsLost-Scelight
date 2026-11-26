package hu.aleks.larvasecondslostextmod.update;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.security.cert.CertPathBuilderException;
import java.security.cert.CertificateException;

import javax.net.ssl.SSLHandshakeException;

import org.junit.Test;

/** The old-Java certificate failure gets an actionable text; other failures keep their own message. */
public class ConnectionProblemsTest {

    /** What Java 1.8.0_60 throws for GitHub Pages: SSLHandshakeException caused by ValidatorException caused by CertPathBuilderException. */
    private static SSLHandshakeException pkixFailure() {
        final SSLHandshakeException e = new SSLHandshakeException( "sun.security.validator.ValidatorException: PKIX path building failed" );
        e.initCause( new CertificateException( "PKIX path building failed", new CertPathBuilderException(
                "unable to find valid certification path to requested target" ) ) );
        return e;
    }

    @Test
    public void certificateFailureSuggestsNewerJavaOrManualUpdate() {
        final String text = ConnectionProblems.describe( pkixFailure(), "1.8.0_60" );
        assertTrue( text, text.contains( "Java (1.8.0_60) is too old" ) );
        assertTrue( text, text.contains( "jre folder" ) );
        assertTrue( text, text.contains( "download page" ) );
    }

    @Test
    public void detectsPkixMessageWithoutCause() {
        assertTrue( ConnectionProblems.isCertificateProblem( new SSLHandshakeException( "PKIX path validation failed" ) ) );
    }

    @Test
    public void detectsCertificateProblemWrappedInIOException() {
        assertTrue( ConnectionProblems.isCertificateProblem( new IOException( "wrapped", pkixFailure() ) ) );
    }

    @Test
    public void otherFailuresKeepTheirMessage() {
        final SocketTimeoutException timeout = new SocketTimeoutException( "connect timed out" );
        assertFalse( ConnectionProblems.isCertificateProblem( timeout ) );
        assertEquals( "connect timed out", ConnectionProblems.describe( timeout, "1.8.0_60" ) );
        assertFalse( ConnectionProblems.isCertificateProblem( new SSLHandshakeException( "Received fatal alert: handshake_failure" ) ) );
    }

}
