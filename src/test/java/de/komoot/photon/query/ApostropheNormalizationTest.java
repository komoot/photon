package de.komoot.photon.query;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.IOException;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static de.komoot.photon.PhotonResultAssert.PHOTONRESULT;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ApostropheNormalizationTest extends BaseTesterQuery {

    @BeforeAll
    void setUp(@TempDir Path dataDirectory) throws IOException {
        setDefaultAnalyzerConfig();
        setUpES(dataDirectory);

        setupDocs(
                createDoc("name", "Tiffany’s").osmId(1),
                createDoc("name", "Hawaiʻi").osmId(2),
                createDoc("name", "O'Connor").osmId(3)
        );
    }

    @AfterAll
    @Override
    public void tearDown() {
        super.tearDown();
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
            // Indexed with curly, queried with ASCII
            "'Tiffany''s',  1",
            // Indexed with ASCII, queried with curly
            "'O’Connor', 3",
            // Modifier letter folds the same way
            "'Hawai''i',  2"
    })
    void curlyAndAsciiApostrophesMatchEachOther(String query, int osmId) {
        assertThat(search(query)).singleElement(PHOTONRESULT).sameOsmID(osmId);
    }
}
