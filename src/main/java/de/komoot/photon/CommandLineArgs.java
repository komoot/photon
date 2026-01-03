package de.komoot.photon;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.ParametersDelegate;
import de.komoot.photon.config.ApiServerConfig;
import de.komoot.photon.config.PostgresqlConfig;
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

    @Parameter(names = "-structured", description = "(unused) Structured queries are always enabled.")
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

    @Parameter(names = "-country-codes", description = "[import-only] Comma-separated list of country codes for countries the importer should import, comma separated. An empty list means the full database is imported.")
    private List<String> countryCodes = new ArrayList<>();

    @Parameter(names = "-extra-tags", description = "Comma-separated list of additional tags to save for each place.")
    private List<String> extraTags = null;

    @Parameter(names = "-json", description = "Read from nominatim database and dump it to the given file in a json-like format (use '-' for dumping to stdout).")
    private String jsonDump = null;

    @Parameter(names = "-import-file", description = "Import data from the given json file.")
    private String importFile = null;


    @Parameter(names = "-data-dir", description = "Photon data directory.")
    private String dataDirectory = new File(".").getAbsolutePath();

    @Parameter(names = "-h", description = "Show help / usage")
    private boolean usage = false;

    @Parameter(names = "-import-geometry-column", description = "[import-only] Add the 'geometry' column from Nominatim on import (i.e. add Polygons/Linestrings/Multipolygons etc. for cities, countries etc.). WARNING: This will increase the Elasticsearch Index size! (~575GB for Planet)")
    private boolean importGeometryColumn = false;

    @ParametersDelegate
    private PostgresqlConfig postgresqlConfig = new PostgresqlConfig();

    @ParametersDelegate
    private ApiServerConfig apiServerConfig = new ApiServerConfig();

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

    public String[] getCountryCodes() {
        return this.countryCodes.toArray(new String[0]);
    }

    public ConfigExtraTags getExtraTags() {
        return new ConfigExtraTags(extraTags == null? List.of() : extraTags);
    }

    public boolean isExtraTagsSet() { return this.extraTags == null; }

    public String getJsonDump() {
        return this.jsonDump;
    }

    public String getImportFile() { return this.importFile; }

    public String getDataDirectory() {
        return this.dataDirectory;
    }

    public boolean isUsage() {
        return this.usage;
    }
    
    public boolean getImportGeometryColumn() {
        return importGeometryColumn;
    }

    public PostgresqlConfig getPostgresqlConfig() { return postgresqlConfig; }

    public ApiServerConfig getApiServerConfig() { return apiServerConfig; }

    public DatabaseProperties getDatabaseProperties() {
        final var dbProps = new DatabaseProperties();
        if (!languages.isEmpty()) {
            dbProps.setLanguages(languages.toArray(new String[0]));
        }
        dbProps.setSupportGeometries(importGeometryColumn);

        if (extraTags != null) {
            dbProps.setExtraTags(extraTags);
        }

        return dbProps;
    }
}

