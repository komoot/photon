package de.komoot.photon.opensearch;

import de.komoot.photon.searcher.StructuredSearchHandler;
import de.komoot.photon.searcher.PhotonResult;
import de.komoot.photon.query.StructuredPhotonRequest;
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
public class OpenSearchStructuredSearchHandler implements StructuredSearchHandler {
    private final OpenSearchClient client;
    private final String[] supportedLanguages;
    private final String queryTimeout;

    public OpenSearchStructuredSearchHandler(OpenSearchClient client, String[] languages, int queryTimeoutSec) {
        this.client = client;
        this.supportedLanguages = languages;
        queryTimeout = queryTimeoutSec + "s";
    }

    @Override
    public List<PhotonResult> search(StructuredPhotonRequest photonRequest) {
        var queryBuilder = buildQuery(photonRequest, false);

        // for the case of deduplication we need a bit more results, #300
        int limit = photonRequest.getLimit();
        int extLimit = limit > 1 ? (int) Math.round(photonRequest.getLimit() * 1.5) : 1;

        var results = sendQuery(queryBuilder.buildQuery(), extLimit);

        if (results.hits().total().value() == 0) {
            results = sendQuery(buildQuery(photonRequest, true).buildQuery(), extLimit);

            if (results.hits().total().value() == 0 && photonRequest.hasStreet()) {
                var street = photonRequest.getStreet();
                var houseNumber = photonRequest.getHouseNumber();
                photonRequest.setStreet(null);
                photonRequest.setHouseNumber(null);
                results = sendQuery(buildQuery(photonRequest, true).buildQuery(), extLimit);
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

    public SearchQueryBuilder buildQuery(StructuredPhotonRequest photonRequest, boolean lenient) {
        return new SearchQueryBuilder(photonRequest, photonRequest.getLanguage(), supportedLanguages, lenient).
                withOsmTagFilters(photonRequest.getOsmTagFilters()).
                withLayerFilters(photonRequest.getLayerFilters()).
                withLocationBias(photonRequest.getLocationForBias(), photonRequest.getScaleForBias(), photonRequest.getZoomForBias()).
                withBoundingBox(photonRequest.getBbox());
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
