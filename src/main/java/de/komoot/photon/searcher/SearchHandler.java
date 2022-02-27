package de.komoot.photon.searcher;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.QueryBuilder;

public interface SearchHandler {
    SearchResponse search(QueryBuilder queryBuilder, Integer limit);
}
