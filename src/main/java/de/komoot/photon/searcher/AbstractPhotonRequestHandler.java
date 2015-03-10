package de.komoot.photon.searcher;

import de.komoot.photon.query.PhotonRequest;
import de.komoot.photon.query.TagFilterQueryBuilder;
import de.komoot.photon.utils.ConvertToJson;
import org.elasticsearch.action.search.SearchResponse;
import org.json.JSONObject;

import java.util.List;

/**
 * Created by Sachin Dole on 2/20/2015.
 */
public abstract class AbstractPhotonRequestHandler<R extends PhotonRequest> implements PhotonRequestHandler<R> {

    private final ElasticsearchSearcher elasticsearchSearcher;

    public AbstractPhotonRequestHandler(ElasticsearchSearcher elasticsearchSearcher) {
        this.elasticsearchSearcher = elasticsearchSearcher;
    }        

    @Override
    public final List<JSONObject> handle(R photonRequest) {
        TagFilterQueryBuilder queryBuilder = buildQuery(photonRequest);
        Integer limit = photonRequest.getLimit();
        SearchResponse results = elasticsearchSearcher.search(queryBuilder.buildQuery(), limit);
        if (results.getHits().getTotalHits() == 0) {
            results = elasticsearchSearcher.search(queryBuilder.withLenientMatch().buildQuery(), limit);
        }
        List<JSONObject> resultJsonObjects = new ConvertToJson(photonRequest.getLanguage()).convert(results);
        StreetDupesRemover streetDupesRemover = new StreetDupesRemover(photonRequest.getLanguage());
        resultJsonObjects = streetDupesRemover.execute(resultJsonObjects);
        if (resultJsonObjects.size() > limit) {
            resultJsonObjects = resultJsonObjects.subList(0, limit);
        }
        return resultJsonObjects;
    }

    /**
     * Given a {@link PhotonRequest photon request}, build a {@link TagFilterQueryBuilder photon specific query builder} that can be used in the {@link
     * AbstractPhotonRequestHandler#handle handle} method to execute the search.
     */
    abstract TagFilterQueryBuilder buildQuery(R photonRequest);
}
