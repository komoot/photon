package de.komoot.photon.query;

import com.google.common.collect.ImmutableMap;
import de.komoot.photon.*;
import de.komoot.photon.elasticsearch.IndexSettings;
import de.komoot.photon.elasticsearch.PhotonIndex;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.index.query.QueryBuilder;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Collections;

import static org.junit.Assert.*;


public class QueryByClassificationTest extends ESBaseTester {
    private int testDocId = 10000;

    @Before
    public void setup() throws IOException {
        setUpES();
    }

    private PhotonDoc createDoc(String key, String value, String name) {
       ImmutableMap<String, String> nameMap = ImmutableMap.of("name", name);

        ++testDocId;
        return new PhotonDoc(testDocId, "W", testDocId, key, value).names(nameMap);
    }

    private SearchResponse search(String query) {
        QueryBuilder builder = PhotonQueryBuilder.builder(query, "en", Collections.singletonList("en"), false).buildQuery();
        return getClient().prepareSearch("photon")
                .setSearchType(SearchType.QUERY_THEN_FETCH)
                .setQuery(builder)
                .execute()
                .actionGet();
    }

    @Test
    public void testQueryByClassificationString() {
        Importer instance = makeImporter();
        instance.add(createDoc("amenity", "restaurant", "curliflower"));
        instance.finish();
        refresh();

        String class_term = Utils.buildClassificationString("amenity", "restaurant");

        assertNotNull(class_term);

        GetResponse response = getById(testDocId);
        String classification = (String) response.getSource().get(Constants.CLASSIFICATION);
        assertEquals(classification, class_term);

        SearchResponse result = search(class_term + " curli");

        assertTrue(result.getHits().getTotalHits() > 0);
        assertEquals(Integer.toString(testDocId), result.getHits().getHits()[0].getId());
    }

    @Test
    public void testQueryByClassificationSynonym() {
        Importer instance = makeImporter();
        instance.add(createDoc("amenity", "restaurant", "curliflower"));
        instance.finish();
        refresh();

        JSONArray terms = new JSONArray()
                .put(new JSONObject()
                        .put("key", "amenity")
                        .put("value", "restaurant")
                        .put("terms", new JSONArray().put("pub").put("kneipe"))
                );
        new IndexSettings().setClassificationTerms(terms).updateIndex(getClient(), PhotonIndex.NAME);
        getClient().admin().cluster().prepareHealth().setWaitForYellowStatus().get();

        SearchResponse result = search("pub curli");
        assertTrue(result.getHits().getTotalHits() > 0);
        assertEquals(Integer.toString(testDocId), result.getHits().getHits()[0].getId());


        result = search("curliflower kneipe");
        assertTrue(result.getHits().getTotalHits() > 0);
        assertEquals(Integer.toString(testDocId), result.getHits().getHits()[0].getId());
    }


    @Test
    public void testSynonymDoNotInterfereWithWords() {
        Importer instance = makeImporter();
        instance.add(createDoc("amenity", "restaurant", "airport"));
        instance.add(createDoc("aeroway", "terminal", "Houston"));
        instance.finish();
        refresh();

        JSONArray terms = new JSONArray()
                .put(new JSONObject()
                        .put("key", "aeroway")
                        .put("value", "terminal")
                        .put("terms", new JSONArray().put("airport"))
                );
        new IndexSettings().setClassificationTerms(terms).updateIndex(getClient(), PhotonIndex.NAME);
        getClient().admin().cluster().prepareHealth().setWaitForYellowStatus().get();

        SearchResponse result = search("airport");
        assertTrue(result.getHits().getTotalHits() > 0);
        assertEquals(Integer.toString(testDocId - 1), result.getHits().getHits()[0].getId());


        result = search("airport houston");
        assertTrue(result.getHits().getTotalHits() > 0);
        assertEquals(Integer.toString(testDocId), result.getHits().getHits()[0].getId());
    }

    @Test
    public void testSameSynonymForDifferentTags() {
        Importer instance = makeImporter();
        instance.add(createDoc("railway", "halt", "Newtown"));
        instance.add(createDoc("railway", "station", "King's Cross"));
        instance.finish();
        refresh();

        JSONArray terms = new JSONArray()
                .put(new JSONObject()
                        .put("key", "railway")
                        .put("value", "station")
                        .put("terms", new JSONArray().put("Station"))
                ).put(new JSONObject()
                        .put("key", "railway")
                        .put("value", "halt")
                        .put("terms", new JSONArray().put("Station").put("Stop"))
                );
        new IndexSettings().setClassificationTerms(terms).updateIndex(getClient(), PhotonIndex.NAME);
        getClient().admin().cluster().prepareHealth().setWaitForYellowStatus().get();

        SearchResponse result = search("Station newtown");
        assertTrue(result.getHits().getTotalHits() > 0);
        assertEquals(Integer.toString(testDocId - 1), result.getHits().getHits()[0].getId());

        result = search("newtown stop");
        assertTrue(result.getHits().getTotalHits() > 0);
        assertEquals(Integer.toString(testDocId - 1), result.getHits().getHits()[0].getId());

        result = search("king's cross Station");
        assertTrue(result.getHits().getTotalHits() > 0);
        assertEquals(Integer.toString(testDocId), result.getHits().getHits()[0].getId());
    }
}
