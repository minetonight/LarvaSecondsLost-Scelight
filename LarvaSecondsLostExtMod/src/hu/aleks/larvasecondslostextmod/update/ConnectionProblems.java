package hu.aleks.larvasecondslostextmod.update;

import java.security.cert.CertPathBuilderException;
import java.security.cert.CertPathValidatorException;
import java.security.cert.CertificateException;

import javax.net.ssl.SSLHandshakeException;

/**
 * Turns a connection error into a text the user can act on (E18 bug fix: old bundled Java).
 *
 * <p>A certificate error almost always means Scelight's Java is too old to know the update site's certificate authority
 * (Scelight ships 1.8.0_60). Its <code>Scelight-win.cmd</code> starts <code>jre\bin\java.exe</code> whenever that folder
 * exists, so installing a newer Java only helps once the bundled <code>jre</code> folder is replaced or removed.</p>
 */
final class ConnectionProblems {

    private ConnectionProblems() {
    }

    /**
     * @param e the error of a failed connection
     * @param javaVersion value of the <code>java.version</code> system property
     * @return a short, actionable description
     */
    static String describe( final Exception e, final String javaVersion ) {
        if ( isCertificateProblem( e ) )
            return "Scelight's Java (" + javaVersion + ") is too old to verify the update site. For automatic updates replace the jre folder"
                    + " in the Scelight folder with a current Java (or delete it to use the installed Java), or update manually from the download page";
        return String.valueOf( e.getMessage() );
    }

    /** @return true if <code>e</code> or one of its causes is a TLS certificate validation failure */
    static boolean isCertificateProblem( final Throwable e ) {
        for ( Throwable t = e; t != null; t = t.getCause() == t ? null : t.getCause() ) {
            if ( t instanceof CertificateException || t instanceof CertPathBuilderException || t instanceof CertPathValidatorException )
                return true;
            if ( t instanceof SSLHandshakeException && String.valueOf( t.getMessage() ).contains( "PKIX" ) )
                return true;
        }
        return false;
    }

}
