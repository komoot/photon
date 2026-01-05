package de.komoot.photon.config;

import com.beust.jcommander.Parameter;

public class ExportDumpConfig {

    @Parameter(names = {"-json", "-export-file"}, description = "File to dump to (use '-' for dumping to stdout).")
    private String exportFile = null;

    public String getExportFile() { return exportFile; }
}
