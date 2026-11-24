package hu.aleks.larvasecondslostextmod;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import javax.swing.JFileChooser;

/**
 * E17-S17 dev commands for the remembered chooser folders (CR-1.5). They build the real choosers without showing them and
 * feed a "confirmed selection" through the same method the chooser result uses.
 *
 * <ul>
 * <li>{@code chooser-folder-state}: memory file, remembered folder and start folder of both choosers, and the current
 * directory of the freshly built (not shown) choosers</li>
 * <li>{@code chooser-folder-confirm <open|multi> <path>}: as if the user confirmed a selection of that file</li>
 * </ul>
 */
final class LarvaDevChooserCommands {

    private final LarvaSecondsLostModule module;

    private LarvaDevChooserCommands( final LarvaSecondsLostModule module ) {
        this.module = module;
    }

    static void registerAll( final LarvaDevCommandRegistry registry, final LarvaSecondsLostModule module ) {
        final LarvaDevChooserCommands c = new LarvaDevChooserCommands( module );
        registry.register( c.state() );
        registry.register( c.confirm() );
    }

    private LarvaDevCommand state() {
        return new Cmd( "chooser-folder-state" ) {
            @Override
            LarvaDevCommandResult run( final List< String > args ) {
                final LarvaReplayFileChooser chooser = module.getHubActions().getFileChooser();
                final LarvaChooserFolderMemory memory = chooser.getFolderMemory();
                final Path lastDetected = module.getLastDetectedReplayPath();
                final JFileChooser open = chooser.newReplayChooser( lastDetected ).asFileChooser();
                final JFileChooser multi = chooser.newMultiChooser( lastDetected ).asFileChooser();
                return LarvaDevCommandResult.ok( "memoryFile=" + memory.getFile() + " lastDetected=" + lastDetected,
                        java.util.Arrays.asList( describe( "open", LarvaChooserFolderMemory.Kind.OPEN_REPLAY, memory, lastDetected, open ),
                                describe( "multi", LarvaChooserFolderMemory.Kind.MULTI_ANALYSIS, memory, lastDetected, multi ) ) );
            }
        };
    }

    private static String describe( final String label, final LarvaChooserFolderMemory.Kind kind, final LarvaChooserFolderMemory memory,
            final Path lastDetected, final JFileChooser chooser ) {
        final Path remembered = memory.getRemembered( kind );
        return label + ": remembered=" + remembered + " exists=" + ( remembered != null && java.nio.file.Files.isDirectory( remembered ) )
                + " startFolder=" + memory.resolveStartFolder( kind, lastDetected ) + " chooserCurrentDirectory=" + chooser.getCurrentDirectory();
    }

    private LarvaDevCommand confirm() {
        return new Cmd( "chooser-folder-confirm" ) {
            @Override
            LarvaDevCommandResult run( final List< String > args ) {
                if ( args.size() != 2 )
                    return LarvaDevCommandResult.fail( "usage: chooser-folder-confirm <open|multi> <path>" );
                final LarvaChooserFolderMemory.Kind kind = "open".equals( args.get( 0 ) ) ? LarvaChooserFolderMemory.Kind.OPEN_REPLAY
                        : "multi".equals( args.get( 0 ) ) ? LarvaChooserFolderMemory.Kind.MULTI_ANALYSIS : null;
                if ( kind == null )
                    return LarvaDevCommandResult.fail( "first argument must be open or multi" );
                final LarvaReplayFileChooser chooser = module.getHubActions().getFileChooser();
                chooser.rememberSelection( kind, Paths.get( args.get( 1 ) ) );
                return LarvaDevCommandResult.ok( "remembered " + kind + " = " + chooser.getFolderMemory().getRemembered( kind ) );
            }
        };
    }

    private abstract static class Cmd implements LarvaDevCommand {
        private final String name;

        Cmd( final String name ) {
            this.name = name;
        }

        abstract LarvaDevCommandResult run( List< String > args );

        @Override
        public String getName() {
            return name;
        }

        @Override
        public boolean runsOnEdt() {
            return true;
        }

        @Override
        public LarvaDevCommandResult execute( final List< String > args ) throws Exception {
            return run( args );
        }
    }
}
