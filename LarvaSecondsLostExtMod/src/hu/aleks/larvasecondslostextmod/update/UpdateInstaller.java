package hu.aleks.larvasecondslostextmod.update;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import hu.scelightapibase.service.log.ILogger;

/**
 * Installs a {@link RemoteRelease} next to the running version (E18-S10).
 *
 * <p>Steps: download the zip into the cache folder and check its size and SHA-256; check every zip entry against the
 * module.xml file list (no other files, no paths outside the new version folder); extract into a hidden staging folder and
 * check each file's SHA-256; then move the staging folder to its version name. Scelight loads the highest version folder,
 * so the new version is used after the next restart. The running version is never touched. On any error the staging folder
 * and the download are removed.</p>
 *
 * <p>Blocking (network and disk): never call it on the EDT.</p>
 */
public final class UpdateInstaller {

    /** Prefix of the staging folder. Scelight ignores it: a name starting with a dot is never a version. */
    static final String STAGING_PREFIX = ".incoming-";

    /** Reports progress as short texts. */
    public interface Progress {
        /** @param text for example "Downloading 40%" */
        void report( String text );
    }

    private final ReleaseDownloader downloader;
    private final Path modRootFolder;
    private final Path downloadFolder;
    private final Path runningVersionFolder;
    private final ILogger logger;

    /**
     * @param modRootFolder <code>Scelight/mod-x/larva-seconds-lost</code> ({@code IModEnv.getRootFolder()})
     * @param downloadFolder where the zip is downloaded to (inside the module cache folder)
     * @param runningVersionFolder the running version folder ({@code IModEnv.getVersionFolder()}); never replaced
     */
    public UpdateInstaller( final ReleaseDownloader downloader, final Path modRootFolder, final Path downloadFolder, final Path runningVersionFolder,
            final ILogger logger ) {
        this.downloader = downloader;
        this.modRootFolder = modRootFolder;
        this.downloadFolder = downloadFolder;
        this.runningVersionFolder = runningVersionFolder;
        this.logger = logger;
    }

    /**
     * @return the installed version folder
     * @throws UpdateException with a readable reason if anything fails; nothing is left behind then
     */
    public Path install( final RemoteRelease release, final Progress progress ) throws UpdateException {
        final String versionFolderName = checkFileListLayout( release );
        final Path target = modRootFolder.resolve( versionFolderName );
        if ( target.toAbsolutePath().normalize().equals( runningVersionFolder.toAbsolutePath().normalize() ) )
            throw new UpdateException( "version " + versionFolderName + " is the running version" );

        if ( Files.isDirectory( target ) ) {
            if ( isComplete( target, release, versionFolderName ) ) {
                logger.info( "[larva-update] version " + versionFolderName + " is already installed at " + target );
                return target;
            }
            logger.info( "[larva-update] removing incomplete folder " + target );
            deleteOrFail( target );
        }

        final Path zip = download( release, progress );
        final Path staging = modRootFolder.resolve( STAGING_PREFIX + versionFolderName );
        try {
            progress.report( "Installing..." );
            deleteOrFail( staging );
            extract( zip, release, versionFolderName, staging );
            moveIntoPlace( staging, target );
            logger.info( "[larva-update] installed version " + versionFolderName + " to " + target );
            return target;
        } catch ( final UpdateException e ) {
            deleteQuietly( staging );
            throw e;
        } finally {
            deleteQuietly( zip );
        }
    }

    /**
     * @return <code>true</code> if the release is already installed completely (waiting for a restart); blocking (hashes files)
     */
    public boolean isInstalled( final RemoteRelease release ) {
        try {
            final String versionFolderName = checkFileListLayout( release );
            final Path target = modRootFolder.resolve( versionFolderName );
            return Files.isDirectory( target ) && isComplete( target, release, versionFolderName );
        } catch ( final UpdateException e ) {
            return false;
        }
    }

