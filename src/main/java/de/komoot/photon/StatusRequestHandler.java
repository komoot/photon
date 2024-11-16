package de.komoot.photon;

import java.io.IOException;
import java.util.Date;

import org.json.JSONObject;

import spark.Request;
import spark.Response;
import spark.RouteImpl;

public class StatusRequestHandler extends RouteImpl {
    private Server server;

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
        
        final JSONObject out = new JSONObject();
        out.put("status", "Ok");
        out.put("import_date", importDateStr);

        return out.toString();
    }
    
}
