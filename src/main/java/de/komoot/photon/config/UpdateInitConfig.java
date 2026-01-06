package de.komoot.photon.config;

import com.beust.jcommander.Parameter;

public class UpdateInitConfig {
    public static final String GROUP = "Initialisation options";

    @Parameter(names = "-import-user", category = GROUP, description = """
            Name of PostgreSQL user running the updates
            """)
    private String importUser = null;

    public String getImportUser() {
        return importUser;
    }

    public void setImportUser(String userName) {
        importUser = userName;
    }
}
