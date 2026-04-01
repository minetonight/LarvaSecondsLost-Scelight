package hu.aleks.larvasecondslostextmod;

import hu.scelightapibase.gui.comp.IFileChooser;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.nio.file.Path;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * Module-owned replay diagnostics page used as the Epic 2 fallback replay-view surface.
 */
@SuppressWarnings("serial")
public class LarvaReplayPageComp extends JPanel {

    /** Owning external module. */
    private final LarvaSecondsLostModule module;

    /** Status label displayed above the replay summary. */
    private final JLabel statusLabel;

    /** Text area that shows replay diagnostics. */
    private final JTextArea detailsArea;

    /** Timeline preview component. */
    private final LarvaTimelinePreviewComp timelinePreviewComp;

    /** Capability label shown above the replay tools. */
    private final JLabel capabilityLabel;

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

        statusLabel = new JLabel( "Replay diagnostics page ready.", module.getLarvaIcon().get(), SwingConstants.LEADING );
        detailsArea = new JTextArea();
        timelinePreviewComp = new LarvaTimelinePreviewComp();
        capabilityLabel = new JLabel();

        buildGui();
        showIdleMessage();
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

        final JPanel capabilityPanel = new JPanel( new BorderLayout() );
        capabilityPanel.add( capabilityLabel, BorderLayout.CENTER );
        capabilityPanel.setBorder( BorderFactory.createEmptyBorder( 0, 0, 4, 0 ) );

        final JPanel northPanel = new JPanel( new BorderLayout( 0, 8 ) );
        northPanel.add( headerPanel, BorderLayout.NORTH );
        northPanel.add( capabilityPanel, BorderLayout.CENTER );
        northPanel.add( buttonPanel, BorderLayout.SOUTH );

        detailsArea.setEditable( false );
        detailsArea.setLineWrap( true );
        detailsArea.setWrapStyleWord( true );
        detailsArea.setRows( 18 );
        detailsArea.setBorder( BorderFactory.createEmptyBorder( 8, 8, 8, 8 ) );

