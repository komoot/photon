package de.komoot.search;

import de.komoot.search.model.SolrDocument;
import de.komoot.search.utils.CSVLine;
import de.komoot.search.utils.CSVLoader;
import de.komoot.search.utils.Constants;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CommonsHttpSolrServer;
import org.junit.*;
import org.junit.runner.*;
import org.junit.runners.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static de.komoot.search.SolrSearcherTester.isSuccessful;
import static org.junit.Assert.*;

/**
 * @author christoph
 * @date 15.08.13
 */

@RunWith(Parameterized.class)
public class LiveSolrTest {
	private final static Logger LOGGER = LoggerFactory.getLogger(LiveSolrTest.class);

	private final String SOLR_URL = "http://christoph.komoot.de:8983/solr/";

	private final String testCase;
	private CommonsHttpSolrServer searcher;

	@Before
	public void initialize() throws MalformedURLException {
		searcher = AbstractSolrTest.getSearcher(SOLR_URL);
	}

	public LiveSolrTest(String testCase) {
		this.testCase = testCase;
	}

	@Parameterized.Parameters(name = "{0}.csv")
	public static Collection testCases() {
		return Arrays.asList(new String[][]{
				{"dircksenstrasse51"},
				{"berlin"}
		});
	}

	@Test
	public void test() throws IOException, URISyntaxException, SolrServerException {
		LOGGER.info("running live test of " + this.testCase);

		CSVLoader csvLoader = new CSVLoader("/testcases/" + testCase + ".csv");
		List<CSVLine> lines = csvLoader.getTest().getLines();

		boolean success = true;
		for(CSVLine line : lines) {
			LOGGER.info(String.format("search for [%s]", line.get(Constants.COLUMNS.USER_INPUT)));
			List<SolrDocument> results = AbstractSolrTest.search(line);
			success = success && isSuccessful(results, line);
		}

		if(success == false) {
			fail("some tests failed: ");
		}
	}
}