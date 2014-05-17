package de.komoot.photon.importer;

/**
 * Created with IntelliJ IDEA.
 * User: felix
 * Date: 5/15/14
 * Time: 1:20 PM
 * To change this template use File | Settings | File Templates.
 */
import com.beust.jcommander.Parameter;
import lombok.Data;

import java.io.File;

@Data
public class JCommanderStdin {

    @Parameter(names = "-index", description = "Start elasticsearch indexing")
    private boolean indexer = false;

    @Parameter(names = "-server", description = "Run the webserver")
    private boolean server = false;

    @Parameter(names="-host", description="Postgres host (default 127.0.0.1)")
    private String host = "127.0.0.1";

    @Parameter(names="-port", description="Postgres port (default 5432)")
    private Integer port = 5432;

    @Parameter(names="-database", description="Postgres host (default nominatim)")
    private String database = "nominatim";

    @Parameter(names="-user", description="Postgres user (default nominatim)")
    private String user = "nominatim";

    @Parameter(names="-password", description="Postgres host (default '')")
    private String password = "";


    @Parameter(names="-data-dir", description="Data directory (default '.')")
    private String dataDirectory = new File(".").getAbsolutePath();
}

