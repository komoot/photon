package de.komoot.photon.importer;

/**
 * Command Line Arguments parsed by {@link com.beust.jcommander.JCommander} and used to start photon.
 */

import com.beust.jcommander.Parameter;
import lombok.Data;

import java.io.File;

@Data
public class CommandLineArgs {
	@Parameter(names = "-cluster", description = "name of elasticsearch cluster to put the server into (default: photon)")
	private String cluster = "photon";
	
	@Parameter(names = "-nominatim-import", description = "import nominatim database into photon (this will delete previous index)")
	private boolean nominatimImport = false;
        
        @Parameter(names = "-languages", description = "languages nominatim importer should import and use at run-time, comma seperated (default: 'en,fr,de,it')")
	private String languages = "en,fr,de,it";
        
        @Parameter(names = "-tag-whitelist-file", description = "optional tag whitelist json file of the type tags that should be imported. If not specified, importing all types")
	private String tagWhitelistFile = null;

	@Parameter(names = "-json", description = "import nominatim database and dump it to a json like files in (useful for developing)")
	private String jsonDump = null;

	@Parameter(names = "-delete-index", description = "delete index and all documents, creates a new and empty photon index")
	private boolean deleteIndex = false;

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

	@Parameter(names = "-h", description = "show help / usage")
	private boolean usage = false;
}

