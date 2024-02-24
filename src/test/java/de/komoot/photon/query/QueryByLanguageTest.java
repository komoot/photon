package de.komoot.photon.query;

import de.komoot.photon.ESBaseTester;
import de.komoot.photon.PhotonDoc;
import de.komoot.photon.Importer;
import de.komoot.photon.nominatim.model.AddressType;
import de.komoot.photon.searcher.PhotonResult;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;


import java.io.IOException;
import java.util.*;

/**
 * Tests for queries in different languages.
 */
@Slf4j
public class QueryByLanguageTest extends ESBaseTester {
    private int testDocId = 10001;
    private String[] languageList;

    private Importer setup(String... languages) throws IOException {
        languageList = languages;
        setUpES(dataDirectory, languages);
        return makeImporterWithLanguages(languages);
    }

    private PhotonDoc createDoc(String... names) {
        Map<String, String> nameMap = new HashMap<>();

        for (int i = 0; i < names.length - 1; i += 2) {
            nameMap.put(names[i], names[i+1]);
        }

        ++testDocId;
        return new PhotonDoc(testDocId, "W", testDocId, "place", "city").names(nameMap);
    }

    private List<PhotonResult> search(String query, String lang) {
        return getServer().createSearchHandler(languageList, 1).search(new PhotonRequest(query, lang));
    }

    @Test
    public void queryNonStandardLanguages() throws IOException {
        Importer instance = setup("en", "fi");

        instance.add(createDoc("name", "original", "name:fi", "finish", "name:ru", "russian"), 0);

        instance.finish();
        refresh();

        assertEquals(1, search("original", "en").size());
        assertEquals(1, search("finish", "en").size());
        assertEquals(0, search("russian", "en").size());

        double enScore = search("finish", "en").get(0).getScore();
        double fiScore = search("finish", "fi").get(0).getScore();

        assertTrue(fiScore > enScore);
    }

    @Test
    public void queryAltNames() throws IOException {
        Importer instance = setup("de");
        instance.add(createDoc("name", "simple", "alt_name", "ancient", "name:de", "einfach"), 0);
        instance.finish();
        refresh();

        assertEquals(1, search("simple", "de").size());
        assertEquals(1, search("einfach", "de").size());
        assertEquals(1, search("ancient", "de").size());

    }

    @ParameterizedTest
    @EnumSource(names = {"STREET", "LOCALITY", "DISTRICT", "CITY", "COUNTRY", "STATE"})
    public void queryAddressPartsLanguages(AddressType addressType) throws IOException {
        Importer instance = setup("en", "de");

        Map<String, String> address_names = new HashMap<>();
        address_names.put("name", "original");
        address_names.put("name:de", "Deutsch");

        PhotonDoc doc = new PhotonDoc(45, "N", 3, "place", "house")
                .names(Collections.singletonMap("name", "here"));

        doc.setAddressPartIfNew(addressType, address_names);

        instance.add(doc, 0);
        instance.finish();
        refresh();

        assertEquals(1, search("here, original", "de").size());
        assertEquals(1, search("here, Deutsch", "de").size());
    }

    @ParameterizedTest
    @ValueSource(strings = {"default", "de", "en"})
    public void queryAltNamesFuzzy(String lang) throws IOException {
        Importer instance = setup("de", "en");
        instance.add(createDoc("name", "simple", "alt_name", "ancient", "name:de", "einfach"), 0);
        instance.finish();
        refresh();

        assertEquals(1, search("simplle", lang).size());
        assertEquals(1, search("einfah", lang).size());
        assertEquals(1, search("anciemt", lang).size());
        assertEquals(0, search("sinister", lang).size());

    }
}
