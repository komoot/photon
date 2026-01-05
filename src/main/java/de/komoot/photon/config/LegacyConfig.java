package de.komoot.photon.config;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

@Parameters(hidden = true)
public class LegacyConfig {
    @Parameter(names = "-nominatim-import", description = "Import nominatim database into photon (deleting the previous index).")
    private boolean nominatimImport = false;

    @Parameter(names = "-nominatim-update-init-for", description = "Set up tracking of updates in the Nominatim database for the given user and exit.")
    private String nominatimUpdateInit = null;

    @Parameter(names = "-nominatim-update", description = "Fetch updates from nominatim database into photon and exit (updates the index only without offering an API).")
    private boolean nominatimUpdate = false;

    public boolean isNominatimImport() {
        return nominatimImport;
    }

    public boolean isNominatimUpdate() {
        return nominatimUpdate;
    }

    public String getNominatimUpdateInit() {
        return nominatimUpdateInit;
    }
}
