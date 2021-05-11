package de.komoot.photon.elasticsearch;

import de.komoot.photon.PhotonDoc;
import de.komoot.photon.Utils;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.client.Client;

import java.io.IOException;

/**
 * elasticsearch importer
 *
 * @author felix
 */
@Slf4j
public class Importer implements de.komoot.photon.Importer {
    private int documentCount = 0;

    private final Client esClient;
    private BulkRequestBuilder bulkRequest;
    private final String[] languages;
    private final String[] extraTags;

    public Importer(Client esClient, String languages, String extraTags) {
        this.esClient = esClient;
        this.bulkRequest = esClient.prepareBulk();
        this.languages = languages.split(",");
        this.extraTags = extraTags.split(",");
    }

    @Override
    public void add(PhotonDoc doc) {
        try {
            this.bulkRequest.add(this.esClient.prepareIndex(PhotonIndex.NAME, PhotonIndex.TYPE).
                    setSource(Utils.convert(doc, languages, extraTags)).setId(doc.getUid()));
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
        this.bulkRequest = this.esClient.prepareBulk();
    }

    @Override
    public void finish() {
        this.saveDocuments();
        this.documentCount = 0;
    }
}
