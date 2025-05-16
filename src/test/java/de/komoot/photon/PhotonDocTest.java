package de.komoot.photon;

import de.komoot.photon.nominatim.model.AddressType;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;

class PhotonDocTest {

    private PhotonDoc simplePhotonDoc() {
        return new PhotonDoc(1, "W", 2, "highway", "residential").houseNumber("4");
    }

    @Test
    void testCompleteAddressOverwritesStreet() {
        PhotonDoc doc = simplePhotonDoc();
        
        doc.setAddressPartIfNew(AddressType.STREET, Map.of("name", "parent place street"));
        doc.address(Map.of("street", "test street"));

        assertThat(doc.getAddressParts().get(AddressType.STREET))
                .containsEntry("name", "test street");
    }

    @Test
    void testCompleteAddressCreatesStreetIfNonExistantBefore() {
        PhotonDoc doc = simplePhotonDoc();

        doc.address(Map.of("street", "test street"));

        assertThat(doc.getAddressParts().get(AddressType.STREET))
                .containsEntry("name", "test street");

    }

    @Test
    void testAddCountryCode() {
        PhotonDoc doc = new PhotonDoc(1, "W", 2, "highway", "residential").countryCode("de");

        assertThat(doc.getCountryCode())
                .isEqualTo("DE");
    }

}
