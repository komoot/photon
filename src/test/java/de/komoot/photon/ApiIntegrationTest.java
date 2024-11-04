package de.komoot.photon;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
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
        setUpES();
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

    /*
     * Test that the Access-Control-Allow-Origin header is set to the matching domain
     */
    @Test
    void testCorsOriginIsSetToMatchingDomain() throws Exception {
        App.main(new String[]{"-cluster", TEST_CLUSTER_NAME, "-listen-port", Integer.toString(LISTEN_PORT), "-transport-addresses", "127.0.0.1",
                "-cors-origin", "www.poole.ch,alt.poole.ch"});
        awaitInitialization();
        String[] origins = {"www.poole.ch", "alt.poole.ch"};
        for (String origin: origins) {
            URLConnection urlConnection = new URL("http://127.0.0.1:" + port() + "/api?q=berlin").openConnection();

            HttpURLConnection connection = (HttpURLConnection) urlConnection;
            connection.setRequestProperty("Origin", origin);
            assertEquals(origin, connection.getRequestProperty("Origin"));
            assertEquals(origin, connection.getHeaderField("Access-Control-Allow-Origin"));
        }
    }

    /*
     * Test that the Access-Control-Allow-Origin header does not return mismatching origins
     */
    @Test
    void testMismatchedCorsOriginsAreBlock() throws Exception {
        App.main(new String[]{"-cluster", TEST_CLUSTER_NAME, "-listen-port", Integer.toString(LISTEN_PORT), "-transport-addresses", "127.0.0.1",
                "-cors-origin", "www.poole.ch,alt.poole.ch"});
        awaitInitialization();
        String[] origins = {"www.randomsite.com", "www.arbitrary.com"};
        for (String origin: origins) {
            URLConnection urlConnection = new URL("http://127.0.0.1:" + port() + "/api?q=berlin").openConnection();
            urlConnection.setRequestProperty("Origin", origin);
            HttpURLConnection connection = (HttpURLConnection) urlConnection;
            assertEquals("www.poole.ch", connection.getHeaderField("Access-Control-Allow-Origin"));
        }
    }

    @ParameterizedTest
    @CsvSource({
            "city, /api?q=berlin&limit=1",                                    // basic search
            "town, /api?q=berlin&limit=1&lat=52.54714&lon=13.39026&zoom=16",  // search with location bias
            "city, /api?q=berlin&limit=1&lat=52.54714&lon=13.39026&zoom=12&location_bias_scale=0.6",  // search with large location bias
            "city, /reverse/?lon=13.38886&lat=52.51704" // basic reverse
    })
    void testApi(String osmValue, String url) throws Exception {
        App.main(new String[]{"-cluster", TEST_CLUSTER_NAME, "-listen-port", Integer.toString(LISTEN_PORT), "-transport-addresses", "127.0.0.1"});
        awaitInitialization();
        HttpURLConnection connection = (HttpURLConnection) new URL("http://127.0.0.1:" + port() + url).openConnection();
        JSONObject json = new JSONObject(
                new BufferedReader(new InputStreamReader(connection.getInputStream())).lines().collect(Collectors.joining("\n")));
        JSONArray features = json.getJSONArray("features");
        assertEquals(1, features.length());
        JSONObject feature = features.getJSONObject(0);
        JSONObject properties = feature.getJSONObject("properties");
        assertEquals("W", properties.getString("osm_type"));
        assertEquals("place", properties.getString("osm_key"));
        assertEquals(osmValue, properties.getString("osm_value"));
        assertEquals("berlin", properties.getString("name"));
    }


    @Test
    void testApiStatus() throws Exception {
        App.main(new String[]{"-cluster", TEST_CLUSTER_NAME, "-listen-port", Integer.toString(LISTEN_PORT), "-transport-addresses", "127.0.0.1"});
        awaitInitialization();
        DatabaseProperties prop = getServer().loadFromDatabase();
        HttpURLConnection connection = (HttpURLConnection) new URL("http://127.0.0.1:" + port() + "/status").openConnection();
        JSONObject json = new JSONObject(
                new BufferedReader(new InputStreamReader(connection.getInputStream())).lines().collect(Collectors.joining("\n")));
        assertEquals("Ok", json.getString("status"));
        assertEquals(prop.getImportDate().toInstant().toString(), json.getString("import_date"));
    }
}
