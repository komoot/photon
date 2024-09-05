package de.komoot.photon;

import de.komoot.photon.query.BadRequestException;
import de.komoot.photon.query.PhotonRequest;
import de.komoot.photon.query.PhotonRequestFactory;
import de.komoot.photon.searcher.*;
import org.json.JSONObject;
import spark.Request;
import spark.Response;
import spark.RouteImpl;

import java.util.Arrays;
import java.util.List;

import static spark.Spark.halt;

/**
 * Webserver route for forward geocoding requests.
 */
public class SearchRequestHandler extends RouteImpl {
    private final PhotonRequestFactory photonRequestFactory;
    private final SearchHandler requestHandler;
    private final int maxResults;
    private final boolean supportPolygons;

    SearchRequestHandler(String path, SearchHandler dbHandler, String[] languages, String defaultLanguage, int maxResults, boolean supportPolygons) {
        super(path);
        List<String> supportedLanguages = Arrays.asList(languages);
        this.photonRequestFactory = new PhotonRequestFactory(supportedLanguages, defaultLanguage, maxResults);
        this.requestHandler = dbHandler;
        this.maxResults = maxResults;
        this.supportPolygons = supportPolygons;
    }

    @Override
    public String handle(Request request, Response response) throws BadRequestException {
        PhotonRequest photonRequest = null;
        try {
            photonRequest = photonRequestFactory.create(request);
        } catch (BadRequestException e) {
            JSONObject json = new JSONObject();
            json.put("message", e.getMessage());
            throw halt(e.getHttpStatus(), json.toString());
        }

        List<PhotonResult> results = requestHandler.search(photonRequest);

        // Further filtering
        results = new StreetDupesRemover(photonRequest.getLanguage()).execute(results);

        // Restrict to the requested limit.
        if (results.size() > photonRequest.getLimit()) {
            results = results.subList(0, photonRequest.getLimit());
        }

        String debugInfo = null;
        if (photonRequest.getDebug()) {
            debugInfo = requestHandler.dumpQuery(photonRequest);
        }

        boolean returnPolygon = supportPolygons;
        if (photonRequest.isPolygonRequest()) {
            returnPolygon = photonRequest.getReturnPolygon();
        }

        if (!supportPolygons && (photonRequest.isPolygonRequest() && photonRequest.getReturnPolygon())) {
            throw new BadRequestException(400, "You're requesting a polygon, but polygons are not imported!");
        }

        return new GeocodeJsonFormatter(photonRequest.getDebug(), photonRequest.getLanguage(), returnPolygon).convert(results, debugInfo);
    }
}