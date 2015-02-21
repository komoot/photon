package de.komoot.photon.searcher;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.QueryBuilder;

/**
 * Created by sachi_000 on 2/12/2015.
 */
public interface PhotonSearcher {
    SearchResponse search(QueryBuilder queryBuilder, Integer limit);

    SearchResponse searchStrict(QueryBuilder queryBuilder, Integer limit);
}
