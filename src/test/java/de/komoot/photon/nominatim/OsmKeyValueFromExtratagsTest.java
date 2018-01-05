package de.komoot.photon.nominatim;

import de.komoot.photon.PhotonDoc;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.List;

/**
 * date: 13.02.15
 *
 * @author christoph
 */
@Slf4j
@Ignore // test is ignored because it depends on a global nominatim database
public class OsmKeyValueFromExtratagsTest {

    private NominatimConnector connector;

    @Before
    public void setUp() {
        connector = new NominatimConnector("localhost", 10044, "nominatim", "username", "password");
    }

    @Test
    public void testLiverpool() {
        List<PhotonDoc> docs = connector.readDocument(172987, 'R');
        final PhotonDoc doc = docs.get(0);

        // check that administrative=boundary got replaced by the more specific place=city (from extratags)
        assertEquals(doc.getTagKey(), "place");
        assertEquals(doc.getTagValue(), "city");
    }

    @Test
    public void testHorhausen() {
        List<PhotonDoc> docs = connector.readDocument(537200, 'R');
        final PhotonDoc doc = docs.get(0);

        assertEquals(doc.getTagKey(), "place");
        assertEquals(doc.getTagValue(), "village");
    }

    @Test
    public void testHelsinki() {
        List<PhotonDoc> docs = connector.readDocument(34914, 'R');
        final PhotonDoc doc = docs.get(0);

        assertEquals(doc.getTagKey(), "place");
        assertEquals(doc.getTagValue(), "city");
    }

    @Test
    public void testBailleul() {
        List<PhotonDoc> docs = connector.readDocument(1135408, 'R');
        final PhotonDoc doc = docs.get(0);

        assertEquals(doc.getTagKey(), "place");
        assertEquals(doc.getTagValue(), "village");
    }
}
