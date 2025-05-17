package de.komoot.photon.elasticsearch;

import de.komoot.photon.DatabaseProperties;
import de.komoot.photon.PhotonDoc;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.client.Client;

import java.io.IOException;

/**
 * Elasticsearch importer
 */
public class Importer implements de.komoot.photon.Importer {
    private static final Logger LOGGER = LogManager.getLogger();

    private int documentCount = 0;

    private final Client esClient;
    private BulkRequestBuilder bulkRequest;
    private final DatabaseProperties dbProperties;

    public Importer(Client esClient, DatabaseProperties dbProperties) {
        this.esClient = esClient;
        this.bulkRequest = esClient.prepareBulk();
        this.dbProperties = dbProperties;
    }

    @Override
    public void add(Iterable<PhotonDoc> docs) {
        long placeID = 0;
        int objectId = 0;
        for (var doc : docs) {
            if (objectId == 0) {
                placeID = doc.getPlaceId();
            }
            final String uid = PhotonDoc.makeUid(placeID, objectId++);
            try {
                bulkRequest.add(esClient.prepareIndex(PhotonIndex.NAME, PhotonIndex.TYPE).
                        setSource(PhotonDocConverter.convert(doc, dbProperties)).setId(uid));
            } catch (IOException e) {
                LOGGER.error("Could not bulk add document {}", uid, e);
                return;
            }
            ++documentCount;

            if (documentCount % 10000 == 0) {
                saveDocuments();
            }
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
