package hu.aleks.larvasecondslostextmod;

import hu.scelightapibase.gui.comp.multipage.IPageDisposedListener;

import java.awt.BorderLayout;
import java.awt.CardLayout;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

/**
 * Content of one multi-analysis sub-page (E17-S14). Running view: {@code Analyzing k / N: <file>}, a progress bar and
 * {@code Failed: f}. When done it shows the {@link LarvaMultiAnalysisResultsPanel} (E17-S15) ; Scelight's page scroll pane does the scrolling.
 *
 * <p>It only renders what its controller tells it. It does not react to {@code pageSelected}, so the repeated
 * selection events fired by a label change are harmless. Closing the page disposes the controller (cancels the run).</p>
 */
@SuppressWarnings("serial")
final class LarvaMultiAnalysisPageComp extends JPanel implements LarvaMultiAnalysisView, IPageDisposedListener {

    private static final String CARD_RUNNING = "running";
    private static final String CARD_RESULT = "result";
    private static final int PAGE_PADDING = 12;
    private static final int GAP = 8;

    private final LarvaMultiAnalysisPageController controller;
    private final JProgressBar progressBar;
    private final JLabel statusLabel = new JLabel();
    private final JLabel failedLabel = new JLabel();
    private final JLabel resultHeaderLabel = new JLabel();
    private final JPanel resultHolder = new JPanel( new BorderLayout() );
    private LarvaMultiAnalysisResultsPanel resultsPanel;
    private final CardLayout cards = new CardLayout();
    private final JPanel cardPanel = new JPanel( cards );

    /**
     * @param controller state holder of this page; the current state is rendered at once
     * @param progressBar the progress bar (from {@code guiFactory.newProgressBar().asProgressBar()})
     */
    LarvaMultiAnalysisPageComp( final LarvaMultiAnalysisPageController controller, final JProgressBar progressBar ) {
        super( new BorderLayout() );
        this.controller = controller;
        this.progressBar = progressBar;
        buildGui();
        controller.attach( this );
    }

    @Override
    public void pageDisposed() {
        controller.dispose();
    }

    private void buildGui() {
        setBorder( BorderFactory.createEmptyBorder( PAGE_PADDING, PAGE_PADDING, PAGE_PADDING, PAGE_PADDING ) );
        progressBar.setStringPainted( true );

        final JPanel runningPanel = new JPanel( new BorderLayout( 0, GAP ) );
        runningPanel.add( statusLabel, BorderLayout.NORTH );
        runningPanel.add( progressBar, BorderLayout.CENTER );
        runningPanel.add( failedLabel, BorderLayout.SOUTH );
        final JPanel runningCard = new JPanel( new BorderLayout() );
        runningCard.add( runningPanel, BorderLayout.NORTH );

        final JPanel resultCard = new JPanel( new BorderLayout( 0, GAP ) );
        resultCard.add( resultHeaderLabel, BorderLayout.NORTH );
        resultCard.add( resultHolder, BorderLayout.CENTER );

        cardPanel.add( runningCard, CARD_RUNNING );
        cardPanel.add( resultCard, CARD_RESULT );
        add( cardPanel, BorderLayout.CENTER );
    }

    @Override
    public void showProgress( final int k, final int n, final String fileName, final int failedCount ) {
        statusLabel.setText( k == 0 ? "Preparing " + n + " replays..." : "Analyzing " + k + " / " + n + ": " + fileName );
        progressBar.setMaximum( n );
        progressBar.setValue( k );
        failedLabel.setText( "Failed: " + failedCount );
        cards.show( cardPanel, CARD_RUNNING );
    }

    @Override
    public void showResult( final LarvaMultiAnalysisViewModel viewModel ) {
        resultHeaderLabel.setText( "" );
        resultsPanel = new LarvaMultiAnalysisResultsPanel( viewModel );
        resultHolder.removeAll();
        resultHolder.add( resultsPanel, BorderLayout.NORTH );
        resultHolder.revalidate();
        resultHolder.repaint();
        cards.show( cardPanel, CARD_RESULT );
    }

    @Override
    public void showStopped( final String message ) {
        resultHeaderLabel.setText( message );
        resultsPanel = null;
        resultHolder.removeAll();
        resultHolder.revalidate();
        resultHolder.repaint();
        cards.show( cardPanel, CARD_RESULT );
    }

    /** @return the "Analyzing k / N: file" line, for the dev command {@code multi-page-state} */
    String getStatusText() {
        return statusLabel.getText();
    }

    String getFailedText() {
        return failedLabel.getText();
    }

    String getResultHeaderText() {
        return resultHeaderLabel.getText();
    }

    /** @return the results panel, or null while running or after a stop */
    LarvaMultiAnalysisResultsPanel getResultsPanel() {
        return resultsPanel;
    }

    int getProgressValue() {
        return progressBar.getValue();
    }

    int getProgressMaximum() {
        return progressBar.getMaximum();
    }
}
