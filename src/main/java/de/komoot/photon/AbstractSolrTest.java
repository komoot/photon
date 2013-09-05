package de.komoot.photon;

import de.komoot.photon.model.SolrDocument;
import de.komoot.photon.utils.CSVFile;
import de.komoot.photon.utils.CSVLine;
import de.komoot.photon.utils.CSVReader;
import de.komoot.photon.utils.Constants.COLUMNS;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.lang.LocaleUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CommonsHttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.junit.*;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Locale;

import static org.junit.Assert.*;

/**
 * Encapsulates test logic for solr-searcher tests.
 *
 * @author richard
 */
public abstract class AbstractSolrTest extends SolrSearcherTester {
	private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(AbstractSolrTest.class);
	private CommonsHttpSolrServer solrSearcher;
	private CSVFile csvFile;

	protected static String SOLR_URL = "http://christoph.komoot.de:8983/solr/";

	/**
	 * Returns filename of testdata .csv
	 *
	 * @return
	 */
	public abstract String getTestcases();

	/**
	 * Returns filename of inputdata which will be uploaded to solr.
	 *
	 * @return
	 */
	public abstract String getInputData();

	public AbstractSolrTest() {
		super();
	}

	@Before
	public void setUp() throws URISyntaxException, IOException {
		LOGGER.info("Will run tests against " + SOLR_URL);

		overrideIndex(SOLR_URL, getInputData());

		solrSearcher = getSearcher(SOLR_URL);
		InputStream resource = this.getClass().getResourceAsStream(getTestcases());
		CSVReader csvReader = new CSVReader(resource, Charset.forName("UTF-8"));
		csvFile = new CSVFile(csvReader, getTestcases());
	}

	public static CommonsHttpSolrServer getSearcher(String URL) throws MalformedURLException {
		return new CommonsHttpSolrServer(URL, new HttpClient());
	}

	@Test
	public void test() throws IOException, SolrServerException {
		int failed = 0;

		List<CSVLine> lines = csvFile.getLines();
		for(CSVLine line : lines) {
			List<SolrDocument> docs = search(line);

			LOGGER.info("testcase " + line.toString());
			if(false == isSuccessful(docs, line)) {
				failed++;
			}
		}
		if(failed > 0) {
			fail("some tests failed: " + failed + " of " + lines.size());
		}
	}

	public static List<SolrDocument> search(CSVLine line) throws SolrServerException, MalformedURLException {
		SolrQuery solrQuery = new SolrQuery();
		solrQuery.setQuery(line.get(COLUMNS.USER_INPUT));
		Locale locale = LocaleUtils.toLocale(line.get(COLUMNS.LOCALE));
		if(false == Locale.GERMAN.getLanguage().equals(locale.getLanguage())) {
			solrQuery.setParam("qt", "english");
		}

		QueryResponse response = getSearcher(SOLR_URL).query(solrQuery);
		return response.getBeans(SolrDocument.class);
	}
}