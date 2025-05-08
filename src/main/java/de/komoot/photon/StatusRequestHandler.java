package de.komoot.photon;

import java.io.IOException;
import java.util.Date;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import spark.Request;
import spark.Response;
import spark.RouteImpl;

public class StatusRequestHandler extends RouteImpl {
    private final Server server;
    private final ObjectMapper mapper = new ObjectMapper();

    protected StatusRequestHandler(String path, Server server) {
        super(path);
        this.server = server;
    }

    @Override
    public String handle(Request request, Response response) throws IOException {
        DatabaseProperties dbProperties = server.loadFromDatabase();
        String importDateStr = null;
        if (dbProperties.getImportDate() instanceof Date) {
            importDateStr = dbProperties.getImportDate().toInstant().toString();
        }
        
        return mapper.writeValueAsString(Map.of("status", "Ok", "import_date", importDateStr));
    }
    
}
