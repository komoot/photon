package de.komoot.photon.elasticsearch;

import de.komoot.photon.PhotonDoc;
import de.komoot.photon.Utils;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.client.Requests;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import java.io.IOException;

@Slf4j
public class CustomImporter implements de.komoot.photon.Importer {
    private TransportClient esClient;
    private int documentCount = 0;
    private final String indexName = "photon";
    private final String indexType = "place";
    private BulkRequestBuilder bulkRequest;
    private final String[] languages;

    public CustomImporter(TransportClient esClient, String languages) {
        this.esClient = esClient;
        this.bulkRequest = esClient.prepareBulk();
        this.languages = languages.split(",");
    }

    @Override
    public void add(PhotonDoc doc) {
        try {
            this.bulkRequest.add(this.esClient.prepareIndex(indexName, indexType).
                    setSource(Utils.convert(doc, languages)).setId(doc.getUid()));
        } catch (IOException e) {
            log.error("could not bulk add document " + doc.getUid(), e);
            return;
        }
        this.documentCount += 1;
        if (this.documentCount > 0 && this.documentCount % 10000 == 0) {
            this.saveDocuments();
        }
    }

    private void saveDocuments() {
        if (this.documentCount < 1) return;

        BulkResponse bulkResponse = bulkRequest.execute().actionGet();
        if (bulkResponse.hasFailures()) {
            log.error("error while bulk import:" + bulkResponse.buildFailureMessage());
        }

        if (true) {
            for (BulkItemResponse r : bulkResponse.getItems()) {
                if (r.isFailed()) {
                    log.error("Failure - " + r.getFailure());
                } else if (false) {
                    log.error("Success - " + r.getResponse());
                }
            }
        }
        this.bulkRequest = this.esClient.prepareBulk();
    }

    @Override
    public void finish() {
        this.saveDocuments();
        this.documentCount = 0;
    }

    public long count() {
        return this.esClient.search(Requests.searchRequest(indexName).types(indexType).source(SearchSourceBuilder.searchSource().size(0))).actionGet().getHits()
                .getTotalHits().value;
    }
}
