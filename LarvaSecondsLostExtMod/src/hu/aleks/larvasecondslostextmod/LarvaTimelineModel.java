package hu.aleks.larvasecondslostextmod;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Normalized presentation model for the module-owned Larva timeline.
 */
public class LarvaTimelineModel {

    /** Chart title. */
    private final String title;

    /** Short chart subtitle. */
    private final String subtitle;

    /** Fallback-mode note shown above the preview. */
    private final String modeLabel;

    /** Per-player overview messages shown with player group headers. */
    private final Map< String, String > groupOverviewLabelMap;

    /** Replay length in milliseconds. */
    private final long replayLengthMs;

    /** Replay length formatted for the axis. */
    private final String replayLengthLabel;

    /** Empty-state message when the model cannot render rows yet. */
    private final String emptyMessage;

    /** Timeline rows. */
    private final List< LarvaTimelineRow > rowList;

    /**
     * Creates a new timeline model.
     *
     * @param title chart title
     * @param subtitle short chart subtitle
     * @param modeLabel fallback mode note
     * @param groupOverviewLabelMap overview messages shown with player group headers
     * @param replayLengthMs replay length in milliseconds
     * @param replayLengthLabel formatted replay length
     * @param emptyMessage empty-state message
     * @param rowList timeline rows
     */
    public LarvaTimelineModel( final String title, final String subtitle, final String modeLabel, final Map< String, String > groupOverviewLabelMap,
            final long replayLengthMs, final String replayLengthLabel, final String emptyMessage, final List< LarvaTimelineRow > rowList ) {
        this.title = title;
        this.subtitle = subtitle;
        this.modeLabel = modeLabel;
        this.groupOverviewLabelMap = Collections.unmodifiableMap( new LinkedHashMap<>( groupOverviewLabelMap ) );
        this.replayLengthMs = replayLengthMs;
        this.replayLengthLabel = replayLengthLabel;
        this.emptyMessage = emptyMessage;
        this.rowList = Collections.unmodifiableList( new ArrayList<>( rowList ) );
    }

    public String getTitle() {
        return title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public String getModeLabel() {
        return modeLabel;
    }

    public Map< String, String > getGroupOverviewLabelMap() {
        return groupOverviewLabelMap;
    }

    public long getReplayLengthMs() {
        return replayLengthMs;
    }

    public String getReplayLengthLabel() {
        return replayLengthLabel;
    }

    public String getEmptyMessage() {
        return emptyMessage;
    }

    public List< LarvaTimelineRow > getRowList() {
        return rowList;
    }

}