package de.komoot.photon.opensearch;

import de.komoot.photon.searcher.PhotonResult;
import de.komoot.photon.query.StructuredSearchRequest;
import de.komoot.photon.searcher.SearchHandler;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.SearchType;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch.core.SearchResponse;

import java.io.IOException;
import java.util.stream.Stream;

/**
 * Execute a structured forward lookup.
 */
@NullMarked
public class OpenSearchStructuredSearchHandler implements SearchHandler<StructuredSearchRequest> {
    private final OpenSearchClient client;
    private final String queryTimeout;

    public OpenSearchStructuredSearchHandler(OpenSearchClient client, int queryTimeoutSec) {
        this.client = client;
        queryTimeout = queryTimeoutSec + "s";
    }

    @Override
    public Stream<PhotonResult> search(StructuredSearchRequest photonRequest) {
        // for the case of deduplication we need a bit more results, #300
        int limit = photonRequest.getLimit();
        int extLimit = limit > 1 ? (int) Math.round(photonRequest.getLimit() * 1.5) : 1;

        var results = sendQuery(buildQuery(photonRequest, false), extLimit);

        var total = results.hits().total();
        if (total == null || total.value() == 0) {
            results = sendQuery(buildQuery(photonRequest, true), extLimit);

            total = results.hits().total();
            if (total != null && total.value() == 0 && photonRequest.hasStreet()) {
                var street = photonRequest.getStreet();
                var houseNumber = photonRequest.getHouseNumber();
                photonRequest.setStreet(null);
                photonRequest.setHouseNumber(null);
                results = sendQuery(buildQuery(photonRequest, true), extLimit);
                photonRequest.setStreet(street);
                photonRequest.setHouseNumber(houseNumber);
            }
        }

        return ResultScorer.hitsToResultStream(results, 0)
                .map(r -> r);
    }

    @Override
    public @Nullable String dumpQuery(StructuredSearchRequest searchRequest) {
        return null;
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
