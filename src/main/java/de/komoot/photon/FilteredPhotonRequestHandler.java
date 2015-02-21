package de.komoot.photon;

import de.komoot.photon.query.FilteredPhotonRequest;
import de.komoot.photon.query.PhotonQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;

/**
 * Created by Sachin Dole on 2/20/2015.
 */
public class FilteredPhotonRequestHandler extends AbstractPhotonRequestHandler<FilteredPhotonRequest> implements PhotonRequestHandler<FilteredPhotonRequest> {

    @Override
    QueryBuilder buildQuery(FilteredPhotonRequest photonRequest) {
        QueryBuilder queryBuilder = PhotonQueryBuilder.builder(photonRequest.getQuery()).
                withTags(photonRequest.tag()).
                                                              withKeys(photonRequest.key()).
                                                              withValues(photonRequest.value()).
                                                              withoutTags(photonRequest.notTag()).
                                                              withoutKeys(photonRequest.notKey()).
                                                              withoutValues(photonRequest.notValue()).
                                                              buildQuery();
        return queryBuilder;

    }

}
