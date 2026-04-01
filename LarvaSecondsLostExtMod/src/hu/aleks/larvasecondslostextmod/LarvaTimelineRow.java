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

    /** Visible row start in milliseconds. */
    private final long startMs;

    /** Visible row end in milliseconds. */
    private final long endMs;

    /** Number of missed-larva thresholds reached on the row. */
    private final int missedLarvaCount;

    /** Row segments. */
    private final List< LarvaTimelineSegment > segmentList;

    /** Row markers. */
    private final List< LarvaTimelineMarker > markerList;

    /**
     * Creates a new timeline row.
     *
     * @param groupLabel player grouping label
     * @param rowLabel row label
     * @param detailLabel row detail label
     * @param startMs visible row start in milliseconds
     * @param endMs visible row end in milliseconds
     * @param missedLarvaCount number of missed-larva thresholds reached on the row
     * @param segmentList row segments
     * @param markerList row markers
     */
    public LarvaTimelineRow( final String groupLabel, final String rowLabel, final String detailLabel, final long startMs, final long endMs,
            final int missedLarvaCount, final List< LarvaTimelineSegment > segmentList, final List< LarvaTimelineMarker > markerList ) {
        this.groupLabel = groupLabel;
        this.rowLabel = rowLabel;
        this.detailLabel = detailLabel;
        this.startMs = startMs;
        this.endMs = endMs;
        this.missedLarvaCount = missedLarvaCount;
        this.segmentList = Collections.unmodifiableList( new ArrayList<>( segmentList ) );
        this.markerList = Collections.unmodifiableList( new ArrayList<>( markerList ) );
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

    public long getStartMs() {
        return startMs;
    }

    public long getEndMs() {
        return endMs;
    }

    public int getMissedLarvaCount() {
        return missedLarvaCount;
    }

    public List< LarvaTimelineSegment > getSegmentList() {
        return segmentList;
    }

    public List< LarvaTimelineMarker > getMarkerList() {
        return markerList;
    }

}