package de.komoot.photon.searcher;

import com.vividsolutions.jts.geom.Point;
import de.komoot.photon.query.PhotonQueryBuilder;
import de.komoot.photon.query.PhotonRequest;
import de.komoot.photon.query.TagFilterQueryBuilder;

/**
 * Created by Sachin Dole on 2/12/2015.
 */
public class SimplePhotonRequestHandler extends AbstractPhotonRequestHandler<PhotonRequest> implements PhotonRequestHandler<PhotonRequest> {
    public SimplePhotonRequestHandler(ElasticsearchSearcher elasticsearchSearcher) {
        super(elasticsearchSearcher);
    }

    @Override
    public TagFilterQueryBuilder buildQuery(PhotonRequest photonRequest) {
        Point point = photonRequest.getLocationForBias();

        return PhotonQueryBuilder.builder(photonRequest.getQuery(), photonRequest.getLanguage()).withLocationBias(point, photonRequest.getRadiusForBias());
    }
}
