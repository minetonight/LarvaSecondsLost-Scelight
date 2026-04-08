package hu.aleks.larvasecondslostextmod;

import hu.scelightapi.sc2.rep.factory.IRepParserEngine;
import hu.scelightapi.sc2.rep.model.IReplay;
import hu.scelightapi.sc2.rep.repproc.IRepProcessor;
import hu.scelightapibase.bean.IVersionBean;

import java.lang.reflect.Field;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.file.Path;

/**
 * Reflection-backed replay parser engine bridge for standalone extraction.
 */
public class ReflectiveScelightRepParserEngine implements IRepParserEngine {

    /** Internal Scelight parser-engine class name. */
    private static final String REP_PARSER_ENGINE_CLASS = "hu.scelight.sc2.rep.factory.RepParserEngine";

    /** Internal Scelight replay-processor class name. */
    private static final String REP_PROCESSOR_CLASS = "hu.scelight.sc2.rep.repproc.RepProcessor";

    /** Internal Scelight replay-processor cache class name. */
    private static final String REP_PROC_CACHE_CLASS = "hu.scelight.sc2.rep.cache.RepProcCache";

    @Override
    public IVersionBean getRepParserEngineVersionBean() {
        return getStaticVersionField( REP_PARSER_ENGINE_CLASS );
    }

    @Override
    public IVersionBean getRepProcessorVersionBean() {
        return getStaticVersionField( REP_PROCESSOR_CLASS );
    }

    @Override
    public IVersionBean getRepProcCacheVersionBean() {
        return getStaticVersionField( REP_PROC_CACHE_CLASS );
    }

    @Override
    public IReplay parseReplay( final Path file ) {
        return (IReplay) invokeStatic( REP_PARSER_ENGINE_CLASS, "parseReplay", new Class< ? >[] { Path.class }, new Object[] { file } );
    }

    @Override
    public IRepProcessor parseAndWrapReplay( final Path file ) {
        return newStandaloneRepProcessor( file );
    }

    @Override
    public IRepProcessor getRepProc( final Path file ) {
        return newStandaloneRepProcessor( file );
    }

    /**
     * Instantiates a full replay processor directly, bypassing the Scelight replay cache path.
     */
    private IRepProcessor newStandaloneRepProcessor( final Path file ) {
        try {
            final Constructor< ? > constructor = Class.forName( REP_PROCESSOR_CLASS ).getConstructor( Path.class );
            return (IRepProcessor) constructor.newInstance( file );
        } catch ( final Exception e ) {
            throw new IllegalStateException( "Failed to instantiate standalone replay processor.", e );
        }
    }

    /**
     * Resolves a static version field from a Scelight runtime class.
     */
    private IVersionBean getStaticVersionField( final String className ) {
        try {
            final Field field = Class.forName( className ).getField( "VERSION" );
            return (IVersionBean) field.get( null );
        } catch ( final Exception e ) {
            return null;
        }
    }

    /**
     * Invokes a static Scelight method via reflection.
     */
    private Object invokeStatic( final String className, final String methodName, final Class< ? >[] parameterTypes, final Object[] arguments ) {
        try {
            final Method method = Class.forName( className ).getMethod( methodName, parameterTypes );
            return method.invoke( null, arguments );
        } catch ( final Exception e ) {
            throw new IllegalStateException( "Failed to invoke Scelight runtime method " + className + '.' + methodName + "().", e );
        }
    }

}