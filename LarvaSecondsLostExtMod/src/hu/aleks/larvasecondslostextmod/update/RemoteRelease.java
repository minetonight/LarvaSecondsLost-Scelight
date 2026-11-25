package hu.aleks.larvasecondslostextmod.update;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The published release described by the hosted <code>module.xml</code> (E18-S08). Immutable.
 */
public final class RemoteRelease {

    private final String name;
    private final String folder;
    private final ModuleVersion version;
    private final String archiveFileName;
    private final String archiveSha256;
    private final long archiveSize;
    private final List< String > archiveUrlList;
    private final Map< String, String > fileSha256ByPath;

    /**
     * @param fileSha256ByPath
     *            zip paths (like <code>Scelight/mod-x/larva-seconds-lost/3.1/larva-seconds-lost.jar</code>) to lower-case hex SHA-256
     */
    public RemoteRelease( final String name, final String folder, final ModuleVersion version, final String archiveFileName,
            final String archiveSha256, final long archiveSize, final List< String > archiveUrlList,
            final Map< String, String > fileSha256ByPath ) {
        this.name = name;
        this.folder = folder;
        this.version = version;
        this.archiveFileName = archiveFileName;
        this.archiveSha256 = archiveSha256;
        this.archiveSize = archiveSize;
        this.archiveUrlList = Collections.unmodifiableList( new ArrayList<>( archiveUrlList ) );
        this.fileSha256ByPath = Collections.unmodifiableMap( new LinkedHashMap<>( fileSha256ByPath ) );
    }

    public String getName() {
        return name;
    }

    public String getFolder() {
        return folder;
    }

    public ModuleVersion getVersion() {
        return version;
    }

    public String getArchiveFileName() {
        return archiveFileName;
    }

    /** @return lower-case hex SHA-256 of the zip */
    public String getArchiveSha256() {
        return archiveSha256;
    }

    public long getArchiveSize() {
        return archiveSize;
    }

    /** @return download URLs of the zip, first one preferred; never empty */
    public List< String > getArchiveUrlList() {
        return archiveUrlList;
    }

    /** @return expected files of the zip (path to lower-case hex SHA-256), in module.xml order */
    public Map< String, String > getFileSha256ByPath() {
        return fileSha256ByPath;
    }

}
