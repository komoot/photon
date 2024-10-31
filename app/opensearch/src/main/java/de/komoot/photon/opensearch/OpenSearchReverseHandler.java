package de.komoot.photon.opensearch;

import de.komoot.photon.query.ReverseRequest;
import de.komoot.photon.searcher.PhotonResult;
import de.komoot.photon.searcher.ReverseHandler;
import org.locationtech.jts.geom.Point;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.SearchType;
import org.opensearch.client.opensearch._types.SortOrder;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch.core.SearchResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class OpenSearchReverseHandler implements ReverseHandler {
    final private OpenSearchClient client;
    final private String queryTimeout;

    public OpenSearchReverseHandler(OpenSearchClient client, int queryTimeoutSec) {
        this.client = client;
        queryTimeout = queryTimeoutSec + "s";
    }

    @Override
    public List<PhotonResult> reverse(ReverseRequest request) {
        final var queryBuilder = new ReverseQueryBuilder(request.getLocation(), request.getRadius(), request.getQueryStringFilter(), request.getLayerFilters())
                .withOsmTagFilters(request.getOsmTagFilters());


        final var results = search(queryBuilder.buildQuery(),
                request.getLimit(),
                request.getLocationDistanceSort() ? request.getLocation() : null);

        final List<PhotonResult> ret = new ArrayList<>();
        for (var hit : results.hits().hits()) {
            ret.add(hit.source());
        }

        return ret;
    }

    @Override
    public String dumpQuery(ReverseRequest photonRequest) {
        return "{}";
    }

    private SearchResponse<OpenSearchResult> search(Query query, int limit, Point location) {
        try {
            return client.search(s -> {
                s.index(PhotonIndex.NAME)
                        .searchType(SearchType.QueryThenFetch)
                        .query(query)
                        .size(limit)
                        .timeout(queryTimeout);

                if (location != null) {
                    s.sort(sq -> sq
                            .geoDistance(gd -> gd
                                    .field("coordinate")
                                    .location(l -> l.latlon(ll -> ll.lat(location.getY()).lon(location.getX())))
                                    .order(SortOrder.Asc)));
                }
                return s;
            }, OpenSearchResult.class);
        } catch (IOException e) {
            throw new RuntimeException("IO error during search", e);
        }
    }

}
