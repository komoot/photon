package de.komoot.photon;

import de.komoot.photon.model.SolrDocument;
import de.komoot.photon.utils.CSVLine;
import de.komoot.photon.utils.Constants;
import de.komoot.photon.utils.ResultEvaluator;

import java.util.List;

/**
 * Abstract test class. Cares about creating connection to solr, offers functionality to update the solr index, and to
 * delete the index.
 *
 * @author richard
 */
public abstract class SolrSearcherTester {
	private final static org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(SolrSearcherTester.class);

	/**
	 * Update the solr index with data specified by filename.
	 *
	 * @param solrURL
	 * @param filename Name of file which contains solr data.
	 */
	public void overrideIndex(String solrURL, String filename) {
		SolrIndexManager indexManager = new SolrIndexManager(solrURL);
		indexManager.deleteIndex();
		indexManager.updateIndex(filename);
	}

	/**
	 * returns true if search results comply test case defined in line
	 *
	 * @param results
	 * @param line
	 * @return
	 */
	public static boolean isSuccessful(List<SolrDocument> results, CSVLine line) {

		if(results.size() < 1) {
			LOGGER.warn(String.format("test failed: [%s] empty result list", line.get(Constants.COLUMNS.USER_INPUT)));
			return false;
		}

		boolean success = ResultEvaluator.hasSuccessfulResult(results, line);

		if(success) {
			return true;
		}

		int i = 0;
		LOGGER.warn(String.format("test failed: [%s]", line.get(Constants.COLUMNS.USER_INPUT)));
		for(SolrDocument r : results) {
			LOGGER.warn(r.toString());
			LOGGER.warn(ResultEvaluator.generateReports(results, line).get(i).entrySet().toString());
			i++;
		}

		return false;
	}
}
