package de.komoot.photon;

import de.komoot.photon.query.PhotonRequest;
import de.komoot.photon.query.TagFilterQueryBuilder;
import de.komoot.photon.searcher.BasePhotonSearcher;
import de.komoot.photon.searcher.PhotonSearcher;
import de.komoot.photon.searcher.PhotonSearcherFactory;
import org.elasticsearch.action.search.SearchResponse;
import org.json.JSONObject;

import java.util.List;

/**
 * Created by Sachin Dole on 2/20/2015.
 */
public abstract class AbstractPhotonRequestHandler<R extends PhotonRequest> implements PhotonRequestHandler<R> {
    private final PhotonSearcherFactory searcherFactory = new PhotonSearcherFactory();

    private PhotonSearcher photonSearcher = new BasePhotonSearcher();

    @Override
    public final List<JSONObject> handle(R photonRequest) {
        TagFilterQueryBuilder queryBuilder = buildQuery(photonRequest);
        System.out.println(queryBuilder.buildQuery().toString());
        Integer limit = photonRequest.getLimit();
        SearchResponse results = photonSearcher.search(queryBuilder.buildQuery(), limit);
        if (results.getHits().getTotalHits() == 0) {
            results = photonSearcher.search(queryBuilder.withStrictMatch().buildQuery(), limit);
        }
        List<JSONObject> resultJsonObjects = new ConvertToJson(photonRequest.getLanguage()).convert(results);
        StreetDupesRemover streetDupesRemover = new StreetDupesRemover(photonRequest.getLanguage());
        resultJsonObjects = streetDupesRemover.execute(resultJsonObjects);
        if(resultJsonObjects.size() > limit) {
            resultJsonObjects = resultJsonObjects.subList(0, limit);
        }
        return resultJsonObjects;
    }

    abstract TagFilterQueryBuilder buildQuery(R photonRequest);
}
