package de.komoot.photon.searcher;

import de.komoot.photon.query.ReverseQueryBuilder;
import de.komoot.photon.query.ReverseRequest;
import de.komoot.photon.utils.ConvertToJson;
import org.elasticsearch.action.search.SearchResponse;
import org.json.JSONObject;

import java.util.List;

public class ReverseRequestHandler {
    private final ReverseElasticsearchSearcher elasticsearchSearcher;

    public ReverseRequestHandler(ReverseElasticsearchSearcher elasticsearchSearcher) {
        this.elasticsearchSearcher = elasticsearchSearcher;
    }

    public List<JSONObject> handle(ReverseRequest photonRequest) {
        ReverseQueryBuilder queryBuilder = buildQuery(photonRequest);
        SearchResponse results = elasticsearchSearcher.search(queryBuilder.buildQuery(), photonRequest.getLimit(), photonRequest.getLocation(),
                photonRequest.getLocationDistanceSort());
        List<JSONObject> resultJsonObjects = new ConvertToJson(photonRequest.getLanguage()).convert(results, false);
        if (resultJsonObjects.size() > photonRequest.getLimit()) {
            resultJsonObjects = resultJsonObjects.subList(0, photonRequest.getLimit());
        }
        return resultJsonObjects;
    }

    public ReverseQueryBuilder buildQuery(ReverseRequest photonRequest) {
        return ReverseQueryBuilder.builder(photonRequest.getLocation(), photonRequest.getRadius(), photonRequest.getQueryStringFilter());
    }
}
