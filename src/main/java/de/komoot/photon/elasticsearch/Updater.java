package de.komoot.photon.elasticsearch;

import de.komoot.photon.PhotonDoc;
import de.komoot.photon.Utils;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.client.Client;
import org.slf4j.Logger;

import java.io.IOException;

/**
 * Updater for elasticsearch
 */
public class Updater implements de.komoot.photon.Updater {
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(Updater.class);

    private final Client esClient;
    private BulkRequestBuilder bulkRequest;
    private final String[] languages;
    private final String[] extraTags;

    public Updater(Client esClient, String[] languages, String[] extraTags) {
        this.esClient = esClient;
        this.bulkRequest = esClient.prepareBulk();
        this.languages = languages;
        this.extraTags = extraTags;
    }

    public void finish() {
        this.updateDocuments();
    }

    @Override
    public void create(PhotonDoc doc, int object_id) {
        String uid = doc.getUid(object_id);
        try {
            bulkRequest.add(esClient.prepareIndex(PhotonIndex.NAME, PhotonIndex.TYPE).setSource(Utils.convert(doc, languages, extraTags)).setId(uid));
        } catch (IOException e) {
            LOGGER.error("Creation of new doc {} failed", uid, e);
        }
    }

    public void delete(long doc_id, int object_id) {
        this.bulkRequest.add(this.esClient.prepareDelete(PhotonIndex.NAME, PhotonIndex.TYPE, makeUid(doc_id, object_id)));
    }

    public boolean exists(long doc_id, int object_id) {
        return esClient.prepareGet(PhotonIndex.NAME, PhotonIndex.TYPE, makeUid(doc_id, object_id)).execute().actionGet().isExists();
    }

    private String makeUid(Long doc_id, int object_id) {
        if (object_id <= 0) {
            return String.valueOf(doc_id);
        }
        return String.format("%d.%d", doc_id, object_id);
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
