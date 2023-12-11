package de.komoot.photon.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._helpers.bulk.BulkIngester;
import de.komoot.photon.PhotonDoc;
import de.komoot.photon.Utils;
import lombok.extern.slf4j.Slf4j;
import java.util.concurrent.TimeUnit;

/**
 * Updater for elasticsearch
 *
 * @author felix
 */
@Slf4j
public class Updater implements de.komoot.photon.Updater {
    private final BulkIngester<Void> ingester;
    private final String[] languages;
    private final String[] extraTags;
    private final boolean allExtraTags;
    private final boolean includeExtraNames;

    public Updater(ElasticsearchClient client, String[] languages, String[] extraTags, boolean allExtraTags, boolean includeExtraNames) {

        this.languages = languages;
        this.extraTags = extraTags;
        this.allExtraTags = allExtraTags;
        this.includeExtraNames = includeExtraNames;
        this.ingester = new BulkIngester.Builder<Void>()
                .client(client)
                .flushInterval(1, TimeUnit.SECONDS)
                .maxOperations(10000)
                .build();
    }

    public void finish() {
        this.updateDocuments();
    }

    @Override
    public void create(PhotonDoc doc) {
        this.ingester.add(
                op -> op
                        .index(v -> v
                                .index(PhotonIndex.NAME)
                                .document(Utils.convert(doc, languages, extraTags, allExtraTags, includeExtraNames))
                                .id(String.valueOf(doc.getPlaceId()))
                        )
        );

        // TODO reimplement error handling using listener
        // log.error(String.format("creation of new doc [%s] failed", doc), e);
    }

    public void delete(Long id) {
        this.ingester.add(op -> op.delete(fn -> fn.index(PhotonIndex.NAME).id(String.valueOf(id))));
    }

    private void updateDocuments() {
        if (this.ingester.pendingOperations() == 0) {
            log.warn("Update empty");
            return;
        }
        this.ingester.flush();
        /* TODO reimplement error handling using listener
        if (bulkResponse.hasFailures()) {
            log.error("error while bulk update: " + bulkResponse.buildFailureMessage());
        }
        this.bulkRequest = this.esClient.prepareBulk();
         */
    }
}
