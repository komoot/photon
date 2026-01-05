package de.komoot.photon.cli;

import com.beust.jcommander.Parameters;
import com.beust.jcommander.ParametersDelegate;
import de.komoot.photon.config.*;

@Parameters(commandDescription = """
        Create a Photon database from a Nominatim database or a Json dump.
        
        When the -import-file option is given, Photon will load the data from
        the given dump. Otherwise it will try to connect to a Nominatim
        database.
        
        If the Photon database doesn't exist yet, it will be created.
        If a database already exists at the given place, any content that
        may still exist in the cluster is dropped.
        """)
public class CommandImport {

    public CommandImport(GeneralConfig gCfg, ImportFileConfig ifCfg,
                         PostgresqlConfig pgCfg, PhotonDBConfig dbCfg, ImportFilterConfig filtCfg) {
        generalConfig = gCfg;
        importFileConfig = ifCfg;
        postgresqlConfig = pgCfg;
        photonDBConfig = dbCfg;
        importFilterConfig = filtCfg;
    }

    @ParametersDelegate
    private final GeneralConfig generalConfig;

    @ParametersDelegate
    private final ImportFileConfig importFileConfig;

    @ParametersDelegate
    private final PostgresqlConfig postgresqlConfig;

    @ParametersDelegate
    private final PhotonDBConfig photonDBConfig;

    @ParametersDelegate
    private final ImportFilterConfig importFilterConfig;
}
