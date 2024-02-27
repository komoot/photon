package de.komoot.photon.nominatim.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AddressTypeTest {

    /**
     * All ranks covered by Nominatim must return a corresponding Photon rank.
     */
    @Test
    void testAllRanksAreCovered() {
        for (int i = 4; i <= 30; ++i) {
            assertNotNull(AddressType.fromRank(i));
        }
    }
}