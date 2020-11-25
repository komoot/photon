package de.komoot.photon.nominatim.model;

import org.junit.Test;

import static org.junit.Assert.*;

public class AddressTypeTest {

    /**
     * All ranks coverd by Nominatim must return a corresponding Photon rank.
     */
    @Test
    public void testAllRanksAreCovered() {
        for (int i = 4; i <= 30; ++i) {
            assertNotNull(AddressType.fromRank(i));
        }
    }
}