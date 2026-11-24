package hu.aleks.larvasecondslostextmod;

import java.util.List;

/**
 * A command of the dev command channel. Register implementations in {@link LarvaDevCommandRegistry}.
 */
interface LarvaDevCommand {

    /** @return the command name as typed in the command file, lower case */
    String getName();

    /** @return <code>true</code> if {@link #execute} must run on the EDT (the channel waits for it) */
    boolean runsOnEdt();

    /**
     * Executes the command.
     *
     * @param args command arguments
     * @return result; never <code>null</code>
     * @throws Exception any exception is logged with its stack trace and reported as FAIL
     */
    LarvaDevCommandResult execute( List< String > args ) throws Exception;

}
