package hu.aleks.larvasecondslostextmod;

/**
 * Adds the Battle.net region to a player name (CR-1.4, FR-8), so two accounts with the same name can be told apart.
 *
 * <p>The region comes from the first field of the identity key ({@code region-program-realm-id}, D-5). The identity key itself
 * is not changed. Pure and stateless.</p>
 */
final class LarvaRegionNames {

    private static final String NAME_KEY_PREFIX = "name:";

    private LarvaRegionNames() {
    }

    /**
     * @param regionId
     *            Battle.net region id
     * @return {@code US} (1), {@code EU} (2), {@code KR} (3), {@code CN} (5), {@code PTR} (98), otherwise {@code R<id>}
     */
    static String regionLabel( final int regionId ) {
        switch ( regionId ) {
        case 1:
            return "US";
        case 2:
            return "EU";
        case 3:
            return "KR";
        case 5:
            return "CN";
        case 98:
            return "PTR";
        default:
            return "R" + regionId;
        }
    }

    /**
     * @param name
     *            display name
     * @param identityKey
     *            identity key of the player; may be null
     * @return {@code name (REGION)}; the plain name if the key is a {@code name:} fallback, missing or malformed
     */
    static String withRegionSuffix( final String name, final String identityKey ) {
        final Integer regionId = parseRegionId( identityKey );
        return regionId == null ? name : name + " (" + regionLabel( regionId.intValue() ) + ")";
    }

    private static Integer parseRegionId( final String identityKey ) {
        if ( identityKey == null || identityKey.startsWith( NAME_KEY_PREFIX ) )
            return null;
        final int dash = identityKey.indexOf( '-' );
        if ( dash <= 0 )
            return null;
        try {
            return Integer.valueOf( identityKey.substring( 0, dash ) );
        } catch ( final NumberFormatException e ) {
            return null;
        }
    }
}
