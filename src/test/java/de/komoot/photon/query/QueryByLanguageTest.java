package de.komoot.photon.query;

import de.komoot.photon.ESBaseTester;
import de.komoot.photon.PhotonDoc;
import de.komoot.photon.Importer;
import de.komoot.photon.elasticsearch.PhotonQueryBuilder;
import de.komoot.photon.nominatim.model.AddressType;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.index.query.QueryBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.*;


import java.io.IOException;
import java.util.*;

/**
 * Tests for queries in different languages.
 */
public class QueryByLanguageTest extends ESBaseTester {
    private int testDocId = 10001;
    private List<String> languageList;

    private Importer setup(String... languages) throws IOException {
        languageList = Arrays.asList(languages);
        setUpES(languages);
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

    private SearchResponse search(String query, String lang) {
        QueryBuilder builder = PhotonQueryBuilder.builder(query, lang, languageList, false).buildQuery();
        return getClient().prepareSearch("photon")
                .setSearchType(SearchType.QUERY_THEN_FETCH)
                .setQuery(builder)
                .execute()
                .actionGet();
    }

    @Test
    public void queryNonStandardLanguages() throws IOException {
        Importer instance = setup("en", "fi");

        instance.add(createDoc("name", "original", "name:fi", "finish", "name:ru", "russian"));

        instance.finish();
        refresh();

        assertEquals(1, search("original", "en").getHits().getTotalHits());
        assertEquals(1, search("finish", "en").getHits().getTotalHits());
        assertEquals(0, search("russian", "en").getHits().getTotalHits());

        float enScore = search("finish", "en").getHits().getHits()[0].getScore();
        float fiScore = search("finish", "fi").getHits().getHits()[0].getScore();

        assertTrue(fiScore > enScore);
    }

    @Test
    public void queryAltNames() throws IOException {
        Importer instance = setup("de");
        instance.add(createDoc("name", "simple", "alt_name", "ancient", "name:de", "einfach"));
        instance.finish();
        refresh();

        assertEquals(1, search("simple", "de").getHits().getTotalHits());
        assertEquals(1, search("einfach", "de").getHits().getTotalHits());
        assertEquals(1, search("ancient", "de").getHits().getTotalHits());

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

        instance.add(doc);
        instance.finish();
        refresh();

        assertEquals(1, search("here, original", "de").getHits().getTotalHits());
        assertEquals(1, search("here, Deutsch", "de").getHits().getTotalHits());
    }
}
