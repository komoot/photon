package de.komoot.photon.nominatim;

import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;
import de.komoot.photon.PhotonDoc;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

public class NominatimResultTest {
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

    private void assertSimpleOnly(List<PhotonDoc> docs) {
        assertEquals(1, docs.size());
        assertSame(simpleDoc, docs.get(0));
    }

    @Test
    public void testIsUsefulForIndex() {
        assertFalse(simpleDoc.isUsefulForIndex());
        assertFalse(new NominatimResult(simpleDoc).isUsefulForIndex());
    }

    @Test
    public void testGetDocsWithHousenumber() {
        List<PhotonDoc> docs = new NominatimResult(simpleDoc).getDocsWithHousenumber();
        assertSimpleOnly(docs);
    }

    @Test
    public void testAddHousenumbersFromStringSimple() {
        NominatimResult res = new NominatimResult(simpleDoc);
        res.addHousenumbersFromString("34");

        assertDocWithHousenumbers(Arrays.asList("34"), res.getDocsWithHousenumber());
    }

    @Test
    public void testAddHousenumbersFromStringList() {
        NominatimResult res = new NominatimResult(simpleDoc);
        res.addHousenumbersFromString("34; 50b");

        assertDocWithHousenumbers(Arrays.asList("34", "50b"), res.getDocsWithHousenumber());

        res.addHousenumbersFromString("4;");
        assertDocWithHousenumbers(Arrays.asList("34", "50b", "4"), res.getDocsWithHousenumber());
    }

    @Test
    public void testAddHouseNumbersFromInterpolationBad() throws ParseException {
        NominatimResult res = new NominatimResult(simpleDoc);

        WKTReader reader = new WKTReader();
        res.addHouseNumbersFromInterpolation(34, 33, "odd",
                                              reader.read("LINESTRING(0.0 0.0 ,0.0 0.1)"));
        assertSimpleOnly(res.getDocsWithHousenumber());

        res.addHouseNumbersFromInterpolation(1, 10000, "odd",
                reader.read("LINESTRING(0.0 0.0 ,0.0 0.1)"));
        assertSimpleOnly(res.getDocsWithHousenumber());
    }

    @Test
    public void testAddHouseNumbersFromInterpolationOdd() throws ParseException {
        NominatimResult res = new NominatimResult(simpleDoc);

        WKTReader reader = new WKTReader();

        res.addHouseNumbersFromInterpolation(1, 5, "odd",
                reader.read("LINESTRING(0.0 0.0 ,0.0 0.1)"));
        assertDocWithHousenumbers(Arrays.asList("3"), res.getDocsWithHousenumber());
        res.addHouseNumbersFromInterpolation(10, 13, "odd",
                reader.read("LINESTRING(0.0 0.0 ,0.0 0.1)"));
        assertDocWithHousenumbers(Arrays.asList("3", "11"), res.getDocsWithHousenumber());

        res.addHouseNumbersFromInterpolation(101, 106, "odd",
                reader.read("LINESTRING(0.0 0.0 ,0.0 0.1)"));
        assertDocWithHousenumbers(Arrays.asList("3", "11", "103", "105"), res.getDocsWithHousenumber());

    }

    @Test
    public void testAddHouseNumbersFromInterpolationEven() throws ParseException {
        NominatimResult res = new NominatimResult(simpleDoc);

        WKTReader reader = new WKTReader();

        res.addHouseNumbersFromInterpolation(1, 5, "even",
                reader.read("LINESTRING(0.0 0.0 ,0.0 0.1)"));
        assertDocWithHousenumbers(Arrays.asList("2", "4"), res.getDocsWithHousenumber());

        res.addHouseNumbersFromInterpolation(10, 16, "even",
                reader.read("LINESTRING(0.0 0.0 ,0.0 0.1)"));
        assertDocWithHousenumbers(Arrays.asList("2", "4", "12", "14"), res.getDocsWithHousenumber());

        res.addHouseNumbersFromInterpolation(51, 52, "even",
                reader.read("LINESTRING(0.0 0.0 ,0.0 0.1)"));
        assertDocWithHousenumbers(Arrays.asList("2", "4", "12", "14"), res.getDocsWithHousenumber());
    }

    @Test
    public void testAddHouseNumbersFromInterpolationAll() throws ParseException {
        NominatimResult res = new NominatimResult(simpleDoc);

        WKTReader reader = new WKTReader();

        res.addHouseNumbersFromInterpolation(1, 3, "",
                reader.read("LINESTRING(0.0 0.0 ,0.0 0.1)"));
        assertDocWithHousenumbers(Arrays.asList("2"), res.getDocsWithHousenumber());

        res.addHouseNumbersFromInterpolation(22, 22, null,
                reader.read("LINESTRING(0.0 0.0 ,0.0 0.1)"));
        assertDocWithHousenumbers(Arrays.asList("2"), res.getDocsWithHousenumber());

        res.addHouseNumbersFromInterpolation(100, 106, "all",
                reader.read("LINESTRING(0.0 0.0 ,0.0 0.1)"));
        assertDocWithHousenumbers(Arrays.asList("2", "101", "102", "103", "104", "105"), res.getDocsWithHousenumber());
    }

}