package hu.aleks.larvasecondslostextmod;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * One normalized row in the module-owned Larva preview timeline.
 */
public class LarvaTimelineRow {

    /** Player grouping label. */
    private final String groupLabel;

    /** Row label. */
    private final String rowLabel;

    /** Row detail text. */
    private final String detailLabel;

    /** Row segments. */
    private final List< LarvaTimelineSegment > segmentList;

    /**
     * Creates a new timeline row.
     *
     * @param groupLabel player grouping label
     * @param rowLabel row label
     * @param detailLabel row detail label
     * @param segmentList row segments
     */
    public LarvaTimelineRow( final String groupLabel, final String rowLabel, final String detailLabel, final List< LarvaTimelineSegment > segmentList ) {
        this.groupLabel = groupLabel;
        this.rowLabel = rowLabel;
        this.detailLabel = detailLabel;
        this.segmentList = Collections.unmodifiableList( new ArrayList<>( segmentList ) );
    }

    public String getGroupLabel() {
        return groupLabel;
    }

    public String getRowLabel() {
        return rowLabel;
    }

    public String getDetailLabel() {
        return detailLabel;
    }

    public List< LarvaTimelineSegment > getSegmentList() {
        return segmentList;
    }

}