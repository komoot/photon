package de.komoot.photon;

import de.komoot.photon.query.BadRequestException;
import de.komoot.photon.query.LookupRequest;
import de.komoot.photon.query.LookupRequestFactory;
import de.komoot.photon.searcher.GeocodeJsonFormatter;
import de.komoot.photon.searcher.PhotonResult;
import de.komoot.photon.searcher.LookupHandler;
import org.json.JSONObject;
import spark.Request;
import spark.Response;
import spark.RouteImpl;

import java.util.Arrays;
import java.util.List;

import static spark.Spark.halt;

public class LookupSearchRequestHandler extends RouteImpl {
    private final LookupRequestFactory photonRequestFactory;
    private final LookupHandler requestHandler;

    LookupSearchRequestHandler(String path, LookupHandler dbHandler, String[] languages, String defaultLanguage) {
        super(path);
        List<String> supportedLanguages = Arrays.asList(languages);
        this.photonRequestFactory = new LookupRequestFactory(supportedLanguages, defaultLanguage);
        this.requestHandler = dbHandler;
    }

    public String handle(Request request, Response response) {
        LookupRequest lookupRequest = null;

        try {
            lookupRequest = photonRequestFactory.create(request);
        } catch (BadRequestException e) {
            JSONObject json = new JSONObject();
            json.put("message", e.getMessage());
            halt(e.getHttpStatus(), json.toString());
        }

        List<PhotonResult> results = requestHandler.lookup(lookupRequest);

        return new GeocodeJsonFormatter(false, lookupRequest.getLanguage()).convert(results, null);
    }
}
