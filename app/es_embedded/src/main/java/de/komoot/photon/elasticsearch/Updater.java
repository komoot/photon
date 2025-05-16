package de.komoot.photon.elasticsearch;

import de.komoot.photon.DatabaseProperties;
import de.komoot.photon.PhotonDoc;
import de.komoot.photon.ConfigExtraTags;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.client.Client;

import java.io.IOException;

/**
 * Updater for ElasticSearch.
 */
public class Updater implements de.komoot.photon.Updater {
    private static final Logger LOGGER = LogManager.getLogger();

    private final Client esClient;
    private BulkRequestBuilder bulkRequest;
    private final DatabaseProperties dbProperties;

    public Updater(Client esClient, DatabaseProperties dbProperties) {
        this.esClient = esClient;
        this.bulkRequest = esClient.prepareBulk();
        this.dbProperties = dbProperties;
    }

    @Override
    public void finish() {
        this.updateDocuments();
        esClient.admin().indices().prepareRefresh(PhotonIndex.NAME).get();
    }

    @Override
    public void addOrUpdate(Iterable<PhotonDoc> docs) {
        long placeID = 0;
        int objectId = 0;

        for (var doc: docs) {
            if (objectId == 0) {
                placeID = doc.getPlaceId();
            }
            final String uid = PhotonDoc.makeUid(placeID, objectId++);

            try {
                bulkRequest.add(
                        esClient.prepareIndex(PhotonIndex.NAME, PhotonIndex.TYPE)
                                .setSource(PhotonDocConverter.convert(doc, dbProperties))
                                .setId(uid));
            } catch (IOException ex) {
                LOGGER.error("Parse error in document {}: {}", placeID, ex);
            }
        }

        deleteSubset(placeID, objectId);

        if (bulkRequest.numberOfActions() > 10000) {
            updateDocuments();
        }
    }

    @Override
    public void delete(long placeId) {
        deleteSubset(placeId, 0);

        if (bulkRequest.numberOfActions() > 10000) {
            updateDocuments();
        }
    }

    private void deleteSubset(long docId, int fromObjectId) {
        int objectId = fromObjectId;

        while (exists(docId, objectId++)) {
            final String uid = PhotonDoc.makeUid(docId, objectId);
            bulkRequest.add(
                    esClient.prepareDelete(PhotonIndex.NAME, PhotonIndex.TYPE, uid));
        }
    }

    private boolean exists(long docId, int objectId) {
        return esClient.prepareGet(PhotonIndex.NAME, PhotonIndex.TYPE, PhotonDoc.makeUid(docId, objectId)).execute().actionGet().isExists();
    }

    private void updateDocuments() {
        if (this.bulkRequest.numberOfActions() == 0) {
            LOGGER.warn("Update empty");
            return;
        }
        BulkResponse bulkResponse = bulkRequest.execute().actionGet();
        if (bulkResponse.hasFailures()) {
            LOGGER.error("Error while bulk update: {}", bulkResponse.buildFailureMessage());
        }
        this.bulkRequest = this.esClient.prepareBulk();
    }
}
