package de.komoot.photon.nominatim;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import de.komoot.photon.PhotonDoc;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class NominatimResultTest {
    private final PhotonDoc simpleDoc = new PhotonDoc(10000, "N", 123, "place", "house")
                                                .countryCode("de");

    private void assertDocWithHousenumbers(List<String> housenumbers, List<PhotonDoc> docs) {
        assertEquals(housenumbers.size(), docs.size());

        List<String> outnumbers = new ArrayList<>();

        for (PhotonDoc doc: docs) {
            assertNotSame(simpleDoc, doc);
            assertEquals("place", doc.getTagKey());
            assertEquals("house", doc.getTagValue());
            assertEquals(10000, doc.getPlaceId());
            assertEquals("N", doc.getOsmType());
            assertEquals(123, doc.getOsmId());

            outnumbers.add(doc.getHouseNumber());
        }

        Collections.sort(outnumbers);
        Collections.sort(housenumbers);

        assertEquals(housenumbers, outnumbers);
    }

    private void assertNoHousenumber(List<PhotonDoc> docs) {
        assertEquals(1, docs.size());
        assertNull(docs.get(0).getHouseNumber());
    }

    private void assertSimpleOnly(List<PhotonDoc> docs) {
        assertEquals(1, docs.size());
        assertSame(simpleDoc, docs.get(0));
    }

    private Map<String, String> housenumberAddress(String housenumber) {
        Map<String, String> address = new HashMap<>(1);
        address.put("housenumber", housenumber);
        return address;
    }

    @Test
    void testIsUsefulForIndex() {
        assertFalse(simpleDoc.isUsefulForIndex());
        assertFalse(NominatimResult.fromAddress(simpleDoc, null).isUsefulForIndex());
    }

    @Test
    void testGetDocsWithHousenumber() {
        List<PhotonDoc> docs = NominatimResult.fromAddress(simpleDoc, null).getDocsWithHousenumber();
        assertSimpleOnly(docs);
    }

    @Test
    void testAddHousenumbersFromStringSimple() {
        NominatimResult res = NominatimResult.fromAddress(simpleDoc, housenumberAddress("34"));

        assertDocWithHousenumbers(Arrays.asList("34"), res.getDocsWithHousenumber());
    }

    @Test
    void testAddHousenumbersFromStringList() {
        NominatimResult res = NominatimResult.fromAddress(simpleDoc, housenumberAddress("34; 50b"));

        assertDocWithHousenumbers(Arrays.asList("34", "50b"), res.getDocsWithHousenumber());

        res = NominatimResult.fromAddress(simpleDoc, housenumberAddress("4;"));
        assertDocWithHousenumbers(Arrays.asList("4"), res.getDocsWithHousenumber());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "987987誰も住んでいないスーパーマーケット誰も住んでいないスーパーマーケット誰も住んでいないスーパーマーケット誰も住んでいないスーパーマーケット誰も住んでいないスーパーマーケット誰も住んでいないスーパーマーケット誰も住んでいないスーパーマーケット誰も住んでいないスーパーマーケット誰も住んでいないスーパーマー",
            "something bad",
            "14, portsmith"
    })
    void testLongHousenumber(String houseNumber) {
        NominatimResult res = NominatimResult.fromAddress(simpleDoc, housenumberAddress(houseNumber));

        assertNoHousenumber(res.getDocsWithHousenumber());
    }

    @Test
    void testAddHouseNumbersFromInterpolationBad() throws ParseException {
        WKTReader reader = new WKTReader();
        NominatimResult res = NominatimResult.fromInterpolation(simpleDoc, 34, 33, "odd",
                                              reader.read("LINESTRING(0.0 0.0 ,0.0 0.1)"));
        assertSimpleOnly(res.getDocsWithHousenumber());

        res = NominatimResult.fromInterpolation(simpleDoc, 1, 10000, "odd",
                reader.read("LINESTRING(0.0 0.0 ,0.0 0.1)"));
        assertSimpleOnly(res.getDocsWithHousenumber());
    }

    @Test
    void testAddHouseNumbersFromInterpolationOdd() throws ParseException {
        WKTReader reader = new WKTReader();
        NominatimResult res = NominatimResult.fromInterpolation(simpleDoc, 1, 5, "odd",
                reader.read("LINESTRING(0.0 0.0 ,0.0 0.1)"));
        assertDocWithHousenumbers(Arrays.asList("3"), res.getDocsWithHousenumber());
        res = NominatimResult.fromInterpolation(simpleDoc, 10, 13, "odd",
                reader.read("LINESTRING(0.0 0.0 ,0.0 0.1)"));
        assertDocWithHousenumbers(Arrays.asList("11"), res.getDocsWithHousenumber());

        res = NominatimResult.fromInterpolation(simpleDoc, 101, 106, "odd",
                reader.read("LINESTRING(0.0 0.0 ,0.0 0.1)"));
        assertDocWithHousenumbers(Arrays.asList("103", "105"), res.getDocsWithHousenumber());

    }

    @Test
    void testAddHouseNumbersFromInterpolationEven() throws ParseException {
        WKTReader reader = new WKTReader();
        NominatimResult res = NominatimResult.fromInterpolation(simpleDoc, 1, 5, "even",
                reader.read("LINESTRING(0.0 0.0 ,0.0 0.1)"));
        assertDocWithHousenumbers(Arrays.asList("2", "4"), res.getDocsWithHousenumber());

        res= NominatimResult.fromInterpolation(simpleDoc, 10, 16, "even",
                reader.read("LINESTRING(0.0 0.0 ,0.0 0.1)"));
        assertDocWithHousenumbers(Arrays.asList("12", "14"), res.getDocsWithHousenumber());

        res= NominatimResult.fromInterpolation(simpleDoc, 51, 52, "even",
                reader.read("LINESTRING(0.0 0.0 ,0.0 0.1)"));
        assertSimpleOnly(res.getDocsWithHousenumber());
    }

    @Test
    void testAddHouseNumbersFromInterpolationAll() throws ParseException {
        WKTReader reader = new WKTReader();
        NominatimResult res = NominatimResult.fromInterpolation(simpleDoc, 1, 3, "",
                reader.read("LINESTRING(0.0 0.0 ,0.0 0.1)"));
        assertDocWithHousenumbers(Arrays.asList("2"), res.getDocsWithHousenumber());

        res = NominatimResult.fromInterpolation(simpleDoc, 22, 22, null,
                reader.read("LINESTRING(0.0 0.0 ,0.0 0.1)"));
        assertSimpleOnly(res.getDocsWithHousenumber());

        res = NominatimResult.fromInterpolation(simpleDoc, 100, 106, "all",
                reader.read("LINESTRING(0.0 0.0 ,0.0 0.1)"));
        assertDocWithHousenumbers(Arrays.asList("101", "102", "103", "104", "105"), res.getDocsWithHousenumber());
    }

}