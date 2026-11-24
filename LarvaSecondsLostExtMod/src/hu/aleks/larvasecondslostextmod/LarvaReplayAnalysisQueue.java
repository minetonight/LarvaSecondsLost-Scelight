package hu.aleks.larvasecondslostextmod;

/**
 * Accepts analysis requests of single-replay pages. The production implementation is
 * {@link LarvaSingleReplayAnalysisQueue}; tests use a synchronous fake.
 */
interface LarvaReplayAnalysisQueue {

    /**
     * Queues the analysis of the controller's replay. The result is delivered on the EDT through
     * {@link LarvaReplayPageController#onAnalysisSucceeded} or {@link LarvaReplayPageController#onAnalysisFailed}.
     *
     * @param controller the page controller asking for the analysis
     * @param sourceDescription short label of where the request came from
     * @param requestId id given by the controller; it is handed back with the result
     */
    void submit( LarvaReplayPageController controller, String sourceDescription, long requestId );

}
