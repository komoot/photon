package de.komoot.photon.elasticsearch;

import de.komoot.photon.PhotonDoc;
import de.komoot.photon.Utils;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.Requests;
import org.elasticsearch.common.bytes.BytesArray;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.json.JSONObject;

import java.io.IOException;

/**
 * elasticsearch importer
 *
 * @author felix
 */
@Slf4j
public class Importer implements de.komoot.photon.Importer {
    private int documentCount = 0;

    private final String indexName = "photon";
    private final String indexType = "place";
    private final Client esClient;
    private BulkRequestBuilder bulkRequest;
    private final String[] languages;
    private boolean readUid;

    public Importer(Client esClient, String languages, boolean readUidFromJson) {
        this.esClient = esClient;
        this.bulkRequest = esClient.prepareBulk();
        this.languages = languages.split(",");
        this.readUid = readUidFromJson;
    }

    @Override
    public void add(PhotonDoc doc) {
        try {
            add(Utils.convert(doc, languages).bytes(), doc.getUid());
        } catch (IOException e) {
            log.error("could not bulk add document " + doc.getUid(), e);
        }
    }

    /**
     * add a json formatted photon document.
     *
     * @param source json formatted photon document
     * @param uid    optional uid if not present it will parse the source and picking uid or
     *               if empty id. If both is empty it will be generated.
     */
    public void add(String source, String uid) {
        if (uid == null && readUid) {
            // this is expensive as ES is parsing the BytesArray again but we would need e.g. jackson that
            // parses the string into a Map that could be used from ES without doing the parsing work again
            JSONObject json = new JSONObject(source);
            uid = json.getString("uid");
            if (uid == null)
                // if null ES will create a random
                uid = json.getString("id");
        }
        add(new BytesArray(source), uid);
    }

    private void add(BytesReference sourceBytes, String uid) {
        this.bulkRequest.add(this.esClient.prepareIndex(indexName, indexType)
                .setSource(sourceBytes, XContentType.JSON).setId(uid));
        this.documentCount += 1;
        if (this.documentCount > 0 && this.documentCount % 10000 == 0) {
            this.saveDocuments();
        }
    }

    private void saveDocuments() {
        if (this.documentCount < 1) return;

        BulkResponse bulkResponse = bulkRequest.execute().actionGet();
        if (bulkResponse.hasFailures()) {
            log.error("error while bulk import:" + bulkResponse.buildFailureMessage());
        }
        this.bulkRequest = this.esClient.prepareBulk();
    }

    @Override
    public void finish() {
        this.saveDocuments();
        this.documentCount = 0;
    }

    public long count() {
        return this.esClient.search(Requests.searchRequest(indexName).types(indexType).source(SearchSourceBuilder.searchSource().size(0))).actionGet().getHits()
                .getTotalHits();
    }
}
