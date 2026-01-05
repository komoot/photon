package de.komoot.photon.config;

import com.beust.jcommander.Parameter;

public class ExportDumpConfig {
    public static final String GROUP = "Export file options";

    @Parameter(names = {"-export-file", "-json"}, category = GROUP, placeholder = "[-|FILE]", description = """
            File to dump the export to; use '-' for dumping to stdout.
            """)
    private String exportFile = null;

    public String getExportFile() { return exportFile; }
}
