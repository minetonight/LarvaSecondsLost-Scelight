package hu.aleks.larvasecondslostextmod;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Builds a structured verification report that separates replay-analysis, timeline-model,
 * integration, and runtime packaging concerns.
 */
public class LarvaVerificationReportFormatter {

    /**
     * Formats a verification report for an in-progress replay analysis.
     *
     * @param replayFile replay file being analyzed
     * @param sourceDescription replay source description
     * @param lifecycleState current lifecycle state
     * @param dumpFile resolved dump file path
     * @param moduleName module name
     * @param moduleVersion module version
     * @param enabled tells if diagnostics are enabled
     * @param chartCapability chart capability summary
     * @param baseControlCapability Base Control capability summary
     * @return structured verification report
     */
    public String formatInProgress( final Path replayFile, final String sourceDescription, final String lifecycleState, final Path dumpFile,
            final String moduleName, final String moduleVersion, final boolean enabled, final ChartIntegrationCapability chartCapability,
            final BaseControlAugmentationCapability baseControlCapability ) {
        final StringBuilder builder = new StringBuilder();
        appendReplayAnalysisStatus( builder, "running", sourceDescription, replayFile, null, null );
        appendTimelineModelStatus( builder, null );
        appendIntegrationStatus( builder, null, chartCapability, baseControlCapability );
        appendRuntimeStatus( builder, lifecycleState, dumpFile, moduleName, moduleVersion, enabled );
        return builder.toString();
    }

    /**
     * Formats a verification report for a successful replay analysis.
     *
     * @param summary replay page summary
     * @param lifecycleState current lifecycle state
     * @param dumpFile resolved dump file path
     * @param moduleName module name
     * @param moduleVersion module version
     * @param enabled tells if diagnostics are enabled
     * @param chartCapability chart capability summary
     * @param baseControlCapability Base Control capability summary
     * @return structured verification report
     */
    public String formatSuccess( final LarvaReplayPageSummary summary, final String lifecycleState, final Path dumpFile, final String moduleName,
            final String moduleVersion, final boolean enabled, final ChartIntegrationCapability chartCapability,
            final BaseControlAugmentationCapability baseControlCapability ) {
        final StringBuilder builder = new StringBuilder();
        appendReplayAnalysisStatus( builder, "succeeded", summary.getReplaySummary().getSourceDescription(), summary.getReplaySummary().getReplayFile(),
                summary.getLarvaAnalysisReport(), null );
        appendTimelineModelStatus( builder, summary.getTimelineModel() );
        appendIntegrationStatus( builder, summary.getIntegrationMode(), chartCapability, baseControlCapability );
        appendRuntimeStatus( builder, lifecycleState, dumpFile, moduleName, moduleVersion, enabled );
        return builder.toString();
    }

    /**
     * Formats a verification report for a failed replay analysis.
     *
     * @param replayFile replay file
     * @param sourceDescription replay source description
     * @param message failure message
     * @param lifecycleState current lifecycle state
     * @param dumpFile resolved dump file path
     * @param moduleName module name
     * @param moduleVersion module version
     * @param enabled tells if diagnostics are enabled
     * @param chartCapability chart capability summary
     * @param baseControlCapability Base Control capability summary
     * @return structured verification report
     */
    public String formatFailure( final Path replayFile, final String sourceDescription, final String message, final String lifecycleState,
            final Path dumpFile, final String moduleName, final String moduleVersion, final boolean enabled,
            final ChartIntegrationCapability chartCapability, final BaseControlAugmentationCapability baseControlCapability ) {
        final StringBuilder builder = new StringBuilder();
        appendReplayAnalysisStatus( builder, "failed", sourceDescription, replayFile, null, message );
        appendTimelineModelStatus( builder, null );
        appendIntegrationStatus( builder, null, chartCapability, baseControlCapability );
        appendRuntimeStatus( builder, lifecycleState, dumpFile, moduleName, moduleVersion, enabled );
        return builder.toString();
    }

