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
 * @author Christoph
 */
public class RemoveStreetDuplicatesTest extends ESBaseTester {
	GeometryFactory FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

	@Before
	public void setUp() throws IOException {
		setUpES();
		deleteAll();
		PhotonDoc street1 = this.createStreetDoc(1, "Walserstraße", "6993");
		PhotonDoc street2 = this.createStreetDoc(2, "Walserstraße", "6993");
		PhotonDoc street3 = this.createStreetDoc(3, "Walserstraße", "6991");

		Importer instance = new Importer(getClient(), "en"); // hardcoded lang
		instance.add(street1);
		instance.add(street2);
		instance.add(street3);
		instance.finish();
		refresh();
	}

	private PhotonDoc createStreetDoc(int id, String name, String postcode) {
		final PhotonDoc doc = new PhotonDoc(id, "way", id, "highway", "primary", ImmutableMap.of("name", name),
				null, null, null, 0, 0.5, null,
				FACTORY.createPoint(new Coordinate(10., 47.)),
				0, 0);
		doc.setPostcode(postcode);
		return doc;
	}

	@Test
	public void checkDuplicates() {
		final Searcher searcher = new Searcher(getClient());
		final List<JSONObject> results = searcher.search("Walserstraße", "en", null, null, null,null,10, true);
		assertEquals(2, results.size());
	}
}
