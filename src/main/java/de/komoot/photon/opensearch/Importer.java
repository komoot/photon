package de.komoot.photon.opensearch;

import de.komoot.photon.PhotonDoc;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.core.BulkRequest;
import org.opensearch.client.opensearch.core.bulk.BulkResponseItem;

import java.io.IOException;

public class Importer implements de.komoot.photon.Importer {
    private static final Logger LOGGER = LogManager.getLogger();

    private final OpenSearchClient client;
    private BulkRequest.Builder bulkRequest = new BulkRequest.Builder();
    private int todoDocuments = 0;
    private boolean hasPrintedNoUpdates = false;

    public Importer(OpenSearchClient client) {
        this.client = client;
    }

    @Override
    public void add(Iterable<PhotonDoc> docs) {
        String placeID = null;
        int objectId = 0;
        for (var doc : docs) {
            if (objectId == 0) {
                placeID = doc.getPlaceId();
            }
            if (placeID == null) {
                if (!hasPrintedNoUpdates) {
                    LOGGER.warn("Documents have no place_id. Updates will not be possible.");
                    hasPrintedNoUpdates = true;
                }
                bulkRequest.operations(op -> op
                        .create(i -> i
                                .index(PhotonIndex.NAME)
                                .document(doc)));
            } else {
                final String uuid = PhotonDoc.makeUid(placeID, objectId++);
                bulkRequest.operations(op -> op
                        .create(i -> i
                                .index(PhotonIndex.NAME)
                                .id(uuid)
                                .document(doc)));
            }
            ++todoDocuments;

            if (todoDocuments % 10000 == 0) {
                saveDocuments();
            }
        }
    }

    @Override
    public void finish() {
        if (todoDocuments > 0) {
            saveDocuments();
        }

        try {
            client.indices().refresh(r -> r.index(PhotonIndex.NAME));
        } catch (IOException e) {
            LOGGER.warn("Refresh of database failed", e);
        }
    }

    private void saveDocuments() {
        try {
            final var request = bulkRequest.build();
            final var response = client.bulk(request);

            if (response.errors()) {
                for (BulkResponseItem bri: response.items()) {
                    if (bri.status() != 201) {
                        LOGGER.error("Error during bulk import: {}", bri.toJsonString());
                        for (var op : request.operations()) {
                            if (op.isCreate() && bri.id() != null && bri.id().equals(op.create().id())) {
                                LOGGER.error("Bad document: {}", op.create().document());
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.error("Error during bulk import", e);
        }

        bulkRequest = new BulkRequest.Builder();
        todoDocuments = 0;
    }
}
