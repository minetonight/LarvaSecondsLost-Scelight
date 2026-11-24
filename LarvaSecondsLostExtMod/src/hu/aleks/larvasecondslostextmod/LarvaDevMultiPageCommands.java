package hu.aleks.larvasecondslostextmod;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

/**
 * E17-S14 dev commands for multi-analysis pages. They bypass only the file chooser and the mouse. Pages are addressed
 * by their 0-based index among the multi pages in tree order (labels are not unique while several runs are going).
 *
 * <ul>
 * <li>{@code multi-open <dir|listFile|replay> [true|false] [confirm=yes|no]}: the Multi Analysis button after the chooser;
 * the confirm dialog (above 500 files) is answered with {@code confirm} (default yes) and its text is reported</li>
 * <li>{@code multi-page-state <index> [full]}: label, state, k/N, failed, and what the component shows</li>
 * <li>{@code multi-page-snapshot <index> <png file>}: paints the whole results panel (not only the visible part) into a PNG (S15)</li>
 * <li>{@code multi-page-toggle-skipped <index>}: expands or collapses the skipped section like its button (S15)</li>
 * <li>{@code multi-page-cancel <index>}: asks the run of that page to stop (ends in CANCELLED)</li>
 * <li>{@code multi-page-close <index>}: selects the page and clicks the host's close button (like the X)</li>
 * <li>{@code multi-chooser-config}: builds the real multi chooser (not shown) and reports its configuration</li>
 * <li>{@code set-default-locale <language> [country]}: sets the JVM default locale (dev only, to check the label in
 * {@code bg_BG} without a launcher start, which Scelight treats as an unregistered system)</li>
 * </ul>
 */
final class LarvaDevMultiPageCommands {

    private static final String SAMPLE_REPLAY = "x.SC2Replay";
    private static final String SAMPLE_OTHER = "x.txt";

    private final LarvaSecondsLostModule module;

    private LarvaDevMultiPageCommands( final LarvaSecondsLostModule module ) {
        this.module = module;
    }

    static void registerAll( final LarvaDevCommandRegistry registry, final LarvaSecondsLostModule module ) {
        final LarvaDevMultiPageCommands c = new LarvaDevMultiPageCommands( module );
        registry.register( c.open() );
        registry.register( c.pageState() );
        registry.register( c.pageToggleSkipped() );
        registry.register( c.pageSnapshot() );
        registry.register( c.pageCancel() );
        registry.register( c.pageClose() );
        registry.register( c.chooserConfig() );
        registry.register( c.setLocale() );
    }

    private LarvaDevCommand open() {
        return new Cmd( "multi-open" ) {
            @Override
            LarvaDevCommandResult run( final List< String > args ) throws IOException {
                final List< String > plain = new ArrayList< String >( args );
                final boolean confirmAnswer = !plain.remove( "confirm=no" );
                plain.remove( "confirm=yes" );
                if ( plain.isEmpty() )
                    return LarvaDevCommandResult.fail( "usage: multi-open <dir|listFile|replay> [true|false] [confirm=yes|no]" );
                final List< Path > files = LarvaDevMultiCommands.resolveFiles( Paths.get( plain.get( 0 ) ) );
                if ( files.isEmpty() )
                    return LarvaDevCommandResult.fail( "no replay files found for " + plain.get( 0 ) );
                final boolean ladderOnly = plain.size() < 2 || !"false".equalsIgnoreCase( plain.get( 1 ) );
                return openPage( files, ladderOnly, confirmAnswer );
            }
        };
    }

