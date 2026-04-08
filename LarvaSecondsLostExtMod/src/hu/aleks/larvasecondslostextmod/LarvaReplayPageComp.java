package hu.aleks.larvasecondslostextmod;

import hu.scelightapibase.gui.comp.multipage.IPageSelectedListener;
import hu.scelightapibase.gui.comp.IFileChooser;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.nio.file.Path;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.SwingConstants;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * Module-owned replay diagnostics page used as the Epic 2 fallback replay-view surface.
 */
@SuppressWarnings("serial")
public class LarvaReplayPageComp extends JPanel implements IPageSelectedListener, HierarchyListener {

    /** Owning external module. */
    private final LarvaSecondsLostModule module;

    /** Status label displayed above the replay summary. */
    private final JLabel statusLabel;

    /** Text area that shows replay diagnostics. */
    private final JTextArea detailsArea;

    /** Timeline preview component. */
    private final LarvaTimelinePreviewComp timelinePreviewComp;

    /** Formats compact player-facing benchmark tier strings. */
    private final LarvaPhaseBenchmarkRatingFormatter phaseBenchmarkRatingFormatter;

    /** Last replay file displayed on this page. */
    private Path currentReplayFile;

    /**
     * Creates a new replay page component.
     *
     * @param module owning external module
     */
    public LarvaReplayPageComp( final LarvaSecondsLostModule module ) {
        super( new BorderLayout( 10, 10 ) );

        this.module = module;

        statusLabel = new JLabel( "Larva timeline ready.", module.getLarvaIcon().get(), SwingConstants.LEADING );
        detailsArea = new JTextArea();
        timelinePreviewComp = new LarvaTimelinePreviewComp();
        phaseBenchmarkRatingFormatter = new LarvaPhaseBenchmarkRatingFormatter();

        buildGui();
        addHierarchyListener( this );
        showIdleMessage();
    }

    @Override
    public void pageSelected() {
        requestAutomaticLatestReplayLoad( "Larva page navigation" );
    }

    @Override
    public void hierarchyChanged( final HierarchyEvent event ) {
        if ( ( event.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED ) == 0L || !isShowing() )
            return;

        requestAutomaticLatestReplayLoad( "Larva page shown" );
    }

    /**
     * Attempts to load the latest replay automatically.
     *
     * @param sourceDescription source label describing the automatic trigger
     */
    private void requestAutomaticLatestReplayLoad( final String sourceDescription ) {
        final Path replayFile = module.resolveLatestReplayPath();
        if ( replayFile == null ) {
            if ( module.getLatestReplaySummary() == null )
                showIdleMessage();
            return;
        }

        final LarvaReplayPageSummary latestSummary = module.getLatestReplaySummary();
        final Path latestLoadedReplay = latestSummary == null || latestSummary.getReplaySummary() == null ? null : latestSummary.getReplaySummary().getReplayFile();
        if ( replayFile.equals( latestLoadedReplay ) )
            return;

        module.requestReplayAnalysis( replayFile, sourceDescription );
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
     * Creates the button used to pick a replay file manually.
     *
     * @return the configured button
     */
    private JButton createOpenReplayButton() {
        final JButton button = new JButton( "Open Replay..." );
        button.addActionListener( new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed( final java.awt.event.ActionEvent event ) {
                openReplayChooser();
            }
        } );

        return button;
    }

    /**
     * Creates the button used to analyze the latest replay that Scelight would consider current.
     *
     * @return the configured button
     */
    private JButton createAnalyzeLatestReplayButton() {
        final JButton button = new JButton( "Analyze Latest Replay" );
        button.addActionListener( new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed( final java.awt.event.ActionEvent event ) {
                final Path replayFile = module.resolveLatestReplayPath();
                if ( replayFile == null ) {
                    module.guiUtils.showInfoMsg( "No replay could be resolved from the replay monitor or monitored replay folders yet." );
                    return;
                }

                module.requestReplayAnalysis( replayFile, "Latest replay resolver" );
            }
        } );

