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
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

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
        return new PhotonDoc()
                .placeId(Integer.toString(testDocId)).osmType("W").osmId(testDocId)
                .tagKey(key).tagValue(value)
                .categories(List.of(String.join(".", "osm", key, value)))
                .names(makeDocNames("name", name));
    }

    private List<PhotonResult> search(String query) {
        final var request = new SimpleSearchRequest();
        request.setQuery(query);

        return getServer().createSearchHandler(1).search(request);
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
    void testQueryByClassificationString() {
        Importer instance = makeImporter();
        instance.add(List.of(createDoc("amenity", "restaurant", "curliflower")));
        instance.finish();
        refresh();

        assertThat(search("#osm.amenity.restaurant curli"))
                .element(0)
                .satisfies(p -> assertThat(p.get("osm_id")).isEqualTo(testDocId));
    }

    @Test
    void testQueryByClassificationSynonym() throws IOException {
        Importer instance = makeImporter();
        instance.add(List.of(createDoc("amenity", "restaurant", "curliflower")));
        instance.finish();
        refresh();

        updateClassification("amenity", "restaurant", "pub", "kneipe");

        assertThat(search("pub curli"))
                .element(0)
                .satisfies(p -> assertThat(p.get("osm_id")).isEqualTo(testDocId));

        assertThat(search("curliflower kneipe"))
                .element(0)
                .satisfies(p -> assertThat(p.get("osm_id")).isEqualTo(testDocId));
    }


    @Test
    void testSynonymDoNotInterfereWithWords() throws IOException {
        Importer instance = makeImporter();
        instance.add(List.of(createDoc("amenity", "restaurant", "airport")));
        instance.add(List.of(createDoc("aeroway", "terminal", "Houston")));
        instance.finish();
        refresh();

        updateClassification("aeroway", "terminal", "airport");

        assertThat(search("airport"))
                .element(0)
                .satisfies(p -> assertThat(p.get("osm_id")).isEqualTo(testDocId - 1));


        assertThat(search("airport houston"))
                .element(0)
                .satisfies(p -> assertThat(p.get("osm_id")).isEqualTo(testDocId));
    }

    @Test
    void testSameSynonymForDifferentTags() throws IOException {
        Importer instance = makeImporter();
        instance.add(List.of(createDoc("railway", "halt", "Newtown")));
        instance.add(List.of(createDoc("railway", "station", "King's Cross")));
        instance.finish();
        refresh();

        Path synonymPath = sharedTempDir.resolve("synonym.json");

        new ObjectMapper().writeValue(synonymPath.toFile(), Map.of(
                "classification_terms", List.of(
                        Map.of(
                                "key", "railway",
                                "value", "station",
                                "terms", List.of("Station")
                        ),
                        Map.of(
                                "key", "railway",
                                "value", "halt",
                                "terms", List.of("Station", "stop")
                        )
                )
        ));

        getServer().updateIndexSettings(synonymPath.toString());
        getServer().waitForReady();

        assertThat(search("Station newtown"))
                .element(0)
                .satisfies(p -> assertThat(p.get("osm_id")).isEqualTo(testDocId - 1));

        assertThat(search("newtown stop"))
                .element(0)
                .satisfies(p -> assertThat(p.get("osm_id")).isEqualTo(testDocId - 1));

        assertThat(search("king's cross Station"))
                .element(0)
                .satisfies(p -> assertThat(p.get("osm_id")).isEqualTo(testDocId));
    }
}
