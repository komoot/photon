package de.komoot.photon.api;

import de.komoot.photon.App;
import de.komoot.photon.Importer;
import de.komoot.photon.PhotonDoc;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.FieldSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.Path;
import java.util.Date;
import java.util.List;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;

/**
 * API tests that check queries against an already running ES instance and
 * API server.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ApiIntegrationTest extends ApiBaseTester {
    private static final String[] BASE_URLS = {
            "/api?q=Berlin", "/reverse?lat=52.54714&lon=13.39026"};
    private static final Date TEST_DATE = new Date();

    @BeforeAll
    void setUp(@TempDir Path dataDirectory) throws Exception {
        getProperties().setSupportGeometries(true);
        getProperties().setImportDate(TEST_DATE);
        setUpES(dataDirectory);
        Importer instance = makeImporter();
        instance.add(List.of(new PhotonDoc()
                .placeId(1000).osmType("N").osmId(1000).tagKey("place").tagValue("city")
                .importance(0.6).rankAddress(16)
                .centroid(makePoint(13.38886, 52.51704))
                .geometry(makeDocGeometry("POINT(13.38886 52.51704)"))
                .names(makeDocNames("name", "berlin"))
        ));
        instance.add(List.of(new PhotonDoc()
                .placeId(1001).osmType("R").osmId(1001).tagKey("place").tagValue("suburb")
                .importance(0.3).rankAddress(17)
                .centroid(makePoint(13.39026, 52.54714))
                .geometry(makeDocGeometry("POLYGON ((6.4440619 52.1969454, 6.4441094 52.1969158, 6.4441408 52.1969347, 6.4441138 52.1969516, 6.4440933 52.1969643, 6.4440619 52.1969454))"))
                .names(makeDocNames("name", "berlin"))
        ));
        instance.add(List.of(new PhotonDoc()
                .placeId(1002).osmType("W").osmId(1002).tagKey("place").tagValue("hamlet")
                .importance(0.3).rankAddress(25)
                .centroid(makePoint(13.39026, 52.54714))
                .geometry(makeDocGeometry("LINESTRING (30 10, 10 30, 40 40)"))
                .names(makeDocNames("name", "berlin"))
        ));

        instance.finish();
        refresh();
        startAPI();
    }

    @AfterAll
    @Override
    public void tearDown() {
        App.shutdown();
        shutdownES();
    }



    @ParameterizedTest
    @FieldSource("BASE_URLS")
    void testBogus(String baseUrl)  {
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
        assertThatJson(readURL(url)).isObject()
                .node("features").isArray().hasSize(1)
                .element(0).isObject()
                .node("properties").isObject()
                .containsEntry("osm_key", "place")
                .containsEntry("osm_value", osmValue)
                .containsEntry("name", "berlin");
    }

    @Test
    void testApiStatus() throws Exception {
        assertThatJson(readURL("/status")).isObject()
                .containsEntry("status", "Ok")
                .containsEntry("import_date", TEST_DATE.toInstant().toString());
    }

    @ParameterizedTest
    @FieldSource("BASE_URLS")
    void testSearchAndGetGeometry(String baseUrl) throws Exception {
        var obj = assertThatJson(readURL(baseUrl + "&geometry=true&layer=district"))
                .isObject()
                .containsEntry("type", "FeatureCollection")
                .node("features").isArray().hasSize(1)
                .element(0).isObject();

        obj.node("geometry").isObject()
                .containsEntry("type", "Polygon");

        obj.node("properties").isObject()
                .containsEntry("osm_key", "place")
                .containsEntry("osm_value", "suburb")
                .containsEntry("name", "berlin");

    }

    @ParameterizedTest
    @FieldSource("BASE_URLS")
    void testSearchAndGetLineString(String baseUrl) throws Exception {
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
                .containsEntry("osm_key", "place")
                .containsEntry("osm_value", "hamlet")
                .containsEntry("name", "berlin");
    }

    @ParameterizedTest
    @FieldSource("BASE_URLS")
    void testSearchWithoutGeometry(String baseUrl) throws Exception {
        assertThatJson(readURL(baseUrl)).isObject()
                .node("features").isArray()
                .element(0).isObject()
                .node("geometry").isObject()
                .containsEntry("type", "Point");
    }

    @ParameterizedTest
    @FieldSource("BASE_URLS")
    void testLimitParameter(String baseUrl) throws Exception {
        assertThatJson(readURL(baseUrl + "&limit=20")).isObject()
                .node("features").isArray()
                .hasSizeGreaterThan(1);
        assertThatJson(readURL(baseUrl + "&limit=1")).isObject()
                .node("features").isArray()
                .hasSize(1);
    }

    @ParameterizedTest
    @FieldSource("BASE_URLS")
    void testBadLimitParameter(String baseUrl)  {
        assertHttpError(baseUrl + "&limit=NaN", 400);
    }

    @ParameterizedTest
    @FieldSource("BASE_URLS")
    void testDebugOutput(String baseUrl) throws Exception {
        assertThatJson(readURL(baseUrl + "&debug=1")).isObject()
                .node("properties").isObject()
                .containsKeys("debug", "raw_data");
    }

    @ParameterizedTest
    @FieldSource("BASE_URLS")
    void testSimpleLayer(String baseUrl) throws Exception {
        assertThatJson(readURL(baseUrl + "&layer=locality")).isObject()
                .node("features").isArray().hasSize(1)
                .element(0).isObject()
                .node("properties").isObject()
                .containsEntry("osm_value", "hamlet");
    }

    @ParameterizedTest
    @FieldSource("BASE_URLS")
    void testManyLayers(String baseUrl) throws Exception {
        assertThatJson(readURL(baseUrl + "&layer=locality&layer=district")).isObject()
                .node("features").isArray().hasSize(2);
    }

    @ParameterizedTest
    @FieldSource("BASE_URLS")
    void testBadLayerParameter(String baseUrl) {
        assertHttpError(baseUrl + "&layer=locality&layer=suburb", 400);
    }

    @ParameterizedTest
    @FieldSource("BASE_URLS")
    void testOsmKeyFilter(String baseUrl) throws Exception {
        assertThatJson(readURL(baseUrl + "&osm_tag=place"))
                .node("features").isArray().hasSizeGreaterThan(1);
    }

    @ParameterizedTest
    @FieldSource("BASE_URLS")
    void testOsmKeyFilterNoMatch(String baseUrl) throws Exception {
        assertThatJson(readURL(baseUrl + "&osm_tag=highway"))
                .node("features").isArray().hasSize(0);
    }

    @ParameterizedTest
    @FieldSource("BASE_URLS")
    void testOsmKeyFilterNeg(String baseUrl) throws Exception {
        assertThatJson(readURL(baseUrl + "&osm_tag=!place"))
                .node("features").isArray().hasSize(0);
    }

    @ParameterizedTest
    @FieldSource("BASE_URLS")
    void testOsmValueFilter(String baseUrl) throws Exception {
        assertThatJson(readURL(baseUrl + "&osm_tag=:hamlet")).isObject()
                .node("features").isArray().hasSize(1)
                .element(0).isObject()
                .node("properties").isObject()
                .containsEntry("osm_value", "hamlet");
    }

    @ParameterizedTest
    @FieldSource("BASE_URLS")
    void testOsmValueFilterNeg(String baseUrl) throws Exception {
        assertThatJson(readURL(baseUrl + "&layer=locality&layer=district&osm_tag=:!hamlet")).isObject()
                .node("features").isArray().hasSize(1)
                .element(0).isObject()
                .node("properties").isObject()
                .containsEntry("osm_value", "suburb");
    }

    @ParameterizedTest
    @FieldSource("BASE_URLS")
    void testOsmTagFilter(String baseUrl) throws Exception {
        assertThatJson(readURL(baseUrl + "&osm_tag=place:hamlet")).isObject()
                .node("features").isArray().hasSize(1)
                .element(0).isObject()
                .node("properties").isObject()
                .containsEntry("osm_value", "hamlet");
    }

    @ParameterizedTest
    @FieldSource("BASE_URLS")
    void testOsmTagFilterNeg(String baseUrl) throws Exception {
        assertThatJson(readURL(baseUrl + "&layer=locality&layer=district&osm_tag=!place:hamlet")).isObject()
                .node("features").isArray().hasSize(1)
                .element(0).isObject()
                .node("properties").isObject()
                .containsEntry("osm_value", "suburb");
    }

    @ParameterizedTest
    @FieldSource("BASE_URLS")
    void testBadOsmTagParameter(String baseUrl) {
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
    void testReverseBadLocation(String param) {
        assertHttpError("/reverse?" + param, 400);
    }

    @ParameterizedTest
    @ValueSource(strings = {"bad", "NaN", "0.0", "-10.0"})
    void testReverseBadRadius(String param) {
        assertHttpError("/reverse?lat=52.54714&lon=13.39026&radius=" + param, 400);
    }

    @Test
    void testSearchMissingQuery() {
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
    void testSearchBadLocation(String param)  {
        assertHttpError("/api?q=berlin&" + param, 400);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "9.6,52.3,9.8", "9.6,52.3,NaN,9.8",
            "9.6,52.3,-Infinity,9.8", "9.6,52.3,r34,9.8",
            "9.6,-92,9.8,14", "9.6,14,9.8,91",
            "-181, 9, 4, 12", "12, 9, 181, 12"
    })
    void testSearchBadBbox(String param) {
        assertHttpError("/api?q=berlin&bbox=" + param, 400);
    }


    @ParameterizedTest
    @ValueSource(strings = {"bad", "NaN"})
    void testSearchBadLocationBiasScale(String param) {
        assertHttpError("/api?q=berlin&lat=52.54714&lon=13.39026&location_bias_scale=" + param, 400);
    }
}

