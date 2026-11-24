package hu.aleks.larvasecondslostextmod;

/** Column header labels of the phase tables (single page and aggregated tables). */
final class LarvaPhaseHeaderLabels {

    private LarvaPhaseHeaderLabels() {
    }

    /**
     * Resolves the explicit phase header label with the drone-count threshold.
     *
     * @param phase phase; <code>null</code> gives <code>n/a</code>
     * @return header text, e.g. <code>Early game [&lt;37]</code>
     */
    static String resolve( final LarvaGamePhase phase ) {
        if ( phase == null )
            return LarvaStatFormat.NOT_AVAILABLE;

        switch ( phase ) {
            case EARLY :
                return "Early game [<37]";
            case MID :
                return "Mid game [<67]";
            case LATE :
                return "Late game [<90]";
            case END :
            default :
                return "End game [90+]";
        }
    }
}
