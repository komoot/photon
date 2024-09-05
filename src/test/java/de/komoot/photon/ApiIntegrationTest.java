package de.komoot.photon;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static spark.Spark.*;

/**
 * These test connect photon to an already running ES node (setup in ESBaseTester) so that we can directly test the API
 */
class ApiIntegrationTest extends ESBaseTester {
    private static final int LISTEN_PORT = 30234;

    @BeforeEach
    void setUp() throws Exception {
        setUpESWithPolygons();
        Importer instance = makeImporter();
        instance.add(createDoc(13.38886, 52.51704, 1000, 1000, "place", "city").importance(0.6), 0);
        instance.add(createDoc(13.39026, 52.54714, 1001, 1001, "place", "town").importance(0.3), 0);
        instance.finish();
        refresh();
    }

    @AfterEach
    void shutdown() {
        stop();
        awaitStop();
    }

    /**
     * Test that the Access-Control-Allow-Origin header is not set
     */
    @Test
    void testNoCors() throws Exception {
        App.main(new String[]{"-cluster", TEST_CLUSTER_NAME, "-listen-port", Integer.toString(LISTEN_PORT), "-transport-addresses", "127.0.0.1"});
        awaitInitialization();
        HttpURLConnection connection = (HttpURLConnection) new URL("http://127.0.0.1:" + port() + "/api?q=berlin").openConnection();
        assertNull(connection.getHeaderField("Access-Control-Allow-Origin"));
    }

    /**
     * Test that the Access-Control-Allow-Origin header is set to *
     */
    @Test
    void testCorsAny() throws Exception {
        App.main(new String[]{"-cluster", TEST_CLUSTER_NAME, "-listen-port", Integer.toString(LISTEN_PORT), "-transport-addresses", "127.0.0.1",
                "-cors-any"});
        awaitInitialization();
        HttpURLConnection connection = (HttpURLConnection) new URL("http://127.0.0.1:" + port() + "/api?q=berlin").openConnection();
        assertEquals("*", connection.getHeaderField("Access-Control-Allow-Origin"));
    }

    /**
     * Test that the Access-Control-Allow-Origin header is set to a specific domain
     */
    @Test
    void testCorsOriginIsSetToSpecificDomain() throws Exception {
        App.main(new String[]{"-cluster", TEST_CLUSTER_NAME, "-listen-port", Integer.toString(LISTEN_PORT), "-transport-addresses", "127.0.0.1",
                "-cors-origin", "www.poole.ch"});
        awaitInitialization();
        HttpURLConnection connection = (HttpURLConnection) new URL("http://127.0.0.1:" + port() + "/api?q=berlin").openConnection();
        assertEquals("www.poole.ch", connection.getHeaderField("Access-Control-Allow-Origin"));
    }

    @Test
    void testSearchForBerlin() throws Exception {
        App.main(new String[]{"-cluster", TEST_CLUSTER_NAME, "-listen-port", Integer.toString(LISTEN_PORT), "-transport-addresses", "127.0.0.1"});
        awaitInitialization();
        HttpURLConnection connection = (HttpURLConnection) new URL("http://127.0.0.1:" + port() + "/api?q=berlin&limit=1").openConnection();
        JSONObject json = new JSONObject(
                new BufferedReader(new InputStreamReader(connection.getInputStream())).lines().collect(Collectors.joining("\n")));
        JSONArray features = json.getJSONArray("features");
        assertEquals(1, features.length());
        JSONObject feature = features.getJSONObject(0);
        JSONObject properties = feature.getJSONObject("properties");
        assertEquals("W", properties.getString("osm_type"));
        assertEquals("place", properties.getString("osm_key"));
        assertEquals("city", properties.getString("osm_value"));
        assertEquals("berlin", properties.getString("name"));
    }

    /**
     * Search with location bias (this should give the last generated object which is roughly 2km away from the first)
     */
    @Test
    void testApiWithLocationBias() throws Exception {
        App.main(new String[]{"-cluster", TEST_CLUSTER_NAME, "-listen-port", Integer.toString(LISTEN_PORT), "-transport-addresses", "127.0.0.1"});
        awaitInitialization();
        HttpURLConnection connection = (HttpURLConnection) new URL("http://127.0.0.1:" + port() + "/api?q=berlin&limit=1&lat=52.54714&lon=13.39026&zoom=16")
                .openConnection();
        JSONObject json = new JSONObject(
                new BufferedReader(new InputStreamReader(connection.getInputStream())).lines().collect(Collectors.joining("\n")));
        JSONArray features = json.getJSONArray("features");
        assertEquals(1, features.length());
        JSONObject feature = features.getJSONObject(0);
        JSONObject properties = feature.getJSONObject("properties");
        assertEquals("W", properties.getString("osm_type"));
        assertEquals("place", properties.getString("osm_key"));
        assertEquals("town", properties.getString("osm_value"));
        assertEquals("berlin", properties.getString("name"));
    }

