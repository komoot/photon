package de.komoot.photon;

import de.komoot.photon.nominatim.model.AddressType;

import static org.junit.jupiter.api.Assertions.*;

public class AssertUtil {
    private AssertUtil() {}

    public static void assertAddressName(String name, PhotonDoc doc, AddressType addressType) {
        assertNotNull(doc.getAddressParts().get(addressType));
        assertEquals(name, doc.getAddressParts().get(addressType).get("name"));
    }

    public static void assertNoAddress(PhotonDoc doc, AddressType addressType) {
        assertNull(doc.getAddressParts().get(addressType));
    }
}
