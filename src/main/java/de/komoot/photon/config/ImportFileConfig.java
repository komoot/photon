package de.komoot.photon.config;

import com.beust.jcommander.Parameter;

public class ImportFileConfig {
    public static final String GROUP = "Dump file import options";

    @Parameter(names = "-import-file", category = GROUP, placeholder = "FILE", description = """
            Name of json dump file
            """)
    private String importFile = null;

    public String getImportFile() { return this.importFile; }

    public boolean isEnabled() { return this.importFile != null; }
}
