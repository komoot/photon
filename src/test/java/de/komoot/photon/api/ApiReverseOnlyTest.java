package de.komoot.photon.api;

import de.komoot.photon.App;
import de.komoot.photon.PhotonDoc;
import de.komoot.photon.nominatim.model.AddressType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Date;
import java.util.List;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ApiReverseOnlyTest extends ApiBaseTester {

    @BeforeAll
    void setUp(@TempDir Path dataDirectory) throws Exception {
        getProperties().setImportDate(new Date());
        getProperties().setReverseOnly(true);
        setUpES(dataDirectory);

        setupDocs(new PhotonDoc()
                        .placeId("1000").osmType("N").osmId(1000).tagKey("place").tagValue("city")
                        .categories(List.of("osm.place.city"))
                        .importance(0.6).addressType(AddressType.CITY)
                        .centroid(makePoint(13.38886, 52.51704))
                        .names(makeDocNames("name", "berlin")),
                new PhotonDoc()
                        .placeId("1001").osmType("R").osmId(1001).tagKey("place").tagValue("suburb")
                        .categories(List.of("osm.place.suburb"))
                        .importance(0.3).addressType(AddressType.DISTRICT)
                        .centroid(makePoint(13.39026, 52.54714))
                        .names(makeDocNames("name", "berlin")),
                new PhotonDoc()
                        .placeId("1002").osmType("W").osmId(1002).tagKey("place").tagValue("hamlet")
                        .categories(List.of("osm.place.hamlet"))
                        .importance(0.3).addressType(AddressType.LOCALITY)
                        .centroid(makePoint(13.390261, 52.54714))
                        .names(makeDocNames("name", "berlin"))
        );

        startAPI();
    }

    @AfterAll
    @Override
    public void tearDown() {
        App.shutdown();
        shutdownES();
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api?q=berlin", "/structured?city=berlin", "/metrics"})
    void testSearchDisabled(String queryUrl) {
        assertHttpResponseCode(queryUrl, 404);
    }

    @Test
    void testApi() throws IOException {
        assertThatJson(readURL("/reverse?lat=52.54714&lon=13.39026")).isObject()
                .node("features").isArray().hasSize(1)
                .element(0).isObject()
                .node("properties").isObject()
                .containsEntry("osm_key", "place")
                .containsEntry("osm_value", "suburb")
                .containsEntry("name", "berlin");
    }

    @Test
    void testLimitParam() throws IOException {
        assertThatJson(readURL("/reverse?lat=52.54714&lon=13.39026&limit=20")).isObject()
                .node("features").isArray()
                .hasSizeGreaterThan(1);
    }

    @Test
    void testExcludeParam() throws IOException {
        assertThatJson(readURL("/reverse?lat=52.54714&lon=13.39026&exclude=osm.place.suburb")).isObject()
                .node("features").isArray().hasSize(1)
                .element(0).isObject()
                .node("properties").isObject()
                .containsEntry("osm_key", "place")
                .containsEntry("osm_value", "hamlet");
    }
}
