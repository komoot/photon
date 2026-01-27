package de.komoot.photon.config;

import com.beust.jcommander.Parameter;
import org.jspecify.annotations.Nullable;

public class ExportDumpConfig {
    public static final String GROUP = "Export file options";

    @Parameter(names = {"-export-file", "-json"}, category = GROUP, placeholder = "[-|FILE]", description = """
            File to dump the export to; use '-' for dumping to stdout.
            """)
    @Nullable private String exportFile = null;

    @Nullable public String getExportFile() { return exportFile; }
}
