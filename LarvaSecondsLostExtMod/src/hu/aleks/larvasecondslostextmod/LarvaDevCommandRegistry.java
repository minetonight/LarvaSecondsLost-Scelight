package hu.aleks.larvasecondslostextmod;

import java.util.HashMap;
import java.util.Map;

/** Name to command lookup. Later stories register their commands here. */
final class LarvaDevCommandRegistry {

    private final Map< String, LarvaDevCommand > commands = new HashMap< String, LarvaDevCommand >();

    void register( final LarvaDevCommand command ) {
        if ( commands.put( command.getName(), command ) != null )
            throw new IllegalStateException( "Duplicate dev command: " + command.getName() );
    }

    /** @return the command or <code>null</code> if unknown */
    LarvaDevCommand find( final String name ) {
        return commands.get( name );
    }

}
