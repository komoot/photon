package de.komoot.photon;

import de.komoot.photon.query.PhotonQueryBuilder;
import de.komoot.photon.query.PhotonRequest;
import de.komoot.photon.searcher.PhotonSearcherFactory;
import org.elasticsearch.index.query.QueryBuilder;

/**
 * Created by Sachin Dole on 2/12/2015.
 */
public class SimplePhotonRequestHandler extends AbstractPhotonRequestHandler<PhotonRequest> implements PhotonRequestHandler<PhotonRequest> {

    private final PhotonSearcherFactory searcherFactory = new PhotonSearcherFactory();

    @Override
    QueryBuilder buildQuery(PhotonRequest photonRequest) {
        return PhotonQueryBuilder.builder(photonRequest.getQuery()).buildQuery();
    }
}
