package de.komoot.photon.elasticsearch;

import de.komoot.photon.PhotonDoc;
import de.komoot.photon.Utils;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.client.Client;

import java.io.IOException;

/**
 * Updater for elasticsearch
 *
 * @author felix
 */
@Slf4j
public class Updater implements de.komoot.photon.Updater {
    private final Client esClient;
    private BulkRequestBuilder bulkRequest;
    private final String[] languages;
    private final String[] extraTags;

    public Updater(Client esClient, String languages, String extraTags) {
        this.esClient = esClient;
        this.bulkRequest = esClient.prepareBulk();
        this.languages = languages.split(",");
        this.extraTags = extraTags.split(",");
    }

    public void finish() {
        this.updateDocuments();
    }

    @Override
    public void create(PhotonDoc doc) {
        try {
            bulkRequest.add(esClient.prepareIndex(PhotonIndex.NAME, PhotonIndex.TYPE).setSource(Utils.convert(doc, languages, extraTags)).setId(String.valueOf(doc.getPlaceId())));
        } catch (IOException e) {
            log.error(String.format("creation of new doc [%s] failed", doc), e);
        }
    }

    public void delete(Long id) {
        this.bulkRequest.add(this.esClient.prepareDelete(PhotonIndex.NAME, PhotonIndex.TYPE, String.valueOf(id)));
    }

    private void updateDocuments() {
        if (this.bulkRequest.numberOfActions() == 0) {
            log.warn("Update empty");
            return;
        }
        BulkResponse bulkResponse = bulkRequest.execute().actionGet();
        if (bulkResponse.hasFailures()) {
            log.error("error while bulk update: " + bulkResponse.buildFailureMessage());
        }
        this.bulkRequest = this.esClient.prepareBulk();
    }
}