        return button;
    }

    /**
     * Creates the button used to refresh the currently displayed replay.
     *
     * @return the configured button
     */
    private JButton createRefreshButton() {
        final JButton button = new JButton( "Refresh Current Replay" );
        button.addActionListener( new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed( final java.awt.event.ActionEvent event ) {
                if ( currentReplayFile == null ) {
                    final LarvaReplayPageSummary latestSummary = module.getLatestReplaySummary();
                    if ( latestSummary != null )
                        currentReplayFile = latestSummary.getReplaySummary().getReplayFile();
                }

                if ( currentReplayFile == null ) {
                    module.guiUtils.showInfoMsg( "No replay is currently loaded on the Larva page." );
                    return;
                }

                module.requestReplayAnalysis( currentReplayFile, "Larva replay page refresh" );
            }
        } );

        return button;
    }

    /**
     * Opens a replay chooser and starts analysis for the selected replay.
     */
    private void openReplayChooser() {
        final IFileChooser fileChooser = currentReplayFile == null ? module.guiFactory.newFileChooser() : module.guiFactory.newFileChooser( currentReplayFile.getParent() );
        fileChooser.asFileChooser().setFileSelectionMode( JFileChooser.FILES_ONLY );
        fileChooser.asFileChooser().setFileFilter( new FileNameExtensionFilter( "StarCraft II Replay (*.SC2Replay)", "SC2Replay", "sc2replay" ) );

        final Path lastDetectedReplayPath = module.getLastDetectedReplayPath();
        if ( currentReplayFile == null && lastDetectedReplayPath != null && lastDetectedReplayPath.getParent() != null )
            fileChooser.setCurrentFolder( lastDetectedReplayPath.getParent() );

        if ( JFileChooser.APPROVE_OPTION != fileChooser.asFileChooser().showOpenDialog( this ) )
            return;

        module.requestReplayAnalysis( fileChooser.getSelectedPath(), "Manual replay selection" );
    }

    /**
     * Shows the idle message before any replay has been loaded.
     */
    void showIdleMessage() {
        timelinePreviewComp.setTimelineModel( null );
        detailsArea.setText( "Choose a replay to see where Zerg hatcheries floated at 3+ larva.\n\n"
            + "What you are looking at:\n"
            + "- Under each player name, a phase table breaks the game into Early, Mid, Late, and End based on sustained worker-count promotions.\n"
            + "- The first two phase-table rows benchmark larva generation and larva missed against compact gold / plat / dia / masters reference bands.\n"
            + "- Red bars show how long a hatchery stayed at 3 or more larva.\n"
            + "- A green lane shows inject-active windows inferred from replay-derived triple-larva bursts.\n"
            + "- A dark red lane shows conservative missed inject potential windows backed by trustworthy queen evidence.\n"
            + "- Black ticks on the main rail mark every 11 seconds of missed potential larva while a hatchery stayed at 3 or more larva.\n"
            + "- Black ticks on the dark red lane mark every 29 seconds of accumulated missed inject potential, worth 3 potential larva per hatchery.\n"
            + "- Player totals add up both missed larva and potential injected larva missed across visible hatcheries, while the phase table normalizes those values per hatch per minute.\n\n"
            + "Use the buttons above to open a replay, analyze the latest replay, or refresh the current one." );
        detailsArea.setCaretPosition( 0 );
    }

    /**
     * Shows a busy state while replay analysis is running.
     *
     * @param replayFile replay being analyzed
     * @param sourceDescription source of the request
     */
    void showBusy( final Path replayFile, final String sourceDescription ) {
        currentReplayFile = replayFile;
        statusLabel.setText( "Analyzing replay..." );
        timelinePreviewComp.setTimelineModel( null );
        detailsArea.setText( "Analyzing " + ( replayFile == null ? "replay" : replayFile.getFileName() ) + "..." );
    }

    /**
     * Displays a replay summary on the page.
     *
     * @param summary replay summary to display
     */
    void showSummary( final LarvaReplayPageSummary summary ) {
        currentReplayFile = summary.getReplaySummary().getReplayFile();
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
    void showError( final Path replayFile, final String sourceDescription, final String errorMessage ) {
        currentReplayFile = replayFile;
        statusLabel.setText( "Replay could not be analyzed." );
        timelinePreviewComp.setTimelineModel( null );
        detailsArea.setText( "Could not analyze " + ( replayFile == null ? "the selected replay" : replayFile.getFileName() ) + ".\n\n"
            + "Reason:\n"
                + ( errorMessage == null ? "Unknown error" : errorMessage ) );
        detailsArea.setCaretPosition( 0 );
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
        builder.append( "- Ranking rows: compact percentile-inside-tier estimates using gold / plat / dia / masters benchmark anchors.\n" );
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
                        .append( formatPhaseValue( stats == null ? null : stats.getMissedLarvaPerHatchPerMinute() ) )
                        .append( '/' )
                        .append( formatPhaseValue( stats == null ? null : stats.getMissedInjectLarvaPerHatchPerMinute() ) )
                        .append( '/' )
                        .append( formatPhaseValue( stats == null ? null : stats.getSpawnedLarvaPerHatchPerMinute() ) )
                        .append( '/' )
                        .append( formatPhasePercent( stats == null ? null : stats.getInjectUptimePercentage() ) );
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
     * Formats a phase-table rate for the text summary.
     */
    private String formatPhaseValue( final Double value ) {
        if ( value == null )
            return "n/a";

        final long scaled = Math.round( value.doubleValue() * 10.0d );
        return ( scaled / 10L ) + "." + Math.abs( scaled % 10L );
    }

    /**
     * Formats a phase-table percentage for the text summary.
     */
    private String formatPhasePercent( final Double value ) {
        return value == null ? "n/a" : formatPhaseValue( value ) + "%";
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