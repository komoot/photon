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
    public List<JSONObject> handle(R photonRequest) {
        TagFilterQueryBuilder queryBuilder = buildQuery(photonRequest);
        // for the case of deduplication we need a bit more results, #300
        int limit = photonRequest.getLimit();
        int extLimit = limit > 1 ? (int) Math.round(photonRequest.getLimit() * 1.5) : 1;
        SearchResponse results = elasticsearchSearcher.search(queryBuilder.buildQuery(), extLimit);
//        if (results.getHits().getTotalHits() == 0) {
//            results = elasticsearchSearcher.search(queryBuilder.withLenientMatch().buildQuery(), extLimit);
//        }
        int queryTimes = 1;
        while(results.getHits().getTotalHits() == 0 && queryTimes < 5) {
          queryTimes++;
          switch(queryTimes) {
            case 2: 
              results = elasticsearchSearcher.search(queryBuilder.withLenientMatch().buildQuery(), extLimit);
              break;
            case 3:
              results = elasticsearchSearcher.search(queryBuilder.buildQuerySecondRound(1), extLimit);
              break;
            case 4:
              results = elasticsearchSearcher.search(queryBuilder.withLenientMatchSecondRound().buildQuerySecondRound(2), extLimit);
              break;
            default:
              break;
          }
        }
        
        List<JSONObject> resultJsonObjects = new ConvertToJson(photonRequest.getLanguage()).convert(results);
        StreetDupesRemover streetDupesRemover = new StreetDupesRemover(photonRequest.getLanguage());
        resultJsonObjects = streetDupesRemover.execute(resultJsonObjects);
        if (resultJsonObjects.size() > limit) {
            resultJsonObjects = resultJsonObjects.subList(0, limit);
        }
        return resultJsonObjects;
    }

    @Override
    public String dumpQuery(R photonRequest) {
        return buildQuery(photonRequest).buildQuery().toString();
    }
    
    /**
     * Given a {@link PhotonRequest photon request}, build a {@link TagFilterQueryBuilder photon specific query builder} that can be used in the {@link
     * AbstractPhotonRequestHandler#handle handle} method to execute the search.
     * 
     * @param photonRequest a PhotonRequest instance holding the parsed query
     * @return an instance of a TagFilterQueryBuilder
     */
    abstract TagFilterQueryBuilder buildQuery(R photonRequest);
}
