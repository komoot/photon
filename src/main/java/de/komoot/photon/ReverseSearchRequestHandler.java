package de.komoot.photon;

import de.komoot.photon.query.BadRequestException;
import de.komoot.photon.query.ReverseRequest;
import de.komoot.photon.query.ReverseRequestFactory;
import de.komoot.photon.searcher.GeocodeJsonFormatter;
import de.komoot.photon.searcher.PhotonResult;
import de.komoot.photon.searcher.ReverseHandler;
import org.json.JSONObject;
import spark.Request;
import spark.Response;
import spark.RouteImpl;

import java.util.Arrays;
import java.util.List;

import static spark.Spark.halt;

/**
 * Webserver route for reverse geocoding requests.
 */
public class ReverseSearchRequestHandler extends RouteImpl {
    private final ReverseRequestFactory reverseRequestFactory;
    private final ReverseHandler requestHandler;
    private final boolean useGeometryColumn;

    ReverseSearchRequestHandler(String path, ReverseHandler dbHandler, String[] languages, String defaultLanguage, int maxResults, boolean useGeometryColumn) {
        super(path);
        this.useGeometryColumn = useGeometryColumn;
        List<String> supportedLanguages = Arrays.asList(languages);
        this.reverseRequestFactory = new ReverseRequestFactory(supportedLanguages, defaultLanguage, maxResults, useGeometryColumn);
        this.requestHandler = dbHandler;
    }

    @Override
    public String handle(Request request, Response response) {
        ReverseRequest photonRequest = null;
        try {
            photonRequest = reverseRequestFactory.create(request);
        } catch (BadRequestException e) {
            JSONObject json = new JSONObject();
            json.put("message", e.getMessage());
            throw halt(e.getHttpStatus(), json.toString());
        }

        List<PhotonResult> results = requestHandler.reverse(photonRequest);

        // Restrict to the requested limit.
        if (results.size() > photonRequest.getLimit()) {
            results = results.subList(0, photonRequest.getLimit());
        }

        String debugInfo = null;
        if (photonRequest.getDebug()) {
            debugInfo = requestHandler.dumpQuery(photonRequest);
        }

        return new GeocodeJsonFormatter(false, photonRequest.getLanguage(), useGeometryColumn).convert(results, debugInfo);
    }
}
