package de.komoot.photon.opensearch;

import de.komoot.photon.query.SimpleSearchRequest;
import de.komoot.photon.searcher.PhotonResult;
import de.komoot.photon.searcher.SearchHandler;
import org.jspecify.annotations.NullMarked;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.SearchType;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch.core.SearchResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@NullMarked
public class OpenSearchSearchHandler implements SearchHandler<SimpleSearchRequest> {
    private final OpenSearchClient client;
    private final String queryTimeout;

    public OpenSearchSearchHandler(OpenSearchClient client, int queryTimeout) {
        this.client = client;
        this.queryTimeout = queryTimeout + "s";
    }

    @Override
    public List<PhotonResult> search(SimpleSearchRequest request) {
        final int limit = request.getLimit();
        final int extLimit = limit > 1 ? (int) Math.round(limit * 1.5) : 1;

        var results = sendQuery(buildQuery(request, false), extLimit);

        if (results.hits().hits().isEmpty()) {
            results = sendQuery(buildQuery(request, true), extLimit);
        }

        List<PhotonResult> ret = new ArrayList<>();
        for (var hit : results.hits().hits()) {
            var score = hit.score();
            var source = hit.source();
            if (source != null) {
                if (score != null) {
                    source.setScore(score);
                }
                ret.add(source);
            }
        }

        return ret;
    }

    @Override
    public String dumpQuery(SimpleSearchRequest simpleSearchRequest) {
        return "{}";
    }

    private Query buildQuery(SimpleSearchRequest request, boolean lenient) {
        final var query = new SearchQueryBuilder(request.getQuery(), lenient, request.getSuggestAddresses());
        query.addOsmTagFilter(request.getOsmTagFilters());
        query.addLayerFilter(request.getLayerFilters());
        query.addLocationBias(request.getLocationForBias(), request.getScaleForBias(), request.getZoomForBias());
        query.includeCategories(request.getIncludeCategories());
        query.excludeCategories(request.getExcludeCategories());
        query.addBoundingBox(request.getBbox());

        return query.build();
    }

    private SearchResponse<OpenSearchResult> sendQuery(Query query, int limit) {
        try {
            return client.search(s -> s
                    .index(PhotonIndex.NAME)
                    .searchType(SearchType.QueryThenFetch)
                    .query(query)
                    .size(limit)
                    .timeout(queryTimeout), OpenSearchResult.class);
        } catch (IOException e) {
            throw new RuntimeException("IO error during search", e);
        }
    }
}
