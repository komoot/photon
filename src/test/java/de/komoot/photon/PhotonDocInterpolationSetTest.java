package de.komoot.photon;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;

import static org.junit.jupiter.api.Assertions.*;

class PhotonDocInterpolationSetTest {
    private final PhotonDoc baseDoc = new PhotonDoc(10000, "N", 123, "place", "house")
            .countryCode("de");
    private final WKTReader reader = new WKTReader();
    private Geometry lineGeo;

    private void assertDocWithHousenumber(PhotonDoc doc, String housenumber, double y) {
        assertAll(
                () -> assertNotSame(baseDoc, doc),
                () -> assertEquals("place", doc.getTagKey()),
                () -> assertEquals("house", doc.getTagValue()),
                () -> assertEquals(10000, doc.getPlaceId()),
                () -> assertEquals("N", doc.getOsmType()),
                () -> assertEquals(123, doc.getOsmId()),
                () -> assertEquals(housenumber, doc.getHouseNumber()),
                () -> assertEquals(2.5, doc.getCentroid().getCoordinate().x, 0.00001),
                () -> assertEquals(y, doc.getCentroid().getCoordinate().y, 0.00001)
        );
    }

    @BeforeEach
    void setupGeometry() throws ParseException{
        lineGeo = reader.read("LINESTRING(2.5 0.0 ,2.5 0.1)");
    }

    @Test
    void testBadInterpolationReverse() {
        var it = new PhotonDocInterpolationSet(baseDoc, 34, 33, 1, lineGeo)
                .iterator();

        assertFalse(it.hasNext());
    }

    @Test
    void testBadInterpolationLargeMulti()  {
        var it = new PhotonDocInterpolationSet(baseDoc, 1, 2000, 1, lineGeo)
                        .iterator();

        assertFalse(it.hasNext());
    }

    @Test
    void testSinglePointInterpolation() {
        var it = new PhotonDocInterpolationSet(baseDoc, 2000, 2000, 1, lineGeo)
                        .iterator();

        var doc = it.next();
        assertAll(
                () -> assertSame(baseDoc, doc),
                () -> assertEquals("2000", doc.getHouseNumber()),
                () -> assertEquals(2.5, doc.getCentroid().getCoordinate().x, 0.00001),
                () -> assertEquals(0.05, doc.getCentroid().getCoordinate().y, 0.00001)
        );
        assertFalse(it.hasNext());
    }

    @Test
    void testSingleStepInterpolation() {
        var it = new PhotonDocInterpolationSet(baseDoc, 1, 3, 1, lineGeo)
                        .iterator();

        assertDocWithHousenumber(it.next(), "1", 0);
        assertDocWithHousenumber(it.next(), "2", 0.05);
        assertDocWithHousenumber(it.next(), "3", 0.1);
        assertFalse(it.hasNext());
    }

    @Test
    void testTwoStepInterpolation() {
        var it = new PhotonDocInterpolationSet(baseDoc, 16, 20, 2, lineGeo)
                        .iterator();

        assertDocWithHousenumber(it.next(), "16", 0);
        assertDocWithHousenumber(it.next(), "18", 0.05);
        assertDocWithHousenumber(it.next(), "20", 0.1);
        assertFalse(it.hasNext());
    }
}