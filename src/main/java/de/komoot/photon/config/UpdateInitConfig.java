package de.komoot.photon.config;

import com.beust.jcommander.Parameter;

public class UpdateInitConfig {
    @Parameter(names = "-import-user",
               description = "Set up tracking of updates in the Nominatim database for the given user and exit.")
    private String importUser = null;

    public String getImportUser() {
        return importUser;
    }

    public void setImportUser(String userName) {
        importUser = userName;
    }
}
