package de.komoot.photon;

/**
 * Command Line Arguments parsed by {@link com.beust.jcommander.JCommander} and used to start photon.
 */

import com.beust.jcommander.Parameter;
import de.komoot.photon.utils.StringArrayConverter;
import lombok.Data;

import java.io.File;


@Data
public class CommandLineArgs {

    @Parameter(names = "-cluster", description = "name of elasticsearch cluster to put the server into (default is 'photon')")
    private String cluster = "photon";

    @Parameter(names = "-transport-addresses", description = "the comma separated addresses of external elasticsearch nodes where the client can connect to (default is an empty string which forces an internal node to start)", converter = StringArrayConverter.class)
    private String[] transportAddresses = new String[]{};

    @Parameter(names = "-nominatim-import", description = "import nominatim database into photon (this will delete previous index)")
    private boolean nominatimImport = false;

    @Parameter(names = "-nominatim-update", description = "fetch updates from nominatim database into photon and exit (this updates the index only without offering an API)")
    private boolean nominatimUpdate = false;

    @Parameter(names = "-languages", description = "languages nominatim importer should import and use at run-time, comma separated (default is 'en,fr,de,it')", converter = StringArrayConverter.class)
    private String[] languages = new String[]{};

    @Parameter(names = "-default-language", description = "language to return results in when no explicit language is choosen by the user")
    private String defaultLanguage = "default";

    @Parameter(names = "-country-codes", description = "country codes filter that nominatim importer should import, comma separated. If empty full planet is done", converter = StringArrayConverter.class)
    private String[] countryCodes = new String[]{};

    @Parameter(names = "-extra-tags", description = "comma-separated list of additional tags to save for each place", converter = StringArrayConverter.class)
    private String[] extraTags = new String[]{};

    @Parameter(names = "-synonym-file", description = "file with synonym and classification terms")
    private String synonymFile = null;

    @Parameter(names = "-json", description = "import nominatim database and dump it to a json like files in (useful for developing)")
    private String jsonDump = null;

    @Parameter(names = "-host", description = "postgres host (default 127.0.0.1)")
    private String host = "127.0.0.1";

    @Parameter(names = "-port", description = "postgres port (default 5432)")
    private Integer port = 5432;

    @Parameter(names = "-database", description = "postgres host (default nominatim)")
    private String database = "nominatim";

    @Parameter(names = "-user", description = "postgres user (default nominatim)")
    private String user = "nominatim";

    @Parameter(names = "-password", description = "postgres password (default '')")
    private String password = "";

    @Parameter(names = "-data-dir", description = "data directory (default '.')")
    private String dataDirectory = new File(".").getAbsolutePath();

    @Parameter(names = "-listen-port", description = "listen to port (default 2322)")
    private int listenPort = 2322;

    @Parameter(names = "-listen-ip", description = "listen to address (default '0.0.0.0')")
    private String listenIp = "0.0.0.0";

    @Parameter(names = "-cors-any", description = "enable cross-site resource sharing for any origin ((default CORS not supported)")
    private boolean corsAnyOrigin = false;
    
    @Parameter(names = "-cors-origin", description = "enable cross-site resource sharing for the specified origin (default CORS not supported)")
    private String corsOrigin = null;

    @Parameter(names = "-enable-update-api", description = "Enable the additional endpoint /nominatim-update, which allows to trigger updates from a nominatim database")
    private boolean enableUpdateApi = false;

    @Parameter(names = "-h", description = "show help / usage")
    private boolean usage = false;

    public String[] getLanguages(boolean useDefaultIfEmpty) {
        if (useDefaultIfEmpty && languages.length == 0) {
            return new String[]{"en", "de", "fr", "it"};
        }

        return languages;
    }

    public String[] getLanguages() {
        return getLanguages(true);
    }
}

