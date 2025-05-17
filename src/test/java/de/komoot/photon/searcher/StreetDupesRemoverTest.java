package de.komoot.photon.searcher;

import de.komoot.photon.Constants;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

class StreetDupesRemoverTest {

    @Test
    void testDeduplicatesStreets() {
        StreetDupesRemover streetDupesRemover = new StreetDupesRemover("en");
        var allResults = List.of(
            createDummyResult("99999", "Main Street", "highway", "Unclassified"),
            createDummyResult("99999", "Main Street", "highway", "Unclassified"));

        assertThat(streetDupesRemover.execute(allResults))
                .hasSize(1);
    }

    @Test
    void testStreetAndBusStopNotDeduplicated() {
        StreetDupesRemover streetDupesRemover = new StreetDupesRemover("en");
        var allResults = List.of(
            createDummyResult("99999", "Main Street", "highway", "bus_stop"),
            createDummyResult("99999", "Main Street", "highway", "Unclassified"));

        assertThat(streetDupesRemover.execute(allResults))
                .hasSize(2);
    }
    
    private PhotonResult createDummyResult(String postCode, String name, String osmKey,
                    String osmValue) {
        return new MockPhotonResult()
                .put(Constants.POSTCODE, postCode)
                .putLocalized(Constants.NAME, "en", name)
                .put(Constants.OSM_KEY, osmKey)
                .put(Constants.OSM_VALUE, osmValue);
    }

}
