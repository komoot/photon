package de.komoot.photon.importer.elasticsearch;

import de.komoot.photon.importer.Utils;
import de.komoot.photon.importer.model.PhotonDoc;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.count.CountRequest;
import org.elasticsearch.client.Client;

import java.io.IOException;

/**
 * elasticsearch importer
 *
 * @author felix
 */
@Slf4j
public class Importer implements de.komoot.photon.importer.Importer {
	private int documentCount = 0;

	private final String indexName = "photon";
	private final String indexType = "place";
	private final Client esClient;
	private BulkRequestBuilder bulkRequest;

	public Importer(Client esClient) {
		this.esClient = esClient;
		this.bulkRequest = esClient.prepareBulk();
	}

	@Override
	public void add(PhotonDoc doc) {
		try {
			this.bulkRequest.add(this.esClient.prepareIndex(indexName, indexType).
					setSource(Utils.convert(doc)).setId(String.valueOf(doc.getPlaceId())));
		} catch(IOException e) {
			log.error("could not ", e);
		}
		this.documentCount += 1;
		if(this.documentCount > 0 && this.documentCount % 10000 == 0) {
			this.saveDocuments();
		}
	}

	private void saveDocuments() {
		if(this.documentCount < 1) return;

		BulkResponse bulkResponse = bulkRequest.execute().actionGet();
		if(bulkResponse.hasFailures()) {
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
		return this.esClient.count(new CountRequest(indexName).types(indexType)).actionGet().getCount();
	}
}
