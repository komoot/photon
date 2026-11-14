package de.komoot.photon.opensearch;

import de.komoot.photon.query.StructuredSearchRequest;
import de.komoot.photon.ESBaseTester;
import de.komoot.photon.PhotonDoc;
import de.komoot.photon.nominatim.model.AddressType;
import de.komoot.photon.searcher.PhotonResult;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static de.komoot.photon.PhotonResultAssert.*;


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class StructuredQueryTest extends ESBaseTester {

    private final PhotonDoc country = new PhotonDoc("0", "R", 100, "place", "country")
            .names(makeDocNames("name", "Germany"))
            .countryCode("DE")
            .importance(1.0)
            .addressType(AddressType.COUNTRY);

    private final PhotonDoc city = new PhotonDoc("1", "R", 1, "place", "city")
            .names(makeDocNames("name", "Some City"))
            .countryCode("DE")
            .postcode("12345")
            .importance(1.0)
            .addressType(AddressType.CITY);

    private final PhotonDoc suburb = new PhotonDoc("2", "N", 2, "place", "suburb")
            .names(makeDocNames("name", "MajorSuburb"))
            .countryCode("DE")
            .postcode("12346")
            .addAddresses(Map.of("city", "Some City"), getProperties().getLanguages())
            .importance(1.0)
            .addressType(AddressType.DISTRICT);

    private final PhotonDoc street = new PhotonDoc("3", "W", 3, "place", "street")
            .names(makeDocNames("name", "Some street"))
            .countryCode("DE")
            .postcode("12345")
            .addAddresses(Map.of("city", "Some City"), getProperties().getLanguages())
            .importance(1.0)
            .addressType(AddressType.STREET);

    private final PhotonDoc house = new PhotonDoc("4", "R", 4, "place", "house")
            .countryCode("DE")
            .postcode("12345")
            .addAddresses(Map.of("city", "Some City", "street", "Some street"), getProperties().getLanguages())
            .houseNumber("42")
            .importance(1.0)
            .addressType(AddressType.HOUSE);

    private final PhotonDoc busStop = new PhotonDoc("8", "N", 8, "highway", "house")
            .names(makeDocNames("name", "Some City Some street"))
            .countryCode("DE")
            .postcode("12345")
            .importance(0.9)
            .addressType(AddressType.HOUSE);

    private final PhotonDoc postcode = new PhotonDoc("10", "P", 1000, "place", "postcode")
            .names(makeDocNames("name", "12346"))
            .countryCode("DE")
            .addAddresses(Map.of("city", "Some City"), getProperties().getLanguages())
            .importance(0.2)
            .categories(List.of("osm.place.postcode"))
            .addressType(AddressType.OTHER);

    private final PhotonDoc postcode2 = new PhotonDoc("11", "P", 1001, "place", "postcode")
            .names(makeDocNames("name", "44512"))
            .countryCode("DE")
            .addAddresses(Map.of("city", "Some City"), getProperties().getLanguages())
            .importance(0.2)
            .categories(List.of("osm.place.postcode"))
            .addressType(AddressType.OTHER);

    private final PhotonDoc hamletHouse1 = addHamletHouse(5, "1");
    private final PhotonDoc hamletHouse2 = addHamletHouse(6, "2");
    private final PhotonDoc hamletHouse3 = addHamletHouse(7, "3");
    private final PhotonDoc hamletHouse4 = addHamletHouse(99, "5a");


    private PhotonDoc addHamletHouse(int id, String houseNumber) {
        return new PhotonDoc(Integer.toString(id), "R", id, "place", "house")
                .countryCode("DE")
                .addAddresses(Map.of("city", "Some City", "suburb", "Hamlet"), getProperties().getLanguages())
                .houseNumber(houseNumber)
                .importance(1.0)
                .addressType(AddressType.HOUSE);
    }


    private List<PhotonResult> search(StructuredSearchRequest request) {
        var queryHandler = getServer().createStructuredSearchHandler(1);
        return queryHandler.search(request).toList();
    }

    @BeforeAll
    void setUp(@TempDir Path dataDirectory) throws Exception {
        getProperties().setLanguages(Set.of("en", "de", "fr"));
        setUpES(dataDirectory);

        setupDocs(country, city, suburb, street, house, postcode, postcode2, busStop,
                hamletHouse1, hamletHouse2, hamletHouse3, hamletHouse4);
    }

    @AfterAll
    @Override
    public void tearDown() {
        super.tearDown();
    }

    @Test
    void findsDistrictFuzzy() {
        var request = new StructuredSearchRequest();
        request.setCountryCode("DE");
        request.setDistrict("MajorSuburbb");

        assertThat(search(request)).first(PHOTONRESULT).sameOsmID(suburb);
    }

    @Test
    void findsPostcode() {
        var request = new StructuredSearchRequest();
        request.setCountryCode("DE");
        request.setPostCode("12346");

        assertThat(search(request)).first(PHOTONRESULT).sameOsmID(postcode);
    }

    @Test
    void findsDistrictByPostcode() {
        var request = new StructuredSearchRequest();
        request.setCountryCode("DE");
        request.setCity("Some City");
        request.setPostCode("12346");

        assertThat(search(request)).first(PHOTONRESULT).sameOsmID(suburb);
    }

    @Test
    void findsHouseNumberInHamletWithoutStreetName() {
        var request = new StructuredSearchRequest();
        request.setDistrict("Hamlet");
        request.setHouseNumber("2");

        assertThat(search(request)).singleElement(PHOTONRESULT).sameOsmID(hamletHouse2);
    }

    @Test
    void streetSearchDoesNotReturnBusStops() {
        var request = new StructuredSearchRequest();
        request.setCountryCode("DE");
        request.setCity("Some City");
        request.setStreet("Some street");

        assertThat(search(request))
                .noneSatisfy(p -> assertThat(p).sameOsmID(busStop));
    }

    @Test
    void returnsOnlyCountryForCountryRequests() {
        var request = new StructuredSearchRequest();
        request.setCountryCode("DE");

        assertThat(search(request)).singleElement(PHOTONRESULT).sameOsmID(country);
    }

    @Test
    void doesNotReturnHousesForCityRequest() {
        var request = new StructuredSearchRequest();
        request.setCountryCode("DE");
        request.setCity("Some City");

        assertThat(search(request))
                .allSatisfy(p -> assertThat(p)
                        .hasNoField(DocFields.HOUSENUMBER)
                        .hasNoLocalisedField(DocFields.STREET, "en"));
    }

    @Test
    void testNonexistingStreetFallsBackToCity() {
        var request = new StructuredSearchRequest();
        request.setCountryCode("DE");
        request.setCity("Some City");
        request.setStreet("totally wrong");
        request.setHouseNumber("42");

        assertThat(search(request)).singleElement(PHOTONRESULT).sameOsmID(city);
    }

    @Test
    void testDistrictAsCity() {
        var request = new StructuredSearchRequest();
        request.setCountryCode("DE");
        request.setCity("MajorSuburb");

        assertThat(search(request)).singleElement(PHOTONRESULT).sameOsmID(suburb);
    }

    @Test
    void testMissingHouseNumberFallsBackToStreet() {
        var request = new StructuredSearchRequest();
        request.setCountryCode("DE");
        request.setCity("Some City");
        request.setStreet("Some street");
        request.setHouseNumber("1");

        assertThat(search(request)).singleElement(PHOTONRESULT).sameOsmID(street);
    }

    @Test
    void testWrongHouseNumberAndWrongStreetFallsBackToCity() {
        var request = new StructuredSearchRequest();
        request.setCountryCode("DE");
        request.setCity("Some City");
        request.setStreet("does not exist");
        request.setHouseNumber("1");

        assertThat(search(request)).singleElement(PHOTONRESULT).sameOsmID(city);
    }

    @Test
    void testHouse() {
        var request = new StructuredSearchRequest();
        request.setCountryCode("DE");
        request.setCity("Some City");
        request.setStreet("Some street");
        request.setHouseNumber("42");

        assertThat(search(request)).singleElement(PHOTONRESULT).sameOsmID(house);
    }

    @ParameterizedTest
    @ValueSource(strings = {"5A", "5 a"})
    void testHouseNumberWithSpace(String hnr) {
        var request = new StructuredSearchRequest();
        request.setCity("Some City");
        request.setHouseNumber(hnr);

        assertThat(search(request)).singleElement(PHOTONRESULT).sameOsmID(hamletHouse4);
    }

}
