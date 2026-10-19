package de.komoot.photon.query;

import de.komoot.photon.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.core.JsonEncoding;
import tools.jackson.databind.exc.MismatchedInputException;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static de.komoot.photon.PhotonResultAssert.assertThat;

class QueryByClassificationTest extends BaseTesterQuery {
    @TempDir
    private Path sharedTempDir;

    @BeforeEach
    void setup() throws IOException {
        setUpES(sharedTempDir.resolve("db"));
    }

    private PhotonDoc createDoc(String key, String value, String name) {
        return testDoc()
                .osmType("W")
                .tagKey(key).tagValue(value)
                .categories(List.of(String.join(".", "osm", key, value)))
                .names(makeDocNames("name", name));
    }

    private void updateClassification(String key, String value, String... terms) throws IOException {
        final var synonymPath = sharedTempDir.resolve("synonym.json");

        try (var writer = JsonMapper.shared().createGenerator(synonymPath.toFile(), JsonEncoding.UTF8)) {
            writer.writeStartObject();
            writer.writeArrayPropertyStart("classification_terms");
            writer.writeStartObject();
            writer.writeStringProperty("key", key);
            writer.writeStringProperty("value", value);
            writer.writePOJOProperty("terms", terms);
            writer.writeEndObject();
            writer.writeEndArray();
            writer.writeEndObject();
        }

        getServer().updateIndexSettings(ConfigSynonyms.loadFromFile(synonymPath.toString()));
        getServer().waitForReady();
    }

    @Test
    void testQueryByClassificationString() {
        var doc = createDoc("amenity", "restaurant", "curliflower");
        setupDocs(doc);

        assertThat(search("#osm.amenity.restaurant curli"), 0).sameOsmID(doc);
    }

    @Test
    void testQueryByClassificationSynonym() throws IOException {
        var doc = createDoc("amenity", "restaurant", "curliflower");
        setupDocs(doc);

        updateClassification("amenity", "restaurant", "pub", "kneipe");

        assertThat(search("pub curli"), 0).sameOsmID(doc);
        assertThat(search("curliflower kneipe"), 0).sameOsmID(doc);
    }


    @Test
    void testSynonymDoNotInterfereWithWords() throws IOException {
        var restaurant = createDoc("amenity", "restaurant", "airport");
        var terminal = createDoc("aeroway", "terminal", "Houston");
        setupDocs(restaurant, terminal);

        updateClassification("aeroway", "terminal", "airport");

        assertThat(search("airport"), 0).sameOsmID(restaurant);
        assertThat(search("airport houston"), 0).sameOsmID(terminal);
    }

    @Test
    void testSameSynonymForDifferentTags() throws IOException {
        var halt = createDoc("railway", "halt", "Newtown");
        var station = createDoc("railway", "station", "King's Cross");
        setupDocs(halt, station);

        Path synonymPath = sharedTempDir.resolve("synonym.json");

        JsonMapper.shared().writeValue(synonymPath.toFile(), Map.of(
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

        getServer().updateIndexSettings(ConfigSynonyms.loadFromFile(synonymPath.toString()));
        getServer().waitForReady();

        assertThat(search("Station newtown"), 0).sameOsmID(halt);
        assertThat(search("newtown stop"), 0).sameOsmID(halt);
        assertThat(search("king's cross Station"), 0).sameOsmID(station);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            """
            {"classification_terms": [{"key": null, "value": "a", "terms":[]}]}
            """,
            """
            {"classification_terms": [{"key": "a", "value": null, "terms":[]}]}
            """,
            """
            {"classification_terms": [{"key": "a", "value": "a", "terms":null}]}
            """
    })
    void testSynonymFileWithMissingField(String json) throws IOException {
        setupDocs();

        Path synonymPath = sharedTempDir.resolve("synonym.json");

        Files.write(synonymPath, List.of(json), StandardCharsets.UTF_8);

        assertThatExceptionOfType(MismatchedInputException.class)
                .isThrownBy(() -> getServer().updateIndexSettings(
                        ConfigSynonyms.loadFromFile(synonymPath.toString())));
    }
}
