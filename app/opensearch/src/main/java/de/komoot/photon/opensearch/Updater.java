package de.komoot.photon.opensearch;

import de.komoot.photon.PhotonDoc;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.core.BulkRequest;
import org.slf4j.Logger;

import java.io.IOException;

public class Updater implements de.komoot.photon.Updater {
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(Updater.class);

    private final OpenSearchClient client;
    private BulkRequest.Builder bulkRequest = new BulkRequest.Builder();
    private int todoDocuments = 0;

    public Updater(OpenSearchClient client) {
        this.client = client;
    }

    @Override
    public void create(PhotonDoc doc, int objectId) {
        bulkRequest.operations(op -> op
                .index(i -> i
                        .index(PhotonIndex.NAME)
                        .id(doc.getUid(objectId))
                        .document(doc)));
        ++todoDocuments;
    }

    @Override
    public void delete(long docId, int objectId) {
        bulkRequest.operations(op -> op
                .delete(d -> d
                        .index(PhotonIndex.NAME)
                        .id(PhotonDoc.makeUid(docId, objectId))));
        ++todoDocuments;
    }

    @Override
    public boolean exists(long docId, int objectId) {
        try {
            return client.exists(e -> e.index(PhotonIndex.NAME).id(PhotonDoc.makeUid(docId, objectId))).value();
        } catch (IOException e) {
            LOGGER.warn("IO error on exists operation", e);
        }
        return false;
    }

    @Override
    public void finish() {
        updateDocuments();
        try {
            client.indices().refresh();
        } catch (IOException e) {
            LOGGER.warn("IO error on refresh.");
        }
    }

    private void updateDocuments() {
        if (todoDocuments == 0) {
            return;
        }

        try {
            var response = client.bulk(bulkRequest.build());

            if (response.errors()) {
                LOGGER.error("Errors during bulk update.");
            }
        } catch (IOException e) {
            LOGGER.error("IO error during bulk update", e);
        }

        bulkRequest = new BulkRequest.Builder();
        todoDocuments = 0;
    }
}
