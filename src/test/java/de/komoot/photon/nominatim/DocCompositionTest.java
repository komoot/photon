package de.komoot.photon.nominatim;

import de.komoot.photon.PhotonDoc;
import lombok.extern.slf4j.Slf4j;
import org.junit.*;

import java.util.List;

import static org.junit.Assert.*;

/**
 * date: 12.01.15
 *
 * @author christoph
 */
@Slf4j
@Ignore // test is ignored because it depends on a local nominatim database with some data imported see /src/test/resources/nominatim_import_test_data.sh
public class DocCompositionTest {

	private NominatimConnector connector;

	@Before
	public void setUp() throws Exception {
		connector = new NominatimConnector("localhost", 10044, "nominatim", "christoph", "christoph");
	}

	@Test
	@Ignore
	public void testMissingCity() throws Exception {
		List<PhotonDoc> docs = connector.readDocument(27683414, 'W');
		assertCity(docs, "Reykjavik");
	}

	@Test
	public void testFrauenkircheMunich() throws Exception {
		List<PhotonDoc> docs = connector.readDocument(225698612, 'W');
		assertCity(docs, "München");
		assertState(docs, "Bayern");
	}

	@Test
	public void testSelkestrasseHalle() throws Exception {
		List<PhotonDoc> docs = connector.readDocument(22634735, 'W');
		assertCity(docs, "Halle (Saale)");
		assertState(docs, "Sachsen-Anhalt");
	}

	private void assertState(List<PhotonDoc> docs, String expectedName) {
		assertEquals("there are more or less than one document: len=" + docs.size(), docs.size(), 1);
		assertNotNull("city name is missing", docs.get(0).getState());
		assertEquals("city name is incorrect", expectedName, docs.get(0).getState().get("name"));
	}

	@Test
	public void testNationalGalleryLondon() throws Exception {
		List<PhotonDoc> docs = connector.readDocument(4372002, 'W');
		assertCity(docs, "London");
	}

	private void assertCity(List<PhotonDoc> docs, String expectedName) {
		assertEquals("there are more or less than one document: len=" + docs.size(), docs.size(), 1);
		assertNotNull("city name is missing", docs.get(0).getCity());
		assertEquals("city name is incorrect", expectedName, docs.get(0).getCity().get("name"));
	}
}
