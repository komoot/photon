package de.komoot.photon.importer.elasticsearch;

import org.elasticsearch.client.Client;
import org.elasticsearch.action.bulk.*;
import de.komoot.photon.importer.model.PhotonDoc;

/**
 *Importer for elaticsearch
 *
 * @author felix
 */
public class Importer implements de.komoot.photon.importer.Importer {
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(Importer.class);

    private int documentCount = 0;

    private Client esClient;
    private BulkRequestBuilder bulkRequest;

    public void addDocument(PhotonDoc doc){
        this.bulkRequest.add(this.esClient.prepareIndex().setSource(doc).setId(String.valueOf(doc.getPlaceId())));
        this.documentCount += 1;
        if(this.documentCount > 0 && this.documentCount % 10000 == 0)
        {
            this.saveDocuments();
        }
    }

    private void saveDocuments(){
        BulkResponse bulkResponse = bulkRequest.execute().actionGet();
        if (bulkResponse.hasFailures()) {
            LOGGER.error("Error while Bulkimport");
        }
        this.bulkRequest = this.esClient.prepareBulk();
    }

    public void finish(){
        this.saveDocuments();
    }

    public Importer(Client esClient){
        this.esClient = esClient;
        this.bulkRequest = esClient.prepareBulk();

    }
}
