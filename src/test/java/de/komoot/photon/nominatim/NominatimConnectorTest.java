package de.komoot.photon.nominatim;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class NominatimConnectorTest {

    @Test
    public void testConvertCountryCode() {
        assertEquals("", NominatimConnector.convertCountryCode("".split(",")));
        assertEquals("'uk'", NominatimConnector.convertCountryCode("uk".split(",")));
        assertEquals("'uk','de'", NominatimConnector.convertCountryCode("uk,de".split(",")));
    }
}