package de.komoot.photon.searcher;

import de.komoot.photon.App;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilder;

/**
 * Created by Sachin Dole on 2/12/2015.
 */
public class BasePhotonSearcher implements PhotonSearcher {
    @Override
    public SearchResponse search(QueryBuilder queryBuilder, Integer limit) {
        TimeValue timeout = TimeValue.timeValueSeconds(7);
        Client client = App.getClient();
        return client.prepareSearch("photon").
                setSearchType(SearchType.QUERY_AND_FETCH).
                             setQuery(queryBuilder).
                             setSize(limit).
                             setTimeout(timeout).
                             execute().
                             actionGet();

    }

}
