package de.komoot.photon.utils;

import de.komoot.photon.ESBaseTester;
import de.komoot.photon.PhotonDoc;
import de.komoot.photon.elasticsearch.DatabaseProperties;
import de.komoot.photon.elasticsearch.Importer;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.index.query.QueryBuilders;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConvertToJsonTest extends ESBaseTester {

    private SearchResponse databaseFromDoc(PhotonDoc doc) throws IOException {
        setUpES();
        Importer instance = makeImporterWithExtra("maxspeed,website");
        instance.add(doc);
        instance.finish();
        refresh();

        return getClient().prepareSearch("photon")
                .setSearchType(SearchType.QUERY_THEN_FETCH)
                .setQuery(QueryBuilders.boolQuery().must(QueryBuilders.matchAllQuery())
                        .filter(QueryBuilders.boolQuery().mustNot(QueryBuilders.idsQuery().addIds(DatabaseProperties.PROPERTY_DOCUMENT_ID))))
                .execute()
                .actionGet();
    }

    @Test
    public void testConvertWithExtraTags() throws IOException {
        Map<String, String> extratags = new HashMap<>();
        extratags.put("website", "foo");
        extratags.put("maxspeed", "100 mph");

        SearchResponse response = databaseFromDoc(new PhotonDoc(1234, "N", 1000, "place", "city").extraTags(extratags));

        List<JSONObject> json = new ConvertToJson("de").convert(response, false);

        JSONObject extra = json.get(0).getJSONObject("properties").getJSONObject("extra");

        assertEquals(2, extra.length());
        assertEquals("foo", extra.getString("website"));
        assertEquals("100 mph", extra.getString("maxspeed"));
    }


    @Test
    public void testConvertWithoutExtraTags() throws IOException {
        SearchResponse response = databaseFromDoc(new PhotonDoc(1234, "N", 1000, "place", "city"));

        List<JSONObject> json = new ConvertToJson("de").convert(response, false);

        assertNull(json.get(0).getJSONObject("properties").optJSONObject("extra"));
    }
}
