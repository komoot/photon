package de.komoot.photon.cli;

import com.beust.jcommander.ParametersDelegate;
import de.komoot.photon.config.*;

public class CommandMain {
    @ParametersDelegate
    private final PostgresqlConfig postgresqlConfig = new PostgresqlConfig();

    @ParametersDelegate
    private final ApiServerConfig apiServerConfig = new ApiServerConfig();

    @ParametersDelegate
    private final PhotonDBConfig photonDBConfig = new PhotonDBConfig();

    @ParametersDelegate
    private final ImportFilterConfig importFilterConfig = new ImportFilterConfig();

    @ParametersDelegate
    private final GeneralConfig generalConfig = new GeneralConfig();

    @ParametersDelegate
    private final ImportFileConfig importFileConfig = new ImportFileConfig();

    @ParametersDelegate
    private final ExportDumpConfig exportDumpConfig = new ExportDumpConfig();

    @ParametersDelegate
    private final UpdateInitConfig updateInitConfig = new UpdateInitConfig();

    @ParametersDelegate
    private final LegacyConfig legacyConfig = new LegacyConfig();

    PostgresqlConfig getPostgresqlConfig() {
        return postgresqlConfig;
    }

    ApiServerConfig getApiServerConfig() {
        return apiServerConfig;
    }

    PhotonDBConfig getPhotonDBConfig() {
        return photonDBConfig;
    }

    ImportFilterConfig getImportFilterConfig() {
        return importFilterConfig;
    }

    GeneralConfig getGeneralConfig() {
        return generalConfig;
    }

    ImportFileConfig getImportFileConfig() {
        return importFileConfig;
    }

    ExportDumpConfig getExportDumpConfig() {
        return exportDumpConfig;
    }

    UpdateInitConfig getUpdateInitConfig() {
        return updateInitConfig;
    }

    Commands guessCommand() {
        if (legacyConfig.isNominatimImport()) {
            if (exportDumpConfig.getExportFile() != null) {
                return Commands.CMD_JSON_DUMP;
            }
            return Commands.CMD_IMPORT;
        }
        if (legacyConfig.isNominatimUpdate()) {
            return Commands.CMD_UPDATE;
        }
        if (legacyConfig.getNominatimUpdateInit() != null) {
            updateInitConfig.setImportUser(legacyConfig.getNominatimUpdateInit());
            return Commands.CMD_UPDATE_INIT;
        }

        return Commands.CMD_SERVE;
    }
}
