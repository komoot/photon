package de.komoot.photon;

import de.komoot.photon.searcher.PhotonSearcherFactory;
import spark.Request;
import spark.Response;
import spark.Route;

import java.util.Arrays;
import java.util.HashSet;

/**
 * Created by Sachin Dole on 2/12/2015.
 */
public class SearchRequestHandler extends Route {
    private final HashSet<String> supportedLanguages;
    private final PhotonSearcherFactory searcherFactory;

    SearchRequestHandler(String path, String languages) {
        super(path);
        this.searcherFactory = new PhotonSearcherFactory();
        this.supportedLanguages = new HashSet<String>(Arrays.asList(languages.split(",")));
    }

    @Override
    public String handle(Request request, Response response) {
        
        
        return null;
    }
}
