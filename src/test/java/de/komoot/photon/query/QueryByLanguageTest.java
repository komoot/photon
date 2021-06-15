package de.komoot.photon.query;

import de.komoot.photon.ESBaseTester;
import de.komoot.photon.PhotonDoc;
import de.komoot.photon.elasticsearch.Importer;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.index.query.QueryBuilder;
import org.junit.Test;
import static org.junit.Assert.*;


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
}
