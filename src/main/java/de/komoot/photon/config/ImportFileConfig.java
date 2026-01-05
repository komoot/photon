package de.komoot.photon.config;

import com.beust.jcommander.Parameter;

public class ImportFileConfig {

    @Parameter(names = "-import-file", description = "Name of json dump file.")
    private String importFile = null;

    public String getImportFile() { return this.importFile; }

    public boolean isEnabled() { return this.importFile != null; }
}