    private LarvaDevCommandResult openPage( final List< Path > files, final boolean ladderOnly, final boolean confirmAnswer ) {
        final List< String > asked = new ArrayList< String >();
        final LarvaMultiAnalysisPageController controller = module.getHubActions().openMultiAnalysisPage( files, ladderOnly,
                new LarvaModuleHubActions.Confirmer() {
                    @Override
                    public boolean confirm( final String message ) {
                        asked.add( message );
                        return confirmAnswer;
                    }
                } );
        final String confirmText = asked.isEmpty() ? "confirmAsked=false" : "confirmAsked=true answer=" + ( confirmAnswer ? "yes" : "no" )
                + " dialogText='" + asked.get( 0 ) + "'";
        if ( controller == null )
            return asked.isEmpty() ? LarvaDevCommandResult.fail( "page not opened (navigator not ready)" )
                    : LarvaDevCommandResult.ok( "declined, no page; " + confirmText + " pages=" + pages().size() );
        return LarvaDevCommandResult.ok( "opened new page '" + controller.getLabel() + "' index=" + ( pages().size() - 1 ) + " files=" + files.size()
                + " ladderOnly=" + ladderOnly + " pages=" + pages().size() + " " + confirmText );
    }

    private LarvaDevCommand pageState() {
        return new Cmd( "multi-page-state" ) {
            @Override
            LarvaDevCommandResult run( final List< String > args ) {
                final LarvaSubPage page = pageOf( args );
                if ( page == null )
                    return LarvaDevCommandResult.fail( "usage: multi-page-state <index> [full]; no such multi page (pages=" + pages().size() + ")" );
                return describe( page, args.contains( "full" ) );
            }
        };
    }

    private LarvaDevCommandResult describe( final LarvaSubPage page, final boolean full ) {
        final LarvaMultiAnalysisPageController c = page.getMultiController();
        final StringBuilder sb = new StringBuilder();
        sb.append( "label='" ).append( page.getDisplayName() ).append( "' state=" ).append( c.getState() ).append( " k=" ).append( c.getCurrentK() )
                .append( '/' ).append( c.getTotal() ).append( " failed=" ).append( c.getFailedCount() ).append( " ladderOnly=" ).append( c.isLadderOnly() );
        final List< String > detail = new ArrayList< String >();
        final LarvaMultiAnalysisView view = c.getView();
        if ( view instanceof LarvaMultiAnalysisPageComp ) {
            final LarvaMultiAnalysisPageComp comp = (LarvaMultiAnalysisPageComp) view;
            sb.append( " comp[status='" ).append( comp.getStatusText() ).append( "' failedText='" ).append( comp.getFailedText() )
                    .append( "' bar=" ).append( comp.getProgressValue() ).append( '/' ).append( comp.getProgressMaximum() ).append( " header='" )
                    .append( comp.getResultHeaderText() ).append( "']" );
            if ( comp.getResultsPanel() != null ) {
                detail.addAll( describeLayout( comp ) );
                if ( full )
                    detail.addAll( Arrays.asList( comp.getResultsPanel().getModel().toText().split( "\n" ) ) );
            }
        } else
            sb.append( " comp=notCreated" );
        return LarvaDevCommandResult.ok( sb.toString(), detail );
    }

    /** Layout facts of the results view, so an agent can check AC-14.1 and OQ-1 without a screenshot (E17-S15). */
    private static List< String > describeLayout( final LarvaMultiAnalysisPageComp comp ) {
        final LarvaMultiAnalysisResultsPanel panel = comp.getResultsPanel();
        final List< String > lines = new ArrayList< String >();
        final javax.swing.JScrollPane scroll = (javax.swing.JScrollPane) javax.swing.SwingUtilities.getAncestorOfClass( javax.swing.JScrollPane.class, comp );
        lines.add( "layout header='" + panel.getHeaderText() + "'" );
        lines.add( "layout summary='" + panel.getSummaryText() + "'" );
        lines.add( "layout help='" + panel.getHelpText() + "' empty='" + panel.getEmptyText() + "'" );
        lines.add( "layout panel=" + panel.getWidth() + "x" + panel.getHeight() + " preferred=" + panel.getPreferredSize().width + "x"
                + panel.getPreferredSize().height + " pageScrollPane=" + ( scroll == null ? "none" : "viewport=" + scroll.getViewport().getWidth() + "x"
                        + scroll.getViewport().getHeight() + " horizontalScrollBar=" + scroll.getHorizontalScrollBar().isVisible()
                        + " verticalScrollBar=" + scroll.getVerticalScrollBar().isVisible() ) );
        boolean anyClipped = false;
        for ( int column = 0; column < panel.getColumnCount(); column++ ) {
            final java.awt.Rectangle c = panel.getColumnBounds( column );
            lines.add( "layout column[" + column + "] x=" + c.x + " width=" + c.width + " tables=" + panel.getTables( column ).size() );
            for ( final LarvaTableComp table : panel.getTables( column ) ) {
                final java.awt.Rectangle t = panel.boundsInPanel( table );
                final boolean clipped = LarvaMultiAnalysisResultsPanel.isClipped( table );
                anyClipped |= clipped;
                lines.add( "layout column[" + column + "] table title='" + table.getModel().getTitle() + "' x=" + t.x + " y=" + t.y + " width="
                        + t.width + " height=" + t.height + " preferredWidth=" + table.getPreferredSize().width + " clipped=" + clipped );
            }
        }
        lines.add( "layout anyTableClipped=" + anyClipped + " skipped=" + panel.getModel().getSkippedLines().size() + " skippedExpanded="
                + panel.isSkippedExpanded() + " toggle='" + panel.getSkippedToggleText() + "'" );
        return lines;
    }

