package de.komoot.photon;

import de.komoot.photon.query.PhotonRequest;
import de.komoot.photon.searcher.PhotonSearcher;
import de.komoot.photon.searcher.PhotonSearcherFactory;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.QueryBuilder;
import org.json.JSONObject;

import java.util.List;

/**
 * Created by Sachin Dole on 2/20/2015.
 */
public abstract class AbstractPhotonRequestHandler<R extends PhotonRequest> implements PhotonRequestHandler<R> {
    private final PhotonSearcherFactory searcherFactory = new PhotonSearcherFactory();

    @Override
    public final List<JSONObject> handle(R photonRequest) {
        QueryBuilder queryBuilder = buildQuery(photonRequest);
        PhotonSearcher searcher = searcherFactory.getSearcher(photonRequest);
        SearchResponse results = searcher.searchStrict(queryBuilder,photonRequest.getLimit());
        if (results.getHits().getTotalHits() == 0) {
            results = searcher.search(queryBuilder,photonRequest.getLimit());
        }
        List<JSONObject> resultJsonObjects = new ConvertToJson(photonRequest.getLanguage()).convert(results);
        List<JSONObject> filteredResultsJsonObjects = new StreetDupesRemover(photonRequest.getLanguage()).execute(resultJsonObjects);
        return filteredResultsJsonObjects;
    }

    abstract QueryBuilder buildQuery(R photonRequest) ;
}
