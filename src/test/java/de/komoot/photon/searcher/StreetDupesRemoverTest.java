package de.komoot.photon.searcher;

import de.komoot.photon.Constants;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class StreetDupesRemoverTest {

    @Test
    public void testDeduplicatesStreets() {
        StreetDupesRemover streetDupesRemover = new StreetDupesRemover("en");
        List<JSONObject> allResults = new ArrayList<>();
        allResults.add(createDummyResult("99999", "Main Street", "highway", "Unclassified"));
        allResults.add(createDummyResult("99999", "Main Street", "highway", "Unclassified"));

        List<JSONObject> dedupedResults = streetDupesRemover.execute(allResults);
        Assert.assertEquals(1, dedupedResults.size());
    }

    @Test
    public void testStreetAndBusStopNotDeduplicated() {
        StreetDupesRemover streetDupesRemover = new StreetDupesRemover("en");
        List<JSONObject> allResults = new ArrayList<>();
        allResults.add(createDummyResult("99999", "Main Street", "highway", "bus_stop"));
        allResults.add(createDummyResult("99999", "Main Street", "highway", "Unclassified"));

        List<JSONObject> dedupedResults = streetDupesRemover.execute(allResults);
        Assert.assertEquals(2, dedupedResults.size());
    }
    
    private JSONObject createDummyResult(String postCode, String name, String osmKey,
                    String osmValue) {
        return new JSONObject().put(Constants.PROPERTIES, new JSONObject()
                        .put(Constants.POSTCODE, postCode).put(Constants.NAME, name)
                        .put(Constants.OSM_KEY, osmKey).put(Constants.OSM_VALUE, osmValue));
    }

}
