package de.komoot.photon;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import de.komoot.photon.utils.CorsMutuallyExclusiveValidator;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Command Line Arguments parsed by {@link com.beust.jcommander.JCommander} and used to start photon.
 */
@Parameters(parametersValidators = CorsMutuallyExclusiveValidator.class)
public class CommandLineArgs {

    @Parameter(names = "-j", description = "Number of threads to use for import.")
    private int threads = 1;

    @Parameter(names = "-structured", description = "Enable support for structured queries.")
    private boolean supportStructuredQueries = false;

    @Parameter(names = "-cluster", description = "Name of ElasticSearch cluster to put the server into.")
    private String cluster = "photon";

    @Parameter(names = "-transport-addresses", description = "Comma-separated list of addresses of external ElasticSearch nodes the client can connect to. An empty list (the default) forces an internal node to start.")
    private List<String> transportAddresses = new ArrayList<>();

    @Parameter(names = "-nominatim-import", description = "Import nominatim database into photon (deleting the previous index).")
    private boolean nominatimImport = false;

    @Parameter(names = "-nominatim-update-init-for", description = "Set up tracking of updates in the Nominatim database for the given user and exit.")
    private String nominatimUpdateInit = null;

    @Parameter(names = "-nominatim-update", description = "Fetch updates from nominatim database into photon and exit (updates the index only without offering an API).")
    private boolean nominatimUpdate = false;

    @Parameter(names = "-languages", description = "Comma-separated list of languages to use. On import sets the name translations to use (default: de,en,fr,it). When running, the languages to be searched may be further restricted.")
    private List<String> languages = new ArrayList<>();

    @Parameter(names = "-default-language", description = "Language to return results in when no explicit language is chosen by the user.")
    private String defaultLanguage = "default";

    @Parameter(names = "-country-codes", description = "[import-only] Comma-separated list of country codes for countries the importer should import, comma separated. An empty list means the full database is imported.")
    private List<String> countryCodes = new ArrayList<>();

    @Parameter(names = "-extra-tags", description = "Comma-separated list of additional tags to save for each place.")
    private List<String> extraTags = new ArrayList<>();

    @Parameter(names = "-synonym-file", description = "File with synonym and classification terms.")
    private String synonymFile = null;

    @Parameter(names = "-query-timeout", description = "Time after which to cancel queries to the ES database (in seconds).")
    private int queryTimeout = 7;

    @Parameter(names = "-json", description = "Read from nominatim database and dump it to the given file in a json-like format (useful for developing).")
    private String jsonDump = null;

    @Parameter(names = "-host", description = "Hostname of the PostgreSQL database.")
    private String host = "127.0.0.1";

    @Parameter(names = "-port", description = "Port of the PostgreSQL database.")
    private Integer port = 5432;

    @Parameter(names = "-database", description = "Database name of the Nominatim database.")
    private String database = "nominatim";

    @Parameter(names = "-user", description = "Username in the PostgreSQL database.")
    private String user = "nominatim";

    @Parameter(names = "-password", description = "Password for the PostgreSQL database.")
    private String password = null;

    @Parameter(names = "-data-dir", description = "Photon data directory.")
    private String dataDirectory = new File(".").getAbsolutePath();

    @Parameter(names = "-listen-port", description = "Port for the Photon server to listen to.")
    private int listenPort = 2322;

    @Parameter(names = "-listen-ip", description = "Address for the Photon server to listen to.")
    private String listenIp = "0.0.0.0";

    @Parameter(names = "-cors-any", description = "Enable cross-site resource sharing for any origin.")
    private boolean corsAnyOrigin = false;
    
    @Parameter(names = "-cors-origin", description = "Comma-separated list of origins for which to enable cross-site resource sharing.")
    private List<String> corsOrigin = new ArrayList<>();

    @Parameter(names = "-enable-update-api", description = "Enable the additional endpoint /nominatim-update, which allows to trigger updates from a nominatim database")
    private boolean enableUpdateApi = false;

    @Parameter(names = "-h", description = "Show help / usage")
    private boolean usage = false;

    @Parameter(names = "-max-results", description = "The maximum possible 'limit' parameter for geocoding searches")
    private int maxResults = 50;

    @Parameter(names = "-max-reverse-results", description = "The maximum possible 'limit' parameter for reverse geocoding searches")
    private int maxReverseResults = 50;

    @Parameter(names = "-import-geometry-column", description = "[import-only] Add the 'geometry' column from Nominatim on import (i.e. add Polygons/Linestrings/Multipolygons etc. for cities, countries etc.). WARNING: This will increase the Elasticsearch Index size! (~575GB for Planet)")
    private boolean importGeometryColumn = false;

    public String[] getLanguages(boolean useDefaultIfEmpty) {
        if (useDefaultIfEmpty && languages.isEmpty()) {
            return new String[]{"en", "de", "fr", "it"};
        }

        return languages.toArray(new String[0]);
    }

    public String[] getLanguages() {
        return getLanguages(true);
    }

    public int getThreads() {
        return Integer.min(10, Integer.max(0, threads));
    }

    public String getCluster() {
        return this.cluster;
    }

    public String[] getTransportAddresses() {
        return this.transportAddresses.toArray(new String[0]);
    }

    public boolean isNominatimImport() {
        return this.nominatimImport;
    }

    public String getNominatimUpdateInit() {
        return this.nominatimUpdateInit;
    }

    public boolean isNominatimUpdate() {
        return this.nominatimUpdate;
    }

    public String getDefaultLanguage() {
        return this.defaultLanguage;
    }

    public String[] getCountryCodes() {
        return this.countryCodes.toArray(new String[0]);
    }

    public String[] getExtraTags() {
        return this.extraTags.toArray(new String[0]);
    }

    public String getSynonymFile() {
        return this.synonymFile;
    }

    public int getQueryTimeout() {
        return this.queryTimeout;
    }

    public String getJsonDump() {
        return this.jsonDump;
    }

    public String getHost() {
        return this.host;
    }

    public Integer getPort() {
        return this.port;
    }

    public String getDatabase() {
        return this.database;
    }

    public String getUser() {
        return this.user;
    }

    public String getPassword() {
        return this.password;
    }

    public String getDataDirectory() {
        return this.dataDirectory;
    }

    public int getListenPort() {
        return this.listenPort;
    }

    public String getListenIp() {
        return this.listenIp;
    }

    public boolean isCorsAnyOrigin() {
        return this.corsAnyOrigin;
    }

    public String[] getCorsOrigin() {
        return this.corsOrigin.toArray(new String[0]);
    }

    public boolean isEnableUpdateApi() {
        return this.enableUpdateApi;
    }

    public boolean isUsage() {
        return this.usage;
    }
    
    public boolean getSupportStructuredQueries() { return supportStructuredQueries; }

    public int getMaxReverseResults() {
        return maxReverseResults;
    }

    public int getMaxResults() {
        return maxResults;
    }

    public boolean getImportGeometryColumn() {
        return importGeometryColumn;
    }
}

