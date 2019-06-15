package de.komoot.photon;

import de.komoot.photon.elasticsearch.ESImporter;
import de.komoot.photon.nominatim.NominatimConnector;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static spark.Spark.awaitInitialization;
import static spark.Spark.port;

/**
 * These test connect photon to an already running ES node (setup in ESBaseTester) so that we can directly test the API
 */
public class ImportIntegrationTest extends ESBaseTester {
    private static final int LISTEN_PORT = 30234;
    private static boolean importDone = false;

    @Before
    public void setUp() throws Exception {
        File file = new File("./target/es_photon_import");
        if (!importDone)
            importDone = file.exists();
        if (importDone) {
            setUpES(file, false);
        } else {
            try {
                setUpES(file, true);
                ESImporter importer = new ESImporter(getClient(), "en");
                NominatimConnector nominatimConnector = new NominatimConnector("localhost", 5432, "photontest", "peter", "buxdehude");
                nominatimConnector.setImporter(importer);
                nominatimConnector.readEntireDatabase("");
                importDone = true;
            } catch (Exception ex) {
                removeDir(file);
                throw new RuntimeException(ex);
            }
        }
    }

    @Test
    public void testImport() throws Exception {
        App.main(new String[]{"-cluster", clusterName, "-listen-port", Integer.toString(LISTEN_PORT), "-transport-addresses", "127.0.0.1"});
        awaitInitialization();

        HttpURLConnection connection = (HttpURLConnection) new URL("http://127.0.0.1:" + port() + "/api?q=berlin&limit=1").openConnection();
        JSONObject json = new JSONObject(
                new BufferedReader(new InputStreamReader(connection.getInputStream())).lines().collect(Collectors.joining("\n")));
        JSONArray features = json.getJSONArray("features");
        assertEquals(1, features.length());
        JSONObject feature = features.getJSONObject(0);
        JSONObject properties = feature.getJSONObject("properties");
        assertEquals("W", properties.getString("osm_type"));
        assertEquals("highway", properties.getString("osm_key"));
        assertEquals("residential", properties.getString("osm_value"));
        assertEquals("Rue de Berlin", properties.getString("name"));
    }

    static boolean removeDir(File file) {
        if (!file.exists())
            return true;

        if (file.isDirectory())
            for (File f : file.listFiles()) {
                removeDir(f);
            }

        return file.delete();
    }
}
