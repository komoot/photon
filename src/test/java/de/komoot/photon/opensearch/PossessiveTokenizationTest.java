package de.komoot.photon.opensearch;

import de.komoot.photon.ESBaseTester;
import de.komoot.photon.PhotonDoc;
import de.komoot.photon.query.SimpleSearchRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PossessiveTokenizationTest extends ESBaseTester {

    @BeforeAll
    void setUp(@TempDir Path dataDirectory) throws IOException {
        setUpES(dataDirectory);
        var importer = makeImporter();
        importer.add(List.of(
                doc(1, "Tiffany's"),
                doc(2, "Lio's Cafe Bar"),
                doc(3, "O'Connor"),
                doc(4, "L'Etoile"),
                doc(5, "Oslo S")
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

    @Test
    void indexNameNgramAnalyzerOutput() {
        assertThat(getTestServer().analyze("index_name_ngram", "lio's")).contains("lio", "lios").doesNotContain("s");
    }

    @Test
    void osloSDoesNotPullInPossessivePois() {
        var hits = hitNames("Oslo S");
        assertThat(hits).contains("Oslo S");
        assertThat(hits).doesNotContain("Tiffany's", "Lio's Cafe Bar");
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
            "'Tiffany',    'Tiffany''s'",
            "'Lio',        'Lio''s Cafe Bar'",
            "'O''Connor',  'O''Connor'",
            "'Connor',     'O''Connor'",
            "'O Connor',   'O''Connor'",
            "'L''Etoile',  'L''Etoile'",
            "'Etoile',     'L''Etoile'"
    })
    void queryFindsExpectedHit(String query, String expectedName) {
        assertThat(hitNames(query)).contains(expectedName);
    }

    @ParameterizedTest(name = "{0} must NOT surface {1}")
    @CsvSource({
            "'Tiffany',  'Lio''s Cafe Bar'",
            "'Tiffany',  'O''Connor'",
            "'Connor',   'Tiffany''s'",
            "'Connor',   'Lio''s Cafe Bar'"
    })
    void queryDoesNotReturnUnrelatedHit(String query, String forbiddenName) {
        assertThat(hitNames(query)).doesNotContain(forbiddenName);
    }
}