    /**
     * Every file must be under <code>Scelight/mod-x/&lt;folder&gt;/&lt;version&gt;/</code>, all with the same version folder,
     * and that folder name must be the release version.
     *
     * @return the version folder name
     */
    static String checkFileListLayout( final RemoteRelease release ) throws UpdateException {
        final String prefix = modPrefix( release );
        String versionFolderName = null;
        for ( final String path : release.getFileSha256ByPath().keySet() ) {
            if ( !path.startsWith( prefix ) )
                throw new UpdateException( "file outside the module folder in module.xml: " + path );
            final String rest = path.substring( prefix.length() );
            final int slash = rest.indexOf( '/' );
            if ( slash <= 0 || slash == rest.length() - 1 )
                throw new UpdateException( "file outside a version folder in module.xml: " + path );
            final String folderName = rest.substring( 0, slash );
            if ( versionFolderName == null )
                versionFolderName = folderName;
            else if ( !versionFolderName.equals( folderName ) )
                throw new UpdateException( "module.xml mixes version folders " + versionFolderName + " and " + folderName );
            checkSafeRelativePath( rest.substring( slash + 1 ), path );
        }
        if ( versionFolderName == null )
            throw new UpdateException( "module.xml has no files" );
        if ( !release.getVersion().equals( ModuleVersion.parseOrNull( versionFolderName ) ) )
            throw new UpdateException( "version folder " + versionFolderName + " does not match version " + release.getVersion() );
        return versionFolderName;
    }

    private static String modPrefix( final RemoteRelease release ) {
        return "Scelight/mod-x/" + release.getFolder() + "/";
    }

    private static void checkSafeRelativePath( final String relative, final String fullPath ) throws UpdateException {
        if ( relative.startsWith( "/" ) || relative.indexOf( ':' ) >= 0 || relative.indexOf( '\\' ) >= 0 )
            throw new UpdateException( "unsafe file path: " + fullPath );
        for ( final String segment : relative.split( "/", -1 ) )
            if ( segment.length() == 0 || ".".equals( segment ) || "..".equals( segment ) )
                throw new UpdateException( "unsafe file path: " + fullPath );
    }

    private Path download( final RemoteRelease release, final Progress progress ) throws UpdateException {
        final String fileName = release.getArchiveFileName();
        if ( !fileName.matches( "[A-Za-z0-9._-]+\\.zip" ) )
            throw new UpdateException( "unexpected archive name: " + fileName );
        if ( release.getArchiveSize() > UpdateConfig.MAX_ARCHIVE_BYTES )
            throw new UpdateException( "archive is too large: " + release.getArchiveSize() + " bytes" );
        final Path zip = downloadFolder.resolve( fileName );
        String lastProblem = "no download URL";
        for ( final String url : release.getArchiveUrlList() ) {
            try {
                Files.createDirectories( downloadFolder );
                downloadOnce( url, zip, release, progress );
                return zip;
            } catch ( final IOException | UpdateException e ) {
                lastProblem = e.getMessage();
                logger.warning( "[larva-update] download failed from " + url + ": " + lastProblem );
                deleteQuietly( zip );
            }
        }
        throw new UpdateException( "download failed (" + lastProblem + ")" );
    }

    private void downloadOnce( final String url, final Path zip, final RemoteRelease release, final Progress progress ) throws IOException, UpdateException {
        final MessageDigest digest = FileTrees.newSha256();
        final long expectedSize = release.getArchiveSize();
        long written = 0;
        int lastPercent = -1;
        try ( final InputStream in = new DigestInputStream( new LimitedInputStream( downloader.open( url ), expectedSize ), digest );
                final OutputStream out = Files.newOutputStream( zip ) ) {
            final byte[] buffer = new byte[ 64 * 1024 ];
            for ( int n; ( n = in.read( buffer ) ) > 0; ) {
                out.write( buffer, 0, n );
                written += n;
                final int percent = (int) ( written * 100 / expectedSize );
                if ( percent / 10 != lastPercent / 10 ) {
                    lastPercent = percent;
                    progress.report( "Downloading " + percent + "%" );
                }
            }
        }
        if ( written != expectedSize )
            throw new UpdateException( "archive size is " + written + " bytes, expected " + expectedSize );
        final String sha = FileTrees.hex( digest.digest() );
        if ( !sha.equals( release.getArchiveSha256() ) )
            throw new UpdateException( "archive checksum does not match module.xml" );
    }

