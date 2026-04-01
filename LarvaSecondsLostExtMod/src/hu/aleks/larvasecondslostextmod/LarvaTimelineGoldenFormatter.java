package hu.aleks.larvasecondslostextmod;

import java.util.List;
import java.util.Map;

/**
 * Renders a deterministic, diff-friendly timeline snapshot for fixture and golden-output
 * validation.
 */
public class LarvaTimelineGoldenFormatter {

    /** Validation snapshot format version. */
    private static final String FORMAT_VERSION = "1";

    /**
     * Formats a replay page summary into a stable text representation.
     *
     * @param summary replay page summary
     * @return deterministic validation text
     */
    public String format( final LarvaReplayPageSummary summary ) {
        final StringBuilder builder = new StringBuilder();
        builder.append( "formatVersion=" ).append( FORMAT_VERSION ).append( '\n' );
        if ( summary == null ) {
            builder.append( "summary=missing" );
            return builder.toString();
        }

        appendReplaySummary( builder, summary.getReplaySummary() );
        appendTimelineModel( builder, summary.getTimelineModel() );
        appendAnalysisSummary( builder, summary.getLarvaAnalysisReport() );
        return builder.toString();
    }

    /**
     * Appends replay-level metadata.
     *
     * @param builder target builder
     * @param replaySummary replay summary
     */
    private void appendReplaySummary( final StringBuilder builder, final ReplaySummary replaySummary ) {
        if ( replaySummary == null ) {
            builder.append( "replay=missing" ).append( '\n' );
            return;
        }

        builder.append( "replay.map=" ).append( safe( replaySummary.getMapTitle() ) ).append( '\n' );
        builder.append( "replay.players=" ).append( safe( replaySummary.getPlayers() ) ).append( '\n' );
        builder.append( "replay.winners=" ).append( safe( replaySummary.getWinners() ) ).append( '\n' );
        builder.append( "replay.length=" ).append( safe( replaySummary.getLength() ) ).append( '\n' );
        builder.append( "replay.baseBuild=" ).append( safe( replaySummary.getBaseBuild() ) ).append( '\n' );
    }

    /**
     * Appends normalized timeline-model output.
     *
     * @param builder target builder
     * @param timelineModel timeline model
     */
    private void appendTimelineModel( final StringBuilder builder, final LarvaTimelineModel timelineModel ) {
        if ( timelineModel == null ) {
            builder.append( "timeline=missing" ).append( '\n' );
            return;
        }

        builder.append( "timeline.mode=" ).append( safe( timelineModel.getModeLabel() ) ).append( '\n' );
        builder.append( "timeline.rowCount=" ).append( timelineModel.getRowList().size() ).append( '\n' );
        appendGroupTotals( builder, timelineModel.getGroupOverviewLabelMap() );
        appendRows( builder, timelineModel.getRowList() );
    }

    /**
     * Appends per-player totals.
     *
     * @param builder target builder
     * @param groupOverviewLabelMap totals by player
     */
    private void appendGroupTotals( final StringBuilder builder, final Map< String, String > groupOverviewLabelMap ) {
        builder.append( "timeline.groupCount=" ).append( groupOverviewLabelMap.size() ).append( '\n' );
        int index = 0;
        for ( final Map.Entry< String, String > entry : groupOverviewLabelMap.entrySet() ) {
            builder.append( "timeline.group[" ).append( index ).append( "].name=" ).append( safe( entry.getKey() ) ).append( '\n' );
            builder.append( "timeline.group[" ).append( index ).append( "].total=" ).append( safe( entry.getValue() ) ).append( '\n' );
            index++;
        }
    }

    /**
     * Appends rows, windows, and missed-larva markers.
     *
     * @param builder target builder
     * @param rowList timeline rows
     */
    private void appendRows( final StringBuilder builder, final List< LarvaTimelineRow > rowList ) {
        for ( int rowIndex = 0; rowIndex < rowList.size(); rowIndex++ ) {
            final LarvaTimelineRow row = rowList.get( rowIndex );
            builder.append( "row[" ).append( rowIndex ).append( "].group=" ).append( safe( row.getGroupLabel() ) ).append( '\n' );
            builder.append( "row[" ).append( rowIndex ).append( "].label=" ).append( safe( row.getRowLabel() ) ).append( '\n' );
            builder.append( "row[" ).append( rowIndex ).append( "].detail=" ).append( safe( row.getDetailLabel() ) ).append( '\n' );
            builder.append( "row[" ).append( rowIndex ).append( "].rangeMs=" ).append( row.getStartMs() ).append( '-' ).append( row.getEndMs() ).append( '\n' );
            builder.append( "row[" ).append( rowIndex ).append( "].missedLarvaCount=" ).append( row.getMissedLarvaCount() ).append( '\n' );
            appendSegments( builder, rowIndex, row.getSegmentList() );
            appendMarkers( builder, rowIndex, row.getMarkerList() );
        }
    }

