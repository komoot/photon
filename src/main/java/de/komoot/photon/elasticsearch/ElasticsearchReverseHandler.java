package de.komoot.photon.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch._types.SearchType;
import co.elastic.clients.elasticsearch.core.search.Hit;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.vividsolutions.jts.geom.Point;
import de.komoot.photon.query.ReverseRequest;
import de.komoot.photon.searcher.PhotonResult;
import de.komoot.photon.searcher.ReverseHandler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Execute a reverse lookup on an Elasticsearch database.
 */
public class ElasticsearchReverseHandler implements ReverseHandler {
    private final ElasticsearchClient client;

    public ElasticsearchReverseHandler(ElasticsearchClient client) {
        this.client = client;
    }

    @Override
    public List<PhotonResult> reverse(ReverseRequest photonRequest) throws IOException {
        ReverseQueryBuilder queryBuilder = buildQuery(photonRequest);
        SearchResponse<ObjectNode> results = search(
                queryBuilder.buildQuery(),
                photonRequest.getLimit(),
                photonRequest.getLocation(),
                photonRequest.getLocationDistanceSort()
        );

        List<PhotonResult> ret = new ArrayList<>();

        for (Hit<ObjectNode> hit : results.hits().hits()) {
            ret.add(new ElasticResult(hit));
        }

        return ret;
    }

    public String dumpQuery(ReverseRequest photonRequest) {
        return buildQuery(photonRequest).buildQuery().toString();
    }


    private SearchResponse<ObjectNode> search(Query query, Integer limit, Point location, Boolean locationDistanceSort) throws IOException {
        SearchRequest.Builder builder = new SearchRequest.Builder()
            .index(PhotonIndex.NAME)
            .searchType(SearchType.QueryThenFetch)
            .query(query)
            .size(limit)
            .timeout(String.format("%ss", 7));

        if (locationDistanceSort) {
            builder.sort(sortOptionsBuilder -> sortOptionsBuilder
                .geoDistance(geoDistanceSort -> geoDistanceSort
                    .field("coordinate")
                    .location(loc -> loc
                        .latlon(l -> l
                            .lat(location.getY())
                            .lon(location.getX())
                        ))
                    .order(SortOrder.Asc)
                )
            );
        }

        return client.search(builder.build(), ObjectNode.class);
    }

    private ReverseQueryBuilder buildQuery(ReverseRequest photonRequest) {
        return ReverseQueryBuilder
                .builder(
                        photonRequest.getLocation(),
                        photonRequest.getRadius(),
                        photonRequest.getQueryStringFilter(),
                        photonRequest.getLayerFilters()
                )
                .withOsmTagFilters(photonRequest.getOsmTagFilters());
    }
}
