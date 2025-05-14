package de.komoot.photon;

import de.komoot.photon.query.BadRequestException;
import de.komoot.photon.query.StructuredSearchRequest;
import de.komoot.photon.query.StructuredSearchRequestFactory;
import de.komoot.photon.searcher.*;
import spark.Request;
import spark.Response;
import spark.RouteImpl;

import java.util.Arrays;
import java.util.List;

import static spark.Spark.halt;

public class StructuredSearchRequestHandler extends RouteImpl {
    private final StructuredSearchRequestFactory photonRequestFactory;
    private final StructuredSearchHandler requestHandler;
    private final ResultFormatter formatter = new GeocodeJsonFormatter();
    private final boolean supportGeometries;

    StructuredSearchRequestHandler(String path, StructuredSearchHandler dbHandler, String[] languages, String defaultLanguage, int maxResults, boolean supportGeometries) {
        super(path);
        List<String> supportedLanguages = Arrays.asList(languages);
        this.photonRequestFactory = new StructuredSearchRequestFactory(supportedLanguages, defaultLanguage, maxResults, supportGeometries);
        this.requestHandler = dbHandler;
        this.supportGeometries = supportGeometries;
    }

    @Override
    public String handle(Request request, Response response) {
        StructuredSearchRequest photonRequest;
        try {
            photonRequest = photonRequestFactory.create(request);
        } catch (BadRequestException e) {
            throw halt(e.getHttpStatus(), formatter.formatError(e.getMessage()));
        }

        if (!supportGeometries && photonRequest.getReturnGeometry()) {
            throw halt(400, formatter.formatError("You're requesting a Geometry, but Geometries are not imported!"));
        }

        List<PhotonResult> results = requestHandler.search(photonRequest);

        // Further filtering
        results = new StreetDupesRemover(photonRequest.getLanguage()).execute(results);

        // Restrict to the requested limit.
        if (results.size() > photonRequest.getLimit()) {
            results = results.subList(0, photonRequest.getLimit());
        }

        return formatter.convert(
                results, photonRequest.getLanguage(), photonRequest.getReturnGeometry(),
                photonRequest.getDebug(), null);
    }
}