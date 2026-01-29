package de.komoot.photon.searcher;

import de.komoot.photon.opensearch.DocFields;
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
                .put(DocFields.POSTCODE, postCode)
                .putLocalized(DocFields.NAME, "en", name)
                .put(DocFields.OSM_KEY, osmKey)
                .put(DocFields.OSM_VALUE, osmValue);
    }

}