    /**
     * Search with large location bias
     */
    @Test
    void testApiWithLargerLocationBias() throws Exception {
        App.main(new String[]{"-cluster", TEST_CLUSTER_NAME, "-listen-port", Integer.toString(LISTEN_PORT), "-transport-addresses", "127.0.0.1"});
        awaitInitialization();
        HttpURLConnection connection = (HttpURLConnection) new URL("http://127.0.0.1:" + port() + "/api?q=berlin&limit=1&lat=52.54714&lon=13.39026&zoom=12&location_bias_scale=0.6")
                .openConnection();
        JSONObject json = new JSONObject(
                new BufferedReader(new InputStreamReader(connection.getInputStream())).lines().collect(Collectors.joining("\n")));
        JSONArray features = json.getJSONArray("features");
        assertEquals(1, features.length());
        JSONObject feature = features.getJSONObject(0);
        JSONObject properties = feature.getJSONObject("properties");
        assertEquals("W", properties.getString("osm_type"));
        assertEquals("place", properties.getString("osm_key"));
        assertEquals("city", properties.getString("osm_value"));
        assertEquals("berlin", properties.getString("name"));
    }

    /**
     * Reverse geocode test
     */
    @Test
    void testApiReverse() throws Exception {
        App.main(new String[]{"-cluster", TEST_CLUSTER_NAME, "-listen-port", Integer.toString(LISTEN_PORT), "-transport-addresses", "127.0.0.1"});
        awaitInitialization();
        HttpURLConnection connection = (HttpURLConnection) new URL("http://127.0.0.1:" + port() + "/reverse/?lon=13.38886&lat=52.51704").openConnection();
        JSONObject json = new JSONObject(
                new BufferedReader(new InputStreamReader(connection.getInputStream())).lines().collect(Collectors.joining("\n")));
        JSONArray features = json.getJSONArray("features");
        assertEquals(1, features.length());
        JSONObject feature = features.getJSONObject(0);
        JSONObject properties = feature.getJSONObject("properties");
        assertEquals("W", properties.getString("osm_type"));
        assertEquals("place", properties.getString("osm_key"));
        assertEquals("city", properties.getString("osm_value"));
        assertEquals("berlin", properties.getString("name"));
    }

    @Test
    public void testApiStatus() throws Exception {
        App.main(new String[]{"-cluster", TEST_CLUSTER_NAME, "-listen-port", Integer.toString(LISTEN_PORT), "-transport-addresses", "127.0.0.1"});
        awaitInitialization();
        DatabaseProperties prop = new DatabaseProperties();
        getServer().loadFromDatabase(prop);
        HttpURLConnection connection = (HttpURLConnection) new URL("http://127.0.0.1:" + port() + "/status").openConnection();
        JSONObject json = new JSONObject(
                new BufferedReader(new InputStreamReader(connection.getInputStream())).lines().collect(Collectors.joining("\n")));
        assertEquals("Ok", json.getString("status"));
        assertEquals(prop.getImportDate().toInstant().toString(), json.getString("import_date"));
    }

    @Test
    void testSearchAndGetPolygon() throws Exception {
        App.main(new String[]{"-cluster", TEST_CLUSTER_NAME, "-listen-port", Integer.toString(LISTEN_PORT), "-transport-addresses", "127.0.0.1", "-use-geometry-column"});
        awaitInitialization();
        HttpURLConnection connection = (HttpURLConnection) new URL("http://127.0.0.1:" + port() + "/api?q=berlin&limit=1").openConnection();
        JSONObject json = new JSONObject(
                new BufferedReader(new InputStreamReader(connection.getInputStream())).lines().collect(Collectors.joining("\n")));
        JSONArray features = json.getJSONArray("features");
        assertEquals(1, features.length());
        JSONObject feature = features.getJSONObject(0);

        JSONObject geometry = feature.getJSONObject("geometry");
        assertEquals("Polygon", geometry.getString("type"));

        JSONObject properties = feature.getJSONObject("properties");
        assertEquals("W", properties.getString("osm_type"));
        assertEquals("place", properties.getString("osm_key"));
        assertEquals("city", properties.getString("osm_value"));
        assertEquals("berlin", properties.getString("name"));
    }
}
