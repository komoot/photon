package de.komoot.photon.searcher;

import org.locationtech.jts.geom.Point;
import de.komoot.photon.elasticsearch.PhotonIndex;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.geo.GeoPoint;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;

/**
 * @author svantulden
 */
public class ReverseElasticsearchSearcher {
    private Client client;

    public ReverseElasticsearchSearcher(Client client) {
        this.client = client;
    }

    public SearchResponse search(QueryBuilder queryBuilder, Integer limit, Point location,
                                 Boolean locationDistanceSort) {
        TimeValue timeout = TimeValue.timeValueSeconds(7);

        SearchRequestBuilder builder = client.prepareSearch(PhotonIndex.NAME).setSearchType(SearchType.QUERY_THEN_FETCH)
                .setQuery(queryBuilder).setSize(limit).setTimeout(timeout);

        if (locationDistanceSort)
            builder.addSort(SortBuilders.geoDistanceSort("coordinate", new GeoPoint(location.getY(), location.getX()))
                    .order(SortOrder.ASC));

        return builder.execute().actionGet();
    }
}
