package de.komoot.photon.query;

import de.komoot.photon.ESBaseTester;
import de.komoot.photon.PhotonDoc;
import de.komoot.photon.Importer;
import de.komoot.photon.nominatim.model.AddressType;
import de.komoot.photon.searcher.PhotonResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

/**
 * Tests for queries in different languages.
 */
class QueryByLanguageTest extends ESBaseTester {
    private int testDocId = 10001;
    private String[] languageList;

    @TempDir
    private Path dataDirectory;

    private Importer setup(String... languages) throws IOException {
        languageList = languages;
        getProperties().setLanguages(languages);
        setUpES(dataDirectory);
        return makeImporter();
    }

    private PhotonDoc createDoc(String... names) {
        ++testDocId;
        return new PhotonDoc()
                .placeId(testDocId).osmType("W").osmId(testDocId).tagKey("place").tagValue("city")
                .names(makeDocNames(names));
    }

    private List<PhotonResult> search(String query, String lang) {
        final var request = new SimpleSearchRequest();
        request.setQuery(query);
        request.setLanguage(lang);

        return getServer().createSearchHandler(languageList, 1).search(request);
    }

    @Test
    void queryNonStandardLanguages() throws IOException {
        Importer instance = setup("en", "fi");

        instance.add(List.of(
                createDoc("name", "original", "name:fi", "finish", "name:ru", "russian")));

        instance.finish();
        refresh();

        assertThat(search("original", "en")).hasSize(1);
        assertThat(search("finish", "en")).hasSize(1);
        assertThat(search("russian", "en")).hasSize(0);

        assertThat(search("finish", "fi").get(0).getScore())
                .isGreaterThan(search("finish", "en").get(0).getScore());
    }

    @Test
    void queryAltNames() throws IOException {
        Importer instance = setup("de");
        instance.add(List.of(
                createDoc("name", "simple", "alt_name", "ancient", "name:de", "einfach")));
        instance.finish();
        refresh();

        assertThat(search("simple", "de")).hasSize(1);
        assertThat(search("einfach", "de")).hasSize(1);
        assertThat(search("ancient", "de")).hasSize(1);
    }

    @ParameterizedTest
    @EnumSource(names = {"STREET", "LOCALITY", "DISTRICT", "CITY", "COUNTRY", "STATE"})
    void queryAddressPartsLanguages(AddressType addressType) throws IOException {
        Importer instance = setup("en", "de");

        PhotonDoc doc = createDoc("name", "here").tagKey("place").tagValue("house");

        doc.setAddressPartIfNew(addressType, makeAddressNames(
                "name", "original",
                "name:de", "deutsch"));

        instance.add(List.of(doc));
        instance.finish();
        refresh();

        assertThat(search("here, original", "de")).hasSize(1);
        assertThat(search("here, Deutsch", "de")).hasSize(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"default", "de", "en"})
    void queryAltNamesFuzzy(String lang) throws IOException {
        Importer instance = setup("de", "en");
        instance.add(List.of(
                createDoc("name", "simple", "alt_name", "ancient", "name:de", "einfach")));
        instance.finish();
        refresh();

        assertThat(search("simplle", lang)).hasSize(1);
        assertThat(search("einfah", lang)).hasSize(1);
        assertThat(search("anciemt", lang)).hasSize(1);
        assertThat(search("sinister", lang)).hasSize(0);
    }
}
