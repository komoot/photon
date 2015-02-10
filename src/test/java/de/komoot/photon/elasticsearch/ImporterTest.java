package de.komoot.photon.elasticsearch;

import de.komoot.photon.ESBaseTester;
import de.komoot.photon.PhotonDoc;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Karich
 */
public class ImporterTest extends ESBaseTester {

	@Before
	public void setUp() throws IOException {
		setUpES();
		deleteAll();
	}

	@Test
	public void testAdd() {
		Map<String, String> nameMap = new HashMap<String, String>();
		nameMap.put("name", "testing");

		PhotonDoc doc = PhotonDoc.create(1, "way", 1, nameMap);
		Importer instance = new Importer(getClient(), "en"); // hardcoded lang
		instance.add(doc);
		instance.finish();

		refresh();
		assertEquals(1L, instance.count());

		final Searcher searcher = new Searcher(getClient());
		final List<JSONObject> results = searcher.search("testing", "en", null, null,null,null, 10, true);
		assertEquals(1, results.size());
	}
}
