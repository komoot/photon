package de.komoot.photon;

import de.komoot.photon.query.BadRequestException;
import de.komoot.photon.query.ReverseRequest;
import de.komoot.photon.query.ReverseRequestFactory;
import de.komoot.photon.searcher.GeocodeJsonFormatter;
import de.komoot.photon.searcher.PhotonResult;
import de.komoot.photon.searcher.ResultFormatter;
import de.komoot.photon.searcher.ReverseHandler;
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
    private final ResultFormatter formatter = new GeocodeJsonFormatter();

    ReverseSearchRequestHandler(String path, ReverseHandler dbHandler, String[] languages, String defaultLanguage, int maxResults) {
        super(path);
        List<String> supportedLanguages = Arrays.asList(languages);
        this.reverseRequestFactory = new ReverseRequestFactory(supportedLanguages, defaultLanguage, maxResults);
        this.requestHandler = dbHandler;
    }

    @Override
    public String handle(Request request, Response response) {
        ReverseRequest photonRequest;
        try {
            photonRequest = reverseRequestFactory.create(request);
        } catch (BadRequestException e) {
            throw halt(e.getHttpStatus(), formatter.formatError(e.getMessage()));
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

        return formatter.convert(
                results, photonRequest.getLanguage(), photonRequest.getGeometry(),
                photonRequest.getDebug(), debugInfo);
    }
}
