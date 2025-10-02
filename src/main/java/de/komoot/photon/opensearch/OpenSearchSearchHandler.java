package de.komoot.photon.opensearch;

import de.komoot.photon.query.SimpleSearchRequest;
import de.komoot.photon.searcher.PhotonResult;
import de.komoot.photon.searcher.SearchHandler;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.SearchType;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch.core.SearchResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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

        var results = sendQuery(buildQuery(request, false).buildQuery(), extLimit);

        if (results.hits().hits().isEmpty()) {
            results = sendQuery(buildQuery(request, true).buildQuery(), extLimit);
        }

        List<PhotonResult> ret = new ArrayList<>();
        for (var hit : results.hits().hits()) {
            ret.add(hit.source().setScore(hit.score()));
        }

        return ret;
    }

    @Override
    public String dumpQuery(SimpleSearchRequest simpleSearchRequest) {
        return "{}";
    }

    private SearchQueryBuilder buildQuery(SimpleSearchRequest request, boolean lenient) {
        return new SearchQueryBuilder(request.getQuery(), lenient)
                .withOsmTagFilters(request.getOsmTagFilters())
                .withLayerFilters(request.getLayerFilters())
                .withIncludeCategories(request.getIncluded())
                .withExcludeCategories(request.getExcluded())
                .withLocationBias(request.getLocationForBias(), request.getScaleForBias(), request.getZoomForBias())
                .withBoundingBox(request.getBbox());
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
