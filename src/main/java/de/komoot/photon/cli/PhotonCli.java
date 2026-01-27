package de.komoot.photon.cli;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import de.komoot.photon.config.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class PhotonCli {
    private static final Logger LOGGER = LogManager.getLogger();

    private final JCommander jCommander;

    private final CommandMain cmdline;

    public PhotonCli() {
        cmdline = new CommandMain();
        jCommander = JCommander.newBuilder()
                .addObject(cmdline)
                .addCommand(
                        Commands.CMD_IMPORT.getCmd(),
                        new CommandImport(
                                cmdline.getGeneralConfig(),
                                cmdline.getImportFileConfig(),
                                cmdline.getPostgresqlConfig(),
                                cmdline.getPhotonDBConfig(),
                                cmdline.getImportFilterConfig()))
                .addCommand(
                        Commands.CMD_UPDATE.getCmd(),
                        new CommandUpdate(
                                cmdline.getGeneralConfig(),
                                cmdline.getImportFileConfig(),
                                cmdline.getPostgresqlConfig(),
                                cmdline.getPhotonDBConfig(),
                                cmdline.getImportFilterConfig()))
                .addCommand(
                        Commands.CMD_UPDATE_INIT.getCmd(),
                        new CommandUpdateInit(
                                cmdline.getGeneralConfig(),
                                cmdline.getUpdateInitConfig(),
                                cmdline.getPostgresqlConfig()))
                .addCommand(
                        Commands.CMD_JSON_DUMP.getCmd(),
                        new CommandJsonDump(
                                cmdline.getGeneralConfig(),
                                cmdline.getPostgresqlConfig(),
                                cmdline.getImportFilterConfig(),
                                cmdline.getExportDumpConfig()))
                .addCommand(
                        Commands.CMD_SERVE.getCmd(),
                        new CommandServe(
                                cmdline.getGeneralConfig(),
                                cmdline.getPhotonDBConfig(),
                                cmdline.getApiServerConfig(),
                                cmdline.getPostgresqlConfig()))
                .programName("photon")
                .build();
        jCommander.setUsageFormatter(new PhotonUsageFormatter(jCommander));
    }

    @Nullable
    public Commands parse(String[] args) {
        try {
            jCommander.parse(args);
        } catch (ParameterException e) {
            LOGGER.warn("Could not start photon: {}", e.getMessage());
            jCommander.usage();
            return null;
        }

        if (cmdline.getGeneralConfig().isUsage()) {
            jCommander.usage();
            return null;
        }

        String command = jCommander.getParsedCommand();

        if (command == null) {
            LOGGER.warn("DEPRECATION WARNING: the commandline syntax has changed. Please use the command-style syntax. Try '-h' for more info.");
            return cmdline.guessCommand();
        }

        return Commands.byCommand(command);
    }

    public PostgresqlConfig getPostgresqlConfig() {
        return cmdline.getPostgresqlConfig();
    }

    public ApiServerConfig getApiServerConfig() {
        return cmdline.getApiServerConfig();
    }

    public PhotonDBConfig getPhotonDBConfig() {
        return cmdline.getPhotonDBConfig();
    }

    public ImportFilterConfig getImportFilterConfig() {
        return cmdline.getImportFilterConfig();
    }

    public GeneralConfig getGeneralConfig() {
        return cmdline.getGeneralConfig();
    }

    public ImportFileConfig getImportFileConfig() {
        return cmdline.getImportFileConfig();
    }

    public ExportDumpConfig getExportDumpConfig() {
        return cmdline.getExportDumpConfig();
    }

    public UpdateInitConfig getUpdateInitConfig() {
        return cmdline.getUpdateInitConfig();
    }

}
