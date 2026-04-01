package hu.aleks.larvasecondslostextmod;

/**
 * Determines whether the public Scelight external module API exposes a supported way to augment the native Base Control chart.
 */
public class BaseControlAugmentationCapabilityDetector {

    /** Title used when Base Control augmentation is not available. */
    private static final String FALLBACK_TITLE = "Separate larva timeline remains the supported path";

    /**
     * Evaluates Base Control augmentation capability for the current runtime.
     *
     * @return capability report
     */
    public BaseControlAugmentationCapability detect() {
        return new BaseControlAugmentationCapability( false, FALLBACK_TITLE,
                "The public external module API does not expose the replay analyzer's Base Control chart, its data model, or any callback that would let a module add extra rectangles to that chart in a supported way.",
                "Scelight builds Base Control through internal classes only: ChartType.BASE_CONTROL selects BaseControlChartFactory inside ChartsComp, and BaseControlChartFactory creates BaseControlChart instances backed by internal BaseControlChartDataSet objects. No external registry or augmentation hook is exposed in the external module API.",
                "Keep rendering larva windows on the module-owned Larva timeline instead of trying to patch Scelight's internal Base Control chart." );
    }

}