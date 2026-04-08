package hu.aleks.larvasecondslostextmod;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Minimal standalone bootstrap that prepares enough Scelight launcher state for headless benchmark extraction.
 */
public class StandaloneScelightBootstrap {

    /** Internal launcher class name. */
    private static final String SCELIGHT_LAUNCHER_CLASS = "hu.sllauncher.ScelightLauncher";

    /** Internal modules bean class name. */
    private static final String MODULES_BEAN_CLASS = "hu.sllauncher.bean.module.ModulesBean";

    /** Internal modules bean origin enum class name. */
    private static final String MODULES_BEAN_ORIGIN_CLASS = "hu.sllauncher.bean.module.ModulesBeanOrigin";

    /** Unsafe class name. */
    private static final String UNSAFE_CLASS = "sun.misc.Unsafe";

    /** Internal replay processor class name. */
    private static final String REP_PROCESSOR_CLASS = "hu.scelight.sc2.rep.repproc.RepProcessor";

    /** Guard against repeated initialization in the same JVM. */
    private static volatile boolean initialized;

    /**
     * Ensures minimal launcher state exists before Scelight application classes initialize.
     *
     * @param appDir full Scelight application directory containing boot-settings.xml and mod/
     */
    public synchronized void ensureInitialized( final Path appDir ) {
        if ( initialized )
            return;

        final Path resolvedAppDir = resolveAppDir( appDir );
        validateAppDir( resolvedAppDir );

        try {
            System.setProperty( "user.dir", resolvedAppDir.toAbsolutePath().toString() );

            final Class< ? > launcherClass = Class.forName( SCELIGHT_LAUNCHER_CLASS );
            final Field instanceField = launcherClass.getDeclaredField( "INSTANCE" );
            instanceField.setAccessible( true );
            if ( instanceField.get( null ) == null ) {
                final Object launcher = allocateWithoutConstructor( launcherClass );
                initializeLauncherInstance( launcherClass, launcher );
                instanceField.set( null, launcher );
            }

            final Field argumentsField = launcherClass.getDeclaredField( "arguments" );
            argumentsField.setAccessible( true );
            if ( argumentsField.get( null ) == null )
                argumentsField.set( null, new String[ 0 ] );

            initializeReplayProcessorStatics();

            initialized = true;
        } catch ( final Exception e ) {
            throw new IllegalStateException( "Failed to initialize standalone Scelight bootstrap.", e );
        }
    }

    /**
     * Resolves the target Scelight application directory.
     */
    private Path resolveAppDir( final Path appDir ) {
        if ( appDir != null )
            return appDir.toAbsolutePath().normalize();

        final Path workingDir = Paths.get( "" ).toAbsolutePath().normalize();
        final Path[] candidates = new Path[] {
                workingDir.resolve( "scelight/release/Scelight-3.2.0" ),
                workingDir.resolve( "../scelight/release/Scelight-3.2.0" ).normalize(),
                workingDir.resolve( "../../scelight/release/Scelight-3.2.0" ).normalize(),
        };

        for ( final Path candidate : candidates )
            if ( Files.exists( candidate.resolve( "boot-settings.xml" ) ) && Files.isDirectory( candidate.resolve( "mod" ) ) )
                return candidate;

        throw new IllegalArgumentException( "Could not infer Scelight application directory. Pass --scelight-app-dir explicitly." );
    }

    /**
     * Validates that the Scelight application directory looks usable.
     */
    private void validateAppDir( final Path appDir ) {
        if ( !Files.exists( appDir.resolve( "boot-settings.xml" ) ) )
            throw new IllegalArgumentException( "Scelight app dir is missing boot-settings.xml: " + appDir );
        if ( !Files.isDirectory( appDir.resolve( "mod" ) ) )
            throw new IllegalArgumentException( "Scelight app dir is missing mod/: " + appDir );
    }

    /**
     * Initializes required launcher instance fields without invoking the heavy launcher constructor.
     */
    private void initializeLauncherInstance( final Class< ? > launcherClass, final Object launcher ) throws Exception {
        setField( launcherClass, launcher, "proceedAction", new AtomicReference< Runnable >() );
        setField( launcherClass, launcher, "modules", new AtomicReference< Object >( buildFakeModulesBean() ) );
    }

    /**
     * Initializes replay-processor statics that are normally configured by the full application runtime.
     */
    private void initializeReplayProcessorStatics() throws Exception {
        final Field favoredToonListField = Class.forName( REP_PROCESSOR_CLASS ).getField( "favoredToonList" );
        @SuppressWarnings( "unchecked" )
        final AtomicReference< Object > favoredToonList = (AtomicReference< Object >) favoredToonListField.get( null );
        if ( favoredToonList.get() == null )
            favoredToonList.set( Collections.emptyList() );
    }

    /**
     * Builds a fake modules bean sufficient for offline extraction.
     */
    private Object buildFakeModulesBean() throws Exception {
        final Class< ? > modulesBeanClass = Class.forName( MODULES_BEAN_CLASS );
        final Object modulesBean = modulesBeanClass.newInstance();

        final Method setOrigin = modulesBeanClass.getMethod( "setOrigin", Class.forName( MODULES_BEAN_ORIGIN_CLASS ) );
        final Object updaterFakeOrigin = Enum.valueOf( (Class) Class.forName( MODULES_BEAN_ORIGIN_CLASS ), "UPDATER_FAKE" );
        setOrigin.invoke( modulesBean, updaterFakeOrigin );

        modulesBeanClass.getMethod( "setModList", java.util.List.class ).invoke( modulesBean, Collections.emptyList() );
        modulesBeanClass.getMethod( "setExtModRefList", java.util.List.class ).invoke( modulesBean, Collections.emptyList() );
        modulesBeanClass.getMethod( "setRetrievedExtModList", java.util.List.class ).invoke( modulesBean, Collections.emptyList() );
        modulesBeanClass.getMethod( "setDigest", String.class ).invoke( modulesBean, "standalone-benchmark" );
        return modulesBean;
    }

    /**
     * Allocates an instance without invoking its constructor.
     */
    private Object allocateWithoutConstructor( final Class< ? > targetClass ) throws Exception {
        final Class< ? > unsafeClass = Class.forName( UNSAFE_CLASS );
        final Field theUnsafeField = unsafeClass.getDeclaredField( "theUnsafe" );
        theUnsafeField.setAccessible( true );
        final Object unsafe = theUnsafeField.get( null );
        final Method allocateInstance = unsafeClass.getMethod( "allocateInstance", Class.class );
        return allocateInstance.invoke( unsafe, targetClass );
    }

    /**
     * Sets a declared field value.
     */
    private void setField( final Class< ? > ownerClass, final Object target, final String fieldName, final Object value ) throws Exception {
        final Field field = ownerClass.getDeclaredField( fieldName );
        field.setAccessible( true );
        field.set( target, value );
    }

}