package hu.aleks.larvasecondslostextmod.update;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import hu.aleks.larvasecondslostextmod.StdErrLogger;

/** Builds fake releases (zip bytes plus a matching module.xml) and serves them without a network. */
final class UpdateTestFixtures {

    static final Charset UTF8 = Charset.forName( "UTF-8" );
    static final String NAME = "Larva Seconds Lost";
    static final String FOLDER = "larva-seconds-lost";
    static final String MODULE_XML_URL = "https://test.invalid/module.xml";
    static final String ZIP_URL_BASE = "https://test.invalid/";

    static final StdErrLogger LOGGER = new StdErrLogger( false, false );

    /** Downloads served from memory; an unknown URL gives an IOException like a 404. */
    static final class FakeDownloader implements ReleaseDownloader {
        final Map< String, byte[] > content = new HashMap<>();
        int openCount;

        @Override
        public InputStream open( final String url ) throws IOException {
            openCount++;
            final byte[] bytes = content.get( url );
            if ( bytes == null )
                throw new IOException( "HTTP 404 from " + url );
            return new ByteArrayInputStream( bytes );
        }
    }

    /** A release under construction: files (path inside the version folder to content). */
    static final class ReleaseBuilder {
        final ModuleVersion version;
        final Map< String, byte[] > files = new LinkedHashMap<>();

        ReleaseBuilder( final String version ) {
            this.version = ModuleVersion.parseOrNull( version );
            files.put( "Scelight-mod-x-manifest.xml", ( "<manifest>" + version + "</manifest>" ).getBytes( UTF8 ) );
            files.put( FOLDER + ".jar", ( "jar of " + version ).getBytes( UTF8 ) );
            files.put( "lib/helper.jar", ( "helper of " + version ).getBytes( UTF8 ) );
        }

        String zipPath( final String relative ) {
            return "Scelight/mod-x/" + FOLDER + "/" + version + "/" + relative;
        }

        String zipFileName() {
            return "LarvaSecondsLost-" + version + ".zip";
        }

        byte[] zip() throws IOException {
            final Map< String, byte[] > entries = new LinkedHashMap<>();
            for ( final Map.Entry< String, byte[] > file : files.entrySet() )
                entries.put( zipPath( file.getKey() ), file.getValue() );
            return zipOf( entries );
        }

        /** module.xml for the given zip bytes, listing {@link #files}. */
        String moduleXml( final byte[] zip ) {
            final StringBuilder sb = new StringBuilder();
            sb.append( "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n<moduleBean v=\"1\">\n" );
            sb.append( "    <name>" ).append( NAME ).append( "</name>\n" );
            sb.append( "    <version major=\"" ).append( version.getMajor() ).append( "\" minor=\"" ).append( version.getMinor() )
                    .append( "\" revision=\"" ).append( version.getRevision() ).append( "\" v=\"1\"/>\n" );
            sb.append( "    <folder>" ).append( FOLDER ).append( "</folder>\n" );
            sb.append( "    <archiveFile path=\"" ).append( zipFileName() ).append( "\" sha256=\"" ).append( sha256( zip ) ).append( "\" v=\"1\"/>\n" );
            sb.append( "    <urlList>" ).append( ZIP_URL_BASE ).append( zipFileName() ).append( "</urlList>\n" );
            sb.append( "    <archiveSize>" ).append( zip.length ).append( "</archiveSize>\n" );
            for ( final Map.Entry< String, byte[] > file : files.entrySet() )
                sb.append( "    <fileList path=\"" ).append( zipPath( file.getKey() ) ).append( "\" sha256=\"" ).append( sha256( file.getValue() ) )
                        .append( "\" v=\"1\"/>\n" );
            sb.append( "</moduleBean>\n" );
            return sb.toString();
        }

        RemoteRelease parse( final String moduleXml ) throws UpdateException {
            return new ModuleXmlParser().parse( new ByteArrayInputStream( moduleXml.getBytes( UTF8 ) ) );
        }

        /** Publishes zip and module.xml on the fake downloader and returns the parsed release. */
        RemoteRelease publish( final FakeDownloader downloader ) throws IOException, UpdateException {
            final byte[] zip = zip();
            final String xml = moduleXml( zip );
            downloader.content.put( ZIP_URL_BASE + zipFileName(), zip );
            downloader.content.put( MODULE_XML_URL, xml.getBytes( UTF8 ) );
            return parse( xml );
        }
    }

    static byte[] zipOf( final Map< String, byte[] > entries ) throws IOException {
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try ( final ZipOutputStream zip = new ZipOutputStream( bytes ) ) {
            for ( final Map.Entry< String, byte[] > entry : entries.entrySet() ) {
                zip.putNextEntry( new ZipEntry( entry.getKey() ) );
                if ( entry.getValue() != null )
                    zip.write( entry.getValue() );
                zip.closeEntry();
            }
        }
        return bytes.toByteArray();
    }

    static String sha256( final byte[] bytes ) {
        return FileTrees.hex( FileTrees.newSha256().digest( bytes ) );
    }

    /** Creates <code>root/&lt;version&gt;/marker.txt</code>. */
    static Path versionFolder( final Path root, final String name ) throws IOException {
        final Path folder = Files.createDirectories( root.resolve( name ) );
        Files.write( folder.resolve( "marker.txt" ), name.getBytes( UTF8 ) );
        return folder;
    }

    static int countChildren( final Path folder ) throws IOException {
        int n = 0;
        try ( final DirectoryStream< Path > children = Files.newDirectoryStream( folder ) ) {
            for ( @SuppressWarnings( "unused" ) final Path child : children )
                n++;
        }
        return n;
    }

    static void deleteTree( final Path path ) throws IOException {
        FileTrees.deleteTree( path );
    }

    private UpdateTestFixtures() {
    }

}
