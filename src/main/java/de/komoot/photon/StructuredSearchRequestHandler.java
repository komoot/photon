package de.komoot.photon;

import de.komoot.photon.query.BadRequestException;
import de.komoot.photon.query.PhotonRequestFactory;
import de.komoot.photon.query.StructuredPhotonRequest;
import de.komoot.photon.searcher.*;
import org.json.JSONObject;
import spark.Request;
import spark.Response;
import spark.RouteImpl;

import java.util.Arrays;
import java.util.List;

import static spark.Spark.halt;

public class StructuredSearchRequestHandler extends RouteImpl {
    private final PhotonRequestFactory photonRequestFactory;
    private final StructuredSearchHandler requestHandler;
    private final boolean supportPolygons;

    StructuredSearchRequestHandler(String path, StructuredSearchHandler dbHandler, String[] languages, String defaultLanguage, int maxResults, boolean supportPolygons) {
        super(path);
        List<String> supportedLanguages = Arrays.asList(languages);
        this.photonRequestFactory = new PhotonRequestFactory(supportedLanguages, defaultLanguage, maxResults);
        this.requestHandler = dbHandler;
        this.supportPolygons = supportPolygons;
    }

    @Override
    public String handle(Request request, Response response) {
        StructuredPhotonRequest photonRequest;
        try {
            photonRequest = photonRequestFactory.createStructured(request);
        } catch (BadRequestException e) {
            JSONObject json = new JSONObject();
            json.put("message", e.getMessage());
            throw halt(e.getHttpStatus(), json.toString());
        }

        if (!supportPolygons && (photonRequest.isPolygonRequest() && photonRequest.getReturnPolygon())) {
            JSONObject json = new JSONObject();
            json.put("message", "You're requesting a polygon, but polygons are not imported!");
            throw halt(400, json.toString());
        }

        List<PhotonResult> results = requestHandler.search(photonRequest);

        // Further filtering
        results = new StreetDupesRemover(photonRequest.getLanguage()).execute(results);

        // Restrict to the requested limit.
        if (results.size() > photonRequest.getLimit()) {
            results = results.subList(0, photonRequest.getLimit());
        }


        String debugInfo = null;
     /*   if (photonRequest.getDebug()) {
            debugInfo = requestHandler.dumpQuery(photonRequest);
        }
 */
        return new GeocodeJsonFormatter(photonRequest.getDebug(), photonRequest.getLanguage(), photonRequest.getReturnPolygon()).convert(results, debugInfo);
    }
}