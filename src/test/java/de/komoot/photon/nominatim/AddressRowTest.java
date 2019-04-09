package de.komoot.photon.nominatim;

import org.junit.Test;

import de.komoot.photon.nominatim.model.AddressRow;

import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

public class AddressRowTest {

    /**
     * Test if Berlin is in the list of curated cities
     */
    @Test
    public void testCuratedCitiesList() {
        Map<String, String> name = new HashMap<>();
        name.put("name", "berlin");
        AddressRow row = new AddressRow(198047960L, name, "boundary", "administrative", 0, 4, null, "city", "R", 62422L);
        assertTrue(row.isCity());
        assertTrue(row.isCuratedCity());
    }
}