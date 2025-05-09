package de.komoot.photon.searcher;

import de.komoot.photon.Constants;
import org.junit.jupiter.api.Test;

import java.util.List;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;

class GeocodeJsonFormatterTest {

    @Test
    void testConvertPointToGeojson() {
        GeocodeJsonFormatter formatter = new GeocodeJsonFormatter();

        final var allPointResults = List.of(
            createDummyPointResult("99999", "Park Foo", "leisure", "park"),
            createDummyPointResult("88888", "Bar Park", "amenity", "bar"));

        String geojsonString = formatter.convert(allPointResults, "en", false, false, null);

        var features = assertThatJson(geojsonString).isObject()
                .containsEntry("type", "FeatureCollection")
                .node("features").isArray().hasSize(2);

        for (int i = 0; i < 2; ++i) {
            features.element(i).isObject()
                    .containsEntry("type", "Feature")
                    .node("geometry").isObject()
                    .containsEntry("type", "Point")
                    .node("coordinates").isArray()
                    .isEqualTo("[42.0, 21.0]");
        }

        features.element(0).isObject().node("properties").isObject()
                .containsEntry("osm_key", "leisure")
                .containsEntry("osm_value", "park");
        features.element(1).isObject().node("properties").isObject()
                .containsEntry("osm_key", "amenity")
                .containsEntry("osm_value", "bar");
    }

    @Test
    void testConvertGeometryToGeojson() {
        GeocodeJsonFormatter formatter = new GeocodeJsonFormatter();

        final var allResults = List.of(
                createDummyGeometryResult("99999", "Park Foo", "leisure", "park"));

        String geojsonString = formatter.convert(allResults, "en", true, false, null);

        assertThatJson(geojsonString).isObject()
                .containsEntry("type", "FeatureCollection")
                .node("features").isArray().hasSize(1)
                .element(0).isObject()
                .containsEntry("type", "Feature")
                .node("geometry").isObject()
                .containsEntry("type", "MultiPolygon");
    }
    
    private PhotonResult createDummyPointResult(String postCode, String name, String osmKey,
                                                String osmValue) {
        return new MockPhotonResult()
                .put(Constants.POSTCODE, postCode)
                .putLocalized(Constants.NAME, "en", name)
                .put(Constants.OSM_KEY, osmKey)
                .put(Constants.OSM_VALUE, osmValue)
                .putGeometry("{\"type\":\"Point\", \"coordinates\": [42, 21]}");
    }

    private PhotonResult createDummyGeometryResult(String postCode, String name, String osmKey,
                                                   String osmValue) {
        return new MockPhotonResult()
                .put(Constants.POSTCODE, postCode)
                .putLocalized(Constants.NAME, "en", name)
                .put(Constants.OSM_KEY, osmKey)
                .put(Constants.OSM_VALUE, osmValue);
    }

}
