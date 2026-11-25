package hu.aleks.larvasecondslostextmod.update;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import hu.scelightapibase.service.log.ILogger;

/**
 * Removes what an update left behind, at module start (E18-S10): version folders lower than the running one, and staging
 * folders of an interrupted install.
 *
 * <p>Only runs from the version Scelight actually started, so an old version is deleted only after the new one works. Never
 * deletes the running version folder or anything higher (an update waiting for a restart). A failed delete (for example a
 * locked file on Windows) is logged and retried on the next start.</p>
 */
public final class OldVersionCleaner {

    private final ILogger logger;

    public OldVersionCleaner( final ILogger logger ) {
        this.logger = logger;
    }

    /**
     * @param modRootFolder <code>Scelight/mod-x/larva-seconds-lost</code>
     * @param runningVersionFolder the running version folder, a direct child of <code>modRootFolder</code>
     * @return the folders that were selected for deletion (deleted unless a warning was logged)
     */
    public List< Path > clean( final Path modRootFolder, final Path runningVersionFolder ) {
        final List< Path > selected = selectForDeletion( modRootFolder, runningVersionFolder );
        for ( final Path folder : selected ) {
            try {
                FileTrees.deleteTree( folder );
                logger.info( "[larva-update] removed old module folder " + folder );
            } catch ( final IOException e ) {
                logger.warning( "[larva-update] could not remove " + folder + " (retried on the next start): " + e.getMessage() );
            }
        }
        return selected;
    }

    /**
     * @return the sibling folders to delete; empty if the running folder is not inside <code>modRootFolder</code> or its name is
     *         not a version (then nothing is safe to judge)
     */
    List< Path > selectForDeletion( final Path modRootFolder, final Path runningVersionFolder ) {
        final List< Path > result = new ArrayList<>();
        final Path root = modRootFolder.toAbsolutePath().normalize();
        final Path running = runningVersionFolder.toAbsolutePath().normalize();
        final ModuleVersion runningVersion = running.getFileName() == null ? null : ModuleVersion.parseOrNull( running.getFileName().toString() );
        if ( runningVersion == null || !root.equals( running.getParent() ) || !Files.isDirectory( running ) ) {
            logger.debug( "[larva-update] skipping cleanup, running folder " + running + " is not a version folder in " + root );
            return result;
        }
        try ( final DirectoryStream< Path > children = Files.newDirectoryStream( root ) ) {
            for ( final Path child : children ) {
                if ( !Files.isDirectory( child, java.nio.file.LinkOption.NOFOLLOW_LINKS ) || child.equals( running ) )
                    continue;
                final String name = child.getFileName().toString();
                if ( name.startsWith( UpdateInstaller.STAGING_PREFIX ) ) {
                    result.add( child );
                    continue;
                }
                final ModuleVersion version = ModuleVersion.parseOrNull( name );
                if ( version != null && runningVersion.isNewerThan( version ) )
                    result.add( child );
            }
        } catch ( final IOException e ) {
            logger.warning( "[larva-update] could not list " + root + " for cleanup: " + e.getMessage() );
        }
        return result;
    }

}
