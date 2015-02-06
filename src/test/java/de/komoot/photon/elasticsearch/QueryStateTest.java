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
 * validates that state information is queried
 *
 * @author Christoph
 */
public class QueryStateTest extends ESBaseTester {
	GeometryFactory FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

	@Before
	public void setUp() throws IOException {
		setUpES();
		deleteAll();

		PhotonDoc street1 = this.createStateDoc(1, "streetname", "bavaria");
		PhotonDoc street2 = this.createStateDoc(2, "streetname", "vorarlberg");
		PhotonDoc street3 = this.createStateDoc(3, "streetname", "berlin");

		Importer instance = new Importer(getClient(), "en"); // hardcoded lang
		instance.add(street1);
		instance.add(street2);
		instance.add(street3);
		instance.finish();
		refresh();
	}

	private PhotonDoc createStateDoc(int id, String name, String state) {
		final PhotonDoc doc = new PhotonDoc(id, "way", id, "highway", "primary", ImmutableMap.of("name", name), null, null, null, 0, 0.5, null, FACTORY.createPoint(new Coordinate(10., 47.)), 0, 0);
		doc.setState(ImmutableMap.of("name", state));
		return doc;
	}

	@Test
	public void checkState() {
		final Searcher searcher = new Searcher(getClient());

		assertId(searcher.search("streetname bavaria", "en", null, null, null,null,10, false), 1);
		assertId(searcher.search("streetname vorarlberg", "en", null, null, null,null,10, false), 2);
		assertId(searcher.search("streetname berlin", "en", null, null,null,null,10, false), 3);

		assertId(searcher.search("streetname bavaria", "en", null, null, null,null,10, true), 1);
		assertId(searcher.search("streetname vorarlberg", "en", null, null, null,null,10, true), 2);
		assertId(searcher.search("streetname berlin", "en", null, null, null,null,10, true), 3);
	}

	private void assertId(List<JSONObject> results, long expectedFirstId) {
		assertTrue("results are empty", results.size() > 0);
		long osmId = results.get(0).getJSONObject("properties").getLong("osm_id");
		assertEquals("wrong first results", expectedFirstId, osmId);
	}
}
