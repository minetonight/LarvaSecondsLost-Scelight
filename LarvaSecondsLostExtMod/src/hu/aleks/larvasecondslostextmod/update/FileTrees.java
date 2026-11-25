package hu.aleks.larvasecondslostextmod.update;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Small file helpers of the updater: recursive delete and SHA-256.
 */
final class FileTrees {

    private static final char[] HEX = "0123456789abcdef".toCharArray();

    /** Deletes a file or a folder with everything in it. Does nothing if it does not exist. Does not follow links. */
    static void deleteTree( final Path path ) throws IOException {
        if ( !Files.exists( path, java.nio.file.LinkOption.NOFOLLOW_LINKS ) )
            return;
        Files.walkFileTree( path, new SimpleFileVisitor< Path >() {
            @Override
            public FileVisitResult visitFile( final Path file, final BasicFileAttributes attrs ) throws IOException {
                Files.delete( file );
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory( final Path dir, final IOException exc ) throws IOException {
                if ( exc != null )
                    throw exc;
                Files.delete( dir );
                return FileVisitResult.CONTINUE;
            }
        } );
    }

    /** @return a new SHA-256 digest */
    static MessageDigest newSha256() {
        try {
            return MessageDigest.getInstance( "SHA-256" );
        } catch ( final NoSuchAlgorithmException e ) {
            throw new IllegalStateException( "SHA-256 is required by every Java platform", e );
        }
    }

    /** @return lower-case hex SHA-256 of a file */
    static String sha256( final Path file ) throws IOException {
        final MessageDigest digest = newSha256();
        final byte[] buffer = new byte[ 64 * 1024 ];
        try ( final InputStream in = Files.newInputStream( file ) ) {
            for ( int n; ( n = in.read( buffer ) ) > 0; )
                digest.update( buffer, 0, n );
        }
        return hex( digest.digest() );
    }

    /** @return lower-case hex form of the bytes */
    static String hex( final byte[] bytes ) {
        final char[] chars = new char[ bytes.length * 2 ];
        for ( int i = 0; i < bytes.length; i++ ) {
            chars[ i * 2 ] = HEX[ ( bytes[ i ] >> 4 ) & 0xF ];
            chars[ i * 2 + 1 ] = HEX[ bytes[ i ] & 0xF ];
        }
        return new String( chars );
    }

    private FileTrees() {
    }

}
