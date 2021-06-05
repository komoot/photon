package de.komoot.photon.query;

import com.google.common.collect.ImmutableMap;
import de.komoot.photon.*;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.index.query.QueryBuilder;
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

    private PhotonDoc createDoc(String key, String value) {
       ImmutableMap<String, String> nameMap = ImmutableMap.of("name", "curliflower");

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
        instance.add(createDoc("amenity", "restaurant"));
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
}
