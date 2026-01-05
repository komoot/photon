package de.komoot.photon.cli;

import com.beust.jcommander.ParametersDelegate;
import de.komoot.photon.config.*;

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
