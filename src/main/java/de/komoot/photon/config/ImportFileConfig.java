package de.komoot.photon.config;

import com.beust.jcommander.Parameter;
import org.jspecify.annotations.Nullable;

public class ImportFileConfig {
    public static final String GROUP = "Dump file import options";

    @Parameter(names = "-import-file", category = GROUP, placeholder = "FILE", description = """
            Name of json dump file
            """)
    @Nullable private String importFile = null;

    public String getImportFile() {
        assert this.importFile != null; // must only be called after isEnabled()
        return this.importFile;
    }

    public boolean isEnabled() { return this.importFile != null; }
}
