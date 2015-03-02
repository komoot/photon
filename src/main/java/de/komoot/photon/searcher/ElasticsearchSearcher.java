package de.komoot.photon.searcher;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.QueryBuilder;

/**
 * This class handles all search execution that is specific to elastic search.
 * <p/>
 * Created by Sachin Dole on 2/12/2015.
 */
public interface ElasticsearchSearcher {
    SearchResponse search(QueryBuilder queryBuilder, Integer limit);

}
