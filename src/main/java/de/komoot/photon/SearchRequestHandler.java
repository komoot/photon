package de.komoot.photon;

import de.komoot.photon.query.BadRequestException;
import de.komoot.photon.query.PhotonRequest;
import de.komoot.photon.query.PhotonRequestFactory;
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
    private final PhotonRequestFactory photonRequestFactory;
    private final PhotonRequestHandler<PhotonRequest> photonRequestHandler;

    SearchRequestHandler(String path, String languages) {
        super(path);
        this.photonRequestHandler = new SimplePhotonRequestHandler();
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
        return photonRequestHandler.handle(photonRequest);
    }
}
