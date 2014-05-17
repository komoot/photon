package de.komoot.photon.importer.elasticsearch;

import de.komoot.photon.importer.Utils;
import de.komoot.photon.importer.model.PhotonDoc;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.client.Client;
import spark.utils.IOUtils;

import java.io.IOException;
import java.io.InputStream;

/**
 * Importer for elaticsearch
 *
 * @author felix
 */
public class Importer implements de.komoot.photon.importer.Importer {
	private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(Importer.class);

	private int documentCount = 0;

	private Client esClient;
	private BulkRequestBuilder bulkRequest;

	public void addDocument(PhotonDoc doc) {
		try {
			this.bulkRequest.add(this.esClient.prepareIndex("photon", "place").setSource(Utils.convert(doc)).setId(String.valueOf(doc.getPlaceId())));
		} catch(IOException e) {
			LOGGER.error("TODO: add description", e);
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
			LOGGER.error("Error while Bulkimport");
		}
		this.bulkRequest = this.esClient.prepareBulk();
	}

	public void finish() {
		this.saveDocuments();
		this.documentCount = 0;
	}

	public Importer(Client esClient) {
		this.esClient = esClient;
		this.bulkRequest = esClient.prepareBulk();
		try {
			this.esClient.admin().indices().prepareDelete("photon").execute().actionGet();
		} catch(Exception e) {
			LOGGER.info("cannot delete index", e);
		}
		if(!this.esClient.admin().indices().prepareExists("photon").execute().actionGet().isExists()) {
			final InputStream mappings = Thread.currentThread().getContextClassLoader().getResourceAsStream("mappings.json");
			final InputStream index_settings = Thread.currentThread().getContextClassLoader().getResourceAsStream("index_settings.json");

			try {
				this.esClient.admin().indices().prepareCreate("photon").setSettings(IOUtils.toString(mappings)).execute().actionGet();
				this.esClient.admin().indices().preparePutMapping("photon").setType("place").setSource(IOUtils.toString(index_settings)).execute().actionGet();
			} catch(IOException e) {
				LOGGER.error("cannot setup index, es config files not readable", e);
			}
		}
	}
}