    /**
     * Formats a runtime-only verification report.
     *
     * @param lifecycleState current lifecycle state
     * @param dumpFile resolved dump file path
     * @param moduleName module name
     * @param moduleVersion module version
     * @param enabled tells if diagnostics are enabled
     * @param chartCapability chart capability summary
     * @param baseControlCapability Base Control capability summary
     * @return structured verification report
     */
    public String formatRuntimeOnly( final String lifecycleState, final Path dumpFile, final String moduleName, final String moduleVersion,
            final boolean enabled, final ChartIntegrationCapability chartCapability, final BaseControlAugmentationCapability baseControlCapability ) {
        final StringBuilder builder = new StringBuilder();
        appendReplayAnalysisStatus( builder, "idle", null, null, null, null );
        appendTimelineModelStatus( builder, null );
        appendIntegrationStatus( builder, null, chartCapability, baseControlCapability );
        appendRuntimeStatus( builder, lifecycleState, dumpFile, moduleName, moduleVersion, enabled );
        return builder.toString();
    }

    /**
     * Appends replay-analysis verification details.
     */
    private void appendReplayAnalysisStatus( final StringBuilder builder, final String status, final String sourceDescription, final Path replayFile,
            final LarvaAnalysisReport report, final String failureMessage ) {
        builder.append( "Replay-analysis result: " ).append( status ).append( '\n' );
        builder.append( "- source: " ).append( safe( sourceDescription ) ).append( '\n' );
        builder.append( "- replay file: " ).append( replayFile == null ? "n/a" : replayFile.toString() ).append( '\n' );
        if ( report == null ) {
            builder.append( "- assignment stage: not available" ).append( '\n' );
            builder.append( "- unassigned reason categories: not available" ).append( '\n' );
        } else {
            builder.append( "- assignment stage: tracked hatcheries=" ).append( report.getTrackedHatcheryCount() )
                    .append( ", larva births=" ).append( report.getLarvaBirthCount() )
                    .append( ", assigned=" ).append( report.getAssignedLarvaCount() )
                    .append( ", unassigned=" ).append( report.getUnassignedLarvaCount() ).append( '\n' );
            builder.append( "- unassigned reason categories: ambiguous=" ).append( report.getAmbiguousLarvaCount() )
                    .append( ", no eligible hatchery=" ).append( report.getNoEligibleHatcheryLarvaCount() ).append( '\n' );
        }
        if ( failureMessage != null && failureMessage.length() > 0 )
            builder.append( "- failure detail: " ).append( failureMessage ).append( '\n' );
    }

    /**
     * Appends timeline-model verification details.
     */
    private void appendTimelineModelStatus( final StringBuilder builder, final LarvaTimelineModel timelineModel ) {
        builder.append( "Timeline-model result: " );
        if ( timelineModel == null ) {
            builder.append( "not available" ).append( '\n' );
            builder.append( "- model assembly: no timeline rows available" ).append( '\n' );
            return;
        }

        int segmentCount = 0;
        int saturationWindowCount = 0;
        int markerCount = 0;
        int missedLarvaTotal = 0;
        int missedInjectedLarvaTotal = 0;
        for ( final LarvaTimelineRow row : timelineModel.getRowList() ) {
            final List< LarvaTimelineSegment > segmentList = row.getSegmentList();
            final List< LarvaTimelineMarker > markerList = row.getMarkerList();
            segmentCount += segmentList.size();
            markerCount += markerList.size();
            missedLarvaTotal += row.getMissedLarvaCount();
            missedInjectedLarvaTotal += row.getPotentialInjectedLarvaMissedCount();
            for ( final LarvaTimelineSegment segment : segmentList )
                if ( LarvaTimelineSegment.Kind.SATURATION_WINDOW == segment.getKind() )
                    saturationWindowCount++;
        }

        builder.append( "available" ).append( '\n' );
        builder.append( "- row assembly: rows=" ).append( timelineModel.getRowList().size() )
            .append( ", player groups=" ).append( timelineModel.getGroupOverviewLabelMap().size() )
            .append( ", phase tables=" ).append( timelineModel.getPlayerPhaseTableMap().size() ).append( '\n' );
        builder.append( "- saturation-window conversion: windows=" ).append( saturationWindowCount )
                .append( ", total segments=" ).append( segmentCount ).append( '\n' );
        builder.append( "- marker accumulation: markers=" ).append( markerCount )
            .append( ", missed larva total=" ).append( missedLarvaTotal )
            .append( ", potential injected larva missed total=" ).append( missedInjectedLarvaTotal ).append( '\n' );
        builder.append( "- per-player totals: " ).append( formatGroupTotals( timelineModel.getGroupOverviewLabelMap() ) ).append( '\n' );
        builder.append( "- per-player phase tables: " ).append( formatPhaseTableNames( timelineModel.getPlayerPhaseTableMap() ) ).append( '\n' );
    }

