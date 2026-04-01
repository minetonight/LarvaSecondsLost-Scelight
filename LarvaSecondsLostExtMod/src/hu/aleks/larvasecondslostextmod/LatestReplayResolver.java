package hu.aleks.larvasecondslostextmod;

import hu.scelightapibase.bean.settings.type.ISetting;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.Collections;
import java.util.List;

/**
 * Resolves the latest replay file for the module.
 *
 * <p>This class first prefers the last replay reported by the replay-folder monitor. If that is not
 * available or is older than the latest replay found in Scelight's monitored replay folders, it uses a
 * reflective best-effort scan of those monitored folders. Reflection is used intentionally so the module
 * keeps compiling only against the public external module API.</p>
 */
public class LatestReplayResolver {

    /** Owning external module. */
    private final LarvaSecondsLostModule module;

    /**
     * Creates a new latest replay resolver.
     *
     * @param module owning external module
     */
    public LatestReplayResolver( final LarvaSecondsLostModule module ) {
        this.module = module;
    }

    /**
     * Resolves the best available latest replay.
     *
     * @param lastDetectedReplayPath replay last reported by the replay-folder monitor; may be <code>null</code>
     * @return best available latest replay; may be <code>null</code>
     */
    public Path resolveLatestReplay( final Path lastDetectedReplayPath ) {
        final Path scannedReplayPath = scanNativeMonitoredReplayFolders();

        if ( lastDetectedReplayPath == null )
            return scannedReplayPath;
        if ( scannedReplayPath == null )
            return lastDetectedReplayPath;

        final FileTime detectedTime = readLastModifiedTime( lastDetectedReplayPath );
        final FileTime scannedTime = readLastModifiedTime( scannedReplayPath );

        if ( scannedTime != null && detectedTime != null && scannedTime.compareTo( detectedTime ) > 0 )
            return scannedReplayPath;

        if ( detectedTime != null )
            return lastDetectedReplayPath;

        return scannedReplayPath;
    }

    /**
     * Scans Scelight's monitored replay folders using reflection.
     *
     * @return newest replay file found in monitored folders; may be <code>null</code>
     */
    private Path scanNativeMonitoredReplayFolders() {
        try {
            final Object repFoldersBean = resolveReplayFoldersBean();
            if ( repFoldersBean == null )
                return null;

            final Method getReplayFolderBeanList = repFoldersBean.getClass().getMethod( "getReplayFolderBeanList" );
            @SuppressWarnings( "unchecked" )
            final List< Object > replayFolderBeans = (List< Object >) getReplayFolderBeanList.invoke( repFoldersBean );

            Path latestReplay = null;
            FileTime latestTime = FileTime.fromMillis( 0L );
            for ( final Object replayFolderBean : replayFolderBeans ) {
                if ( !Boolean.TRUE.equals( invokeNoArg( replayFolderBean, "getMonitored" ) ) )
                    continue;

                final Path folder = (Path) invokeNoArg( replayFolderBean, "getPath" );
                final Boolean recursive = (Boolean) invokeNoArg( replayFolderBean, "getRecursive" );
                final LatestReplayCandidate candidate = findLatestReplayInFolder( folder, recursive != null && recursive.booleanValue() );
                if ( candidate != null && candidate.lastModifiedTime.compareTo( latestTime ) > 0 ) {
                    latestReplay = candidate.replayFile;
                    latestTime = candidate.lastModifiedTime;
                }
            }

            if ( latestReplay != null )
                module.logger.debug( module.manifest.getName() + " resolved latest replay by scanning monitored folders: " + latestReplay );

            return latestReplay;
        } catch ( final Throwable t ) {
            module.logger.debug( module.manifest.getName() + " could not reflectively scan Scelight monitored replay folders." );
            module.logger.debug( "Reason:", t );
            return null;
        }
    }

