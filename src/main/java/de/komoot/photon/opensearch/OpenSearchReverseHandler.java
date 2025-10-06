package de.komoot.photon.opensearch;

import de.komoot.photon.query.ReverseRequest;
import de.komoot.photon.searcher.PhotonResult;
import de.komoot.photon.searcher.SearchHandler;
import org.locationtech.jts.geom.Point;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.SearchType;
import org.opensearch.client.opensearch._types.SortOrder;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.opensearch.core.search.Hit;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class OpenSearchReverseHandler implements SearchHandler<ReverseRequest> {
    private final OpenSearchClient client;
    private final String queryTimeout;

    public OpenSearchReverseHandler(OpenSearchClient client, int queryTimeoutSec) {
        this.client = client;
        queryTimeout = queryTimeoutSec + "s";
    }

    @Override
    public List<PhotonResult> search(ReverseRequest request) {
        final var queryBuilder = new ReverseQueryBuilder(request.getLocation(), request.getRadius());
        queryBuilder.addQueryFilter(request.getQueryStringFilter());
        queryBuilder.addLayerFilter(request.getLayerFilters());
        queryBuilder.addOsmTagFilter(request.getOsmTagFilters());

        final var results = search(queryBuilder.build(),
                request.getLimit(),
                request.getLocationDistanceSort() ? request.getLocation() : null);

        return results.hits().hits().stream()
                .map(Hit::source)
                .collect(Collectors.toList());
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
