package de.komoot.photon.query;

import de.komoot.photon.*;
import de.komoot.photon.searcher.PhotonResult;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.get.GetResponse;
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

@Slf4j
public class QueryByClassificationTest extends ESBaseTester {
    @TempDir
    static Path sharedTempDir;

    private int testDocId = 10000;

    @BeforeEach
    public void setup() throws IOException {
        setUpES();
    }

    private PhotonDoc createDoc(String key, String value, String name) {
        ++testDocId;
        return new PhotonDoc(testDocId, "W", testDocId, key, value).names(Collections.singletonMap("name", name));
    }

    private List<PhotonResult> search(String query) {
        return getServer().createSearchHandler(new String[]{"en"}).search(new PhotonRequest(query, "en"));
    }

    private void updateClassification(String key, String value, String... terms) {
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
            getServer().setMaxShards(null);
            getServer().updateIndexSettings(synonymPath.toString());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        getServer().waitForReady();
    }

    @Test
    public void testQueryByClassificationString() {
        Importer instance = makeImporter();
        instance.add(createDoc("amenity", "restaurant", "curliflower"));
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
    public void testQueryByClassificationSynonym() {
        Importer instance = makeImporter();
        instance.add(createDoc("amenity", "restaurant", "curliflower"));
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
    public void testSynonymDoNotInterfereWithWords() {
        Importer instance = makeImporter();
        instance.add(createDoc("amenity", "restaurant", "airport"));
        instance.add(createDoc("aeroway", "terminal", "Houston"));
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
    public void testSameSynonymForDifferentTags() {
        Importer instance = makeImporter();
        instance.add(createDoc("railway", "halt", "Newtown"));
        instance.add(createDoc("railway", "station", "King's Cross"));
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
            getServer().setMaxShards(null);
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
