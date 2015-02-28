package de.komoot.photon.elasticsearch;

import com.google.common.collect.ImmutableMap;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.PrecisionModel;
import de.komoot.photon.ESBaseTester;
import de.komoot.photon.PhotonDoc;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Created by Sachin Dole on 2/6/2015.
 */
public class FilterByTagSearcherTest extends ESBaseTester {
    GeometryFactory FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

    @Before
    public void setUp() throws IOException {
        //TODO @sdole this could be replaces with a JUnit4 annotation TestRunner that starts the server only once for all tests.
        setUpES();
        deleteAll();

        PhotonDoc doc = this.createDoc(1, "Madame Tussaud", "tourism", null);
        PhotonDoc doc2 = this.createDoc(2, "Madame Tussaud", "tourism", "museum");
        PhotonDoc doc3 = this.createDoc(3, "Madame Tussaud", "tourism", "museum");
        PhotonDoc doc4 = this.createDoc(4, "Madame Tussaud", "tourism", "attraction");
        PhotonDoc doc5 = this.createDoc(5, "Madame Tussaud", "vacation", "museum");
        Importer instance = new Importer(getClient(), "en");
        instance.add(doc);
        instance.add(doc2);
        instance.add(doc3);
        instance.add(doc4);
        instance.add(doc5);
        instance.finish();
        refresh();
    }

    @Test
    public void testSearchWithTagKeyNoValueNoBias() {
        final Searcher searcher = new Searcher(getClient());
        List<JSONObject> searchResults = searcher.search("Madame", "en", null, null, "wrong_tag", null, 15, false);
        assertEquals(0, searchResults.size());
        searchResults = searcher.search("Madame", "en", null, null, "tourism", null, 15, false);
        assertEquals(4, searchResults.size());
    }


    @Test
    public void testSearchWithTagKeyValueNoBias() {
        final Searcher searcher = new Searcher(getClient());
        List<JSONObject> searchResults = searcher.search("Madame", "en", null, null, "wrong_tag", "museum", 15, false);
        assertEquals(0, searchResults.size());
        searchResults = searcher.search("Madame", "en", null, null, "tourism", "museum", 15, false);
        assertEquals(2, searchResults.size());
        assertEquals(2, searchResults.get(0).getJSONObject("properties").getInt("osm_id"));
        assertEquals(3, searchResults.get(1).getJSONObject("properties").getInt("osm_id"));
    }

    @Test
    public void testSearchWithTagKeyNoValueWithBias() {
        final Searcher searcher = new Searcher(getClient());
        List<JSONObject> searchResults = searcher.search("Madame", "en", -87.0, 41.0, "wrong_tag", null, 15, false);
        assertEquals(0, searchResults.size());
        searchResults = searcher.search("Madame", "en", -87.0, 41.0, "tourism", "museum", 15, false);
        assertEquals(2, searchResults.size());
        assertEquals(2, searchResults.get(0).getJSONObject("properties").getInt("osm_id"));
        assertEquals(3, searchResults.get(1).getJSONObject("properties").getInt("osm_id"));
    }

    @Test
    public void testSearchWithTagKeyValueWithBias() {
        final Searcher searcher = new Searcher(getClient());
        List<JSONObject> searchResults = searcher.search("Madame", "en", -87.0, 41.0, "wrong_tag", "museum", 15, false);
        assertEquals(0, searchResults.size());
        searchResults = searcher.search("Madame", "en", 41.0, -87., "tourism", "museum", 15, false);
        assertEquals(2, searchResults.size());
        assertEquals(2, searchResults.get(0).getJSONObject("properties").getInt("osm_id"));
        assertEquals(3, searchResults.get(1).getJSONObject("properties").getInt("osm_id"));
    }

    private PhotonDoc createDoc(int id, String name, String tagKey, String tagValue) {
        ImmutableMap<String, String> nameMap = ImmutableMap.of("name", name, "reg_name", "regName");
        return new PhotonDoc(id, "way", id, tagKey, tagValue, nameMap, null, null, null, 0, 0.5, null, FACTORY.createPoint(new
                                                                                                                                                                                Coordinate(-87., 41.)), 0, 0);
    }
}
