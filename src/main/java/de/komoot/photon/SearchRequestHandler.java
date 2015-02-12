package de.komoot.photon;

import de.komoot.photon.query.BadRequestException;
import de.komoot.photon.query.PhotonRequest;
import de.komoot.photon.query.PhotonRequestFactory;
import de.komoot.photon.searcher.PhotonSearcher;
import de.komoot.photon.searcher.PhotonSearcherFactory;
import org.json.JSONObject;
import spark.Request;
import spark.Response;
import spark.Route;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

/**
 * Created by Sachin Dole on 2/12/2015.
 */
public class SearchRequestHandler extends Route {
    private final HashSet<String> supportedLanguages;
    private final PhotonSearcherFactory searcherFactory;
    private final PhotonRequestFactory photonRequestFactory;

    SearchRequestHandler(String path, String languages) {
        super(path);
        this.searcherFactory = new PhotonSearcherFactory();
        this.supportedLanguages = new HashSet<String>(Arrays.asList(languages.split(",")));
        this.photonRequestFactory = new PhotonRequestFactory();
    }

    @Override
    public String handle(Request request, Response response) {
        PhotonRequest photonRequest;
        try {
            photonRequest = photonRequestFactory.create(request);
        } catch (BadRequestException e) {
            response.status(400);
            return "bad request: " + e.getMessage();
        }

        PhotonSearcher searcher = searcherFactory.getSearcher(photonRequest);
        List<JSONObject> results = searcher.search(photonRequest, true);
        if (results.size()==0){
            results = searcher.search(photonRequest, false);
        }
        return "";
    }
}
