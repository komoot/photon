package de.komoot.photon.opensearch;

import de.komoot.photon.query.PhotonRequest;
import de.komoot.photon.searcher.PhotonResult;
import de.komoot.photon.searcher.SearchHandler;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.SearchType;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch.core.SearchResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class OpenSearchSearchHandler implements SearchHandler {
    final private OpenSearchClient client;
    final private String[] supportedLanguages;
    final private String queryTimeout;

    public OpenSearchSearchHandler(OpenSearchClient client, String[] supportedLanguages, int queryTimeout) {
        this.client = client;
        this.supportedLanguages = supportedLanguages;
        this.queryTimeout = queryTimeout + "s";
    }

    @Override
    public List<PhotonResult> search(PhotonRequest request) {
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
    public String dumpQuery(PhotonRequest photonRequest) {
        return "{}";
    }

    private SearchQueryBuilder buildQuery(PhotonRequest request, boolean lenient) {
        return new SearchQueryBuilder(request.getQuery(), request.getLanguage(), supportedLanguages, lenient).
                withOsmTagFilters(request.getOsmTagFilters()).
                withLayerFilters(request.getLayerFilters()).
                withLocationBias(request.getLocationForBias(), request.getScaleForBias(), request.getZoomForBias()).
                withBoundingBox(request.getBbox());
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
