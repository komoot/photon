package de.komoot.photon.query;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.komoot.photon.*;
import de.komoot.photon.searcher.PhotonResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class QueryByClassificationTest extends ESBaseTester {
    @TempDir
    private Path sharedTempDir;

    private int testDocId = 10000;

    @BeforeEach
    void setup() throws IOException {
        setUpES(sharedTempDir.resolve("db"));
    }

    private PhotonDoc createDoc(String key, String value, String name) {
        ++testDocId;
        return new PhotonDoc(testDocId, "W", testDocId, key, value).names(Collections.singletonMap("name", name));
    }

    private List<PhotonResult> search(String query) {
        return getServer().createSearchHandler(new String[]{"en"}, 1).search(new SimpleSearchRequest(query, "en"));
    }

    private void updateClassification(String key, String value, String... terms) throws IOException {
        final var mapper = new ObjectMapper();
        final var synonymPath = sharedTempDir.resolve("synonym.json");

        final var writer = mapper.createGenerator(synonymPath.toFile(), JsonEncoding.UTF8);
        writer.writeStartObject();
        writer.writeArrayFieldStart("classification_terms");
        writer.writeStartObject();
        writer.writeStringField("key", key);
        writer.writeStringField("value", value);
        writer.writeObjectField("terms", terms);
        writer.writeEndObject();
        writer.writeEndArray();
        writer.writeEndObject();
        writer.close();

        getServer().updateIndexSettings(synonymPath.toString());
        getServer().waitForReady();
    }

    @Test
    void testQueryByClassificationString() throws IOException {
        Importer instance = makeImporter();
        instance.add(List.of(createDoc("amenity", "restaurant", "curliflower")));
        instance.finish();
        refresh();

        String classTerm = Utils.buildClassificationString("amenity", "restaurant");

        assertNotNull(classTerm);

        PhotonResult response = getById(testDocId);
        String classification = (String) response.get(Constants.CLASSIFICATION);
        assertEquals(classification, classTerm);

        List<PhotonResult> result = search(classTerm + " curli");

        assertTrue(result.size() > 0);
        assertEquals(testDocId, result.get(0).get("osm_id"));
    }

    @Test
    void testQueryByClassificationSynonym() throws IOException {
        Importer instance = makeImporter();
        instance.add(List.of(createDoc("amenity", "restaurant", "curliflower")));
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
        instance.add(List.of(createDoc("amenity", "restaurant", "airport")));
        instance.add(List.of(createDoc("aeroway", "terminal", "Houston")));
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
        instance.add(List.of(createDoc("railway", "halt", "Newtown")));
        instance.add(List.of(createDoc("railway", "station", "King's Cross")));
        instance.finish();
        refresh();

        Path synonymPath = sharedTempDir.resolve("synonym.json");

        final var mapper = new ObjectMapper();
        final var writer = mapper.createGenerator(synonymPath.toFile(), JsonEncoding.UTF8);
        writer.writeStartObject();
        writer.writeArrayFieldStart("classification_terms");
        writer.writeStartObject();
        writer.writeStringField("key", "railway");
        writer.writeStringField("value", "station");
        writer.writeObjectField("terms", List.of("Station"));
        writer.writeEndObject();
        writer.writeStartObject();
        writer.writeStringField("key", "railway");
        writer.writeStringField("value", "halt");
        writer.writeObjectField("terms", List.of("Station", "Stop"));
        writer.writeEndObject();
        writer.writeEndArray();
        writer.writeEndObject();
        writer.close();

        getServer().updateIndexSettings(synonymPath.toString());
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
