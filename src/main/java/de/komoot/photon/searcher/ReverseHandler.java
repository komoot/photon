package de.komoot.photon.searcher;

import com.vividsolutions.jts.geom.Point;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.QueryBuilder;

public interface ReverseHandler {
    SearchResponse search(QueryBuilder queryBuilder, Integer limit, Point location,
                          Boolean locationDistanceSort);
}
