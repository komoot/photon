package de.komoot.photon.importer.elasticsearch;

import de.komoot.photon.importer.Utils;
import de.komoot.photon.importer.model.PhotonDoc;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.client.Client;

import java.io.IOException;

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
		this.esClient.admin().indices().prepareDelete("photon").execute().actionGet();
		if(!this.esClient.admin().indices().prepareExists("photon").execute().actionGet().isExists()) {
			this.esClient.admin().indices().prepareCreate("photon").setSettings("{\"index\": {\"analysis\": {\"filter\": {\"photonngram\": {\"max_gram\": 15, \"type\": \"edgeNGram\", \"min_gram\": 2}}, \"char_filter\": {\"punctuationgreedy\": {\"pattern\": \"[\\\\.,]\", \"type\": \"pattern_replace\"}}, \"analyzer\": {\"raw_stringanalyser\": {\"filter\": [\"word_delimiter\", \"lowercase\", \"asciifolding\"], \"char_filter\": [\"punctuationgreedy\"], \"tokenizer\": \"standard\"}, \"stringanalyser\": {\"filter\": [\"word_delimiter\", \"lowercase\", \"asciifolding\", \"photonngram\"], \"char_filter\": [\"punctuationgreedy\"], \"tokenizer\": \"standard\"}}}}}\n").execute().actionGet();
			this.esClient.admin().indices().preparePutMapping("photon").setType("place").setSource("{\"place\": {\"_all\": {\"enabled\": false}, \"_boost\": {\"name\": \"ranking\", \"null_value\": 1.0}, \"_id\": {\"path\": \"id\"}, \"dynamic\": false, \"properties\": {\"street\": {\"index\": \"no\", \"type\": \"string\", \"copy_to\": [\"collector.en\", \"collector.de\", \"collector.fr\", \"collector.it\"]}, \"postcode\": {\"index\": \"no\", \"type\": \"string\", \"copy_to\": [\"collector.en\", \"collector.de\", \"collector.fr\", \"collector.it\"], \"store\": true}, \"osm_id\": {\"index\": \"not_analyzed\", \"type\": \"long\"}, \"osm_value\": {\"index\": \"no\", \"type\": \"string\"}, \"osm_key\": {\"index\": \"no\", \"type\": \"string\"}, \"city\": {\"type\": \"object\", \"properties\": {\"default\": {\"index\": \"no\", \"type\": \"string\", \"copy_to\": [\"collector.en\", \"collector.de\", \"collector.fr\", \"collector.it\"]}, \"de\": {\"index\": \"no\", \"type\": \"string\", \"copy_to\": [\"collector.de\"]}, \"en\": {\"index\": \"no\", \"type\": \"string\", \"copy_to\": [\"collector.en\"]}, \"fr\": {\"index\": \"no\", \"type\": \"string\", \"copy_to\": [\"collector.fr\"]}, \"it\": {\"index\": \"no\", \"type\": \"string\", \"copy_to\": [\"collector.it\"]}}}, \"collector\": {\"type\": \"object\", \"properties\": {\"de\": {\"fields\": {\"raw\": {\"search_analyzer\": \"raw_stringanalyser\", \"index_analyzer\": \"raw_stringanalyser\", \"type\": \"string\"}}, \"type\": \"string\", \"analyzer\": \"stringanalyser\"}, \"en\": {\"fields\": {\"raw\": {\"search_analyzer\": \"raw_stringanalyser\", \"index_analyzer\": \"raw_stringanalyser\", \"type\": \"string\"}}, \"type\": \"string\", \"analyzer\": \"stringanalyser\"}, \"fr\": {\"fields\": {\"raw\": {\"search_analyzer\": \"raw_stringanalyser\", \"index_analyzer\": \"raw_stringanalyser\", \"type\": \"string\"}}, \"type\": \"string\", \"analyzer\": \"stringanalyser\"}, \"it\": {\"fields\": {\"raw\": {\"search_analyzer\": \"raw_stringanalyser\", \"index_analyzer\": \"raw_stringanalyser\", \"type\": \"string\"}}, \"type\": \"string\", \"analyzer\": \"stringanalyser\"}}}, \"name\": {\"type\": \"object\", \"properties\": {\"default\": {\"fields\": {\"raw\": {\"search_analyzer\": \"raw_stringanalyser\", \"index_analyzer\": \"raw_stringanalyser\", \"type\": \"string\"}}, \"type\": \"string\", \"copy_to\": [\"collector.en\", \"collector.de\", \"collector.fr\", \"collector.it\"], \"analyzer\": \"stringanalyser\"}, \"de\": {\"fields\": {\"raw\": {\"search_analyzer\": \"raw_stringanalyser\", \"index_analyzer\": \"raw_stringanalyser\", \"type\": \"string\"}}, \"type\": \"string\", \"copy_to\": [\"collector.de\"], \"analyzer\": \"stringanalyser\"}, \"en\": {\"fields\": {\"raw\": {\"search_analyzer\": \"raw_stringanalyser\", \"index_analyzer\": \"raw_stringanalyser\", \"type\": \"string\"}}, \"type\": \"string\", \"copy_to\": [\"collector.en\"], \"analyzer\": \"stringanalyser\"}, \"fr\": {\"fields\": {\"raw\": {\"search_analyzer\": \"raw_stringanalyser\", \"index_analyzer\": \"raw_stringanalyser\", \"type\": \"string\"}}, \"type\": \"string\", \"copy_to\": [\"collector.fr\"], \"analyzer\": \"stringanalyser\"}, \"it\": {\"fields\": {\"raw\": {\"search_analyzer\": \"raw_stringanalyser\", \"index_analyzer\": \"raw_stringanalyser\", \"type\": \"string\"}}, \"type\": \"string\", \"copy_to\": [\"collector.it\"], \"analyzer\": \"stringanalyser\"}}}, \"country\": {\"type\": \"object\", \"properties\": {\"default\": {\"index\": \"no\", \"type\": \"string\", \"copy_to\": [\"collector.en\", \"collector.de\", \"collector.fr\", \"collector.it\"]}, \"de\": {\"index\": \"no\", \"type\": \"string\", \"copy_to\": [\"collector.de\"]}, \"en\": {\"index\": \"no\", \"type\": \"string\", \"copy_to\": [\"collector.en\"]}, \"fr\": {\"index\": \"no\", \"type\": \"string\", \"copy_to\": [\"collector.fr\"]}, \"it\": {\"index\": \"no\", \"type\": \"string\", \"copy_to\": [\"collector.it\"]}}}, \"osm_type\": {\"index\": \"no\", \"type\": \"string\"}, \"context\": {\"type\": \"object\", \"properties\": {\"default\": {\"index\": \"no\", \"type\": \"string\", \"copy_to\": [\"collector.en\", \"collector.de\", \"collector.fr\", \"collector.it\"]}, \"de\": {\"index\": \"no\", \"type\": \"string\", \"copy_to\": [\"collector.de\"]}, \"en\": {\"index\": \"no\", \"type\": \"string\", \"copy_to\": [\"collector.en\"]}, \"fr\": {\"index\": \"no\", \"type\": \"string\", \"copy_to\": [\"collector.fr\"]}, \"it\": {\"index\": \"no\", \"type\": \"string\", \"copy_to\": [\"collector.it\"]}}}, \"coordinate\": {\"type\": \"geo_point\"}, \"housenumber\": {\"index\": \"not_analyzed\", \"type\": \"string\"}}}}").execute().actionGet();
		}
	}
}
