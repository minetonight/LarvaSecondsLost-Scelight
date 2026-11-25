package hu.aleks.larvasecondslostextmod.update;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import hu.aleks.larvasecondslostextmod.StdErrLogger;

/**
 * Release helpers for the Ant build (E18-S07); not used inside Scelight.
 *
 * <pre>
 * check-newer &lt;new module.xml&gt; &lt;published module.xml&gt;   exit 1 unless the new version is higher
 * page &lt;template&gt; &lt;output&gt; &lt;module.xml&gt; [&lt;zip&gt;]          render the landing page (@version@, @archiveFile@, @archiveSizeKb@,
 *                                                         @releaseDate@ = today, or the zip's last-modified date if given)
 * smoke [module.xml URL]                                  download and install the live release into a temp folder, like a user would
 * </pre>
 */
public final class ReleaseTool {

    private static final java.nio.charset.Charset UTF8 = java.nio.charset.Charset.forName( "UTF-8" );

    public static void main( final String[] args ) {
        try {
            if ( args.length == 3 && "check-newer".equals( args[ 0 ] ) )
                checkNewer( Paths.get( args[ 1 ] ), Paths.get( args[ 2 ] ) );
            else if ( ( args.length == 4 || args.length == 5 ) && "page".equals( args[ 0 ] ) )
                renderPage( Paths.get( args[ 1 ] ), Paths.get( args[ 2 ] ), Paths.get( args[ 3 ] ), args.length == 5 ? Paths.get( args[ 4 ] ) : null );
            else if ( ( args.length == 1 || args.length == 2 ) && "smoke".equals( args[ 0 ] ) )
                smoke( args.length == 2 ? args[ 1 ] : UpdateConfig.MODULE_XML_URL );
            else
                fail( "usage: check-newer <new module.xml> <published module.xml> | page <template> <output> <module.xml> [<zip>] | smoke [module.xml URL]" );
        } catch ( final UpdateException | IOException e ) {
            fail( e.getMessage() );
        }
    }

    private static void checkNewer( final Path newXml, final Path publishedXml ) throws IOException, UpdateException {
        final RemoteRelease fresh = parse( newXml );
        if ( !Files.isRegularFile( publishedXml ) ) {
            System.out.println( "No published module.xml yet; " + fresh.getVersion() + " is the first release." );
            return;
        }
        final RemoteRelease published = parse( publishedXml );
        if ( !fresh.getVersion().isNewerThan( published.getVersion() ) )
            fail( "Version " + fresh.getVersion() + " is not higher than the published " + published.getVersion()
                    + ". Bump version.* in release/release.properties (versions only go up)." );
        System.out.println( "Version " + fresh.getVersion() + " is higher than the published " + published.getVersion() + "." );
    }

    private static void renderPage( final Path template, final Path output, final Path moduleXml, final Path zipForDate ) throws IOException,
            UpdateException {
        final RemoteRelease release = parse( moduleXml );
        final long dateMillis = zipForDate == null ? System.currentTimeMillis() : Files.getLastModifiedTime( zipForDate ).toMillis();
        final String html = renderPage( new String( Files.readAllBytes( template ), UTF8 ), release, new java.text.SimpleDateFormat( "yyyy-MM-dd",
                java.util.Locale.ROOT ).format( new java.util.Date( dateMillis ) ) );
        Files.write( output, html.getBytes( UTF8 ) );
        System.out.println( "Wrote " + output + " for version " + release.getVersion() + "." );
    }

    /** Fills the landing page tokens; fails if a token is left over, so a typo never reaches the public page. */
    static String renderPage( final String template, final RemoteRelease release, final String releaseDate ) throws UpdateException {
        final String html = template.replace( "@version@", release.getVersion().toString() ).replace( "@archiveFile@", release.getArchiveFileName() )
                .replace( "@archiveSizeKb@", Long.toString( ( release.getArchiveSize() + 1023 ) / 1024 ) ).replace( "@releaseDate@", releaseDate );
        final java.util.regex.Matcher leftover = java.util.regex.Pattern.compile( "@[A-Za-z]+@" ).matcher( html );
        if ( leftover.find() )
            throw new UpdateException( "unknown token " + leftover.group() + " in the page template" );
        return html;
    }

    private static RemoteRelease parse( final Path file ) throws IOException, UpdateException {
        try ( final InputStream in = Files.newInputStream( file ) ) {
            return new ModuleXmlParser().parse( in );
        }
    }

    private static void smoke( final String moduleXmlUrl ) throws IOException, UpdateException {
        final Path temp = Files.createTempDirectory( "larva-release-smoke" );
        try {
            final Path modRoot = Files.createDirectories( temp.resolve( "Scelight/mod-x/larva-seconds-lost" ) );
            final Path fakeRunning = Files.createDirectories( modRoot.resolve( "0.0" ) );
            final ModuleVersion zero = new ModuleVersion( 0, 0, 0 );
            final HttpReleaseDownloader downloader = new HttpReleaseDownloader( zero );
            final UpdateCheckResult result = new UpdateChecker( downloader, moduleXmlUrl, "Larva Seconds Lost", "larva-seconds-lost", zero ).check();
            if ( result.getKind() != UpdateCheckResult.Kind.AVAILABLE )
                fail( "Live check failed: " + result.getMessage() );
            final RemoteRelease release = result.getRelease();
            System.out.println( "Live module.xml: version " + release.getVersion() + ", archive " + release.getArchiveUrlList().get( 0 ) );
            final Path installed = new UpdateInstaller( downloader, modRoot, temp.resolve( "download" ), fakeRunning, new StdErrLogger() ).install(
                    release, new UpdateInstaller.Progress() {
                        @Override
                        public void report( final String text ) {
                            System.out.println( "  " + text );
                        }
                    } );
            System.out.println( "OK: version " + release.getVersion() + " downloads, matches every checksum and installs (" + installed.getFileName()
                    + ")." );
        } finally {
            FileTrees.deleteTree( temp );
        }
    }

    private static void fail( final String message ) {
        System.err.println( "FAILED: " + message );
        System.exit( 1 );
    }

    private ReleaseTool() {
    }

}
