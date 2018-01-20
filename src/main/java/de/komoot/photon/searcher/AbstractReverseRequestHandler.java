package de.komoot.photon.searcher;

import de.komoot.photon.query.PhotonRequest;
import de.komoot.photon.query.ReverseRequest;
import de.komoot.photon.query.TagFilterQueryBuilder;
import de.komoot.photon.utils.ConvertToJson;
import org.elasticsearch.action.search.SearchResponse;
import org.json.JSONObject;

import java.util.List;

/**
 * @author svantulden
 */
public abstract class AbstractReverseRequestHandler<R extends ReverseRequest> implements ReverseRequestHandler<R> {
    private final ElasticsearchReverseSearcher elasticsearchSearcher;

    public AbstractReverseRequestHandler(ElasticsearchReverseSearcher elasticsearchSearcher) {
        this.elasticsearchSearcher = elasticsearchSearcher;
    }

    @Override
    public List<JSONObject> handle(R photonRequest) {
        TagFilterQueryBuilder queryBuilder = buildQuery(photonRequest);
        SearchResponse results = elasticsearchSearcher.search(queryBuilder.buildQuery(), photonRequest.getLimit(), photonRequest.getLocation());
        List<JSONObject> resultJsonObjects = new ConvertToJson(photonRequest.getLanguage()).convert(results);
        if (resultJsonObjects.size() > photonRequest.getLimit()) {
            resultJsonObjects = resultJsonObjects.subList(0, photonRequest.getLimit());
        }
        return resultJsonObjects;
    }

    /**
     * Given a {@link PhotonRequest photon request}, build a {@link TagFilterQueryBuilder photon specific query builder} that can be used in the {@link
     * AbstractPhotonRequestHandler#handle handle} method to execute the search.
     */
    abstract TagFilterQueryBuilder buildQuery(R photonRequest);
}
