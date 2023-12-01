package de.komoot.photon.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SearchType;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.komoot.photon.query.PhotonRequest;
import de.komoot.photon.searcher.PhotonResult;
import de.komoot.photon.searcher.SearchHandler;

import lombok.extern.slf4j.Slf4j; // for debugging

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Sachin Dole on 2/12/2015.
 */
@Slf4j
public class ElasticsearchSearchHandler implements SearchHandler {
    private final ElasticsearchClient client;
    private final String[] supportedLanguages;
    private boolean lastLenient = false;

    private final ObjectMapper mapper = new ObjectMapper().configure(SerializationFeature.INDENT_OUTPUT, true);

    public ElasticsearchSearchHandler(ElasticsearchClient client, String[] languages) {
        this.client = client;
        this.supportedLanguages = languages;
    }

    @Override
    public List<PhotonResult> search(PhotonRequest photonRequest) throws IOException {
        PhotonQueryBuilder queryBuilder = buildQuery(photonRequest, false);

        // for the case of deduplication we need a bit more results, #300
        int limit = photonRequest.getLimit();
        int extLimit = limit > 1 ? (int) Math.round(photonRequest.getLimit() * 1.5) : 1;

        SearchResponse<ObjectNode> results = sendQuery(queryBuilder.buildQuery(), extLimit);

        if (results.hits().hits().isEmpty()) {
            results = sendQuery(buildQuery(photonRequest, true).buildQuery(), extLimit);
        }

        List<PhotonResult> ret = new ArrayList<>();

        for (Hit<ObjectNode> hit : results.hits().hits()) {
            ret.add(new ElasticResult(hit));
        }

        return ret;
    }

    public String dumpQuery(PhotonRequest photonRequest) {
        return buildQuery(photonRequest, lastLenient).buildQuery().toString();
    }

   public PhotonQueryBuilder buildQuery(PhotonRequest photonRequest, boolean lenient) {
       lastLenient = lenient;
       return PhotonQueryBuilder
               .builder(
                       photonRequest.getQuery(),
                       photonRequest.getLanguage(),
                       supportedLanguages,
                       lenient
               )
               .withOsmTagFilters(photonRequest.getOsmTagFilters())
               .withLayerFilters(photonRequest.getLayerFilters())
               .withLocationBias(
                       photonRequest.getLocationForBias(),
                       photonRequest.getScaleForBias(),
                       photonRequest.getZoomForBias()
               )
               .withBoundingBox(photonRequest.getBbox());
    }

    private SearchResponse<ObjectNode> sendQuery(Query query, Integer limit) throws IOException {
        SearchRequest.Builder builder = new SearchRequest.Builder()
                .index(PhotonIndex.NAME)
                .searchType(SearchType.QueryThenFetch)
                .query(query)
                .size(limit)
                .timeout(String.format("%ss", 7));

        SearchRequest request = builder.build();
        return client.search(request, ObjectNode.class);
    }
}
