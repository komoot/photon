package de.komoot.photon.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._helpers.bulk.BulkIngester;
import co.elastic.clients.elasticsearch._helpers.bulk.BulkListener;
import co.elastic.clients.elasticsearch._types.ErrorCause;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import de.komoot.photon.PhotonDoc;
import de.komoot.photon.Utils;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Objects;

/**
 * elasticsearch importer
 *
 * @author felix
 */
@Slf4j
public class Importer implements de.komoot.photon.Importer {
    private int documentCount = 0;
    private final BulkIngester<String> ingester;
    private final String[] languages;
    private final String[] extraTags;
    private final boolean allExtraTags;

    public Importer(ElasticsearchClient client, String[] languages, String[] extraTags, boolean allExtraTags) {
        this.languages = languages;
        this.extraTags = extraTags;
        this.allExtraTags = allExtraTags;
        BulkListener<String> listener = new BulkListener<>() {
            @Override
            public void beforeBulk(long executionId, BulkRequest request, List<String> contexts) {}

            @Override
            public void afterBulk(long executionId, BulkRequest request, List<String> contexts, BulkResponse response) {
                documentCount += contexts.size();
                if (response.errors()) {
                    List<ErrorCause> errors = response
                            .items()
                            .stream()
                            .map(BulkResponseItem::error)
                            .filter(Objects::nonNull)
                            .toList();
                    for (ErrorCause error : errors) {
                        log.error(String.format("Error during bulk ingest: %s", error.reason()));
                    }
                } else {
                    log.debug(String.format("Successfully ingested %s documents", documentCount));
                }
            }

            @Override
            public void afterBulk(long executionId, BulkRequest request, List<String> contexts, Throwable failure) {
                log.error(String.format("Bulk request with executionId %s failed", executionId), failure);
            }
        };

        this.ingester = new BulkIngester.Builder<String>()
                .client(client)
                .maxOperations(10000)
                .maxConcurrentRequests(32)
                .listener(listener)
                .build();
    }

    @Override
    public void add(PhotonDoc doc) {
        this.ingester.add(op -> op
                .index(idx -> idx
                        .index(PhotonIndex.NAME)
                        .document(Utils.convert(doc, languages, extraTags, allExtraTags))
                        .id(doc.getUid())
                )
        );
    }

    @Override
    public void finish() {
        this.ingester.close();
        this.documentCount = 0;
    }

}
