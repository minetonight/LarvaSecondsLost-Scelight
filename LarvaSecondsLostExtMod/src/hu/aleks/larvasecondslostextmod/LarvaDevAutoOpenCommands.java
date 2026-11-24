package hu.aleks.larvasecondslostextmod;

import java.util.ArrayList;
import java.util.List;

/**
 * E17-S07 dev command {@code auto-open-state}: pending queue length, caps, counters and the last decisions of auto-open.
 * FR-17 itself is tested by copying replays into a monitored folder.
 */
final class LarvaDevAutoOpenCommands {

    private LarvaDevAutoOpenCommands() {
    }

    /**
     * {@code auto-open-submit <path>}: hands a file to the auto-open queue exactly like the replay listener does (for
     * dedupe, cap and lifecycle checks that need a path Scelight would not report twice). Dev-only.
     */
    private static LarvaDevCommand submit( final LarvaSecondsLostModule module ) {
        return new LarvaDevCommand() {
            @Override
            public String getName() {
                return "auto-open-submit";
            }

            @Override
            public boolean runsOnEdt() {
                return false;
            }

            @Override
            public LarvaDevCommandResult execute( final List< String > args ) {
                if ( args.size() != 1 )
                    return LarvaDevCommandResult.fail( "usage: auto-open-submit <path>" );
                final LarvaSingleReplayAnalysisQueue queue = module.getAnalysisQueue();
                if ( queue == null )
                    return LarvaDevCommandResult.fail( "queue not created yet" );
                queue.submitAutoOpen( java.nio.file.Paths.get( args.get( 0 ) ) );
                return LarvaDevCommandResult.ok( "submitted " + args.get( 0 ) );
            }
        };
    }

    static void registerAll( final LarvaDevCommandRegistry registry, final LarvaSecondsLostModule module ) {
        registry.register( submit( module ) );
        registry.register( new LarvaDevCommand() {
            @Override
            public String getName() {
                return "auto-open-state";
            }

            @Override
            public boolean runsOnEdt() {
                return true;
            }

            @Override
            public LarvaDevCommandResult execute( final List< String > args ) {
                final LarvaSingleReplayAnalysisQueue queue = module.getAnalysisQueue();
                final LarvaPageNavigator navigator = module.getPageNavigator();
                if ( queue == null || navigator == null )
                    return LarvaDevCommandResult.fail( "queue or navigator not created yet" );
                final int open = navigator.isReady() ? navigator.getSubPages( LarvaSubPageKind.SINGLE_REPLAY ).size() : -1;
                final List< String > lines = new ArrayList< String >();
                for ( final String decision : queue.getAutoOpenStats().lastDecisions() )
                    lines.add( decision );
                return LarvaDevCommandResult.ok( "pending=" + queue.getPendingAutoOpen().getPending() + "/" + queue.getPendingAutoOpen().getMax()
                        + " openSinglePages=" + open + "/" + LarvaAutoOpenPolicy.MAX_AUTO_OPEN_SINGLE_PAGES + " ready=" + navigator.isReady() + " "
                        + queue.getAutoOpenStats().summary(), lines );
            }
        } );
    }

}
