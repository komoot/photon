package de.komoot.photon;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Map;

public class StatusRequestHandler implements Handler {
    private final Server server;

    protected StatusRequestHandler(Server server) {
        this.server = server;
    }

    @Override
    public void handle(@NotNull Context context) throws IOException {
        DatabaseProperties dbProperties = server.loadFromDatabase();
        String importDateStr = "";
        if (dbProperties.getImportDate() != null) {
            importDateStr = dbProperties.getImportDate().toInstant().toString();
        }
        
        context.json(Map.of("status", "Ok", "import_date", importDateStr));
    }
    
}
