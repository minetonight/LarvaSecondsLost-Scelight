package hu.aleks.larvasecondslostextmod.update;

/**
 * Fixed values of the online update (E18). The hosted URLs are a contract: once published they never change
 * (see <code>RELEASING.md</code>).
 */
public final class UpdateConfig {

    /** Landing page of the module (also the manifest's home page). */
    public static final String HOME_PAGE_URL = "https://minetonight.github.io/scelight/larva-seconds-lost/";

    /** Always describes the latest release. */
    public static final String MODULE_XML_URL = HOME_PAGE_URL + "module.xml";

    /** System property that replaces {@link #MODULE_XML_URL}, for tests against a local or staging copy. */
    public static final String MODULE_XML_URL_PROPERTY = "larva.update.moduleXmlUrl";

    /** Automatic checks run at most this often. */
    public static final long AUTO_CHECK_INTERVAL_MS = 24L * 60L * 60L * 1000L;

    /** Delay of the automatic check after startup, so it never competes with Scelight's own startup work. */
    public static final long AUTO_CHECK_STARTUP_DELAY_MS = 30L * 1000L;

    static final int CONNECT_TIMEOUT_MS = 5000;
    static final int READ_TIMEOUT_MS = 15000;

    /** Upper limit for the module.xml download. */
    static final long MAX_MODULE_XML_BYTES = 1024L * 1024L;

    /** Upper limit for a release zip (3.0 is about 1.6 MB). */
    static final long MAX_ARCHIVE_BYTES = 64L * 1024L * 1024L;

    /**
     * @return the module.xml URL, or the {@link #MODULE_XML_URL_PROPERTY} override if set
     */
    public static String moduleXmlUrl() {
        final String override = System.getProperty( MODULE_XML_URL_PROPERTY );
        return override == null || override.trim().length() == 0 ? MODULE_XML_URL : override.trim();
    }

    private UpdateConfig() {
    }

}
