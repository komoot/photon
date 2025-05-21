package de.komoot.photon.query;

import de.komoot.photon.ESBaseTester;
import de.komoot.photon.Importer;
import de.komoot.photon.PhotonDoc;
import de.komoot.photon.searcher.PhotonResult;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
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

    private List<PhotonResult> search(String query) {
        final var request = new SimpleSearchRequest();
        request.setQuery(query);
        request.setLanguage("en");

        return getServer().createSearchHandler(new String[]{"en"}, 1).search(request);
    }

    private void assertWorking(SoftAssertions soft, String... queries) {
        for (String query : queries) {
            soft.assertThat(search(query)).hasSize(1);
        }
    }

    private void assertNotWorking(SoftAssertions soft, String... queries) {
        for (String query : queries) {
            soft.assertThat(search(query)).hasSize(0);
        }
    }

    @Test
    void testSearchByDefaultName() throws IOException {
        Importer instance = makeImporter();
        instance.add(List.of(createDoc("name", "Muffle Flu")));
        instance.finish();
        refresh();

        SoftAssertions soft = new SoftAssertions();
        assertWorking(soft, "muffle flu", "flu", "muffle", "mufle flu", "muffle flu 9");
        assertNotWorking(soft,"huffle fluff");

        soft.assertAll();
    }

    @Test
    void testSearchNameSkipTerms() throws IOException {
        Importer instance = makeImporter();
        instance.add(List.of(createDoc("name", "Hunted House Hotel")));
        instance.finish();
        refresh();

        SoftAssertions soft = new SoftAssertions();
        assertWorking(soft,
                "hunted", "hunted hotel", "hunted house hotel",
                "hunted house hotel 7", "hunted hotel 7");

        soft.assertAll();
    }
    @Test
    void testSearchByAlternativeNames() throws IOException {
        Importer instance = makeImporter();
        instance.add(List.of(
                createDoc("name", "original", "alt_name", "alt", "old_name", "older", "int_name", "int",
                               "loc_name", "local", "reg_name", "regional", "addr:housename", "house",
                               "other_name", "other")));
        instance.finish();
        refresh();

        SoftAssertions soft = new SoftAssertions();
        assertWorking(soft,
                "original", "alt", "older", "int", "local",
                "regional", "house");
        assertNotWorking(soft, "other");

        soft.assertAll();
    }

    @Test
    void testSearchByNameAndAddress() throws IOException {
        Map<String, String> address = new HashMap<>();
        address.put("street", "Callino");
        address.put("city", "Madrid");
        address.put("suburb", "Quartier");
        address.put("neighbourhood", "El Block");
        address.put("county", "Montagña");
        address.put("state", "Estado");

        Importer instance = makeImporter();
        instance.add(List.of(createDoc("name", "Castillo")
                .addAddresses(address, getProperties().getLanguages())));
        instance.finish();
        refresh();

        SoftAssertions soft = new SoftAssertions();
        assertWorking(soft,
                "castillo", "castillo callino",
                "castillo quartier madrid", "castillo block montagna estado");
        assertNotWorking(soft, "castillo state thing");

        soft.assertAll();
    }

    @Test
    void testSearchMustContainANameTerm() throws IOException {
        Importer instance = makeImporter();
        instance.add(List.of(
                createDoc("name", "Palermo")
                        .addAddresses(Map.of("state", "Sicilia"), getProperties().getLanguages())));
        instance.finish();
        refresh();

        SoftAssertions soft = new SoftAssertions();
        assertWorking(soft,
                "Palermo", "Paler", "Palermo Sici", "Sicilia, Paler");
        assertNotWorking(soft, "Sicilia", "Sici");

        soft.assertAll();
    }

    @Test
    void testSearchWithHousenumberNamed() throws IOException {
        Importer instance = makeImporter();
        instance.add(List.of(
                createDoc("name", "Edeka")
                        .houseNumber("5")
                        .addAddresses(Map.of("street", "Hauptstrasse"), getProperties().getLanguages())));
        instance.finish();
        refresh();

        SoftAssertions soft = new SoftAssertions();
        assertWorking(soft,
                "hauptstrasse 5", "edeka, hauptstrasse 5",
                "edeka, hauptstr 5", "edeka, hauptstrasse");

        soft.assertAll();
    }

    @Test
    void testSearchWithHousenumberUnnamed() throws IOException {
        Importer instance = makeImporter();
        instance.add(List.of(
                createDoc()
                        .houseNumber("5")
                        .addAddresses(Map.of("street", "Hauptstrasse"), getProperties().getLanguages())));
        instance.finish();
        refresh();

        SoftAssertions soft = new SoftAssertions();
        assertWorking(soft, "hauptstrasse 5");
        assertNotWorking(soft, "hauptstrasse");

        soft.assertAll();
    }
}
