package de.komoot.photon;

import java.util.HashMap;

import de.komoot.photon.nominatim.model.AddressType;
import org.junit.Assert;
import org.junit.Test;

public class PhotonDocTest {

    @Test
    public void testCompleteAddressOverwritesStreet() {
        PhotonDoc doc = simplePhotonDoc();
        
        HashMap<String, String> streetNames = new HashMap<>();
        streetNames.put("name", "parent place street");
        doc.setAddressPartIfNew(AddressType.STREET, streetNames);

        HashMap<String, String> address = new HashMap<>();
        address.put("street", "test street");
        doc.address(address);
        AssertUtil.assertAddressName("test street", doc, AddressType.STREET);
    }

    @Test
    public void testCompleteAddressCreatesStreetIfNonExistantBefore() {
        PhotonDoc doc = simplePhotonDoc();

        HashMap<String, String> address = new HashMap<>();
        address.put("street", "test street");
        doc.address(address);
        AssertUtil.assertAddressName("test street", doc, AddressType.STREET);
    }

    @Test
    public void testAddCountryCode() {
        PhotonDoc doc = new PhotonDoc(1, "W", 2, "highway", "residential").countryCode("de");

        Assert.assertNotNull(doc.getCountryCode());
        Assert.assertEquals("DE", doc.getCountryCode().getAlpha2());
    }

    private PhotonDoc simplePhotonDoc() {
        return new PhotonDoc(1, "W", 2, "highway", "residential").houseNumber("4");
    }

}
