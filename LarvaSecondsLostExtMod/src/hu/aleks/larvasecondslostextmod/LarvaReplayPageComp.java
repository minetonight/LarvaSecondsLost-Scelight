package hu.aleks.larvasecondslostextmod;

import hu.scelightapibase.gui.comp.multipage.IPageDisposedListener;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.nio.file.Path;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.SwingConstants;

/**
 * Content of one single-replay sub-page, bound to exactly one replay through its {@link LarvaReplayPageController}.
 * It only renders what the controller tells it (EDT only); it never starts an analysis by itself (no auto-latest).
 */
@SuppressWarnings("serial")
public class LarvaReplayPageComp extends JPanel implements LarvaReplayPageView, IPageDisposedListener {

    /** Owning external module. */
    private final LarvaSecondsLostModule module;

    /** State holder of this page. */
    private final LarvaReplayPageController controller;

    /** Behavior of the Open Replay and Analyze Latest buttons (same as on the hub). */
    private final LarvaHubActions actions;

    /** Status label displayed above the replay summary. */
    private final JLabel statusLabel;

    /** Text area that shows replay diagnostics. */
    private final JTextArea detailsArea;

    /** Timeline preview component. */
    private final LarvaTimelinePreviewComp timelinePreviewComp;

    /** Formats compact player-facing benchmark tier strings. */
    private final LarvaPhaseBenchmarkRatingFormatter phaseBenchmarkRatingFormatter;

    /**
     * Creates the component and renders the controller's current state (the component is created lazily, so the
     * analysis may already be finished).
     *
     * @param module owning external module
     * @param controller state holder of this page
     * @param actions behavior of the Open Replay and Analyze Latest buttons
     */
    LarvaReplayPageComp( final LarvaSecondsLostModule module, final LarvaReplayPageController controller, final LarvaHubActions actions ) {
        super( new BorderLayout( 10, 10 ) );

        this.module = module;
        this.controller = controller;
        this.actions = actions;

        statusLabel = new JLabel( "Larva timeline ready.", module.getLarvaIcon().get(), SwingConstants.LEADING );
        detailsArea = new JTextArea();
        timelinePreviewComp = new LarvaTimelinePreviewComp();
        phaseBenchmarkRatingFormatter = new LarvaPhaseBenchmarkRatingFormatter();

        buildGui();
        controller.attach( this );
    }

    /** Closing the page: pending results of this page are dropped from now on. */
    @Override
    public void pageDisposed() {
        controller.dispose();
    }

    /** @return the status line text, for the dev command {@code page-text} */
    String getStatusText() {
        return statusLabel.getText();
    }

    /** @return the details text, for the dev command {@code page-text} */
    String getDetailsText() {
        return detailsArea.getText();
    }

    /**
     * Builds the page UI.
     */
    private void buildGui() {
        setBorder( BorderFactory.createEmptyBorder( 12, 12, 12, 12 ) );

        final JLabel helpLabel = module.guiFactory.newHelpIcon( module.getHelpContent() ).asLabel();
        final JPanel headerPanel = new JPanel( new BorderLayout( 10, 10 ) );
        headerPanel.add( statusLabel, BorderLayout.CENTER );
        headerPanel.add( helpLabel, BorderLayout.EAST );

        final JPanel buttonPanel = new JPanel( new FlowLayout( FlowLayout.LEFT, 8, 0 ) );
        buttonPanel.add( createOpenReplayButton() );
        buttonPanel.add( createAnalyzeLatestReplayButton() );
        buttonPanel.add( createRefreshButton() );

        final JPanel northPanel = new JPanel( new BorderLayout( 0, 8 ) );
        northPanel.add( headerPanel, BorderLayout.NORTH );
        northPanel.add( buttonPanel, BorderLayout.SOUTH );

        detailsArea.setEditable( false );
        detailsArea.setLineWrap( true );
        detailsArea.setWrapStyleWord( true );
        detailsArea.setRows( 8 );
        detailsArea.setBorder( BorderFactory.createEmptyBorder( 8, 8, 8, 8 ) );

        final JSplitPane splitPane = new JSplitPane( JSplitPane.VERTICAL_SPLIT, timelinePreviewComp, new JScrollPane( detailsArea ) );
        splitPane.setBorder( BorderFactory.createEmptyBorder() );
        splitPane.setResizeWeight( 0.95 );
        SwingUtilities.invokeLater( new Runnable() {
            @Override
            public void run() {
                splitPane.setDividerLocation( 0.95d );
            }
        } );

        add( northPanel, BorderLayout.NORTH );
        add( splitPane, BorderLayout.CENTER );
    }

