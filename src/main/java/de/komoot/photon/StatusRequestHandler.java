package de.komoot.photon;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Map;

public class StatusRequestHandler implements Handler {
    private final Server server;
    private final String version;

    protected StatusRequestHandler(Server server) {
        this.server = server;
        this.version = versionReader();
    }

    @Override
    public void handle(@NotNull Context context) throws IOException {
        DatabaseProperties dbProperties = server.loadFromDatabase();
        String importDateStr = "";
        if (dbProperties.getImportDate() != null) {
            importDateStr = dbProperties.getImportDate().toInstant().toString();
        }
        context.json(Map.of("status", "Ok", "import_date", importDateStr, "version", version));
    }

    private String versionReader() {
        String v = StatusRequestHandler.class.getPackage().getImplementationVersion();
        return v != null ? v : "unknown";
    }
}
