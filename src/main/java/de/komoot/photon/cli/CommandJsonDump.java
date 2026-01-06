package de.komoot.photon.cli;

import com.beust.jcommander.Parameters;
import com.beust.jcommander.ParametersDelegate;
import de.komoot.photon.config.*;

@Parameters(commandDescription = """
        Export contents of a Nominatim database to a Json dump file.
        """)
public class CommandJsonDump {
    public CommandJsonDump(GeneralConfig gCfg, PostgresqlConfig pgCfg,
                           ImportFilterConfig filtCfg, ExportDumpConfig exCfg) {
        generalConfig = gCfg;
        postgresqlConfig = pgCfg;
        importFilterConfig = filtCfg;
        exportDumpConfig = exCfg;
    }

    @ParametersDelegate
    private final GeneralConfig generalConfig;

    @ParametersDelegate
    private final PostgresqlConfig postgresqlConfig;

    @ParametersDelegate
    private final ImportFilterConfig importFilterConfig;

    @ParametersDelegate
    private final ExportDumpConfig exportDumpConfig;
}
