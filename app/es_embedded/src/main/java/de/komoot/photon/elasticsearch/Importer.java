package de.komoot.photon.elasticsearch;

import de.komoot.photon.PhotonDoc;
import de.komoot.photon.Utils;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.client.Client;
import org.slf4j.Logger;

import java.io.IOException;

/**
 * Elasticsearch importer
 */
public class Importer implements de.komoot.photon.Importer {
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(Importer.class);

    private int documentCount = 0;

    private final Client esClient;
    private BulkRequestBuilder bulkRequest;
    private final String[] languages;
    private final String[] extraTags;

    public Importer(Client esClient, String[] languages, String[] extraTags) {
        this.esClient = esClient;
        this.bulkRequest = esClient.prepareBulk();
        this.languages = languages;
        this.extraTags = extraTags;
    }

    @Override
    public void add(PhotonDoc doc, int objectId) {
        String uid = doc.getUid(objectId);
        try {
            this.bulkRequest.add(this.esClient.prepareIndex(PhotonIndex.NAME, PhotonIndex.TYPE).
                    setSource(Utils.convert(doc, languages, extraTags)).setId(uid));
        } catch (IOException e) {
            LOGGER.error("Could not bulk add document {}", uid, e);
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
            LOGGER.error("Error during bulk import: {}", bulkResponse.buildFailureMessage());
        }
        this.bulkRequest = this.esClient.prepareBulk();
    }

    @Override
    public void finish() {
        this.saveDocuments();
        this.documentCount = 0;
    }
}