    private static void extract( final Path zip, final RemoteRelease release, final String versionFolderName, final Path staging )
            throws UpdateException {
        final String prefix = modPrefix( release ) + versionFolderName + "/";
        final Map< String, String > expected = release.getFileSha256ByPath();
        final Set< String > seen = new HashSet<>();
        final Path stagingRoot = staging.toAbsolutePath().normalize();
        try ( final ZipInputStream in = new ZipInputStream( Files.newInputStream( zip ) ) ) {
            Files.createDirectories( staging );
            for ( ZipEntry entry; ( entry = in.getNextEntry() ) != null; ) {
                final String name = entry.getName().replace( '\\', '/' );
                if ( entry.isDirectory() ) {
                    if ( !( prefix.startsWith( name ) || name.startsWith( prefix ) ) || name.contains( ".." ) )
                        throw new UpdateException( "unexpected folder in archive: " + name );
                    continue;
                }
                final String sha = expected.get( name );
                if ( sha == null )
                    throw new UpdateException( "unexpected file in archive: " + name );
                if ( !seen.add( name ) )
                    throw new UpdateException( "file twice in archive: " + name );
                final Path target = stagingRoot.resolve( name.substring( prefix.length() ) ).normalize();
                if ( !target.startsWith( stagingRoot ) )
                    throw new UpdateException( "unsafe file path in archive: " + name );
                Files.createDirectories( target.getParent() );
                writeEntry( in, target, sha, name );
            }
        } catch ( final IOException e ) {
            throw new UpdateException( "cannot extract the archive (" + e.getMessage() + ")", e );
        }
        if ( seen.size() != expected.size() )
            throw new UpdateException( "archive has " + seen.size() + " of the " + expected.size() + " files listed in module.xml" );
    }

    private static void writeEntry( final ZipInputStream in, final Path target, final String expectedSha, final String name ) throws IOException,
            UpdateException {
        final MessageDigest digest = FileTrees.newSha256();
        try ( final OutputStream out = Files.newOutputStream( target ) ) {
            final byte[] buffer = new byte[ 64 * 1024 ];
            for ( int n; ( n = in.read( buffer ) ) > 0; ) {
                digest.update( buffer, 0, n );
                out.write( buffer, 0, n );
            }
        }
        if ( !FileTrees.hex( digest.digest() ).equals( expectedSha ) )
            throw new UpdateException( "checksum of " + name + " does not match module.xml" );
    }

    private static boolean isComplete( final Path folder, final RemoteRelease release, final String versionFolderName ) {
        final String prefix = modPrefix( release ) + versionFolderName + "/";
        try {
            for ( final Map.Entry< String, String > file : release.getFileSha256ByPath().entrySet() ) {
                final Path path = folder.resolve( file.getKey().substring( prefix.length() ) );
                if ( !Files.isRegularFile( path ) || !FileTrees.sha256( path ).equals( file.getValue() ) )
                    return false;
            }
            return true;
        } catch ( final IOException e ) {
            return false;
        }
    }

    private static void moveIntoPlace( final Path staging, final Path target ) throws UpdateException {
        try {
            try {
                Files.move( staging, target, StandardCopyOption.ATOMIC_MOVE );
            } catch ( final AtomicMoveNotSupportedException e ) {
                Files.move( staging, target );
            }
        } catch ( final IOException e ) {
            throw new UpdateException( "cannot move the new version into place (" + e.getMessage() + ")", e );
        }
    }

    private static void deleteOrFail( final Path path ) throws UpdateException {
        try {
            FileTrees.deleteTree( path );
        } catch ( final IOException e ) {
            throw new UpdateException( "cannot delete " + path + " (" + e.getMessage() + ")", e );
        }
    }

    private void deleteQuietly( final Path path ) {
        try {
            FileTrees.deleteTree( path );
        } catch ( final IOException e ) {
            logger.warning( "[larva-update] could not delete " + path + ", it is removed on the next start: " + e.getMessage() );
        }
    }

}
