package de.komoot.photon.query;

import de.komoot.photon.*;
import de.komoot.photon.searcher.PhotonResult;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class QueryByClassificationTest extends ESBaseTester {
    @TempDir
    static Path sharedTempDir;

    private int testDocId = 10000;

    @BeforeEach
    void setup() throws IOException {
        setUpES();
    }

    private PhotonDoc createDoc(String key, String value, String name) {
        ++testDocId;
        return new PhotonDoc(testDocId, "W", testDocId, key, value).names(Collections.singletonMap("name", name));
    }

    private List<PhotonResult> search(String query) {
        return getServer().createSearchHandler(new String[]{"en"}, 1).search(new PhotonRequest(query, "en"));
    }

    private void updateClassification(String key, String value, String... terms) throws IOException {
        JSONArray jsonTerms = new JSONArray();
        for (String term : terms) {
            jsonTerms.put(term);
        }

        JSONObject json = new JSONObject()
                .put("classification_terms", new JSONArray()
                        .put(new JSONObject()
                                .put("key", key)
                                .put("value", value)
                                .put("terms", terms)
                        ));

        Path synonymPath = sharedTempDir.resolve("synonym.json");
        try {
            FileWriter fw = new FileWriter(synonymPath.toFile());
            fw.write(json.toString(3));
            fw.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try {
            getServer().updateIndexSettings(synonymPath.toString());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        getServer().waitForReady();
    }

    @Test
    void testQueryByClassificationString() throws IOException {
        Importer instance = makeImporter();
        instance.add(createDoc("amenity", "restaurant", "curliflower"), 0);
        instance.finish();
        refresh();

        String class_term = Utils.buildClassificationString("amenity", "restaurant");

        assertNotNull(class_term);

        PhotonResult response = getById(testDocId);
        String classification = (String) response.get(Constants.CLASSIFICATION);
        assertEquals(classification, class_term);

        List<PhotonResult> result = search(class_term + " curli");

        assertTrue(result.size() > 0);
        assertEquals(testDocId, result.get(0).get("osm_id"));
    }

    @Test
    void testQueryByClassificationSynonym() throws IOException {
        Importer instance = makeImporter();
        instance.add(createDoc("amenity", "restaurant", "curliflower"), 0);
        instance.finish();
        refresh();

        updateClassification("amenity", "restaurant", "pub", "kneipe");

        List<PhotonResult> result = search("pub curli");
        assertTrue(result.size() > 0);
        assertEquals(testDocId, result.get(0).get("osm_id"));


        result = search("curliflower kneipe");
        assertTrue(result.size() > 0);
        assertEquals(testDocId, result.get(0).get("osm_id"));
    }


    @Test
    void testSynonymDoNotInterfereWithWords() throws IOException {
        Importer instance = makeImporter();
        instance.add(createDoc("amenity", "restaurant", "airport"), 0);
        instance.add(createDoc("aeroway", "terminal", "Houston"), 0);
        instance.finish();
        refresh();

        updateClassification("aeroway", "terminal", "airport");

        List<PhotonResult> result = search("airport");
        assertTrue(result.size() > 0);
        assertEquals(testDocId - 1, result.get(0).get("osm_id"));


        result = search("airport houston");
        assertTrue(result.size() > 0);
        assertEquals(testDocId, result.get(0).get("osm_id"));
    }

    @Test
    void testSameSynonymForDifferentTags() throws IOException {
        Importer instance = makeImporter();
        instance.add(createDoc("railway", "halt", "Newtown"), 0);
        instance.add(createDoc("railway", "station", "King's Cross"), 0);
        instance.finish();
        refresh();

        JSONObject json = new JSONObject()
                .put("classification_terms", new JSONArray()
                        .put(new JSONObject()
                                .put("key", "railway")
                                .put("value", "station")
                                .put("terms", new JSONArray().put("Station"))
                        ).put(new JSONObject()
                                .put("key", "railway")
                                .put("value", "halt")
                                .put("terms", new JSONArray().put("Station").put("Stop"))
                        ));
        Path synonymPath = sharedTempDir.resolve("synonym.json");
        try {
            FileWriter fw = new FileWriter(synonymPath.toFile());
            fw.write(json.toString(3));
            fw.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try {
            getServer().updateIndexSettings(synonymPath.toString());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        getServer().waitForReady();

        List<PhotonResult> result = search("Station newtown");
        assertTrue(result.size() > 0);
        assertEquals(testDocId - 1, result.get(0).get("osm_id"));

        result = search("newtown stop");
        assertTrue(result.size() > 0);
        assertEquals(testDocId - 1, result.get(0).get("osm_id"));

        result = search("king's cross Station");
        assertTrue(result.size() > 0);
        assertEquals(testDocId, result.get(0).get("osm_id"));
    }
}
