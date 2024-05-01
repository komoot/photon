package de.komoot.photon.opensearch;

import de.komoot.photon.PhotonDoc;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.core.BulkRequest;
import org.slf4j.Logger;

import java.io.IOException;

public class Importer implements de.komoot.photon.Importer {
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(Importer.class);

    private final OpenSearchClient client;
    private BulkRequest.Builder bulkRequest = new BulkRequest.Builder();
    private int todoDocuments = 0;

    public Importer(OpenSearchClient client) {
        this.client = client;
    }

    @Override
    public void add(PhotonDoc doc, int objectId) {
        bulkRequest.operations(op -> op
                .index(i -> i
                        .index(PhotonIndex.NAME)
                        .id(doc.getUid(objectId))
                        .document(doc)));
        ++todoDocuments;

        if (todoDocuments % 10000 == 0) {
            saveDocuments();
        }
    }

    @Override
    public void finish() {
        if (todoDocuments > 0) {
            saveDocuments();
        }

        try {
            client.indices().refresh();
        } catch (IOException e) {
            LOGGER.warn("Refresh of database failed", e);
        }
    }

    private void saveDocuments() {
        try {
            var response = client.bulk(bulkRequest.build());

            if (response.errors()) {
                LOGGER.error("Error during bulk import.");
            }
        } catch (IOException e) {
            LOGGER.error("Error during bulk import", e);
        }

        bulkRequest = new BulkRequest.Builder();
        todoDocuments = 0;
    }
}
