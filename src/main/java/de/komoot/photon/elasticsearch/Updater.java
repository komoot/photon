package de.komoot.photon.elasticsearch;

import de.komoot.photon.PhotonDoc;
import de.komoot.photon.Utils;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.reindex.BulkByScrollResponse;
import org.elasticsearch.index.reindex.DeleteByQueryAction;
import org.elasticsearch.index.reindex.DeleteByQueryRequestBuilder;

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

    public Updater(Client esClient, String languages) {
        this.esClient = esClient;
        this.bulkRequest = esClient.prepareBulk();
        this.languages = languages.split(",");
    }

    @Override
    public void finish() {
        this.updateDocuments();
    }

    @Override
    public void updateOrCreate(PhotonDoc updatedDoc) {
        final boolean exists = this.esClient.get(this.esClient.prepareGet("photon", "place", updatedDoc.getUid()).request()).actionGet().isExists();
        if (exists) {
            this.update(updatedDoc);
        } else {
            this.create(updatedDoc);
        }
    }

    @Override
    public void create(PhotonDoc doc) {
        try {
            this.bulkRequest.add(this.esClient.prepareIndex("photon", "place").setSource(Utils.convert(doc, this.languages)).setId(doc.getUid()));
        } catch (IOException e) {
            log.error(String.format("creation of new doc [%s] failed", doc), e);
        }
    }

    @Override
    public void update(PhotonDoc doc) {
        try {
            this.bulkRequest.add(this.esClient.prepareUpdate("photon", "place", doc.getUid()).setDoc(Utils.convert(doc, this.languages)));
        } catch (IOException e) {
            log.error(String.format("update of new doc [%s] failed", doc), e);
        }
    }

    @Override
    public void delete(String id) {
        this.bulkRequest.add(this.esClient.prepareDelete("photon", "place", String.valueOf(id)));
    }
    
    @Override
    public void delete(String osmType, long osmId, String osmKey, String osmValue) {
        BoolQueryBuilder query = QueryBuilders.boolQuery().must(QueryBuilders.matchQuery("osm_id", osmId));
        if (osmKey != null) {
            query.must(QueryBuilders.matchQuery("osm__key", osmKey));
            if (osmValue != null) {
                query.must(QueryBuilders.matchQuery("osm__value", osmValue));
            }
        }
        query.must(QueryBuilders.matchQuery("_source['osm_type']", osmType));
        BulkByScrollResponse response = new DeleteByQueryRequestBuilder(this.esClient, DeleteByQueryAction.INSTANCE)
                .filter(query).source("photon").get();
        log.info(String.format("deleted %d documents based on osm object", response.getDeleted()));
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
