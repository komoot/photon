package de.komoot.photon;

import de.komoot.photon.elasticsearch.Importer;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static spark.Spark.*;

/**
 * These test connect photon to an already running ES node (setup in ESBaseTester) so that we can directly test the API
 */
public class ApiIntegrationTest extends ESBaseTester {
    private static final int LISTEN_PORT = 30234;

    @Before
    public void setUp() throws Exception {
        setUpES();
        Importer instance = makeImporter();
        instance.add(createDoc(13.38886, 52.51704, 1000, 1000, "place", "city").importance(0.6));
        instance.add(createDoc(13.39026, 52.54714, 1001, 1001, "place", "town").importance(0.3));
        instance.finish();
        refresh();
    }

    @After
    public void shutdown() {
        stop();
        awaitStop();
    }

    /**
     * Test that the Access-Control-Allow-Origin header is not set
     */
    @Test
    public void testNoCors() throws Exception {
        App.main(new String[]{"-cluster", TEST_CLUSTER_NAME, "-listen-port", Integer.toString(LISTEN_PORT), "-transport-addresses", "127.0.0.1"});
        awaitInitialization();
        HttpURLConnection connection = (HttpURLConnection) new URL("http://127.0.0.1:" + port() + "/api?q=berlin").openConnection();
        assertNull(connection.getHeaderField("Access-Control-Allow-Origin"));
    }

    /**
     * Test that the Access-Control-Allow-Origin header is set to *
     */
    @Test
    public void testCorsAny() throws Exception {
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
    public void testCorsOriginIsSetToSpecificDomain() throws Exception {
        App.main(new String[]{"-cluster", TEST_CLUSTER_NAME, "-listen-port", Integer.toString(LISTEN_PORT), "-transport-addresses", "127.0.0.1",
                "-cors-origin", "www.poole.ch"});
        awaitInitialization();
        HttpURLConnection connection = (HttpURLConnection) new URL("http://127.0.0.1:" + port() + "/api?q=berlin").openConnection();
        assertEquals("www.poole.ch", connection.getHeaderField("Access-Control-Allow-Origin"));
    }

    @Test
    public void testSearchForBerlin() throws Exception {
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
    public void testApiWithLocationBias() throws Exception {
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
    public void testApiWithLargerLocationBias() throws Exception {
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
    public void testApiReverse() throws Exception {
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
}
