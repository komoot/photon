package de.komoot.photon.searcher;

import de.komoot.photon.App;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilder;

/**
 * Created by Sachin Dole on 2/12/2015.
 */
public class BasePhotonSearcher implements PhotonSearcher {
    @Override
    public SearchResponse search(QueryBuilder queryBuilder, Integer limit) {
        return null;
    }

    @Override
    public SearchResponse searchStrict(QueryBuilder queryBuilder, Integer limit) {
        SearchResponse response = App.getClient().prepareSearch("photon").setSearchType(SearchType.QUERY_AND_FETCH).setQuery(queryBuilder).setSize(limit).setTimeout(TimeValue
                                                                                                                                                                  .timeValueSeconds(7))
                                   .execute().actionGet();
return response;
    }
}
