package de.komoot.search;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CommonsHttpSolrServer;
import org.apache.solr.client.solrj.request.AbstractUpdateRequest.ACTION;
import org.apache.solr.client.solrj.request.ContentStreamUpdateRequest;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.util.ContentStream;
import org.apache.solr.common.util.ContentStreamBase;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Manages solr index. Assumes that a solr instance is running.
 *
 * @author richard
 */
public class SolrIndexManager {
	/**
	 * automatically generated Logger statement
	 */
	private final static org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(SolrIndexManager.class);

	private final static String ALL_DATA = "*:*";
	private final static String UPDATE = "/update";

	private SolrServer solr;

	public SolrIndexManager(String solrUrl) {
		initSolr(solrUrl);
	}

	/**
	 * Connects to solr and deletes all data from index.
	 *
	 * @param solrUrl
	 */
	private void initSolr(String solrUrl) {
		try {
			solr = new CommonsHttpSolrServer(solrUrl);
			deleteIndex();
		} catch(MalformedURLException e) {
			throw new RuntimeException("Malformed url while initializing solr!", e);
		}
	}

	/**
	 * Deletes all data from the solr index.
	 */
	public void deleteIndex() {
		try {
			solr.deleteByQuery(ALL_DATA); // delete all data from index
			solr.commit();
		} catch(SolrServerException e) {
			throw new RuntimeException("Error while deleting solr index!", e);
		} catch(IOException e) {
			throw new RuntimeException("Error while deleting solr index!", e);
		}
	}

	/**
	 * Updates solr index with data in file specified by filename.
	 *
	 * @param filename Name of file which contains solr data.
	 */
	public void updateIndex(String filename) {
		try {
			ContentStreamUpdateRequest up = new ContentStreamUpdateRequest(UPDATE);
			URL resourceUrl = getClass().getResource(filename);
			ContentStream contentStream = new ContentStreamBase.URLStream(resourceUrl);
			up.addContentStream(contentStream);
			up.setAction(ACTION.OPTIMIZE, true, true);
			solr.request(up);
			solr.commit();
		} catch(SolrServerException e) {
			throw new RuntimeException("Error while updating solr index!", e);
		} catch(IOException e) {
			throw new RuntimeException("Error while updating solr index!", e);
		}
	}

	/**
	 * Sends query to solr and prints out the result.
	 *
	 * @param query The query
	 */
	public void querySolr(String query) {
		try {
			QueryResponse rsp = solr.query(new SolrQuery(query));
			System.out.println(rsp);
		} catch(SolrServerException e) {
			LOGGER.error("Cannot query solr!", e);
		}
	}

	public static void main(String[] args) {
		SolrIndexManager indexer = new SolrIndexManager("http://localhost:8081/solr/");
		indexer.updateIndex("/solr_small.xml");
		indexer.updateIndex("/solr_small2.xml");
		indexer.querySolr(ALL_DATA);
	}
}
