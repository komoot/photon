package de.komoot.photon;

import java.util.Date;

import org.json.JSONObject;

import spark.Request;
import spark.Response;
import spark.RouteImpl;

public class StatusRequestHandler extends RouteImpl {

    protected Date importDate;

    protected StatusRequestHandler(String path, Date importDate) {
        super(path);
        this.importDate = importDate;
    }

    @Override
    public String handle(Request request, Response response) {
        final JSONObject out = new JSONObject();
        out.put("status", "Ok");
        out.put("import_date", this.importDate.toString());

        return out.toString();
    }
    
}
