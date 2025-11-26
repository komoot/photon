package de.komoot.photon.query;

import de.komoot.photon.ESBaseTester;
import de.komoot.photon.Importer;
import de.komoot.photon.PhotonDoc;
import de.komoot.photon.searcher.PhotonResult;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Tests that the database backend produces queries which can find all
 * expected results. These tests do not check relevance.
 */
class QueryBasicSearchTest extends ESBaseTester {
    private int testDocId = 10000;

    @BeforeEach
    void setup(@TempDir Path dataDirectory) throws IOException {
        setUpES(dataDirectory);
    }

    private PhotonDoc createDoc(String... names) {
        ++testDocId;
        return new PhotonDoc()
                .placeId(testDocId).osmType("N").osmId(testDocId).tagKey("place").tagValue("city")
                .names(makeDocNames(names));
    }

    private void setupDocs(PhotonDoc... docs) {
        Importer instance = makeImporter();
        instance.add(Arrays.asList(docs));
        instance.finish();
        refresh();
    }

    private List<PhotonResult> search(String query, String lang) {
        final var request = new SimpleSearchRequest();
        request.setQuery(query);
        if (lang != null) {
            request.setLanguage(lang);
        }

        return getServer().createSearchHandler(1).search(request);
    }

    private void assertWorking(SoftAssertions soft, String... queries) {
        for (var lang : new String[]{"default", "en", null}) {
            for (String query : queries) {
                soft.assertThat(search(query, lang))
                        .withFailMessage("Searching for '%s' (language: %s) failed.", query, lang)
                        .hasSize(1);
            }
        }
    }

    private void assertNotWorking(SoftAssertions soft, String... queries) {
        for (var lang : new String[]{"en", "default", null}) {
            for (String query : queries) {
                soft.assertThat(search(query, lang))
                        .withFailMessage("Searching for '%s' (language: %s) unexpectedly succeeded.", query, lang)
                        .hasSize(0);
            }
        }
    }

    @Test
    void testSearchVeryShortNameCaseInsensitive() {
        setupDocs(createDoc("name", "BER"));

        var soft = new SoftAssertions();
        assertWorking(soft,"ber", "Ber", "BER");
        assertNotWorking(soft, "bär");

        soft.assertAll();
    }

    @Test
    void testSearchVeryShortNameNormalised() {
        setupDocs(createDoc("name", "öl"));

        var soft = new SoftAssertions();
        assertWorking(soft,"Ol", "Öl", "öl", "ol");

        soft.assertAll();
    }

    @Test
    void testSearchSingleWordName() {
        setupDocs(createDoc("name", "Müggeln"));

        var soft = new SoftAssertions();
        assertWorking(soft,"müggeln", "Müggeln", "muggeln", "mugglen");
        assertNotWorking(soft, "mukklen");

        soft.assertAll();
    }

    @ParameterizedTest
    @ValueSource(strings = {"with (braces)", "split | up X", "dot.notation.a.x.c"})
    void testSearchComplexName(String value) {
        setupDocs(createDoc("name", value));

        var soft = new SoftAssertions();
        assertWorking(soft, value);
        soft.assertAll();
    }

    @Test
    void testPrefixMatching() {
        setupDocs(createDoc("name", "Mönchengladbach Hbf"));

        var soft = new SoftAssertions();
        assertWorking(soft,"m", "M", "mo", "Mö", "Mon", "mön", "moen", "moncen", "hbf");
        assertNotWorking(soft, "monn");
        soft.assertAll();
    }

    @Test
    void testSearchByDefaultName() {
        setupDocs(createDoc("name", "Muffle Flu"));

        SoftAssertions soft = new SoftAssertions();
        assertWorking(soft, "muffle flu", "flu", "muffle", "mufle flu", "muffle flu 9");
        assertNotWorking(soft, "huffle fluff");

        soft.assertAll();
    }

    @Test
    void testSearchNameSkipTerms() {
        setupDocs(createDoc("name", "Hunted House Hotel"));

        SoftAssertions soft = new SoftAssertions();
        assertWorking(soft,
                "hunted", "hunted hotel", "hunted house hotel",
                "hunted house hotel 7", "hunted hotel 7");

        soft.assertAll();
    }

    @Test
    void testSearchByAlternativeNames() {
        setupDocs(createDoc(
                "name", "original",
                "alt_name", "alt",
                "old_name", "older",
                "int_name", "int",
                "loc_name", "local",
                "reg_name", "regional",
                "addr:housename", "house",
                "other_name", "other"));

        SoftAssertions soft = new SoftAssertions();
        assertWorking(soft,
                "original", "alt", "older", "int", "local",
                "regional", "house");
        assertNotWorking(soft, "other");

        soft.assertAll();
    }

    @Test
    void testSearchByNameAndAddress() {
        setupDocs(createDoc("name", "Castillo")
                .addAddresses(Map.of(
                                "street", "Callino",
                                "city", "Madrid",
                                "suburb", "Quartier",
                                "neighbourhood", "El Block",
                                "county", "Montagña",
                                "state", "Estado"),
                        getProperties().getLanguages()));

        SoftAssertions soft = new SoftAssertions();
        assertWorking(soft,
                "castillo", "castillo callino",
                "castillo quartier madrid", "castillo block montagna estado");
        assertNotWorking(soft, "castillo state thing");

        soft.assertAll();
    }

    @Test
    void testSearchMustContainANameTerm() {
        setupDocs(
                createDoc("name", "Palermo")
                        .addAddresses(Map.of("state", "Sicilia"), getProperties().getLanguages()));

        SoftAssertions soft = new SoftAssertions();
        assertWorking(soft,
                "Palermo", "Paler", "Palermo Sici", "Sicilia, Paler");
        assertNotWorking(soft, "Sicilia", "Sici");

        soft.assertAll();
    }

    @Test
    void testSearchWithHousenumberNamed() {
        setupDocs(
                createDoc("name", "Edeka")
                        .houseNumber("5")
                        .addAddresses(Map.of("street", "Hauptstrasse"), getProperties().getLanguages()));

        SoftAssertions soft = new SoftAssertions();
        assertWorking(soft,
                "hauptstrasse 5", "edeka, hauptstrasse 5",
                "edeka, hauptstr 5", "edeka, hauptstrasse");

        soft.assertAll();
    }

    @Test
    void testSearchWithHousenumberUnnamed() {
        setupDocs(
                createDoc()
                        .houseNumber("5")
                        .addAddresses(Map.of("street", "Hauptstrasse"), getProperties().getLanguages()));

        SoftAssertions soft = new SoftAssertions();
        assertWorking(soft, "hauptstrasse 5");
        assertNotWorking(soft, "hauptstrasse");

        soft.assertAll();
    }
}
