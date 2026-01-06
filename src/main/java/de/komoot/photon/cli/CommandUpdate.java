package de.komoot.photon.cli;

import com.beust.jcommander.Parameters;
import com.beust.jcommander.ParametersDelegate;
import de.komoot.photon.config.*;

@Parameters(commandDescription = """
        Update a Photon database from a Nominatim database.
        
        To run updates, you first must set up an updateable Nominatim database
        and create an initial import of a Photon database from there. Next run
        'photon update-init' to initialise updates. Then the Nominatim database
        can be updated through the usual replication mechanisms. To synchronise
        your Photon database, use this command to apply the latest
        changes while the Photon database is offline.
        
        It is possible to run this command, while the Nominatim database
        itself is updating but not the recommended mode of operation. Ideally
        this command is run after a replication cycle on the Nominatim database
        has finished.
        
        To apply updates while the geocoding service is running, enable the
        update endpoint and trigger updates through the API.
        """)
public class CommandUpdate {
    public CommandUpdate(GeneralConfig gCfg, ImportFileConfig ifCfg,
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
