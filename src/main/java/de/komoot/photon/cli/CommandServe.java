package de.komoot.photon.cli;

import com.beust.jcommander.ParametersDelegate;
import de.komoot.photon.config.*;

public class CommandServe {
    public CommandServe(GeneralConfig gCfg, PhotonDBConfig dbCfg, ApiServerConfig apiCfg) {
        generalConfig = gCfg;
        photonDBConfig = dbCfg;
        apiServerConfig = apiCfg;
    }

    @ParametersDelegate
    private final GeneralConfig generalConfig;

    @ParametersDelegate
    private final PhotonDBConfig photonDBConfig;

    @ParametersDelegate
    private final ApiServerConfig apiServerConfig;
}
