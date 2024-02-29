package de.komoot.photon;

import org.json.JSONObject;

import de.komoot.photon.elasticsearch.Server;
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
    public String handle(Request request, Response response) {
        DatabaseProperties dbProperties = new DatabaseProperties();
        server.loadFromDatabase(dbProperties);
        String importDateStr = dbProperties.getImportDate().toInstant().toString();
        
        final JSONObject out = new JSONObject();
        out.put("status", "Ok");
        out.put("import_date", importDateStr);

        return out.toString();
    }
    
}
