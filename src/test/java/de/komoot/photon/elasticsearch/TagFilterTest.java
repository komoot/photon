package de.komoot.photon.elasticsearch;

import com.google.common.collect.ImmutableMap;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.PrecisionModel;
import de.komoot.photon.ESBaseTester;
import de.komoot.photon.PhotonDoc;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

/**
 * Starter test for tags
 *
 * @author Sachin Jan-26-2014
 */
public class TagFilterTest extends ESBaseTester {
    GeometryFactory FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

    @Before
    public void setUp() throws IOException {
        setUpES();
        deleteAll();

        PhotonDoc street4 = new PhotonDoc(123, "way", 333, "aTag", "aValue", ImmutableMap.of("name", "hello"), "1234", ImmutableMap.of("tag2", "value2", "tag3", "value3"), null, 3333, 0.5, null,
                                          FACTORY.createPoint(new Coordinate(10., 47.)), 0, 0);
        street4.setState(ImmutableMap.of("name", "state"));
        Importer instance = new Importer(getClient(), "en"); // hardcoded lang
        instance.add(street4);
        instance.finish();
        refresh();
    }

    @Test
    public void checkState() {
        final Searcher searcher = new Searcher(getClient());

        List<JSONObject> searchResults = searcher.search("hello state", "en", null, null, 10, false);
        Assert.assertEquals(1, searchResults.size());
        JSONObject searchResultItem = searchResults.get(0);
        JSONObject properties = searchResultItem.getJSONObject("properties");

    }

}
