package de.komoot.photon.elasticsearch;

import com.google.common.collect.ImmutableMap;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.PrecisionModel;
import de.komoot.photon.ESBaseTester;
import de.komoot.photon.PhotonDoc;
import org.json.JSONObject;
import org.junit.*;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.*;

/**
 * regional names can be queried
 *
 * @author Christoph
 */
public class RegionalNameTest extends ESBaseTester {
	GeometryFactory FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

	@Before
	public void setUp() throws IOException {
		setUpES();
		deleteAll();

		PhotonDoc doc = this.createDoc(1, "Hawaii", "Big Island");
		Importer instance = new Importer(getClient(), "en");
		instance.add(doc);
		instance.finish();

		refresh();
	}

	private PhotonDoc createDoc(int id, String name, String regName) {
		return new PhotonDoc(id, "way", id, "highway", "primary", ImmutableMap.of("name", name, "reg_name", regName), null, null, null, 0, 0.5, null, FACTORY.createPoint(new Coordinate(10., 47.)), 0, 0);
	}

	@Test
	public void test() {
		final Searcher searcher = new Searcher(getClient());

		assertId(searcher.search("hawaii", "en", null, null, null,null,10, false), 1);
		assertId(searcher.search("big island", "en", null, null, null,null,10, false), 1);
	}

	private void assertId(List<JSONObject> results, long expectedFirstId) {
		assertTrue("results are empty", results.size() > 0);
		long osmId = results.get(0).getJSONObject("properties").getLong("osm_id");
		assertEquals("wrong first results", expectedFirstId, osmId);
	}
}