    /**
     * Resolves Scelight's replay-folders bean through reflection.
     *
     * @return replay folders bean or <code>null</code>
     * @throws Exception if reflection fails unexpectedly
     */
    private Object resolveReplayFoldersBean() throws Exception {
        final Class< ? > envClass = Class.forName( "hu.scelight.service.env.Env" );
        final Field appSettingsField = envClass.getField( "APP_SETTINGS" );
        final Object appSettings = appSettingsField.get( null );

        final Class< ? > settingsClass = Class.forName( "hu.scelight.service.settings.Settings" );
        final Field replayFoldersField = settingsClass.getField( "REPLAY_FOLDERS_BEAN" );
        final Object replayFoldersSetting = replayFoldersField.get( null );

        final Method getMethod = appSettings.getClass().getMethod( "get", ISetting.class );
        return getMethod.invoke( appSettings, replayFoldersSetting );
    }

    /**
     * Finds the latest replay in a folder.
     *
     * @param folder folder to scan
     * @param recursive tells if subfolders should be scanned too
     * @return latest replay candidate or <code>null</code>
     */
    private LatestReplayCandidate findLatestReplayInFolder( final Path folder, final boolean recursive ) {
        if ( folder == null || !Files.exists( folder ) || !Files.isDirectory( folder ) )
            return null;

        final LatestReplayCandidateHolder holder = new LatestReplayCandidateHolder();
        final int maxDepth = recursive ? Integer.MAX_VALUE : 1;

        final FileVisitor< Path > visitor = new SimpleFileVisitor< Path >() {
            @Override
            public FileVisitResult visitFile( final Path file, final BasicFileAttributes attrs ) throws IOException {
                if ( !attrs.isDirectory() && isReplayFile( file ) && attrs.lastModifiedTime().compareTo( holder.latestTime ) > 0 ) {
                    holder.latestReplay = file;
                    holder.latestTime = attrs.lastModifiedTime();
                }

                return FileVisitResult.CONTINUE;
            }
        };

        try {
            Files.walkFileTree( folder, Collections.< FileVisitOption >emptySet(), maxDepth, visitor );
        } catch ( final IOException e ) {
            module.logger.debug( "Could not scan replay folder: " + folder, e );
            return null;
        }

        return holder.latestReplay == null ? null : new LatestReplayCandidate( holder.latestReplay, holder.latestTime );
    }

    /**
     * Tells if a file looks like a StarCraft II replay.
     *
     * @param file file to check
     * @return <code>true</code> if the file has the replay extension
     */
    private boolean isReplayFile( final Path file ) {
        final String fileName = file.getFileName().toString().toLowerCase();
        return fileName.endsWith( ".sc2replay" );
    }

    /**
     * Reads the last modified time of a file.
     *
     * @param replayFile replay file to inspect
     * @return last modified time or <code>null</code> if not available
     */
    private FileTime readLastModifiedTime( final Path replayFile ) {
        if ( replayFile == null || !Files.exists( replayFile ) )
            return null;

        try {
            return Files.getLastModifiedTime( replayFile );
        } catch ( final IOException e ) {
            module.logger.debug( "Could not read replay timestamp: " + replayFile, e );
            return null;
        }
    }

    /**
     * Invokes a no-argument method reflectively.
     *
     * @param target target object
     * @param methodName method name to invoke
     * @return invocation result
     * @throws Exception if reflection fails
     */
    private Object invokeNoArg( final Object target, final String methodName ) throws Exception {
        return target.getClass().getMethod( methodName ).invoke( target );
    }

    /** Holder for the latest replay while walking a folder. */
    private static class LatestReplayCandidateHolder {
        /** Latest replay found. */
        private Path latestReplay;

        /** Last modified time of the latest replay. */
        private FileTime latestTime = FileTime.fromMillis( 0L );
    }

    /** Immutable replay candidate. */
    private static class LatestReplayCandidate {
        /** Replay file. */
        private final Path replayFile;

        /** Last modified time. */
        private final FileTime lastModifiedTime;

        /**
         * Creates a new candidate.
         *
         * @param replayFile replay file
         * @param lastModifiedTime last modified time
         */
        private LatestReplayCandidate( final Path replayFile, final FileTime lastModifiedTime ) {
            this.replayFile = replayFile;
            this.lastModifiedTime = lastModifiedTime;
        }
    }

}