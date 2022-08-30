package de.komoot.photon.api;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Point;

import de.komoot.photon.App;
import de.komoot.photon.ESBaseTester;
import de.komoot.photon.Importer;
import de.komoot.photon.PhotonDoc;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static spark.Spark.*;

public class ApiLanguagesTest extends ESBaseTester {

    private static final int LISTEN_PORT = 30234;

    @AfterEach
    public void shutdown() {
        stop();
        awaitStop();
    }

    protected PhotonDoc createDoc(int id, String key, String value, String... names) {
        Point location = FACTORY.createPoint(new Coordinate(1.0, 2.34));
        PhotonDoc doc = new PhotonDoc(id, "W", id, key, value).centroid(location);

        Map<String, String> nameMap = new HashMap<>();

        for (int i = 0; i < names.length - 1; i += 2) {
            nameMap.put(names[i], names[i + 1]);
        }

        doc.names(nameMap);

        return doc;
    }

    private void importPlaces(String... languages) throws Exception {
        setUpES(dataDirectory, languages);
        Importer instance = makeImporterWithLanguages(languages);
        instance.add(createDoc(1000, "place", "city",
                "name:en", "thething", "name:fr", "letruc", "name:ch", "dasding"));
        instance.add(createDoc(1001, "place", "town",
                "name:ch", "thething", "name:fr", "letruc", "name:en", "dasding"));
        instance.finish();
        refresh();
    }

    private void startAPI(String languages) throws Exception {
        List<String> params = new ArrayList<>(Arrays.asList("-cluster", TEST_CLUSTER_NAME,
                "-listen-port", Integer.toString(LISTEN_PORT),
                "-transport-addresses", "127.0.0.1",
                "-cors-any"));

        if (languages.length() > 0) {
            params.add("-languages");
            params.add(languages);
        }

        App.main(params.toArray(new String[0]));
        awaitInitialization();
    }

    private JSONArray query(String q) throws Exception {
        HttpURLConnection connection =
                (HttpURLConnection) new URL("http://127.0.0.1:" + port() + "/api?" + q).openConnection();
        JSONObject json = new JSONObject(
                new BufferedReader(new InputStreamReader(connection.getInputStream())).lines().collect(Collectors.joining("\n")));

        return json.getJSONArray("features");
    }

    private int getOsmId(JSONArray results, int idx) {
        return results.getJSONObject(idx).getJSONObject("properties").getInt("osm_id");
    }

    @Test
    public void testOnlyImportSelectedLanguages() throws Exception {
        importPlaces("en");
        startAPI("");

        JSONArray results = query("q=thething");
        assertEquals(1, results.length());
        assertEquals(1000, getOsmId(results, 0));

        assertEquals(0, query("q=letruc").length());
    }

    @Test
    public void testUseImportLanguagesWhenNoOtherIsGiven() throws Exception {
        importPlaces("en", "fr", "ch");
        startAPI("");

        JSONArray results = query("q=thething");
        assertEquals(2, results.length());
    }

    @Test
    public void testUseCommandLineLangauges() throws Exception {
        importPlaces("en", "fr", "ch");
        startAPI("en,fr");

        JSONArray results = query("q=thething");
        assertEquals(1, results.length());
    }

}
