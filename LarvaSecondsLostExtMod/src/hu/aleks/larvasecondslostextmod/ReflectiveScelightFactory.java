package hu.aleks.larvasecondslostextmod;

import hu.scelightapi.sc2.rep.repproc.ISelectionTracker;
import hu.scelightapi.service.IFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Creates a minimal reflection-backed {@link IFactory} suitable for standalone replay analysis.
 */
public class ReflectiveScelightFactory {

    /** Internal Scelight selection-tracker implementation class name. */
    private static final String SELECTION_TRACKER_CLASS = "hu.scelight.sc2.rep.repproc.SelectionTracker";

    /**
     * Creates a new minimal {@link IFactory} implementation.
     *
     * @return reflection-backed factory proxy
     */
    public IFactory createFactory() {
        return (IFactory) Proxy.newProxyInstance( IFactory.class.getClassLoader(), new Class< ? >[] { IFactory.class }, new InvocationHandler() {
            @Override
            public Object invoke( final Object proxy, final Method method, final Object[] args ) throws Throwable {
                final String methodName = method.getName();
                if ( "newSelectionTracker".equals( methodName ) )
                    return newSelectionTracker();
                if ( "toString".equals( methodName ) )
                    return ReflectiveScelightFactory.class.getSimpleName();
                if ( "hashCode".equals( methodName ) )
                    return Integer.valueOf( System.identityHashCode( proxy ) );
                if ( "equals".equals( methodName ) )
                    return Boolean.valueOf( proxy == args[ 0 ] );
                throw new UnsupportedOperationException( "Standalone benchmark extraction only supports IFactory.newSelectionTracker()." );
            }
        } );
    }

    /**
     * Creates a new Scelight selection tracker via reflection.
     */
    private ISelectionTracker newSelectionTracker() {
        try {
            return (ISelectionTracker) Class.forName( SELECTION_TRACKER_CLASS ).newInstance();
        } catch ( final Exception e ) {
            throw new IllegalStateException( "Failed to instantiate Scelight selection tracker.", e );
        }
    }

}