    /**
     * Appends integration-mode verification details.
     */
    private void appendIntegrationStatus( final StringBuilder builder, final String integrationMode, final ChartIntegrationCapability chartCapability,
            final BaseControlAugmentationCapability baseControlCapability ) {
        builder.append( "Integration-mode result: " ).append( safe( integrationMode ) ).append( '\n' );
        builder.append( "- native chart dropdown support: " )
                .append( chartCapability == null ? "unknown" : yesNo( chartCapability.isNativeDropdownSupported() ) ).append( '\n' );
        builder.append( "- Base Control augmentation support: " )
                .append( baseControlCapability == null ? "unknown" : yesNo( baseControlCapability.isAugmentationSupported() ) ).append( '\n' );
        builder.append( "- supported fallback path: " )
                .append( chartCapability != null && !chartCapability.isNativeDropdownSupported() ? "active" : "unknown" ).append( '\n' );
    }

    /**
     * Appends runtime and packaging verification details.
     */
    private void appendRuntimeStatus( final StringBuilder builder, final String lifecycleState, final Path dumpFile, final String moduleName,
            final String moduleVersion, final boolean enabled ) {
        builder.append( "Packaging/runtime result: " ).append( safe( lifecycleState ) ).append( '\n' );
        builder.append( "- module: " ).append( safe( moduleName ) ).append( " " ).append( safe( moduleVersion ) ).append( '\n' );
        builder.append( "- diagnostics enabled: " ).append( yesNo( enabled ) ).append( '\n' );
        builder.append( "- dump file: " ).append( dumpFile == null ? "n/a" : dumpFile.toString() ).append( '\n' );
        builder.append( "- packaging convention: SDK-style Ant build expected under release/Scelight/mod-x/larva-seconds-lost/<version>/ and release/deployment/" );
    }

    /**
     * Formats per-player totals.
     */
    private String formatGroupTotals( final Map< String, String > groupOverviewLabelMap ) {
        if ( groupOverviewLabelMap == null || groupOverviewLabelMap.isEmpty() )
            return "none";

        final StringBuilder builder = new StringBuilder();
        boolean first = true;
        for ( final Map.Entry< String, String > entry : groupOverviewLabelMap.entrySet() ) {
            if ( !first )
                builder.append( ", " );
            builder.append( safe( entry.getKey() ) ).append( '=' ).append( safe( entry.getValue() ) );
            first = false;
        }
        return builder.toString();
    }

    /**
     * Formats per-player phase-table availability.
     */
    private String formatPhaseTableNames( final Map< String, LarvaPlayerPhaseTable > playerPhaseTableMap ) {
        if ( playerPhaseTableMap == null || playerPhaseTableMap.isEmpty() )
            return "none";

        final StringBuilder builder = new StringBuilder();
        boolean first = true;
        for ( final String playerName : playerPhaseTableMap.keySet() ) {
            if ( !first )
                builder.append( ", " );
            builder.append( safe( playerName ) );
            first = false;
        }
        return builder.toString();
    }

    /**
     * Formats a yes/no value.
     */
    private String yesNo( final boolean value ) {
        return value ? "yes" : "no";
    }

    /**
     * Returns safe text.
     */
    private String safe( final String value ) {
        return value == null || value.length() == 0 ? "n/a" : value;
    }

}