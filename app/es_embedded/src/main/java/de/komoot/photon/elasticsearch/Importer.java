package de.komoot.photon.elasticsearch;

import de.komoot.photon.PhotonDoc;
import de.komoot.photon.ConfigExtraTags;
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
    private final ConfigExtraTags extraTags;

    public Importer(Client esClient, String[] languages, ConfigExtraTags extraTags) {
        this.esClient = esClient;
        this.bulkRequest = esClient.prepareBulk();
        this.languages = languages;
        this.extraTags = extraTags;
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
                        setSource(PhotonDocConverter.convert(doc, languages, extraTags)).setId(uid));
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
