package de.komoot.photon.searcher;

import de.komoot.photon.opensearch.DocFields;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;

class GeoJsonFormatterTest {

    @Test
    void testConvertPointToGeojson() throws IOException {
        GeoJsonFormatter formatter = new GeoJsonFormatter();

        final var allPointResults = List.of(
            createDummyPointResult("99999", "Park Foo", "leisure", "park"),
            createDummyPointResult("88888", "Bar Park", "amenity", "bar"));

        String geojsonString = formatter.convert(allPointResults, "en", null,false, false, null);

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
    void testConvertGeometryToGeojson() throws IOException {
        GeoJsonFormatter formatter = new GeoJsonFormatter();

        final var allResults = List.of(
                createDummyGeometryResult("99999", "Park Foo", "leisure", "park"));

        String geojsonString = formatter.convert(allResults, "en", null,true, false, null);

        assertThatJson(geojsonString).isObject()
                .containsEntry("type", "FeatureCollection")
                .node("features").isArray().hasSize(1)
                .element(0).isObject()
                .containsEntry("type", "Feature")
                .node("geometry").isObject()
                .containsEntry("type", "MultiPolygon");
    }

    @Test
    void testFallbackLanguageForLocalisedFields() throws IOException {
        GeoJsonFormatter formatter = new GeoJsonFormatter();

        final var result = new MockPhotonResult()
                .putLocalized(DocFields.NAME, "default", "Αθήνα")
                .putLocalized(DocFields.NAME, "en", "Athens")
                .putLocalized(DocFields.COUNTRY, "default", "Ελλάδα")
                .putLocalized(DocFields.COUNTRY, "en", "Greece")
                .put(DocFields.OSM_KEY, "place")
                .put(DocFields.OSM_VALUE, "city")
                .putGeometry("{\"type\":\"Point\", \"coordinates\": [42, 21]}");

        String geojsonString = formatter.convert(List.of(result), "nl", "en", false, false, null);

        assertThatJson(geojsonString).isObject()
                .node("features").isArray().hasSize(1)
                .element(0).isObject()
                .node("properties").isObject()
                .containsEntry("name", "Athens")
                .containsEntry("country", "Greece");
    }

    @Test
    void testLocalLanguageFallbackWhenFallbackLanguageIsUnset() throws IOException {
        GeoJsonFormatter formatter = new GeoJsonFormatter();

        final var result = new MockPhotonResult()
                .putLocalized(DocFields.NAME, "default", "Αθήνα")
                .putLocalized(DocFields.NAME, "en", "Athens")
                .put(DocFields.OSM_KEY, "place")
                .put(DocFields.OSM_VALUE, "city")
                .putGeometry("{\"type\":\"Point\", \"coordinates\": [42, 21]}");

        String geojsonString = formatter.convert(List.of(result), "nl",  null,false, false, null);

        assertThatJson(geojsonString).isObject()
                .node("features").isArray().hasSize(1)
                .element(0).isObject()
                .node("properties").isObject()
                .containsEntry("name", "Αθήνα");
    }
    
    private PhotonResult createDummyPointResult(String postCode, String name, String osmKey,
                                                String osmValue) {
        return new MockPhotonResult()
                .put(DocFields.POSTCODE, postCode)
                .putLocalized(DocFields.NAME, "en", name)
                .put(DocFields.OSM_KEY, osmKey)
                .put(DocFields.OSM_VALUE, osmValue)
                .putGeometry("{\"type\":\"Point\", \"coordinates\": [42, 21]}");
    }

    private PhotonResult createDummyGeometryResult(String postCode, String name, String osmKey,
                                                   String osmValue) {
        return new MockPhotonResult()
                .put(DocFields.POSTCODE, postCode)
                .putLocalized(DocFields.NAME, "en", name)
                .put(DocFields.OSM_KEY, osmKey)
                .put(DocFields.OSM_VALUE, osmValue)
                .putGeometry("{\"type\":\"MultiPolygon\",\"coordinates\":[[[[-100.0,40.0],[-100.0,45.0],[-90.0,45.0],[-90.0,40.0],[-100.0,40.0]]],[[[-80.0,35.0],[-80.0,40.0],[-70.0,40.0],[-70.0,35.0],[-80.0,35.0]]]]}");
    }

}