    /**
     * Appends row segments.
     *
     * @param builder target builder
     * @param rowIndex row index
     * @param segmentList row segments
     */
    private void appendSegments( final StringBuilder builder, final int rowIndex, final List< LarvaTimelineSegment > segmentList ) {
        builder.append( "row[" ).append( rowIndex ).append( "].segmentCount=" ).append( segmentList.size() ).append( '\n' );
        for ( int segmentIndex = 0; segmentIndex < segmentList.size(); segmentIndex++ ) {
            final LarvaTimelineSegment segment = segmentList.get( segmentIndex );
            builder.append( "row[" ).append( rowIndex ).append( "].segment[" ).append( segmentIndex ).append( "].kind=" )
                    .append( segment.getKind() ).append( '\n' );
            builder.append( "row[" ).append( rowIndex ).append( "].segment[" ).append( segmentIndex ).append( "].label=" )
                    .append( safe( segment.getLabel() ) ).append( '\n' );
            builder.append( "row[" ).append( rowIndex ).append( "].segment[" ).append( segmentIndex ).append( "].rangeMs=" )
                    .append( segment.getStartMs() ).append( '-' ).append( segment.getEndMs() ).append( '\n' );
        }
    }

    /**
     * Appends row markers.
     *
     * @param builder target builder
     * @param rowIndex row index
     * @param markerList row markers
     */
    private void appendMarkers( final StringBuilder builder, final int rowIndex, final List< LarvaTimelineMarker > markerList ) {
        builder.append( "row[" ).append( rowIndex ).append( "].markerCount=" ).append( markerList.size() ).append( '\n' );
        for ( int markerIndex = 0; markerIndex < markerList.size(); markerIndex++ ) {
            final LarvaTimelineMarker marker = markerList.get( markerIndex );
            builder.append( "row[" ).append( rowIndex ).append( "].marker[" ).append( markerIndex ).append( "].kind=" )
                    .append( marker.getKind() ).append( '\n' );
            builder.append( "row[" ).append( rowIndex ).append( "].marker[" ).append( markerIndex ).append( "].loop=" )
                    .append( marker.getLoop() ).append( '\n' );
            builder.append( "row[" ).append( rowIndex ).append( "].marker[" ).append( markerIndex ).append( "].label=" )
                    .append( safe( marker.getLabel() ) ).append( '\n' );
        }
    }

    /**
     * Appends high-level analysis counters.
     *
     * @param builder target builder
     * @param larvaAnalysisReport larva analysis report
     */
    private void appendAnalysisSummary( final StringBuilder builder, final LarvaAnalysisReport larvaAnalysisReport ) {
        if ( larvaAnalysisReport == null ) {
            builder.append( "analysis=missing" ).append( '\n' );
            return;
        }

        builder.append( "analysis.trackedHatcheries=" ).append( larvaAnalysisReport.getTrackedHatcheryCount() ).append( '\n' );
        builder.append( "analysis.assignedLarva=" ).append( larvaAnalysisReport.getAssignedLarvaCount() ).append( '\n' );
        builder.append( "analysis.unassignedLarva=" ).append( larvaAnalysisReport.getUnassignedLarvaCount() ).append( '\n' );
        builder.append( "analysis.ambiguousLarva=" ).append( larvaAnalysisReport.getAmbiguousLarvaCount() ).append( '\n' );
        builder.append( "analysis.noEligibleLarva=" ).append( larvaAnalysisReport.getNoEligibleHatcheryLarvaCount() );
    }

    /**
     * Sanitizes optional text for deterministic output.
     *
     * @param value value to sanitize
     * @return deterministic text value
     */
    private String safe( final String value ) {
        return value == null || value.length() == 0 ? "n/a" : value.replace( '\n', ' ' ).replace( '\r', ' ' );
    }

}