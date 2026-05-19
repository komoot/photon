package de.komoot.photon.opensearch;

import de.komoot.photon.ESBaseTester;
import de.komoot.photon.PhotonDoc;
import de.komoot.photon.query.SimpleSearchRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ApostropheNormalizationTest extends ESBaseTester {

    @BeforeAll
    void setUp(@TempDir Path dataDirectory) throws IOException {
        setUpES(dataDirectory);
        var importer = makeImporter();
        importer.add(List.of(
                doc(1, "Tiffany’s"),   // RIGHT SINGLE QUOTATION MARK
                doc(2, "Hawaiʻi"),     // MODIFIER LETTER TURNED COMMA
                doc(3, "O'Connor")     // ASCII baseline
        ));
        importer.finish();
        refresh();
    }

    @AfterAll
    @Override
    public void tearDown() {
        super.tearDown();
    }

    private PhotonDoc doc(long id, String name) {
        return new PhotonDoc(String.valueOf(id), "N", id, "amenity", "cafe")
                .names(makeDocNames("name", name))
                .countryCode("NO")
                .importance(0.05);
    }

    private List<String> hitNames(String query) {
        var request = new SimpleSearchRequest();
        request.setQuery(query);
        return getServer().createSearchHandler(20).search(request).toList()
                .stream()
                .map(r -> r.getLocalised("name", "en"))
                .toList();
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
            // Indexed with curly, queried with ASCII
            "'Tiffany''s',  'Tiffany’s'",
            // Indexed with ASCII, queried with curly
            "'O’Connor', 'O''Connor'",
            // Modifier letter folds the same way
            "'Hawai''i',    'Hawaiʻi'"
    })
    void curlyAndAsciiApostrophesMatchEachOther(String query, String expectedName) {
        assertThat(hitNames(query)).contains(expectedName);
    }
}