    /**
     * Creates the button used to pick a replay file manually; the replay opens on a new page (or focuses its page).
     *
     * @return the configured button
     */
    private JButton createOpenReplayButton() {
        final JButton button = new JButton( "Open Replay..." );
        button.addActionListener( new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed( final java.awt.event.ActionEvent event ) {
                actions.openReplay( LarvaReplayPageComp.this );
            }
        } );

        return button;
    }

    /**
     * Creates the button used to analyze the latest replay that Scelight would consider current, on a new page.
     *
     * @return the configured button
     */
    private JButton createAnalyzeLatestReplayButton() {
        final JButton button = new JButton( "Analyze Latest Replay" );
        button.addActionListener( new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed( final java.awt.event.ActionEvent event ) {
                actions.analyzeLatestReplay();
            }
        } );

        return button;
    }

    /**
     * Creates the button used to re-analyze this page's replay in place.
     *
     * @return the configured button
     */
    private JButton createRefreshButton() {
        final JButton button = new JButton( "Refresh Current Replay" );
        button.addActionListener( new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed( final java.awt.event.ActionEvent event ) {
                controller.requestAnalysis( LarvaModuleHubActions.SOURCE_REFRESH );
            }
        } );

        return button;
    }

    /**
     * Shows a busy state while replay analysis is running.
     *
     * @param replayFile replay being analyzed
     * @param sourceDescription source of the request
     */
    @Override
    public void showBusy( final Path replayFile, final String sourceDescription ) {
        if ( !requireEdt( "showBusy" ) )
            return;
        statusLabel.setText( "Analyzing replay..." );
        timelinePreviewComp.setTimelineModel( null );
        detailsArea.setText( "Analyzing " + ( replayFile == null ? "replay" : replayFile.getFileName() ) + "..." );
    }

    /**
     * Displays a replay summary on the page.
     *
     * @param summary replay summary to display
     */
    @Override
    public void showSummary( final LarvaReplayPageSummary summary ) {
        if ( !requireEdt( "showSummary" ) )
            return;
        statusLabel.setText( "Replay ready: " + summary.getReplaySummary().getReplayFile().getFileName() );
        timelinePreviewComp.setTimelineModel( summary.getTimelineModel() );
        detailsArea.setText( buildSummaryText( summary ) );
        detailsArea.setCaretPosition( 0 );
    }

    /**
     * Displays an error state on the page.
     *
     * @param replayFile replay file that failed to analyze
     * @param sourceDescription source of the request
     * @param errorMessage error message to show
     */
    @Override
    public void showError( final Path replayFile, final String sourceDescription, final String errorMessage ) {
        if ( !requireEdt( "showError" ) )
            return;
        statusLabel.setText( "Replay could not be analyzed." );
        timelinePreviewComp.setTimelineModel( null );
        detailsArea.setText( "Could not analyze " + ( replayFile == null ? "the selected replay" : replayFile.getFileName() ) + ".\n\n"
            + "Reason:\n"
                + ( errorMessage == null ? "Unknown error" : errorMessage ) );
        detailsArea.setCaretPosition( 0 );
    }

    /** Component methods must only run on the EDT (AC-9); a violation is logged and the update skipped. */
    private boolean requireEdt( final String operation ) {
        if ( SwingUtilities.isEventDispatchThread() )
            return true;
        module.logger.error( "[larva-replay] " + operation + " called off the EDT on thread '" + Thread.currentThread().getName() + "'; ignored" );
        return false;
    }

    /**
     * Builds the multi-line summary text shown for a replay.
     *
     * @param summary replay summary to render
     * @return rendered summary text
     */
    private String buildSummaryText( final LarvaReplayPageSummary summary ) {
        final ReplaySummary replaySummary = summary.getReplaySummary();
        final StringBuilder builder = new StringBuilder();

        builder.append( buildReplayMetadataSection( replaySummary ) ).append( '\n' );
        builder.append( buildPlayerTotalsSection( summary ) ).append( '\n' );
        builder.append( buildPlayerPhaseSection( summary ) ).append( '\n' );
        builder.append( buildHatcheryBreakdownSection( summary ) ).append( '\n' );
        builder.append( "Legend:\n" );
        builder.append( "- Phase table: Early, Mid, Late, End columns based on sustained worker-count thresholds.\n" );
        builder.append( "- Ranking rows: compact percentile-inside-tier estimates using gold / plat / dia / masters / pro benchmark anchors.\n" );
        builder.append( "- Red bars: time spent at 3+ larva.\n" );
        builder.append( "- Green lanes: inject-active uptime inferred from replay-derived bursts where 3 larva appear within 8 loops.\n" );
        builder.append( "- Dark red lanes: conservative missed inject potential windows backed by singleton-queen command attribution and the dedicated queen radius.\n" );
        builder.append( "- Black ticks on the main rail: every 11 seconds of missed potential larva while a hatchery stayed at 3+ larva.\n" );
        builder.append( "- Black ticks on the dark red lane: every 29 seconds of accumulated missed inject potential, worth 3 potential larva per hatchery.\n" );
        builder.append( "- Hover a bar, lane, or tick for resource and supply context." );

        return builder.toString();
    }

    /**
     * Builds the replay metadata section.
     *
     * @param replaySummary core replay metadata summary
     * @return rendered replay metadata section
     */
    private String buildReplayMetadataSection( final ReplaySummary replaySummary ) {
        final StringBuilder builder = new StringBuilder();
        builder.append( "Replay overview:" ).append( '\n' );
        builder.append( "Replay: " ).append( replaySummary.getReplayFile().getFileName() ).append( '\n' );
        builder.append( "Map: " ).append( replaySummary.getMapTitle() ).append( '\n' );
        builder.append( "Players: " ).append( replaySummary.getPlayers() ).append( '\n' );
        builder.append( "Winners: " ).append( replaySummary.getWinners() ).append( '\n' );
        builder.append( "Length: " ).append( replaySummary.getLength() ).append( '\n' );
        builder.append( "Played: " ).append( replaySummary.getReplayEndTime() ).append( '\n' );
        return builder.toString();
    }

    /**
     * Builds the per-player totals section.
     *
     * @param summary page-level replay summary
     * @return rendered totals section
     */
    private String buildPlayerTotalsSection( final LarvaReplayPageSummary summary ) {
        final StringBuilder builder = new StringBuilder();
        builder.append( "Player totals:" ).append( '\n' );
        if ( summary.getTimelineModel() == null || summary.getTimelineModel().getGroupOverviewLabelMap().isEmpty() ) {
            builder.append( "- No missed larva warnings were found." ).append( '\n' );
            return builder.toString();
        }

        for ( final java.util.Map.Entry< String, String > entry : summary.getTimelineModel().getGroupOverviewLabelMap().entrySet() )
            builder.append( "- " ).append( entry.getKey() ).append( ": " ).append( entry.getValue() ).append( '\n' );
        return builder.toString();
    }

    /**
     * Builds the per-player phase summary section.
     *
     * @param summary page-level replay summary
     * @return rendered phase section
     */
    private String buildPlayerPhaseSection( final LarvaReplayPageSummary summary ) {
        final StringBuilder builder = new StringBuilder();
        builder.append( "Player phase table:" ).append( '\n' );
        if ( summary.getTimelineModel() == null || summary.getTimelineModel().getPlayerPhaseTableMap().isEmpty() ) {
            builder.append( "- No phase table data is available." ).append( '\n' );
            return builder.toString();
        }

        for ( final java.util.Map.Entry< String, LarvaPlayerPhaseTable > entry : summary.getTimelineModel().getPlayerPhaseTableMap().entrySet() ) {
            builder.append( "- " ).append( entry.getKey() ).append( ": " );
            boolean first = true;
            for ( final LarvaGamePhase phase : LarvaGamePhase.values() ) {
                final LarvaPhaseStats stats = entry.getValue().getPhaseStats( phase );
                if ( !first )
                    builder.append( " | " );
                builder.append( phase.getDisplayLabel() )
                        .append( ' ' )
                        .append( LarvaStatFormat.formatRate( stats == null ? null : stats.getMissedLarvaPerHatchPerMinute() ) )
                        .append( '/' )
                        .append( LarvaStatFormat.formatRate( stats == null ? null : stats.getMissedInjectLarvaPerHatchPerMinute() ) )
                        .append( '/' )
                        .append( LarvaStatFormat.formatRate( stats == null ? null : stats.getSpawnedLarvaPerHatchPerMinute() ) )
                        .append( '/' )
                        .append( LarvaStatFormat.formatPercent( stats == null ? null : stats.getInjectUptimePercentage() ) );
                first = false;
            }
            builder.append( '\n' );
            builder.append( "  larva gen ranking: " );
            appendPhaseRankingLine( builder, entry.getValue(), true );
            builder.append( '\n' );
            builder.append( "  larva missed ranking: " );
            appendPhaseRankingLine( builder, entry.getValue(), false );
            builder.append( '\n' );
        }

        builder.append( "  order = missed larva / missed inject / spawned / inject uptime" ).append( '\n' );
        return builder.toString();
    }

    /**
     * Appends one compact per-phase ranking line.
     */
    private void appendPhaseRankingLine( final StringBuilder builder, final LarvaPlayerPhaseTable playerPhaseTable, final boolean spawnedLarva ) {
        boolean first = true;
        for ( final LarvaGamePhase phase : LarvaGamePhase.values() ) {
            final LarvaPhaseStats phaseStats = playerPhaseTable == null ? null : playerPhaseTable.getPhaseStats( phase );
            if ( !first )
                builder.append( " | " );
            builder.append( phase.getDisplayLabel() ).append( ' ' )
                    .append( spawnedLarva
                            ? phaseBenchmarkRatingFormatter.formatSpawnedLarvaRanking( phase,
                                    phaseStats == null ? null : phaseStats.getSpawnedLarvaPerHatchPerMinute() )
                            : phaseBenchmarkRatingFormatter.formatMissedLarvaRanking( phase,
                                    phaseStats == null ? null : phaseStats.getMissedLarvaPerHatchPerMinute() ) );
            first = false;
        }
    }

    /**
     * Builds the per-hatchery breakdown section.
     *
     * @param summary page-level replay summary
     * @return rendered hatchery breakdown section
     */
    private String buildHatcheryBreakdownSection( final LarvaReplayPageSummary summary ) {
        final StringBuilder builder = new StringBuilder();
        builder.append( "Hatchery breakdown:" ).append( '\n' );
        if ( summary.getTimelineModel() == null || summary.getTimelineModel().getRowList().isEmpty() ) {
            builder.append( "- No qualifying hatchery rows are available." ).append( '\n' );
            return builder.toString();
        }

        for ( final LarvaTimelineRow row : summary.getTimelineModel().getRowList() ) {
            if ( "Replay context".equals( row.getGroupLabel() ) )
                continue;

            builder.append( "- " )
                .append( row.getGroupLabel() )
                .append( " / " )
                .append( row.getRowLabel() )
                .append( ": " )
                .append( row.getDetailLabel() );
            if ( row.getSecondaryDetailLabel() != null && row.getSecondaryDetailLabel().length() > 0 )
                builder.append( "; " ).append( row.getSecondaryDetailLabel() );
            builder.append( '\n' );
        }
        return builder.toString();
    }

}