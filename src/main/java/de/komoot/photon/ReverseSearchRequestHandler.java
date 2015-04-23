package de.komoot.photon;

import de.komoot.photon.query.BadRequestException;
import de.komoot.photon.query.ReverseRequest;
import de.komoot.photon.query.ReverseRequestFactory;
import de.komoot.photon.searcher.BaseElasticsearchSearcher;
import de.komoot.photon.searcher.ReverseElasticsearchSearcher;
import de.komoot.photon.searcher.ReverseRequestHandler;
import de.komoot.photon.searcher.ReverseRequestHandlerFactory;
import de.komoot.photon.utils.ConvertToGeoJson;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.elasticsearch.client.Client;
import org.json.JSONObject;
import spark.Request;
import spark.Response;
import spark.Route;

/**
 *
 * @author svantulden
 */
public class ReverseSearchRequestHandler <R extends ReverseRequest> extends Route {
    private final ReverseRequestFactory reverseRequestFactory;
    private final ReverseRequestHandlerFactory requestHandlerFactory;
    private final ConvertToGeoJson geoJsonConverter;

    ReverseSearchRequestHandler(String path, Client esNodeClient, String languages) {
        super(path);
        Set<String> supportedLanguages = new HashSet<String>(Arrays.asList(languages.split(",")));
        this.reverseRequestFactory = new ReverseRequestFactory(supportedLanguages);
        this.geoJsonConverter = new ConvertToGeoJson();
        this.requestHandlerFactory = new ReverseRequestHandlerFactory(new ReverseElasticsearchSearcher(esNodeClient));
    }

    @Override
    public String handle(Request request, Response response) {
        R photonRequest = null;
        try {
            photonRequest = reverseRequestFactory.create(request);
        } catch (BadRequestException e) {
            halt(e.getHttpStatus(), "bad request: " + e.getMessage());
        }
        ReverseRequestHandler<R> handler = requestHandlerFactory.createHandler(photonRequest);
        List<JSONObject> results = handler.handle(photonRequest);
        if(results.size() > 1) {
            results = results.subList(0, 1);
        }
        JSONObject geoJsonResults = geoJsonConverter.convert(results);
        response.type("application/json; charset=utf-8");
        response.header("Access-Control-Allow-Origin", "*");
        if (request.queryParams("debug") != null)
            return geoJsonResults.toString(4);

        return geoJsonResults.toString();
    }
}
