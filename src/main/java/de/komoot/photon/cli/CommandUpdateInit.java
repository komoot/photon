package de.komoot.photon.cli;

import com.beust.jcommander.ParametersDelegate;
import de.komoot.photon.config.UpdateInitConfig;

public class CommandUpdateInit {
    public CommandUpdateInit(UpdateInitConfig cfg) {
        updateInitConfig = cfg;
    }

    @ParametersDelegate
    private final UpdateInitConfig updateInitConfig;
}
