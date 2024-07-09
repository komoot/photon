package de.komoot.photon.searcher;

import de.komoot.photon.Constants;
import org.json.JSONObject;
import org.json.JSONArray;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GeocodeJsonFormatterTest {

    @Test
    void testConvertToGeojson() {
        GeocodeJsonFormatter formatter = new GeocodeJsonFormatter(false, "en");
        List<PhotonResult> allPointResults = new ArrayList<>();
        allPointResults.add(createDummyPointResult("99999", "Park Foo", "leisure", "park"));
        allPointResults.add(createDummyPointResult("88888", "Bar Park", "leisure", "park"));

        List<PhotonResult> allPolygonResults = new ArrayList<>();
        allPolygonResults.add(createDummyPolygonResult("99999", "Park Foo", "leisure", "park"));
        allPolygonResults.add(createDummyPolygonResult("88888", "Bar Park", "leisure", "park"));

        // Test Points
        String geojsonString = formatter.convert(allPointResults, null);
        JSONObject jsonObj = new JSONObject(geojsonString);
        assertEquals("FeatureCollection", jsonObj.getString("type"));
        JSONArray features = jsonObj.getJSONArray("features");
        assertEquals(2, features.length());
        for (int i = 0; i < features.length(); i++) {
            JSONObject feature = features.getJSONObject(i);
            assertEquals("Feature", feature.getString("type"));
            assertEquals("Point", feature.getJSONObject("geometry").getString("type"));
            assertEquals("leisure", feature.getJSONObject("properties").getString(Constants.OSM_KEY));
            assertEquals("park", feature.getJSONObject("properties").getString(Constants.OSM_VALUE));
        }

        // Test Polygon
        geojsonString = formatter.convert(allPolygonResults, null);
        jsonObj = new JSONObject(geojsonString);
        assertEquals("FeatureCollection", jsonObj.getString("type"));
        features = jsonObj.getJSONArray("features");
        assertEquals(2, features.length());
        for (int i = 0; i < features.length(); i++) {
            JSONObject feature = features.getJSONObject(i);
            assertEquals("Feature", feature.getString("type"));
            assertEquals("Polygon", feature.getJSONObject("geometry").getString("type"));
            assertEquals("leisure", feature.getJSONObject("properties").getString(Constants.OSM_KEY));
            assertEquals("park", feature.getJSONObject("properties").getString(Constants.OSM_VALUE));
        }
    }
    
    private PhotonResult createDummyPointResult(String postCode, String name, String osmKey,
                                                String osmValue) {
        return new MockPhotonResult()
                .put(Constants.POSTCODE, postCode)
                .putLocalized(Constants.NAME, "en", name)
                .put(Constants.OSM_KEY, osmKey)
                .put(Constants.OSM_VALUE, osmValue)
                .put("geometry", new JSONObject()
                    .put("type", "Point")
                    .put("coordinates", new double[]{42, 21}));
    }

    private PhotonResult createDummyPolygonResult(String postCode, String name, String osmKey,
                                                String osmValue) {
        return new MockPhotonResult()
                .put(Constants.POSTCODE, postCode)
                .putLocalized(Constants.NAME, "en", name)
                .put(Constants.OSM_KEY, osmKey)
                .put(Constants.OSM_VALUE, osmValue)
                .put("geometry", new JSONObject()
                        .put("type", "Polygon")
                        .put("coordinates", new double[][]{{100.0, 0.0}, {101.0, 0.0}, {101.0, 1.0}, {100.0, 1.0}, {100.0, 0.0}}));
    }

}
