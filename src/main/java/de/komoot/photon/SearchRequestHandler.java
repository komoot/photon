package de.komoot.photon;

import de.komoot.photon.query.BadRequestException;
import de.komoot.photon.query.SimpleSearchRequest;
import de.komoot.photon.query.SimpleSearchRequestFactory;
import de.komoot.photon.searcher.*;
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
    private final SimpleSearchRequestFactory photonRequestFactory;
    private final SearchHandler requestHandler;
    private final ResultFormatter formatter = new GeocodeJsonFormatter();
    private final boolean supportGeometries;

    SearchRequestHandler(String path, SearchHandler dbHandler, String[] languages, String defaultLanguage, int maxResults, boolean supportGeometries) {
        super(path);
        List<String> supportedLanguages = Arrays.asList(languages);
        this.photonRequestFactory = new SimpleSearchRequestFactory(supportedLanguages, defaultLanguage, maxResults, supportGeometries);
        this.requestHandler = dbHandler;
        this.supportGeometries = supportGeometries;
    }

    @Override
    public String handle(Request request, Response response) {
        SimpleSearchRequest simpleSearchRequest;
        try {
            simpleSearchRequest = photonRequestFactory.create(request);
        } catch (BadRequestException e) {
            throw halt(e.getHttpStatus(), formatter.formatError(e.getMessage()));
        }

        if (!supportGeometries && simpleSearchRequest.getReturnGeometry()) {
            throw halt(400, formatter.formatError("You're explicitly requesting a geometry, but geometries are not imported!"));
        }

        List<PhotonResult> results = requestHandler.search(simpleSearchRequest);

        // Further filtering
        results = new StreetDupesRemover(simpleSearchRequest.getLanguage()).execute(results);

        // Restrict to the requested limit.
        if (results.size() > simpleSearchRequest.getLimit()) {
            results = results.subList(0, simpleSearchRequest.getLimit());
        }

        String debugInfo = null;
        if (simpleSearchRequest.getDebug()) {
            debugInfo = requestHandler.dumpQuery(simpleSearchRequest);
        }

        return formatter.convert(
                results, simpleSearchRequest.getLanguage(), simpleSearchRequest.getReturnGeometry(),
                simpleSearchRequest.getDebug(), debugInfo);
    }
}