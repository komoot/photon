package de.komoot.photon.config;

import com.beust.jcommander.Parameter;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class UpdateInitConfig {
    public static final String GROUP = "Initialisation options";

    @Parameter(names = "-import-user", category = GROUP, description = """
            Name of PostgreSQL user running the updates
            """)
    @Nullable private String importUser = null;

    @Nullable public String getImportUser() {
        return importUser;
    }

    public void setImportUser(@Nullable String userName) {
        importUser = userName;
    }
}
