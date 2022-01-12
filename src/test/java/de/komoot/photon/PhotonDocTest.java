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

    @Test
    public void testSetNewAddressPart() {
        PhotonDoc doc = simplePhotonDoc();

        HashMap<String, String> streetNames = new HashMap<>();
        streetNames.put("name", "parent place street");
        Assert.assertTrue(doc.setAddressPartIfNew(AddressType.STREET, streetNames));
    }

    @Test
    public void testSetOtherAddressPart() {
        PhotonDoc doc = simplePhotonDoc();

        HashMap<String, String> streetNames = new HashMap<>();
        streetNames.put("name", "parent place street");
        Assert.assertTrue(doc.setAddressPartIfNew(AddressType.STREET, streetNames));

        HashMap<String, String> streetNames2 = new HashMap<>();
        streetNames2.put("name", "parent place street 2");
        Assert.assertFalse(doc.setAddressPartIfNew(AddressType.STREET, streetNames2));
    }

    @Test
    public void testSetNewAddressPartTwice() {
        PhotonDoc doc = simplePhotonDoc();

        HashMap<String, String> streetNames = new HashMap<>();
        streetNames.put("name", "parent place street");
        Assert.assertTrue(doc.setAddressPartIfNew(AddressType.STREET, streetNames));
        Assert.assertTrue(doc.setAddressPartIfNew(AddressType.STREET, streetNames));
    }

    @Test
    public void testSetNewAddressPartTwiceWithOtherObject() {
        PhotonDoc doc = simplePhotonDoc();

        HashMap<String, String> streetNames1 = new HashMap<>();
        streetNames1.put("name", "parent place street");
        Assert.assertTrue(doc.setAddressPartIfNew(AddressType.STREET, streetNames1));

        HashMap<String, String> streetNames2 = new HashMap<>();
        streetNames2.put("name", "parent place street");
        Assert.assertTrue(doc.setAddressPartIfNew(AddressType.STREET, streetNames2));
    }

    private PhotonDoc simplePhotonDoc() {
        return new PhotonDoc(1, "W", 2, "highway", "residential").houseNumber("4");
    }

}
