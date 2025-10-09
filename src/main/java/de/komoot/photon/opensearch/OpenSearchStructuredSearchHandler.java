package de.komoot.photon.opensearch;

import de.komoot.photon.searcher.PhotonResult;
import de.komoot.photon.query.StructuredSearchRequest;
import de.komoot.photon.searcher.SearchHandler;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.SearchType;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch.core.SearchResponse;

import java.util.ArrayList;
import java.util.List;
import java.io.IOException;

/**
 * Execute a structured forward lookup on an Elasticsearch database.
 */
public class OpenSearchStructuredSearchHandler implements SearchHandler<StructuredSearchRequest> {
    private final OpenSearchClient client;
    private final String queryTimeout;

    public OpenSearchStructuredSearchHandler(OpenSearchClient client, int queryTimeoutSec) {
        this.client = client;
        queryTimeout = queryTimeoutSec + "s";
    }

    @Override
    public List<PhotonResult> search(StructuredSearchRequest photonRequest) {
        // for the case of deduplication we need a bit more results, #300
        int limit = photonRequest.getLimit();
        int extLimit = limit > 1 ? (int) Math.round(photonRequest.getLimit() * 1.5) : 1;

        var results = sendQuery(buildQuery(photonRequest, false), extLimit);

        if (results.hits().total().value() == 0) {
            results = sendQuery(buildQuery(photonRequest, true), extLimit);

            if (results.hits().total().value() == 0 && photonRequest.hasStreet()) {
                var street = photonRequest.getStreet();
                var houseNumber = photonRequest.getHouseNumber();
                photonRequest.setStreet(null);
                photonRequest.setHouseNumber(null);
                results = sendQuery(buildQuery(photonRequest, true), extLimit);
                photonRequest.setStreet(street);
                photonRequest.setHouseNumber(houseNumber);
            }
        }

        List<PhotonResult> ret = new ArrayList<>();
        for (var hit : results.hits().hits()) {
            ret.add(hit.source().setScore(hit.score()));
        }

        return ret;
    }

    @Override
    public String dumpQuery(StructuredSearchRequest searchRequest) {
        return "{}";
    }

    public Query buildQuery(StructuredSearchRequest photonRequest, boolean lenient) {
        final var query = new SearchQueryBuilder(photonRequest, lenient);
        query.addOsmTagFilter(photonRequest.getOsmTagFilters());
        query.addLayerFilter(photonRequest.getLayerFilters());
        query.addLocationBias(photonRequest.getLocationForBias(), photonRequest.getScaleForBias(), photonRequest.getZoomForBias());
        query.includeCategories(photonRequest.getIncludeCategories());
        query.excludeCategories(photonRequest.getExcludeCategories());
        query.addBoundingBox(photonRequest.getBbox());

        return query.build();
    }

    private SearchResponse<OpenSearchResult> sendQuery(Query query, Integer limit) {
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
