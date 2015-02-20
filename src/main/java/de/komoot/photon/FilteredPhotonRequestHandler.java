package de.komoot.photon;

import de.komoot.photon.query.FilteredPhotonRequest;
import de.komoot.photon.query.PhotonQueryBuilder;
import de.komoot.photon.searcher.PhotonSearcher;
import de.komoot.photon.searcher.PhotonSearcherFactory;
import org.json.JSONObject;

import java.util.List;

/**
 * Created by Sachin Dole on 2/20/2015.
 */
public class FilteredPhotonRequestHandler implements PhotonRequestHandler<FilteredPhotonRequest> {
    private final PhotonSearcherFactory searcherFactory = new PhotonSearcherFactory();
    @Override
    public String handle(FilteredPhotonRequest photonRequest) {
        PhotonQueryBuilder.builder(photonRequest.getQuery()).
                withTags(photonRequest.tag()).
                withKeys(photonRequest.key()).
                withValues(photonRequest.value()).
                withoutTags(photonRequest.notTag()).
                withoutKeys(photonRequest.notKey()).
                withoutValues(photonRequest.notValue()).
                                  buildQuery();
        PhotonSearcher searcher = searcherFactory.getSearcher(photonRequest);
        List<JSONObject> results = searcher.searchStrict();
        if (results.size() == 0) {
            results = searcher.search();
        }
        return "";

    }
}
