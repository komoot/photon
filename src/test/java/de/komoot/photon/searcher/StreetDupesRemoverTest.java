package de.komoot.photon.searcher;

import de.komoot.photon.opensearch.DocFields;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class StreetDupesRemoverTest {

    @Test
    void testDeduplicatesStreets() {
        assertThat(new StreetDupesRemover())
                .accepts(highway("99999", "Main Street", "unclassified", "de"))
                .rejects(highway("99999", "Main Street", "unclassified", "de"))
                .accepts(highway("99999", "Sub Street", "unclassified", "de"))
                .accepts(highway("99999", "Main Street", "unclassified", "ch"));
    }

    @Test
    void testStreetAndBusStopNotDeduplicated() {
        assertThat(new StreetDupesRemover())
                .accepts(highway("99999", "Main Street", "bus_stop", "de"))
                .accepts(highway("99999", "Main Street", "unclassified", "de"));
    }

    @Test
    void testNLStreet() {
        assertThat(new StreetDupesRemover())
                .accepts(highway("2345 XZ", "Main Street", "unclassified", "nl"))
                .accepts(highway("2346 AB", "Main Street", "unclassified", "nl"))
                .rejects(highway("2345 AB", "Main Street", "unclassified", "nl"))
                .accepts(highway("2", "Main Street", "unclassified", "nl"));
    }

    @Test
    void testGBStreet() {
        assertThat(new StreetDupesRemover())
                .accepts(highway("WN8 6LT", "Main Street", "unclassified", "gb"))
                .accepts(highway("WN8 4LT", "Main Street", "unclassified", "gb"))
                .rejects(highway("WN8 634", "Main Street", "unclassified", "gb"))
                .accepts(highway("W", "Main Street", "unclassified", "gb"));

    }
    
    private PhotonResult highway(String postCode, String name, String osmValue, String countryCode) {
        return new MockPhotonResult()
                .put(DocFields.POSTCODE, postCode)
                .putLocalized(DocFields.NAME, "default", name)
                .put(DocFields.OSM_KEY, "highway")
                .put(DocFields.OSM_VALUE, osmValue)
                .put(DocFields.COUNTRYCODE, countryCode.toUpperCase());
    }

}