    private LarvaDevCommand pageToggleSkipped() {
        return new Cmd( "multi-page-toggle-skipped" ) {
            @Override
            LarvaDevCommandResult run( final List< String > args ) {
                final LarvaSubPage page = pageOf( args );
                if ( page == null )
                    return LarvaDevCommandResult.fail( "usage: multi-page-toggle-skipped <index>; no such multi page" );
                final LarvaMultiAnalysisView view = page.getMultiController().getView();
                if ( !( view instanceof LarvaMultiAnalysisPageComp ) || ( (LarvaMultiAnalysisPageComp) view ).getResultsPanel() == null )
                    return LarvaDevCommandResult.fail( "no results panel (page not done or component not created yet)" );
                final LarvaMultiAnalysisResultsPanel panel = ( (LarvaMultiAnalysisPageComp) view ).getResultsPanel();
                panel.toggleSkipped();
                return LarvaDevCommandResult.ok( "skippedExpanded=" + panel.isSkippedExpanded() + " toggle='" + panel.getSkippedToggleText() + "'",
                        Arrays.asList( panel.getSkippedListText().split( "\n" ) ) );
            }
        };
    }

    private LarvaDevCommand pageSnapshot() {
        return new Cmd( "multi-page-snapshot" ) {
            @Override
            LarvaDevCommandResult run( final List< String > args ) throws IOException {
                final LarvaSubPage page = pageOf( args );
                if ( page == null || args.size() != 2 )
                    return LarvaDevCommandResult.fail( "usage: multi-page-snapshot <index> <png file>; no such multi page" );
                final LarvaMultiAnalysisView view = page.getMultiController().getView();
                if ( !( view instanceof LarvaMultiAnalysisPageComp ) || ( (LarvaMultiAnalysisPageComp) view ).getResultsPanel() == null )
                    return LarvaDevCommandResult.fail( "no results panel (page not done or component not created yet)" );
                final LarvaMultiAnalysisResultsPanel panel = ( (LarvaMultiAnalysisPageComp) view ).getResultsPanel();
                final java.awt.image.BufferedImage image = new java.awt.image.BufferedImage( Math.max( 1, panel.getWidth() ),
                        Math.max( 1, panel.getHeight() ), java.awt.image.BufferedImage.TYPE_INT_RGB );
                final java.awt.Graphics2D g = image.createGraphics();
                try {
                    g.setColor( panel.getBackground() );
                    g.fillRect( 0, 0, image.getWidth(), image.getHeight() );
                    panel.paint( g );
                } finally {
                    g.dispose();
                }
                javax.imageio.ImageIO.write( image, "png", new java.io.File( args.get( 1 ) ) );
                return LarvaDevCommandResult.ok( "wrote " + image.getWidth() + "x" + image.getHeight() + " to " + args.get( 1 ) );
            }
        };
    }

