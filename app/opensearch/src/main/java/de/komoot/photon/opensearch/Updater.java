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

    public void addOrUpdate(Iterable<PhotonDoc> docs) {
        long placeID = 0;
        int objectId = 0;

        for (var doc: docs) {
            if (objectId == 0) {
                placeID = doc.getPlaceId();
            }
            final String uid = PhotonDoc.makeUid(placeID, objectId++);

            bulkRequest.operations(op -> op
                    .index(i -> i.index(PhotonIndex.NAME).id(uid).document(doc)));

            if (++todoDocuments > 10000) {
                updateDocuments();
            }
        }

        deleteSubset(placeID, objectId);
    }

    public void delete(long placeId) {
        deleteSubset(placeId, 0);
    }

    private void deleteSubset(long docId, int fromObjectId) {
        int objectId = fromObjectId;

        while (exists(docId, objectId++)) {
            final String uid = PhotonDoc.makeUid(docId, objectId);
            bulkRequest.operations(op -> op
                    .delete(d -> d.index(PhotonIndex.NAME).id(uid)));

            if (++todoDocuments > 10000) {
                updateDocuments();
            }
        }
    }

    private boolean exists(long docId, int objectId) {
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
        if (todoDocuments > 0) {
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
}
