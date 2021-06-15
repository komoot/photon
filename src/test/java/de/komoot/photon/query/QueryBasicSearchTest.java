package de.komoot.photon.query;

import com.google.common.collect.ImmutableMap;
import de.komoot.photon.ESBaseTester;
import de.komoot.photon.Importer;
import de.komoot.photon.PhotonDoc;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.SearchHits;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Tests that the {@link PhotonQueryBuilder} produces query which can find all
 * expected results at all. These tests do not check relevance.
 */
public class QueryBasicSearchTest extends ESBaseTester {
    private int testDocId = 10000;

    @Before
    public void setup() throws IOException {
        setUpES();
    }

    private PhotonDoc createDoc(String... names) {
        Map<String, String> nameMap = new HashMap<>();

        for (int i = 0; i < names.length - 1; i += 2) {
            nameMap.put(names[i], names[i+1]);
        }

        ++testDocId;
        return new PhotonDoc(testDocId, "N", testDocId, "place", "city").names(nameMap);
    }

    private SearchHits search(String query) {
        QueryBuilder builder = PhotonQueryBuilder.builder(query, "en", Collections.singletonList("en"), false).buildQuery();
        return getClient().prepareSearch("photon")
                .setSearchType(SearchType.QUERY_THEN_FETCH)
                .setQuery(builder)
                .execute()
                .actionGet()
                .getHits();
    }

    private SearchHits searchLenient(String query) {
        QueryBuilder builder = PhotonQueryBuilder.builder(query, "en", Collections.singletonList("en"), true).buildQuery();
        return getClient().prepareSearch("photon")
                .setSearchType(SearchType.QUERY_THEN_FETCH)
                .setQuery(builder)
                .execute()
                .actionGet()
                .getHits();
    }

    @Test
    public void testSearchByDefaultName() {
        Importer instance = makeImporter();
        instance.add(createDoc("name", "Muffle Flu"));
        instance.finish();
        refresh();

        assertEquals(1, search("muffle flu").getTotalHits());
        assertEquals(1, search("flu").getTotalHits());
        assertEquals(1, search("muffle").getTotalHits());
        assertEquals(0, search("mufle flu").getTotalHits());

        assertEquals(1, searchLenient("mufle flu").getTotalHits());
        assertEquals(1, searchLenient("muffle flu 9").getTotalHits());
        assertEquals(0, searchLenient("huffle fluff").getTotalHits());
    }

    @Test
    public void testSearchByAlternativeNames() {
        final String[] alt_names = new String[]{"alt", "int", "loc", "old", "reg", "housename"};

        Importer instance = makeImporter();
        instance.add(createDoc("name", "original", "alt_name", "alt", "old_name", "older", "int_name", "int",
                               "loc_name", "local", "reg_name", "regional", "addr:housename", "house",
                               "other_name", "other"));
        instance.finish();
        refresh();

        assertEquals(1, search("original").getTotalHits());
        assertEquals(1, search("alt").getTotalHits());
        assertEquals(1, search("older").getTotalHits());
        assertEquals(1, search("int").getTotalHits());
        assertEquals(1, search("local").getTotalHits());
        assertEquals(1, search("regional").getTotalHits());
        assertEquals(1, search("house").getTotalHits());
        assertEquals(0, search("other").getTotalHits());
    }

    @Test
    public void testSearchByNameAndAddress() {
        Map<String, String> address = new HashMap<>();
        address.put("street", "Callino");
        address.put("city", "Madrid");
        address.put("suburb", "Quartier");
        address.put("neighbourhood", "El Block");
        address.put("county", "Montagña");
        address.put("state", "Estado");

        Importer instance = makeImporter();
        instance.add(createDoc("name", "Castillo").address(address));
        instance.finish();
        refresh();

        assertEquals(1, search("castillo").getTotalHits());
        assertEquals(1, search("castillo callino").getTotalHits());
        assertEquals(1, search("castillo quartier madrid").getTotalHits());
        assertEquals(1, search("castillo block montagna estado").getTotalHits());

        assertEquals(0, search("castillo state").getTotalHits());
        //assertEquals(0, search("block montagna estado").getTotalHits());
    }

    @Test
    public void testSearchWithHousenumberNamed() {
        Importer instance = makeImporter();
        instance.add(createDoc("name", "Edeka").houseNumber("5").address(Collections.singletonMap("street", "Hauptstrasse")));
        instance.finish();
        refresh();

        assertEquals(1, search("hauptstrasse 5").getTotalHits());
        assertEquals(1, search("edeka, hauptstrasse").getTotalHits());
        assertEquals(1, search("edeka, hauptstrasse 5").getTotalHits());
        assertEquals(1, search("edeka, hauptstr 5").getTotalHits());
        //assertEquals(0, search("hauptstrasse").getTotalHits());
    }

    @Test
    public void testSearchWithHousenumberUnnamed() {
        Importer instance = makeImporter();
        instance.add(createDoc().houseNumber("5").address(Collections.singletonMap("street", "Hauptstrasse")));
        instance.finish();
        refresh();

        assertEquals(1, search("hauptstrasse 5").getTotalHits());
        assertEquals(0, search("hauptstrasse").getTotalHits());
    }
}
