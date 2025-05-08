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
    void testConvertPointToGeojson() {
        GeocodeJsonFormatter formatter = new GeocodeJsonFormatter();
        List<PhotonResult> allPointResults = new ArrayList<>();
        allPointResults.add(createDummyPointResult("99999", "Park Foo", "leisure", "park"));
        allPointResults.add(createDummyPointResult("88888", "Bar Park", "leisure", "park"));

        // Test Points
        String geojsonString = formatter.convert(allPointResults, "en", false, false, null);
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
    }

    @Test
    void testConvertGeometryToGeojson() {
        GeocodeJsonFormatter formatter = new GeocodeJsonFormatter();

        List<PhotonResult> allResults = new ArrayList<>();
        allResults.add(createDummyGeometryResult("99999", "Park Foo", "leisure", "park"));
        allResults.add(createDummyGeometryResult("88888", "Bar Park", "leisure", "park"));

        // Test Geometry
        String geojsonString = formatter.convert(allResults, "en", true, false, null);
        JSONObject jsonObj = new JSONObject(geojsonString);
        assertEquals("FeatureCollection", jsonObj.getString("type"));
        JSONArray features = jsonObj.getJSONArray("features");
        assertEquals(2, features.length());
        for (int i = 0; i < features.length(); i++) {
            JSONObject feature = features.getJSONObject(i);
            assertEquals("Feature", feature.getString("type"));
            assertEquals("MultiPolygon", feature.getJSONObject("geometry").getString("type"));
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

    private PhotonResult createDummyGeometryResult(String postCode, String name, String osmKey,
                                                   String osmValue) {
        return new MockPhotonResult()
                .put(Constants.POSTCODE, postCode)
                .putLocalized(Constants.NAME, "en", name)
                .put(Constants.OSM_KEY, osmKey)
                .put(Constants.OSM_VALUE, osmValue)
                .put(Constants.GEOMETRY, new JSONObject("{\"type\":\"MultiPolygon\",\"coordinates\":[[[[-100.0,40.0],[-100.0,45.0],[-90.0,45.0],[-90.0,40.0],[-100.0,40.0]]],[[[-80.0,35.0],[-80.0,40.0],[-70.0,40.0],[-70.0,35.0],[-80.0,35.0]]]]}"));
    }

}
