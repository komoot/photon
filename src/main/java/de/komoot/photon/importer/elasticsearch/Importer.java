package de.komoot.photon.importer.elasticsearch;

import de.komoot.photon.importer.Utils;
import de.komoot.photon.importer.model.PhotonDoc;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.indices.IndexMissingException;
import spark.utils.IOUtils;

import java.io.IOException;
import java.io.InputStream;

/**
 * elasticsearch importer
 *
 * @author felix
 */
@Slf4j
public class Importer implements de.komoot.photon.importer.Importer {
	private int documentCount = 0;

	private Client esClient;
	private BulkRequestBuilder bulkRequest;

	public Importer(Client esClient) {
		this.esClient = esClient;
		this.bulkRequest = esClient.prepareBulk();

		try {
			this.esClient.admin().indices().prepareDelete("photon").execute().actionGet();
		} catch(IndexMissingException e) { /* ignored */ }

		final boolean indexExists = this.esClient.admin().indices().prepareExists("photon").execute().actionGet().isExists();

		if(!indexExists) {
			final InputStream mappings = Thread.currentThread().getContextClassLoader().getResourceAsStream("mappings.json");
			final InputStream index_settings = Thread.currentThread().getContextClassLoader().getResourceAsStream("index_settings.json");

			try {
				this.esClient.admin().indices().prepareCreate("photon").setSettings(IOUtils.toString(mappings)).execute().actionGet();
				this.esClient.admin().indices().preparePutMapping("photon").setType("place").setSource(IOUtils.toString(index_settings)).execute().actionGet();
			} catch(IOException e) {
				log.error("cannot setup index, elastic search config files not readable", e);
			}
		}
	}

	public void add(PhotonDoc doc) {
		try {
			this.bulkRequest.add(this.esClient.prepareIndex("photon", "place").setSource(Utils.convert(doc)).setId(String.valueOf(doc.getPlaceId())));
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

	public void finish() {
		this.saveDocuments();
		this.documentCount = 0;
	}
}
