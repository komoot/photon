package de.komoot.photon.opensearch;

import de.komoot.photon.PhotonDoc;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.Time;
import org.opensearch.client.opensearch.core.BulkRequest;
import org.opensearch.client.opensearch.core.bulk.BulkOperation;
import org.opensearch.client.opensearch.core.bulk.BulkResponseItem;
import org.slf4j.Logger;

import java.io.IOException;

public class Importer implements de.komoot.photon.Importer {
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(Importer.class);

    private final OpenSearchClient client;
    private BulkRequest.Builder bulkRequest = new BulkRequest.Builder();
    private int todoDocuments = 0;

    public Importer(OpenSearchClient client) {
        this.client = client;
        enableImportSettings(true);
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

        enableImportSettings(false);

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
                for (BulkResponseItem bri: response.items()) {
                    LOGGER.error("Error during bulk import.");
                    if (bri.error() != null) {
                        LOGGER.error(bri.error().reason());
                        LOGGER.error(bri.error().type());
                        LOGGER.error(bri.error().stackTrace());
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.error("Error during bulk import", e);
        }

        bulkRequest = new BulkRequest.Builder();
        todoDocuments = 0;
    }

    private void enableImportSettings(boolean enable) {
        try {
            client.indices().putSettings(s -> s
                    .index(PhotonIndex.NAME)
                    .settings(is -> is
                            .refreshInterval(Time.of(t -> t.time(enable ? "-1" : "15s")))
                            .numberOfReplicas(enable ? "0" : "1")));
        } catch (IOException e) {
            LOGGER.warn("IO error while setting refresh interval", e);
        }

    }
}