    private LarvaDevCommand pageCancel() {
        return new Cmd( "multi-page-cancel" ) {
            @Override
            LarvaDevCommandResult run( final List< String > args ) {
                final LarvaSubPage page = pageOf( args );
                if ( page == null )
                    return LarvaDevCommandResult.fail( "usage: multi-page-cancel <index>; no such multi page" );
                final LarvaMultiAnalysisPageController c = page.getMultiController();
                if ( c.getState() != LarvaMultiAnalysisPageController.State.RUNNING )
                    return LarvaDevCommandResult.fail( "page is not running: " + c.getState() );
                c.cancelRun();
                return LarvaDevCommandResult.ok( "cancel requested at " + c.getCurrentK() + "/" + c.getTotal() );
            }
        };
    }

    private LarvaDevCommand pageClose() {
        return new Cmd( "multi-page-close" ) {
            @Override
            LarvaDevCommandResult run( final List< String > args ) {
                final LarvaSubPage page = pageOf( args );
                if ( page == null )
                    return LarvaDevCommandResult.fail( "usage: multi-page-close <index>; no such multi page" );
                final LarvaMultiAnalysisPageController c = page.getMultiController();
                final LarvaDevCommandResult result = LarvaDevPageCommands.closePage( module, page );
                return result.isOk() ? LarvaDevCommandResult.ok( result.getSummary() + " controllerDisposed=" + c.isDisposed() ) : result;
            }
        };
    }

    private LarvaDevCommand chooserConfig() {
        return new Cmd( "multi-chooser-config" ) {
            @Override
            LarvaDevCommandResult run( final List< String > args ) {
                final JFileChooser chooser = module.getHubActions().getFileChooser().newMultiChooser( module.getLastDetectedReplayPath() ).asFileChooser();
                final FileFilter filter = chooser.getFileFilter();
                return LarvaDevCommandResult.ok( "multiSelection=" + chooser.isMultiSelectionEnabled() + " filesOnly="
                        + ( chooser.getFileSelectionMode() == JFileChooser.FILES_ONLY ) + " filter='" + filter.getDescription() + "' accepts("
                        + SAMPLE_REPLAY + ")=" + filter.accept( new java.io.File( SAMPLE_REPLAY ) ) + " accepts(" + SAMPLE_OTHER + ")="
                        + filter.accept( new java.io.File( SAMPLE_OTHER ) ) + " currentDirectory=" + chooser.getCurrentDirectory()
                        + " lastDetected=" + module.getLastDetectedReplayPath() + " (built, not shown)" );
            }
        };
    }

    private LarvaDevCommand setLocale() {
        return new Cmd( "set-default-locale" ) {
            @Override
            LarvaDevCommandResult run( final List< String > args ) {
                if ( args.isEmpty() || args.size() > 2 )
                    return LarvaDevCommandResult.fail( "usage: set-default-locale <language> [country]" );
                final java.util.Locale locale = args.size() == 1 ? new java.util.Locale( args.get( 0 ) ) : new java.util.Locale( args.get( 0 ), args.get( 1 ) );
                java.util.Locale.setDefault( locale );
                return LarvaDevCommandResult.ok( "default locale=" + java.util.Locale.getDefault() + " javaVersion=" + System.getProperty( "java.version" ) );
            }
        };
    }

    private List< LarvaSubPage > pages() {
        return module.getPageNavigator().getSubPages( LarvaSubPageKind.MULTI_ANALYSIS );
    }

    private LarvaSubPage pageOf( final List< String > args ) {
        if ( args.isEmpty() )
            return null;
        try {
            final int index = Integer.parseInt( args.get( 0 ) );
            final List< LarvaSubPage > pages = pages();
            return index >= 0 && index < pages.size() ? pages.get( index ) : null;
        } catch ( final NumberFormatException e ) {
            return null;
        }
    }

    private abstract static class Cmd implements LarvaDevCommand {
        private final String name;

        Cmd( final String name ) {
            this.name = name;
        }

        abstract LarvaDevCommandResult run( List< String > args ) throws IOException;

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
