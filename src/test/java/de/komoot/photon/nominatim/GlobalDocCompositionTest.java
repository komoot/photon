package de.komoot.photon.nominatim;

import de.komoot.photon.PhotonDoc;
import lombok.extern.slf4j.Slf4j;
import org.junit.*;

import java.util.List;

import static org.junit.Assert.*;

/**
 * date: 06.02.15
 * <p/>
 * these tests checks if documents get composed correctly, they are no classical unit test as an global nominatim import is needed
 *
 * @author christoph
 */
@Slf4j
@Ignore // test is ignored because it depends on a local nominatim database with world-wide data
public class GlobalDocCompositionTest {

	private NominatimConnector connector;

	@Before
	public void setUp() throws Exception {
		connector = new NominatimConnector("localhost", 10044, "nominatim", "christoph", "christoph");
	}

	@Test
	public void testStreetErlangen() throws Exception {
		List<PhotonDoc> docs = connector.readDocument(254151521, 'W');
		assertCity(docs, "Erlangen");
	}

	@Test
	public void testStreetBerlin1() {
		List<PhotonDoc> docs = connector.readDocument(31906497, 'W');
		assertCity(docs, "Berlin");
	}

	@Test
	public void testStreetBerlin2() {
		List<PhotonDoc> docs = connector.readDocument(317350507, 'W');
		assertCity(docs, "Berlin");
	}

	@Test
	public void testRelevanceHelsinki() {
		List<PhotonDoc> docs = connector.readDocument(34914, 'R');
		assertImportance(docs, 0.7);
	}

	private void assertImportance(List<PhotonDoc> docs, double minimalImportance) {
		assertEquals("there are more or less than one document: len=" + docs.size(), docs.size(), 1);
		assertTrue(String.format("importance is smaller than required: %f < %f ", docs.get(0).getImportance(), minimalImportance), docs.get(0).getImportance() >= minimalImportance);
	}

	private void assertCity(List<PhotonDoc> docs, String expectedName) {
		assertEquals("there are more or less than one document: len=" + docs.size(), docs.size(), 1);
		assertNotNull("city name is missing", docs.get(0).getCity());
		assertEquals("city name is incorrect", expectedName, docs.get(0).getCity().get("name"));
	}
}
