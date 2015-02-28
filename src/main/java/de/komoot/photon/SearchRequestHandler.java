package de.komoot.photon;

import de.komoot.photon.query.BadRequestException;
import de.komoot.photon.query.PhotonRequest;
import de.komoot.photon.query.PhotonRequestFactory;
import org.json.JSONObject;
import spark.Request;
import spark.Response;
import spark.Route;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by Sachin Dole on 2/12/2015.
 */
public class SearchRequestHandler<R extends PhotonRequest> extends Route {
    private final PhotonRequestFactory photonRequestFactory;
    private final PhotonRequestHandlerFactory requestHandlerFactory;
    private final ConvertToGeoJson geoJsonConverter;

    SearchRequestHandler(String path, String languages) {
        super(path);
        Set<String> supportedLanguages = new HashSet<String>(Arrays.asList(languages.split(",")));
        this.photonRequestFactory = new PhotonRequestFactory(supportedLanguages);
        this.geoJsonConverter = new ConvertToGeoJson();
        this.requestHandlerFactory = new PhotonRequestHandlerFactory();
    }

    @Override
    public String handle(Request request, Response response) {
        R photonRequest = null;
        try {
            photonRequest = photonRequestFactory.create(request);
        } catch (BadRequestException e) {
            halt(e.getHttpStatus(), "bad request: " + e.getMessage());
        }
        PhotonRequestHandler<R> handler = requestHandlerFactory.createHandler(photonRequest);
        List<JSONObject> results = handler.handle(photonRequest);
        JSONObject geoJsonResults = geoJsonConverter.convert(results);
        response.type("application/json; charset=utf-8");
        response.header("Access-Control-Allow-Origin", "*");
        if (request.queryParams("debug") != null)
            return geoJsonResults.toString(4);

        return geoJsonResults.toString();
    }
}
