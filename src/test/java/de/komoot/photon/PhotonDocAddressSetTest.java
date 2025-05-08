package de.komoot.photon;

import de.komoot.photon.nominatim.model.AddressType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PhotonDocAddressSetTest {
    private final PhotonDoc baseDoc = new PhotonDoc(10000, "N", 123, "place", "house")
            .countryCode("de");

    @BeforeEach
    public void setupPhotonDoc() {
        baseDoc.setAddressPartIfNew(AddressType.CITY, Map.of("name", "Hamburg"));
        baseDoc.setAddressPartIfNew(AddressType.STREET, Map.of("name", "Chaussee"));
    }

    private void assertDocWithHousenumber(PhotonDoc doc, String housenumber) {
        assertAll(
                () -> assertNotSame(baseDoc, doc),
                () -> assertEquals("place", doc.getTagKey()),
                () -> assertEquals("house", doc.getTagValue()),
                () -> assertEquals(10000, doc.getPlaceId()),
                () -> assertEquals("N", doc.getOsmType()),
                () -> assertEquals(123, doc.getOsmId()),
                () -> assertEquals(housenumber, doc.getHouseNumber())
        );
    }

    private void assertDocWithHnrAndStreet(PhotonDoc doc, String housenumber, String street) {
        assertAll(
                () -> assertDocWithHousenumber(doc, housenumber),
                () -> assertEquals(Map.of("name", street), doc.getAddressParts().get(AddressType.STREET)),
                () -> assertEquals(Map.of("name", "Hamburg"), doc.getAddressParts().get(AddressType.CITY))
        );
    }

    @Test
    void testEmptyAddressUselessDocument() {
        var it = new PhotonDocAddressSet(baseDoc, Map.of()).iterator();

        assertFalse(it.hasNext());
    }

    @Test
    void testEmptyAddressUsefulDocument() {
        var it = new PhotonDocAddressSet(baseDoc.names(Map.of("name", "foo")),
                                                            Map.of()).iterator();

        assertSame(it.next(), baseDoc);
        assertFalse(it.hasNext());
    }

    @Test
    void testIrrelevantAddressParts() {
        var it = new PhotonDocAddressSet(
                baseDoc, Map.of("city", "a", "street", "s", "place", "p")
        ).iterator();

        assertFalse(it.hasNext());
    }

    @Test
    void testSimpleHousenumber() {
        var it = new PhotonDocAddressSet(
                baseDoc, Map.of("housenumber", "1")
        ).iterator();

        assertDocWithHnrAndStreet(it.next(), "1", "Chaussee");
        assertFalse(it.hasNext());
    }

    @Test
    void testHousenumberList() {
        var it = new PhotonDocAddressSet(
                baseDoc, Map.of("housenumber", "34;; 50 b;")
        ).iterator();

        assertDocWithHousenumber(it.next(), "34");
        assertDocWithHousenumber(it.next(), "50 b");
        assertFalse(it.hasNext());
    }

    @Test
    void testPlaceAddress() {
        var it = new PhotonDocAddressSet(
                baseDoc, Map.of("housenumber", "34;50 b",
                                "place", "Nowhere",
                                "street", "irrelevant")
        ).iterator();

        assertDocWithHnrAndStreet(it.next(), "34", "Nowhere");
        assertDocWithHnrAndStreet(it.next(), "50 b", "Nowhere");
        assertFalse(it.hasNext());

    }

    @Test
    void testConscriptionAddress() {
        var it = new PhotonDocAddressSet(
                baseDoc, Map.of("housenumber", "34/50",
                                "conscriptionnumber", "50",
                                "streetnumber", "34",
                                "place", "Nowhere"
                              )
        ).iterator();

        assertDocWithHnrAndStreet(it.next(), "50", "Nowhere");
        assertDocWithHnrAndStreet(it.next(), "34", "Chaussee");
        assertFalse(it.hasNext());
    }

    @Test
    void testBlockAddress() {
        var it = new PhotonDocAddressSet(
                baseDoc, Map.of("housenumber", "1",
                                "block_number", "12")
        ).iterator();

        assertDocWithHnrAndStreet(it.next(), "1", "12");
        assertFalse(it.hasNext());

    }

    @ParameterizedTest
    @ValueSource(strings = {
            "987987誰も住んでいないスーパーマーケット誰も住んでいないスーパーマーケット誰も住んでいないスーパーマーケット誰も住んでいないスーパーマーケット誰も住んでいないスーパーマーケット誰も住んでいないスーパーマーケット誰も住んでいないスーパーマーケット誰も住んでいないスーパーマーケット誰も住んでいないスーパーマー",
            "something bad",
            "14, portsmith"
    })
    void testInvalidHousenumber(String houseNumber) {
        var it = new PhotonDocAddressSet(
                baseDoc, Map.of("housenumber", houseNumber)
        ).iterator();

        assertFalse(it.hasNext());
    }

}