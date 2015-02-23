package de.komoot.photon;

import de.komoot.photon.elasticsearch.Server;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.indices.IndexMissingException;
import org.junit.*;

import java.io.File;
import java.io.IOException;

/**
 * @author Peter Karich
 */
@Slf4j
public class ESBaseTester {
	private Server server;

	@After
	public void tearDownClass() {
		shutdownES();
	}

	public void setUpES() throws IOException {
		server = new Server("photon", new File("./target/es_photon").getAbsolutePath(), "en", "", true).start();
		server.recreateIndex();
	}

	protected Client getClient() {
		if(server == null) {
			throw new RuntimeException("call setUpES before using getClient");
		}

		return server.getClient();
	}

	private final String indexName = "photon";

	protected void refresh() {
		getClient().admin().indices().refresh(new RefreshRequest(indexName)).actionGet();
	}

	protected void deleteAll() {
		try {
			getClient().prepareDeleteByQuery(indexName).setQuery(QueryBuilders.matchAllQuery()).execute().actionGet();
		} catch(IndexMissingException ex) {
		}

		refresh();
	}

	public void shutdownES() {
		server.shutdown();
	}
}
