package hu.aleks.larvasecondslostextmod.update;

/**
 * A module version <code>major.minor.revision</code> (E18-S08).
 *
 * <p>Folder names follow Scelight's {@code VersionBean.toString()} and the Ant build: the revision is left out when it is 0
 * (<code>3.1</code>, <code>3.1.2</code>). <code>3.1</code> and <code>3.1.0</code> are equal.</p>
 */
public final class ModuleVersion implements Comparable< ModuleVersion > {

    private final int major;
    private final int minor;
    private final int revision;

    /**
     * @throws IllegalArgumentException if a part is negative
     */
    public ModuleVersion( final int major, final int minor, final int revision ) {
        if ( major < 0 || minor < 0 || revision < 0 )
            throw new IllegalArgumentException( "Negative version part: " + major + "." + minor + "." + revision );
        this.major = major;
        this.minor = minor;
        this.revision = revision;
    }

    /**
     * Parses <code>major.minor</code> or <code>major.minor.revision</code>.
     *
     * @param text the version text, for example a version folder name
     * @return the version, or <code>null</code> if the text is not a version (so other folder names are simply skipped)
     */
    public static ModuleVersion parseOrNull( final String text ) {
        if ( text == null )
            return null;
        final String[] parts = text.split( "\\.", -1 );
        if ( parts.length < 2 || parts.length > 3 )
            return null;
        final int[] numbers = new int[ 3 ];
        for ( int i = 0; i < parts.length; i++ ) {
            if ( !isDigits( parts[ i ] ) )
                return null;
            try {
                numbers[ i ] = Integer.parseInt( parts[ i ] );
            } catch ( final NumberFormatException e ) {
                return null; // too many digits for an int: not a version we ever publish
            }
        }
        return new ModuleVersion( numbers[ 0 ], numbers[ 1 ], numbers[ 2 ] );
    }

    private static boolean isDigits( final String part ) {
        if ( part.length() == 0 || part.length() > 9 )
            return false;
        for ( int i = 0; i < part.length(); i++ )
            if ( part.charAt( i ) < '0' || part.charAt( i ) > '9' )
                return false;
        return true;
    }

    public int getMajor() {
        return major;
    }

    public int getMinor() {
        return minor;
    }

    public int getRevision() {
        return revision;
    }

    /** @return <code>true</code> if this version is strictly higher than <code>other</code> */
    public boolean isNewerThan( final ModuleVersion other ) {
        return compareTo( other ) > 0;
    }

    @Override
    public int compareTo( final ModuleVersion other ) {
        if ( major != other.major )
            return major < other.major ? -1 : 1;
        if ( minor != other.minor )
            return minor < other.minor ? -1 : 1;
        if ( revision != other.revision )
            return revision < other.revision ? -1 : 1;
        return 0;
    }

    @Override
    public boolean equals( final Object obj ) {
        if ( !( obj instanceof ModuleVersion ) )
            return false;
        return compareTo( (ModuleVersion) obj ) == 0;
    }

    @Override
    public int hashCode() {
        return ( major * 31 + minor ) * 31 + revision;
    }

    /** @return the folder name / display form, for example <code>3.1</code> or <code>3.1.2</code> */
    @Override
    public String toString() {
        return revision == 0 ? major + "." + minor : major + "." + minor + "." + revision;
    }

}
