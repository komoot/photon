package de.komoot.photon.elasticsearch;

import com.vividsolutions.jts.geom.Point;
import de.komoot.photon.query.ReverseRequest;
import de.komoot.photon.searcher.ReverseHandler;
import de.komoot.photon.utils.ConvertToJson;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.geo.GeoPoint;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.json.JSONObject;

import java.util.List;

/**
 * @author svantulden
 */
public class ElasticsearchReverseHandler implements ReverseHandler {
    private Client client;

    public ElasticsearchReverseHandler(Client client) {
        this.client = client;
    }

    @Override
    public List<JSONObject> reverse(ReverseRequest photonRequest) {
        ReverseQueryBuilder queryBuilder = buildQuery(photonRequest);
        SearchResponse results = search(queryBuilder.buildQuery(), photonRequest.getLimit(), photonRequest.getLocation(),
                photonRequest.getLocationDistanceSort());
        List<JSONObject> resultJsonObjects = new ConvertToJson(photonRequest.getLanguage()).convert(results, false);
        if (resultJsonObjects.size() > photonRequest.getLimit()) {
            resultJsonObjects = resultJsonObjects.subList(0, photonRequest.getLimit());
        }
        return resultJsonObjects;
    }


    private SearchResponse search(QueryBuilder queryBuilder, Integer limit, Point location,
                                 Boolean locationDistanceSort) {
        TimeValue timeout = TimeValue.timeValueSeconds(7);

        SearchRequestBuilder builder = client.prepareSearch(PhotonIndex.NAME).setSearchType(SearchType.QUERY_THEN_FETCH)
                .setQuery(queryBuilder).setSize(limit).setTimeout(timeout);

        if (locationDistanceSort)
            builder.addSort(SortBuilders.geoDistanceSort("coordinate", new GeoPoint(location.getY(), location.getX()))
                    .order(SortOrder.ASC));

        return builder.execute().actionGet();
    }

    private ReverseQueryBuilder buildQuery(ReverseRequest photonRequest) {
        return ReverseQueryBuilder.builder(photonRequest.getLocation(), photonRequest.getRadius(), photonRequest.getQueryStringFilter());
    }

}
