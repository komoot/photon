package de.komoot.photon.api;

import de.komoot.photon.Importer;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.locationtech.jts.io.WKTReader;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;

/**
 * These test connect photon to an already running ES node (setup in ESBaseTester) so that we can directly test the API
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ApiIntegrationTest extends ApiBaseTester {
    private static final Date TEST_DATE = new Date();

    @BeforeAll
    void setUp(@TempDir Path dataDirectory) throws Exception {
        getProperties().setSupportGeometries(true);
        getProperties().setImportDate(TEST_DATE);
        setUpES(dataDirectory);
        Importer instance = makeImporter();
        instance.add(List.of(createDoc(13.38886, 52.51704, 1000, 1000, "place", "city").importance(0.6)));
        instance.add(List.of(createDoc(13.39026, 52.54714, 1001, 1001, "place", "town").importance(0.3)));
        instance.add(List.of(createDoc(13.39026, 52.54714, 1002, 1002, "place", "city").importance(0.3).names(Collections.singletonMap("name", "linestring")).geometry(new WKTReader().read("LINESTRING (30 10, 10 30, 40 40)"))));
        instance.finish();
        refresh();
    }

    @AfterAll
    @Override
    public void tearDown() throws IOException {
        super.tearDown();
    }

    /**
     * Test that the Access-Control-Allow-Origin header is not set
     */
    @Test
    void testNoCors() throws Exception {
        startAPI();

        assertNull(connect("/api?q=berlin").getHeaderField("Access-Control-Allow-Origin"));
    }

    /**
     * Test that the Access-Control-Allow-Origin header is set to *
     */
    @Test
    void testCorsAny() throws Exception {
        startAPI("-cors-any");

        var connection = connect("/api?q=berlin");
        assertEquals("*", connection.getHeaderField("Access-Control-Allow-Origin"));
    }

    /**
     * Test that the Access-Control-Allow-Origin header is set to a specific domain
     */
    @Test
    void testCorsOriginIsSetToSpecificDomain() throws Exception {
        startAPI("-cors-origin", "www.poole.ch");

        var connection = connect("/api?q=berlin");
        assertEquals("www.poole.ch", connection.getHeaderField("Access-Control-Allow-Origin"));
    }

    /*
     * Test that the Access-Control-Allow-Origin header is set to the matching domain
     */
    @Test
    void testCorsOriginIsSetToMatchingDomain() throws Exception {
        startAPI("-cors-origin", "www.poole.ch,alt.poole.ch");

        String[] origins = {"www.poole.ch", "alt.poole.ch"};
        for (String origin: origins) {
            var connection = connect("/api?q=berlin");
            connection.setRequestProperty("Origin", origin);
            connection.connect();

            assertEquals(origin, connection.getRequestProperty("Origin"));
            assertEquals(origin, connection.getHeaderField("Access-Control-Allow-Origin"));
        }
    }

    /*
     * Test that the Access-Control-Allow-Origin header does not return mismatching origins
     */
    @Test
    void testMismatchedCorsOriginsAreBlock() throws Exception {
        startAPI("-cors-origin", "www.poole.ch,alt.poole.ch");

        String[] origins = {"www.randomsite.com", "www.arbitrary.com"};
        for (String origin: origins) {
            var connection = connect("/api?q=berlin");
            connection.setRequestProperty("Origin", origin);
            connection.connect();

            assertEquals("www.poole.ch", connection.getHeaderField("Access-Control-Allow-Origin"));
        }
    }

    @ParameterizedTest
    @CsvSource({
            "city, /api?q=berlin&limit=1",                                    // basic search
            "town, /api?q=berlin&limit=1&lat=52.54714&lon=13.39026&zoom=16",  // search with location bias
            "city, /api?q=berlin&limit=1&lat=52.54714&lon=13.39026&zoom=12&location_bias_scale=0.6",  // search with large location bias
            "city, /reverse/?lon=13.38886&lat=52.51704" // basic reverse
    })
    void testApi(String osmValue, String url) throws Exception {
        startAPI();

        assertThatJson(readURL(url)).isObject()
                .node("features").isArray().hasSize(1)
                .element(0).isObject()
                .node("properties").isObject()
                .containsEntry("osm_type", "W")
                .containsEntry("osm_key", "place")
                .containsEntry("osm_value", osmValue)
                .containsEntry("name", "berlin");
    }


    @Test
    void testApiStatus() throws Exception {
        startAPI();

        assertThatJson(readURL("/status")).isObject()
                .containsEntry("status", "Ok")
                .containsEntry("import_date", TEST_DATE.toInstant().toString());
    }

    @Test
    void testSearchAndGetGeometry() throws Exception {
        startAPI();

        var obj = assertThatJson(readURL("/api?q=berlin&limit=1"))
                .isObject()
                .containsEntry("type", "FeatureCollection")
                .node("features").isArray().hasSize(1)
                .element(0).isObject();

        obj.node("geometry").isObject()
                .containsEntry("type", "Polygon");

        obj.node("properties").isObject()
                .containsEntry("osm_type", "W")
                .containsEntry("osm_key", "place")
                .containsEntry("osm_value", "city")
                .containsEntry("name", "berlin");

    }

    @Test
    void testSearchAndGetLineString() throws Exception {
        startAPI();

        var obj = assertThatJson(readURL("/api?q=linestring&limit=1"))
                .isObject()
                .containsEntry("type", "FeatureCollection")
                .node("features").isArray().hasSize(1)
                .element(0).isObject();

        obj.node("geometry").isObject()
           .containsEntry("type", "LineString")
                .node("coordinates").isArray()
                .containsExactly(List.of(30, 10), List.of(10, 30), List.of(40, 40));

        obj.node("properties").isObject()
                .containsEntry("osm_type", "W")
                .containsEntry("osm_key", "place")
                .containsEntry("osm_value", "city")
                .containsEntry("name", "linestring");
    }
}
