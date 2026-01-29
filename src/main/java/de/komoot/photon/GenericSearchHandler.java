package de.komoot.photon;

import de.komoot.photon.query.RequestBase;
import de.komoot.photon.query.RequestFactory;
import de.komoot.photon.searcher.ResultFormatter;
import de.komoot.photon.searcher.SearchHandler;
import de.komoot.photon.searcher.StreetDupesRemover;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import org.jspecify.annotations.NullMarked;

import java.io.IOException;

@NullMarked
public class GenericSearchHandler<T extends RequestBase> implements Handler {
    private final RequestFactory<T> requestFactory;
    private final SearchHandler<T> requestHandler;
    private final ResultFormatter formatter;

    public GenericSearchHandler(RequestFactory<T> requestFactory, SearchHandler<T> requestHandler,
                                ResultFormatter formatter) {
        this.requestFactory = requestFactory;
        this.requestHandler = requestHandler;
        this.formatter = formatter;
    }

    @Override
    public void handle(Context context) {
        final T searchRequest = requestFactory.create(context);

        var results = requestHandler.search(searchRequest);

        // Further filtering
        if (searchRequest.getDedupe()){
            results = new StreetDupesRemover(searchRequest.getLanguage()).execute(results);
        }

        // Restrict to the requested limit.
        if (results.size() > searchRequest.getLimit()) {
            results = results.subList(0, searchRequest.getLimit());
        }

        String debugInfo = null;
        if (searchRequest.getDebug()) {
            debugInfo = requestHandler.dumpQuery(searchRequest);
        }

        try {
            context.status(200)
                    .result(formatter.convert(
                            results, searchRequest.getLanguage(),
                            searchRequest.getReturnGeometry(),
                            searchRequest.getDebug(), debugInfo));
        } catch (IOException e) {
            context.status(400)
                    .result("{\"message\": \"Error creating json.\"}");
        }
    }
}