        final JSplitPane splitPane = new JSplitPane( JSplitPane.VERTICAL_SPLIT, timelinePreviewComp, new JScrollPane( detailsArea ) );
        splitPane.setBorder( BorderFactory.createEmptyBorder() );
        splitPane.setResizeWeight( 0.35 );

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
        updateCapabilityLabel();
        timelinePreviewComp.setSummary( null );
        detailsArea.setText( "Epic 2 fallback replay-view surface\n"
            + "Epic 3 now adds a first chart-like preview above the diagnostics text.\n"
            + "Epic 4 now confirms native chart dropdown integration is unsupported for pure external modules.\n"
            + "Epic 5 now confirms native Base Control chart augmentation is unsupported for pure external modules.\n"
            + "Epic 6 now derives per-hatchery larva counts with a calibrated hatchery-to-larva assignment heuristic.\n"
            + "Story 01.07 can also write a dev diagnostic dump file for zero-click verification when enabled by JVM property.\n"
                + "\n"
                + "This page is the replay-scoped entry point currently available to the external module.\n"
                + "\n"
                + "Use one of the actions above to load replay diagnostics:\n"
                + "- Open Replay... loads any replay file manually.\n"
                + "- Analyze Latest Replay first reuses the Replay Folder Monitor event stream, then falls back to scanning Scelight's monitored replay folders.\n"
                + "- Refresh Current Replay reruns the latest successful analysis.\n"
                + "- Dev dump file can be enabled with -D"
                + DevDiagnosticDumpWriter.PROP_ENABLED
                + "=true and optionally -D"
                + DevDiagnosticDumpWriter.PROP_FILE
                + "=/path/to/file.txt.\n"
                + "\n"
            + "The preview timeline intentionally shows a replay-derived placeholder interval so real larva windows can replace it in later epics.\n"
            + "\n"
            + "Native injection into Scelight's internal replay analyzer page is not exposed by the public external module API, so this module uses a dedicated fallback page next to the replay workflow." );
    }

    /**
     * Shows a busy state while replay analysis is running.
     *
     * @param replayFile replay being analyzed
     * @param sourceDescription source of the request
     */
    void showBusy( final Path replayFile, final String sourceDescription ) {
        currentReplayFile = replayFile;
        statusLabel.setText( "Loading replay diagnostics..." );
        updateCapabilityLabel();
        timelinePreviewComp.setSummary( null );
        detailsArea.setText( "Analyzing replay from " + sourceDescription + ":\n" + replayFile );
    }

    /**
     * Displays a replay summary on the page.
     *
     * @param summary replay summary to display
     */
    void showSummary( final LarvaReplayPageSummary summary ) {
        currentReplayFile = summary.getReplaySummary().getReplayFile();
        statusLabel.setText( "Replay diagnostics ready: " + summary.getReplaySummary().getReplayFile().getFileName() );
        updateCapabilityLabel();
        timelinePreviewComp.setSummary( summary );
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
        statusLabel.setText( "Replay diagnostics failed." );
        updateCapabilityLabel();
        timelinePreviewComp.setSummary( null );
        detailsArea.setText( "Failed to analyze replay from " + sourceDescription + ":\n"
                + replayFile
                + "\n\nReason:\n"
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

        builder.append( "Epic 2 replay-view presence confirmed" ).append( '\n' );
        builder.append( "Epic 3 placeholder chart confirmed" ).append( '\n' );
        builder.append( "Epic 5 Base Control augmentation feasibility resolved" ).append( '\n' );
        builder.append( "Epic 6 larva assignment foundation resolved" ).append( '\n' ).append( '\n' );
        builder.append( buildReplayMetadataSection( replaySummary ) ).append( '\n' );
        builder.append( buildPageDiagnosticsSection( summary ) ).append( '\n' );
        builder.append( buildCapabilitySection() ).append( '\n' );
        builder.append( "Preview chart window: " )
                .append( formatMs( summary.getPreviewWindowStartMs() ) )
                .append( " - " )
                .append( formatMs( summary.getPreviewWindowEndMs() ) )
                .append( " (replay-derived placeholder interval)" )
                .append( '\n' )
                .append( '\n' );
        if ( summary.getLarvaAnalysisReport() != null )
            builder.append( summary.getLarvaAnalysisReport().toDisplayText() ).append( '\n' ).append( '\n' );
        builder.append( "Next goal: convert these per-hatchery larva counts into real 3+ larva windows on the supported module-owned Larva timeline." );

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
        builder.append( "Replay metadata:" ).append( '\n' );
        builder.append( "Replay source: " ).append( replaySummary.getSourceDescription() ).append( '\n' );
        builder.append( "Replay file: " ).append( replaySummary.getReplayFile() ).append( '\n' );
        builder.append( "Map: " ).append( replaySummary.getMapTitle() ).append( '\n' );
        builder.append( "Players: " ).append( replaySummary.getPlayers() ).append( '\n' );
        builder.append( "Winners: " ).append( replaySummary.getWinners() ).append( '\n' );
        builder.append( "Length: " ).append( replaySummary.getLength() ).append( '\n' );
        builder.append( "Replay end time: " ).append( replaySummary.getReplayEndTime() ).append( '\n' );
        builder.append( "Replay version: " ).append( replaySummary.getReplayVersion() ).append( '\n' );
        builder.append( "Base build: " ).append( replaySummary.getBaseBuild() ).append( '\n' );
        return builder.toString();
    }

    /**
     * Builds the page-specific diagnostics section.
     *
     * @param summary page-level replay diagnostics summary
     * @return rendered page diagnostics section
     */
    private String buildPageDiagnosticsSection( final LarvaReplayPageSummary summary ) {
        final StringBuilder builder = new StringBuilder();
        builder.append( "Larva page diagnostics:" ).append( '\n' );
        builder.append( "Integration mode: " ).append( summary.getIntegrationMode() ).append( '\n' );
        return builder.toString();
    }

    /**
     * Updates the short capability banner shown above the action buttons.
     */
    private void updateCapabilityLabel() {
        final ChartIntegrationCapability capability = module.getChartIntegrationCapability();
        final BaseControlAugmentationCapability baseControlCapability = module.getBaseControlAugmentationCapability();
        capabilityLabel.setText( "<html><b>Epic 4:</b> " + capability.getIntegrationModeTitle() + "<br><b>Epic 5:</b> "
                + baseControlCapability.getIntegrationModeTitle() + "</html>" );
    }

    /**
     * Builds the capability report section for the details area.
     *
     * @return rendered capability section
     */
    private String buildCapabilitySection() {
        final ChartIntegrationCapability capability = module.getChartIntegrationCapability();
        final BaseControlAugmentationCapability baseControlCapability = module.getBaseControlAugmentationCapability();
        final StringBuilder builder = new StringBuilder();
        builder.append( "Epic 4 native chart dropdown integration: " )
                .append( capability.isNativeDropdownSupported() ? "supported" : "unsupported" )
                .append( '\n' );
        builder.append( capability.getExplanation() ).append( '\n' );
        builder.append( "Technical evidence: " ).append( capability.getTechnicalEvidence() ).append( '\n' );
        builder.append( "Recommended path: " ).append( capability.getRecommendedPath() ).append( '\n' );
        builder.append( '\n' );
        builder.append( "Epic 5 native Base Control augmentation: " )
            .append( baseControlCapability.isAugmentationSupported() ? "supported" : "unsupported" )
            .append( '\n' );
        builder.append( baseControlCapability.getExplanation() ).append( '\n' );
        builder.append( "Technical evidence: " ).append( baseControlCapability.getTechnicalEvidence() ).append( '\n' );
        builder.append( "Recommended path: " ).append( baseControlCapability.getRecommendedPath() ).append( '\n' );
        return builder.toString();
    }

    /**
     * Formats milliseconds as a short $m:ss$ string.
     *
     * @param ms time in milliseconds
     * @return formatted time string
     */
    private String formatMs( final long ms ) {
        final long totalSeconds = ms / 1000L;
        final long minutes = totalSeconds / 60L;
        final long seconds = totalSeconds % 60L;
        return minutes + ":" + ( seconds < 10L ? "0" : "" ) + seconds;
    }

}