package de.komoot.photon;

import de.komoot.photon.nominatim.model.AddressType;
import de.komoot.photon.nominatim.model.NameMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.assertj.core.api.Assertions.*;

class PhotonDocAddressSetTest {
    private final PhotonDoc baseDoc = new PhotonDoc()
            .placeId(10000).osmType("N").osmId(123).tagKey("place").tagValue("house")
            .countryCode("de");

    @BeforeEach
    public void setupPhotonDoc() {
        baseDoc.setAddressPartIfNew(AddressType.CITY, Map.of("default", "Hamburg"));
        baseDoc.setAddressPartIfNew(AddressType.STREET, Map.of("default", "Chaussee"));
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
                () -> assertEquals(Map.of("default", street), doc.getAddressParts().get(AddressType.STREET)),
                () -> assertEquals(Map.of("default", "Hamburg"), doc.getAddressParts().get(AddressType.CITY))
        );
    }

    @Test
    void testEmptyAddressUselessDocument() {
        assertThat(new PhotonDocAddressSet(baseDoc, Map.of()))
                .isEmpty();
    }

    @Test
    void testEmptyAddressUsefulDocument() {
        assertThat(new PhotonDocAddressSet(
                baseDoc.names(NameMap.makeForPlace(Map.of("name", "foo"), new String[]{})),
                Map.of()))
                .satisfiesExactly(
                        d -> assertThat(d).isSameAs(baseDoc)
                );
    }

    @Test
    void testIrrelevantAddressParts() {
        assertThat(new PhotonDocAddressSet(baseDoc, Map.of(
                "city", "a",
                "street", "s",
                "place", "p")))
                .isEmpty();
    }

    @Test
    void testSimpleHousenumber() {
        assertThat(new PhotonDocAddressSet(baseDoc, Map.of(
                "housenumber", "1")))
                .satisfiesExactly(
                        d -> assertDocWithHnrAndStreet(d, "1", "Chaussee"));
    }

    @Test
    void testHousenumberList() {
        assertThat(new PhotonDocAddressSet(baseDoc, Map.of(
                "housenumber", "34;; 50 b;")))
                .satisfiesExactly(
                        d -> assertDocWithHousenumber(d, "34"),
                        d2 -> assertDocWithHousenumber(d2, "50 b"));
    }

    @Test
    void testPlaceAddress() {
        assertThat(new PhotonDocAddressSet(baseDoc, Map.of(
                "housenumber", "34;50 b",
                "place", "Nowhere",
                "street", "irrelevant")))
                .satisfiesExactly(
                        d -> assertDocWithHnrAndStreet(d, "34", "Nowhere"),
                        d2 -> assertDocWithHnrAndStreet(d2, "50 b", "Nowhere"));
    }

    @Test
    void testConscriptionAddress() {
        assertThat(new PhotonDocAddressSet(baseDoc, Map.of(
                "housenumber", "34/50",
                "conscriptionnumber", "50",
                "streetnumber", "34",
                "place", "Nowhere")))
                .satisfiesExactly(
                        d -> assertDocWithHnrAndStreet(d, "50", "Nowhere"),
                        d2 -> assertDocWithHnrAndStreet(d2, "34", "Chaussee"));
    }

    @Test
    void testBlockAddress() {
        assertThat(new PhotonDocAddressSet(baseDoc, Map.of(
                "housenumber", "1",
                "block_number", "12")))
                .satisfiesExactly(
                        d -> assertDocWithHnrAndStreet(d, "1", "12")
                );
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "987987誰も住んでいないスーパーマーケット誰も住んでいないスーパーマーケット誰も住んでいないスーパーマーケット誰も住んでいないスーパーマーケット誰も住んでいないスーパーマーケット誰も住んでいないスーパーマーケット誰も住んでいないスーパーマーケット誰も住んでいないスーパーマーケット誰も住んでいないスーパーマー",
            "something bad",
            "14, portsmith"
    })
    void testInvalidHousenumber(String houseNumber) {
        assertThat(new PhotonDocAddressSet(baseDoc, Map.of("housenumber", houseNumber)))
                .isEmpty();

    }

}