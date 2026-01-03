package de.komoot.photon;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParametersDelegate;
import de.komoot.photon.config.ApiServerConfig;
import de.komoot.photon.config.ImportFilterConfig;
import de.komoot.photon.config.PhotonDBConfig;
import de.komoot.photon.config.PostgresqlConfig;

/**
 * Command Line Arguments parsed by {@link com.beust.jcommander.JCommander} and used to start photon.
 */
public class CommandLineArgs {

    @Parameter(names = "-j", description = "Number of threads to use for import.")
    private int threads = 1;

    @Parameter(names = "-nominatim-import", description = "Import nominatim database into photon (deleting the previous index).")
    private boolean nominatimImport = false;

    @Parameter(names = "-nominatim-update-init-for", description = "Set up tracking of updates in the Nominatim database for the given user and exit.")
    private String nominatimUpdateInit = null;

    @Parameter(names = "-nominatim-update", description = "Fetch updates from nominatim database into photon and exit (updates the index only without offering an API).")
    private boolean nominatimUpdate = false;

    @Parameter(names = "-json", description = "Read from nominatim database and dump it to the given file in a json-like format (use '-' for dumping to stdout).")
    private String jsonDump = null;

    @Parameter(names = "-import-file", description = "Import data from the given json file.")
    private String importFile = null;


    @Parameter(names = "-h", description = "Show help / usage")
    private boolean usage = false;

    @ParametersDelegate
    private PostgresqlConfig postgresqlConfig = new PostgresqlConfig();

    @ParametersDelegate
    private ApiServerConfig apiServerConfig = new ApiServerConfig();

    @ParametersDelegate
    private PhotonDBConfig photonDBConfig = new PhotonDBConfig();

    @ParametersDelegate
    private ImportFilterConfig importFilterConfig = new ImportFilterConfig();

    public int getThreads() {
        return Integer.min(10, Integer.max(0, threads));
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

    public String getJsonDump() {
        return this.jsonDump;
    }

    public String getImportFile() { return this.importFile; }

    public boolean isUsage() {
        return this.usage;
    }
    
    public PostgresqlConfig getPostgresqlConfig() { return postgresqlConfig; }

    public ApiServerConfig getApiServerConfig() { return apiServerConfig; }

    public PhotonDBConfig getPhotonDBConfig() { return photonDBConfig; }

    public ImportFilterConfig getImportFilterConfig() { return importFilterConfig; }
}

