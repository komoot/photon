package de.komoot.photon.searcher;

import de.komoot.photon.query.FilteredPhotonRequest;
import de.komoot.photon.query.PhotonRequest;

/**
 * Created by Sachin Dole on 2/20/2015.
 */
public class PhotonRequestHandlerFactory {

    private final ElasticsearchSearcher elasticsearchSearcher;

    public PhotonRequestHandlerFactory(ElasticsearchSearcher elasticsearchSearcher) {
        this.elasticsearchSearcher = elasticsearchSearcher;
    }

    /**
     * Given a {@link PhotonRequest} create a
     * {@link PhotonRequestHandler handler} that can execute the elastic search
     * search.
     */
    public <R extends PhotonRequest> PhotonRequestHandler<R> createHandler(R request) {
        if (request instanceof FilteredPhotonRequest) {
            return (PhotonRequestHandler<R>) new FilteredPhotonRequestHandler(elasticsearchSearcher);
        } else {
            return (PhotonRequestHandler<R>) new SimplePhotonRequestHandler(elasticsearchSearcher);
        }

    }
}
