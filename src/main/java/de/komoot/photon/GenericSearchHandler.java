package de.komoot.photon;

import de.komoot.photon.query.BadRequestException;
import de.komoot.photon.query.RequestBase;
import de.komoot.photon.query.RequestFactory;
import de.komoot.photon.searcher.GeocodeJsonFormatter;
import de.komoot.photon.searcher.ResultFormatter;
import de.komoot.photon.searcher.SearchHandler;
import de.komoot.photon.searcher.StreetDupesRemover;
import spark.Request;
import spark.Response;
import spark.RouteImpl;
import spark.Spark;

import java.io.IOException;

import static spark.Spark.halt;

public class GenericSearchHandler<T extends RequestBase> extends RouteImpl {
    private final RequestFactory<T> requestFactory;
    private final SearchHandler<T> requestHandler;
    private final ResultFormatter formatter = new GeocodeJsonFormatter();
    private final boolean supportGeometries;

    public GenericSearchHandler(String path, RequestFactory<T> requestFactory, SearchHandler<T> requestHandler, boolean supportGeometries) {
        super(path);
        this.requestFactory = requestFactory;
        this.requestHandler = requestHandler;
        this.supportGeometries = supportGeometries;
    }

    @Override
    public String handle(Request request, Response response) {
        T searchRequest;
        try {
            searchRequest = requestFactory.create(request);
        } catch (BadRequestException e) {
            throw halt(e.getHttpStatus(), formatter.formatError(e.getMessage()));
        }

        if (!supportGeometries && searchRequest.getReturnGeometry()) {
            throw halt(400, formatter.formatError("You're explicitly requesting a geometry, but geometries are not imported!"));
        }

        var results = requestHandler.search(searchRequest);

        // Further filtering
        results = new StreetDupesRemover(searchRequest.getLanguage()).execute(results);

        // Restrict to the requested limit.
        if (results.size() > searchRequest.getLimit()) {
            results = results.subList(0, searchRequest.getLimit());
        }

        String debugInfo = null;
        if (searchRequest.getDebug()) {
            debugInfo = requestHandler.dumpQuery(searchRequest);
        }

        try {
            return formatter.convert(
                    results, searchRequest.getLanguage(), searchRequest.getReturnGeometry(),
                    searchRequest.getDebug(), debugInfo);
        } catch (IOException e) {
            throw Spark.halt(400, "{\"message\": \"Error creating json.\"}");
        }
    }
}
