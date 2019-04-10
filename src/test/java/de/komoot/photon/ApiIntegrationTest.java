package de.komoot.photon;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static spark.Spark.awaitInitialization;
import static spark.Spark.awaitStop;
import static spark.Spark.port;
import static spark.Spark.stop;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

/**
 * These test connect photon to an already running ES node (setup in ESBaseTester) so that we can directly test the API
 * 
 */
public class ApiIntegrationTest extends ESBaseTester {
    private static final int LISTEN_PORT = 30234;

    /**
     * Test that the Access-Control-Allow-Origin header is not set
     *
     */
    @Test
    public void testNoCors() {
        try {
            App.main(new String[] { "-cluster", clusterName, "-listen-port", Integer.toString(LISTEN_PORT), "-transport-addresses", "127.0.0.1" });
            awaitInitialization();
            HttpURLConnection connection = (HttpURLConnection) new URL("http://127.0.0.1:" + port() + "/api?q=berlin").openConnection();
            String result = new BufferedReader(new InputStreamReader(connection.getInputStream())).lines().collect(Collectors.joining("\n"));
            assertNull(connection.getHeaderField("Access-Control-Allow-Origin"));
        } catch (Exception ex) {
            fail(ex.getMessage());
        } finally {
            stop();
            awaitStop();
            deleteIndex();
        }
    }

    /**
     * Test that the Access-Control-Allow-Origin header is set to *
     *
     */
    @Test
    public void testCorsAny() {
        try {
            App.main(new String[] { "-cluster", "photon-test", "-listen-port", Integer.toString(LISTEN_PORT), "-transport-addresses", "127.0.0.1",
                    "-cors-any" });
            awaitInitialization();
            HttpURLConnection connection = (HttpURLConnection) new URL("http://127.0.0.1:" + port() + "/api?q=berlin").openConnection();
            String result = new BufferedReader(new InputStreamReader(connection.getInputStream())).lines().collect(Collectors.joining("\n"));
            assertEquals("*", connection.getHeaderField("Access-Control-Allow-Origin"));
        } catch (Exception ex) {
            fail(ex.getMessage());
        } finally {
            stop();
            awaitStop();
            deleteIndex();
        }
    }

    /**
     * Test that the Access-Control-Allow-Origin header is set to a specific domain
     *
     */
    @Test
    public void testCorsOrigin() {
        try {
            App.main(new String[] { "-cluster", "photon-test", "-listen-port", Integer.toString(LISTEN_PORT), "-transport-addresses", "127.0.0.1",
                    "-cors-origin", "www.poole.ch" });
            awaitInitialization();
            HttpURLConnection connection = (HttpURLConnection) new URL("http://127.0.0.1:" + port() + "/api?q=berlin").openConnection();
            String result = new BufferedReader(new InputStreamReader(connection.getInputStream())).lines().collect(Collectors.joining("\n"));
            assertEquals("www.poole.ch", connection.getHeaderField("Access-Control-Allow-Origin"));
        } catch (Exception ex) {
            fail(ex.getMessage());
        } finally {
            stop();
            awaitStop();
            deleteIndex();
        }
    }

    /**
     * Search for Berlin
     *
     */
    @Test
    public void testApi() {
        try {
            App.main(new String[] { "-cluster", "photon-test", "-listen-port", Integer.toString(LISTEN_PORT), "-transport-addresses", "127.0.0.1" });
            awaitInitialization();
            HttpURLConnection connection = (HttpURLConnection) new URL("http://127.0.0.1:" + port() + "/api?q=berlin&limit=1").openConnection();
            try {
                JSONObject json = new JSONObject(
                        new BufferedReader(new InputStreamReader(connection.getInputStream())).lines().collect(Collectors.joining("\n")));
                JSONArray features = json.getJSONArray("features");
                assertEquals(1, features.length());
                JSONObject feature = features.getJSONObject(0);
                JSONObject properties = feature.getJSONObject("properties");
                assertEquals("way", properties.getString("osm_type"));
                assertEquals("amenity", properties.getString("osm_key"));
                assertEquals("information", properties.getString("osm_value"));
                assertEquals("berlin", properties.getString("name"));
            } catch (JSONException jsex) {
                fail(jsex.getMessage());
            }
        } catch (Exception ex) {
            fail(ex.getMessage());
        } finally {
            stop();
            awaitStop();
            deleteIndex();
        }
    }

    /**
     * Search with location bias (this should give the last generated object which is roughly 2km away from the first)
     *
     */
    @Test
    public void testApiWithLocationBias() {
        try {
            App.main(new String[] { "-cluster", "photon-test", "-listen-port", Integer.toString(LISTEN_PORT), "-transport-addresses", "127.0.0.1" });
            awaitInitialization();
            HttpURLConnection connection = (HttpURLConnection) new URL("http://127.0.0.1:" + port() + "/api?q=berlin&limit=1&lat=52.54714&lon=13.39026")
                    .openConnection();
            try {
                JSONObject json = new JSONObject(
                        new BufferedReader(new InputStreamReader(connection.getInputStream())).lines().collect(Collectors.joining("\n")));
                JSONArray features = json.getJSONArray("features");
                assertEquals(1, features.length());
                JSONObject feature = features.getJSONObject(0);
                JSONObject properties = feature.getJSONObject("properties");
                assertEquals("way", properties.getString("osm_type"));
                assertEquals("railway", properties.getString("osm_key"));
                assertEquals("station", properties.getString("osm_value"));
                assertEquals("berlin", properties.getString("name"));
            } catch (JSONException jsex) {
                fail(jsex.getMessage());
            }
        } catch (Exception ex) {
            fail(ex.getMessage());
        } finally {
            stop();
            awaitStop();
            deleteIndex();
        }
    }

    /**
     * Reverse geocode test
     *
     */
    @Test
    public void testApiReverse() {
        try {
            App.main(new String[] { "-cluster", "photon-test", "-listen-port", Integer.toString(LISTEN_PORT), "-transport-addresses", "127.0.0.1" });
            awaitInitialization();
            HttpURLConnection connection = (HttpURLConnection) new URL("http://127.0.0.1:" + port() + "/reverse/?lon=13.38886&lat=52.51704").openConnection();
            try {
                JSONObject json = new JSONObject(
                        new BufferedReader(new InputStreamReader(connection.getInputStream())).lines().collect(Collectors.joining("\n")));
                JSONArray features = json.getJSONArray("features");
                assertEquals(1, features.length());
                JSONObject feature = features.getJSONObject(0);
                JSONObject properties = feature.getJSONObject("properties");
                assertEquals("way", properties.getString("osm_type"));
                assertEquals("tourism", properties.getString("osm_key"));
                assertEquals("attraction", properties.getString("osm_value"));
                assertEquals("berlin", properties.getString("name"));
            } catch (JSONException jsex) {
                fail(jsex.getMessage());
            }
        } catch (Exception ex) {
            fail(ex.getMessage());
        } finally {
            stop();
            awaitStop();
            deleteIndex();
        }
    }
}
