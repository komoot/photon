package de.komoot.photon;

import de.komoot.photon.nominatim.model.AddressType;
import org.junit.Assert;

public class AssertUtil {
    private AssertUtil() {}

    public static void assertAddressName(String name, PhotonDoc doc, AddressType addressType) {
        Assert.assertNotNull(doc.getAddressParts().get(addressType));
        Assert.assertEquals(name, doc.getAddressParts().get(addressType).get("name"));
    }

    public static void assertNoAddress(PhotonDoc doc, AddressType addressType) {
        Assert.assertNull(doc.getAddressParts().get(addressType));
    }
}
