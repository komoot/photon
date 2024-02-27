package de.komoot.photon.nominatim;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NominatimConnectorTest {

    @Test
    void testConvertCountryCode() {
        assertEquals("", NominatimConnector.convertCountryCode("".split(",")));
        assertEquals("'uk'", NominatimConnector.convertCountryCode("uk".split(",")));
        assertEquals("'uk','de'", NominatimConnector.convertCountryCode("uk,de".split(",")));
    }
}