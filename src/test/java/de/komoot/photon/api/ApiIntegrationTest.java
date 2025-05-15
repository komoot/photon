package de.komoot.photon.api;

import de.komoot.photon.Importer;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.FieldSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.locationtech.jts.io.WKTReader;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;

/**
 * These test connect photon to an already running ES node (setup in ESBaseTester) so that we can directly test the API
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ApiIntegrationTest extends ApiBaseTester {
    private static final String[] BASE_URLS = {"/api?q=Berlin", "/reverse?lat=52.54714&lon=13.39026"};
    private static final Date TEST_DATE = new Date();

    @BeforeAll
    void setUp(@TempDir Path dataDirectory) throws Exception {
        getProperties().setSupportGeometries(true);
        getProperties().setImportDate(TEST_DATE);
        setUpES(dataDirectory);
        Importer instance = makeImporter();
        instance.add(List.of(createDoc(13.38886, 52.51704, 1000, 1000, "place", "city")
                .importance(0.6)
                .rankAddress(16)));
        instance.add(List.of(createDoc(13.39026, 52.54714, 1001, 1001, "place", "suburb")
                .importance(0.3)
                .rankAddress(17)));
        instance.add(List.of(createDoc(13.39026, 52.54714, 1002, 1002, "place", "hamlet")
                .importance(0.3)
                .rankAddress(25)
                .geometry(new WKTReader().read("LINESTRING (30 10, 10 30, 40 40)"))));
        instance.finish();
        refresh();
    }

    @AfterAll
    @Override
    public void tearDown() throws IOException {
        shutdownES();
    }

    /**
     * Test that the Access-Control-Allow-Origin header is not set
     */
    @ParameterizedTest
    @FieldSource("BASE_URLS")
    void testNoCors(String baseUrl) throws Exception {
        startAPI();

        var connection = connect(baseUrl);
        connection.connect();

        assertThat(connection.getHeaderField("Access-Control-Allow-Origin"))
                .isNull();
    }

    /**
     * Test that the Access-Control-Allow-Origin header is set to *
     */
    @ParameterizedTest
    @FieldSource("BASE_URLS")
    void testCorsAny(String baseUrl) throws Exception {
        startAPI("-cors-any");

        var connection = connect(baseUrl);
        connection.connect();

        assertThat(connection.getHeaderField("Access-Control-Allow-Origin"))
                .isEqualTo("*");
    }

    /**
     * Test that the Access-Control-Allow-Origin header is set to a specific domain
     */
    @ParameterizedTest
    @FieldSource("BASE_URLS")
    void testCorsOriginIsSetToSpecificDomain(String baseUrl) throws Exception {
        startAPI("-cors-origin", "www.poole.ch");

        var connection = connect(baseUrl);
        connection.connect();

        assertThat(connection.getHeaderField("Access-Control-Allow-Origin"))
                .isEqualTo("www.poole.ch");
    }

    /*
     * Test that the Access-Control-Allow-Origin header is set to the matching domain
     */
    @ParameterizedTest
    @FieldSource("BASE_URLS")
    void testCorsOriginIsSetToMatchingDomain(String baseUrl) throws Exception {
        startAPI("-cors-origin", "www.poole.ch,alt.poole.ch");

        String[] origins = {"www.poole.ch", "alt.poole.ch"};
        for (String origin : origins) {
            var connection = connect(baseUrl);
            connection.setRequestProperty("Origin", origin);
            connection.connect();

            assertThat(connection.getHeaderField("Access-Control-Allow-Origin"))
                    .isEqualTo(origin);
        }
    }

    /*
     * Test that the Access-Control-Allow-Origin header does not return mismatching origins
     */
    @ParameterizedTest
    @FieldSource("BASE_URLS")
    void testMismatchedCorsOriginsAreBlock(String baseUrl) throws Exception {
        startAPI("-cors-origin", "www.poole.ch,alt.poole.ch");

        var connection = connect(baseUrl);
        connection.setRequestProperty("Origin", "www.randomsite.com");
        connection.connect();

        assertThat(connection.getHeaderField("Access-Control-Allow-Origin"))
                .isEqualTo("www.poole.ch");
    }

    @ParameterizedTest
    @FieldSource("BASE_URLS")
    void testBogus(String baseUrl) throws Exception {
        startAPI();
        assertHttpError(baseUrl + "&bogus=thing", 400);
    }

    @ParameterizedTest
    @CsvSource({
            "city, /api?q=berlin&limit=1",                                    // basic search
            "suburb, /api?q=berlin&limit=1&lat=52.54714&lon=13.39026&zoom=16",  // search with location bias
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

    @ParameterizedTest
    @FieldSource("BASE_URLS")
    void testSearchAndGetGeometry(String baseUrl) throws Exception {
        startAPI();

        var obj = assertThatJson(readURL(baseUrl + "&geometry=true&layer=district"))
                .isObject()
                .containsEntry("type", "FeatureCollection")
                .node("features").isArray().hasSize(1)
                .element(0).isObject();

        obj.node("geometry").isObject()
                .containsEntry("type", "Polygon");

        obj.node("properties").isObject()
                .containsEntry("osm_type", "W")
                .containsEntry("osm_key", "place")
                .containsEntry("osm_value", "suburb")
                .containsEntry("name", "berlin");

    }

    @ParameterizedTest
    @FieldSource("BASE_URLS")
    void testSearchAndGetLineString(String baseUrl) throws Exception {
        startAPI();

        var obj = assertThatJson(readURL(baseUrl + "&geometry=true&layer=locality"))
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
                .containsEntry("osm_value", "hamlet")
                .containsEntry("name", "berlin");
    }

    @ParameterizedTest
    @FieldSource("BASE_URLS")
    void testSearchWithoutGeometry(String baseUrl) throws Exception {
        startAPI();

        assertThatJson(readURL(baseUrl)).isObject()
                .node("features").isArray()
                .element(0).isObject()
                .node("geometry").isObject()
                .containsEntry("type", "Point");
    }

    @ParameterizedTest
    @FieldSource("BASE_URLS")
    void testLimitParameter(String baseUrl) throws Exception {
        startAPI();

        assertThatJson(readURL(baseUrl + "&limit=20")).isObject()
                .node("features").isArray()
                .hasSizeGreaterThan(1);
        assertThatJson(readURL(baseUrl + "&limit=1")).isObject()
                .node("features").isArray()
                .hasSize(1);
    }

    @ParameterizedTest
    @FieldSource("BASE_URLS")
    void testBadLimitParameter(String baseUrl) throws Exception {
        startAPI();
        assertHttpError(baseUrl + "&limit=NaN", 400);
    }

    @ParameterizedTest
    @FieldSource("BASE_URLS")
    void testDebugOutput(String baseUrl) throws Exception {
        startAPI();

        assertThatJson(readURL(baseUrl + "&debug=1")).isObject()
                .node("properties").isObject()
                .containsKeys("debug", "raw_data");
    }

    @ParameterizedTest
    @FieldSource("BASE_URLS")
    void testSimpleLayer(String baseUrl) throws Exception {
        startAPI();

        assertThatJson(readURL(baseUrl + "&layer=locality")).isObject()
                .node("features").isArray().hasSize(1)
                .element(0).isObject()
                .node("properties").isObject()
                .containsEntry("osm_value", "hamlet");
    }

    @ParameterizedTest
    @FieldSource("BASE_URLS")
    void testManyLayers(String baseUrl) throws Exception {
        startAPI();

        assertThatJson(readURL(baseUrl + "&layer=locality&layer=district")).isObject()
                .node("features").isArray().hasSize(2);
    }

    @ParameterizedTest
    @FieldSource("BASE_URLS")
    void testBadLayerParameter(String baseUrl) throws Exception {
        startAPI();
        assertHttpError(baseUrl + "&layer=locality&layer=suburb", 400);
    }

    @ParameterizedTest
    @FieldSource("BASE_URLS")
    void testOsmKeyFilter(String baseUrl) throws Exception {
        startAPI();
        assertThatJson(readURL(baseUrl + "&osm_tag=place"))
                .node("features").isArray().hasSizeGreaterThan(1);
    }

    @ParameterizedTest
    @FieldSource("BASE_URLS")
    void testOsmKeyFilterNoMatch(String baseUrl) throws Exception {
        startAPI();
        assertThatJson(readURL(baseUrl + "&osm_tag=highway"))
                .node("features").isArray().hasSize(0);
    }

    @ParameterizedTest
    @FieldSource("BASE_URLS")
    void testOsmKeyFilterNeg(String baseUrl) throws Exception {
        startAPI();
        assertThatJson(readURL(baseUrl + "&osm_tag=!place"))
                .node("features").isArray().hasSize(0);
    }

    @ParameterizedTest
    @FieldSource("BASE_URLS")
    void testOsmValueFilter(String baseUrl) throws Exception {
        startAPI();

        assertThatJson(readURL(baseUrl + "&osm_tag=:hamlet")).isObject()
                .node("features").isArray().hasSize(1)
                .element(0).isObject()
                .node("properties").isObject()
                .containsEntry("osm_value", "hamlet");
    }

    @ParameterizedTest
    @FieldSource("BASE_URLS")
    void testOsmValueFilterNeg(String baseUrl) throws Exception {
        startAPI();

        assertThatJson(readURL(baseUrl + "&layer=locality&layer=district&osm_tag=:!hamlet")).isObject()
                .node("features").isArray().hasSize(1)
                .element(0).isObject()
                .node("properties").isObject()
                .containsEntry("osm_value", "suburb");
    }

    @ParameterizedTest
    @FieldSource("BASE_URLS")
    void testOsmTagFilter(String baseUrl) throws Exception {
        startAPI();

        assertThatJson(readURL(baseUrl + "&osm_tag=place:hamlet")).isObject()
                .node("features").isArray().hasSize(1)
                .element(0).isObject()
                .node("properties").isObject()
                .containsEntry("osm_value", "hamlet");
    }

    @ParameterizedTest
    @FieldSource("BASE_URLS")
    void testOsmTagFilterNeg(String baseUrl) throws Exception {
        startAPI();

        assertThatJson(readURL(baseUrl + "&layer=locality&layer=district&osm_tag=!place:hamlet")).isObject()
                .node("features").isArray().hasSize(1)
                .element(0).isObject()
                .node("properties").isObject()
                .containsEntry("osm_value", "suburb");
    }

    @ParameterizedTest
    @FieldSource("BASE_URLS")
    void testBadOsmTagParameter(String baseUrl) throws Exception {
        startAPI();
        assertHttpError(baseUrl + "&osm_tag=bad:bad:bad", 400);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "lat=52.54714", "lon=13.39026", "radius=1.0",
            "lat=52.54714&lon=NaN", "lat=Nan&lon=13.39026",
            "lat=52.54714&lon=bad", "lat=bad&lon=13.39026",
            "lat=52.54714&lon=Infinity", "lat=Infinity&lon=13.39026",
            "lat=52.54714&lon=-Infinity", "lat=-Infinity&lon=13.39026",
            "lat=52.54714&lon=", "lat=&lon=13.39026",
            "lat=52.54714&lon=180.01", "lat=90.01&lon=13.39026",
            "lat=52.54714&lon=-180.01", "lat=-90.01&lon=13.39026"
    })
    void testReverseBadLocation(String param) throws Exception {
        startAPI();
        assertHttpError("/reverse?" + param, 400);
    }

    @ParameterizedTest
    @ValueSource(strings = {"bad", "NaN", "0.0", "-10.0"})
    void testReverseBadRadius(String param) throws Exception {
        startAPI();
        assertHttpError("/reverse?lat=52.54714&lon=13.39026&radius=" + param, 400);
    }

    @Test
    void testSearchMissingQuery() throws Exception {
        startAPI();
        assertHttpError("/api?debug=1", 400);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "lat=52.54714", "lon=13.39026",
            "lat=52.54714&lon=NaN", "lat=Nan&lon=13.39026",
            "lat=52.54714&lon=bad", "lat=bad&lon=13.39026",
            "lat=52.54714&lon=Infinity", "lat=Infinity&lon=13.39026",
            "lat=52.54714&lon=-Infinity", "lat=-Infinity&lon=13.39026",
            "lat=52.54714&lon=", "lat=&lon=13.39026",
            "lat=52.54714&lon=180.01", "lat=90.01&lon=13.39026",
            "lat=52.54714&lon=-180.01", "lat=-90.01&lon=13.39026"
    })
    void testSearchBadLocation(String param) throws Exception {
        startAPI();
        assertHttpError("/api?q=berlin&" + param, 400);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "9.6,52.3,9.8", "9.6,52.3,NaN,9.8",
            "9.6,52.3,-Infinity,9.8", "9.6,52.3,r34,9.8",
            "9.6,-92,9.8,14", "9.6,14,9.8,91",
            "-181, 9, 4, 12", "12, 9, 181, 12"
    })
    void testSearchBadBbox(String param) throws Exception {
        startAPI();
        assertHttpError("/api?q=berlin&bbox=" + param, 400);
    }


    @ParameterizedTest
    @ValueSource(strings = {"bad", "NaN"})
    void testSearchBadLocationBiasScale(String param) throws Exception {
        startAPI();
        assertHttpError("/api?q=berlin&lat=52.54714&lon=13.39026&location_bias_scale=" + param, 400);
    }
}

