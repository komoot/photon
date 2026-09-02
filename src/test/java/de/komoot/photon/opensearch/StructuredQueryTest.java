package de.komoot.photon.opensearch;

import de.komoot.photon.query.StructuredSearchRequest;
import de.komoot.photon.ESBaseTester;
import de.komoot.photon.Importer;
import de.komoot.photon.PhotonDoc;
import de.komoot.photon.nominatim.model.AddressType;
import de.komoot.photon.searcher.PhotonResult;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class StructuredQueryTest extends ESBaseTester {

    private static final String COUNTRY_CODE = "DE";
    private static final String LANGUAGE = "en";
    private static final String DISTRICT = "MajorSuburb";
    private static final String HOUSE_NUMBER = "42";
    private static final String CITY = "Some City";
    private static final String HAMLET = "Hamlet";
    private static final String STREET = "Some street";
    public static final String DISTRICT_POST_CODE = "12346";
    private static final String COMPOUND_HOUSE_NUMBER = "275/118";
    private static final String HYPHENATED_POSTCODE_STREET = "Rua Bom Jardim";
    private static final String HYPHENATED_POSTCODE = "68685-000";
    private static final String SAME_SUFFIX_POSTCODE = "99999-000";
    private static final String ALPHANUMERIC_POSTCODE_STREET = "Hwy 4109";
    private static final String ALPHANUMERIC_POSTCODE = "B5A5B1";

    @BeforeAll
    void setUp(@TempDir Path dataDirectory) throws Exception {
        getProperties().setLanguages(Set.of(LANGUAGE, "de", "fr"));
        setUpES(dataDirectory);
        Importer instance = makeImporter();

        var country = new PhotonDoc("0", "R", 0, "place", "country")
                .names(makeDocNames("name", "Germany"))
                .countryCode(COUNTRY_CODE)
                .importance(1.0)
                .addressType(AddressType.COUNTRY);

        var city = new PhotonDoc("1", "R", 1, "place", "city")
                .names(makeDocNames("name", CITY))
                .countryCode(COUNTRY_CODE)
                .postcode("12345")
                .importance(1.0)
                .addressType(AddressType.CITY);

        Map<String, String> address = new HashMap<>();
        address.put("city", CITY);
        var suburb = new PhotonDoc("2", "N", 2, "place", "suburb")
                .names(makeDocNames("name", DISTRICT))
                .countryCode(COUNTRY_CODE)
                .postcode(DISTRICT_POST_CODE)
                .addAddresses(address, getProperties().getLanguages())
                .importance(1.0)
                .addressType(AddressType.DISTRICT);

        var street = new PhotonDoc("3", "W", 3, "place", "street")
                .names(makeDocNames("name", STREET))
                .countryCode(COUNTRY_CODE)
                .postcode("12345")
                .addAddresses(address, getProperties().getLanguages())
                .importance(1.0)
                .addressType(AddressType.STREET);

        address.put("street", STREET);
        var house = new PhotonDoc("4", "R", 4, "place", "house")
                .countryCode(COUNTRY_CODE)
                .postcode("12345")
                .addAddresses(address, getProperties().getLanguages())
                .houseNumber(HOUSE_NUMBER)
                .importance(1.0)
                .addressType(AddressType.HOUSE);

        var busStop = new PhotonDoc("8", "N", 8, "highway", "house")
                .names(makeDocNames("name", CITY + ' ' + STREET))
                .countryCode(COUNTRY_CODE)
                .postcode("12345")
                .addAddresses(address, getProperties().getLanguages())
                .houseNumber(HOUSE_NUMBER)
                .importance(1.0)
                .addressType(AddressType.HOUSE);

        var postcode = new PhotonDoc("10", null, -1, "place", "postcode")
                .names(makeDocNames("name", DISTRICT_POST_CODE))
                .countryCode(COUNTRY_CODE)
                .addAddresses(Map.of("city", CITY), getProperties().getLanguages())
                .importance(0.2)
                .categories(List.of("osm.place.postcode"))
                .addressType(AddressType.OTHER);

        var postcode2 = new PhotonDoc("11", null, -1, "place", "postcode")
                .names(makeDocNames("name", "44512"))
                .countryCode(COUNTRY_CODE)
                .addAddresses(Map.of("city", CITY), getProperties().getLanguages())
                .importance(0.2)
                .categories(List.of("osm.place.postcode"))
                .addressType(AddressType.OTHER);

        instance.add(List.of(country));
        instance.add(List.of(city));
        instance.add(List.of(suburb));
        instance.add(List.of(street));
        instance.add(List.of(house));
        instance.add(List.of(postcode));
        instance.add(List.of(postcode2));
        addHamletHouse(instance, 5, "1");
        addHamletHouse(instance, 6, "2");
        addHamletHouse(instance, 7, "3");
        instance.add(List.of(busStop));

        // A house whose number analyses into two tokens, as in Slovakia, Czechia,
        // Japan or Switzerland ("275/118", "400-2", "13 a"), and a house whose
        // postcode carries a hyphen, as in Brazil, Portugal or Poland.
        var compoundNumberHouse = new PhotonDoc("20", "N", 20, "building", "yes")
                .countryCode(COUNTRY_CODE)
                .postcode("12345")
                .addAddresses(address, getProperties().getLanguages())
                .houseNumber(COMPOUND_HOUSE_NUMBER)
                .importance(1.0)
                .addressType(AddressType.HOUSE);
        instance.add(List.of(compoundNumberHouse));
        var hyphenatedPostcodeAddress = new HashMap<String, String>();
        hyphenatedPostcodeAddress.put("city", CITY);
        hyphenatedPostcodeAddress.put("street", HYPHENATED_POSTCODE_STREET);
        var hyphenatedPostcodeHouse = new PhotonDoc("21", "N", 21, "building", "yes")
                .countryCode(COUNTRY_CODE)
                .postcode(HYPHENATED_POSTCODE)
                .addAddresses(hyphenatedPostcodeAddress, getProperties().getLanguages())
                .houseNumber("7")
                .importance(1.0)
                .addressType(AddressType.HOUSE);
        instance.add(List.of(hyphenatedPostcodeHouse));
        // Same street and number, a postcode that shares only the "000" suffix:
        // a postcode match that accepted any one token would confuse the two.
        var sameSuffixPostcodeHouse = new PhotonDoc("23", "N", 23, "building", "yes")
                .countryCode(COUNTRY_CODE)
                .postcode(SAME_SUFFIX_POSTCODE)
                .addAddresses(hyphenatedPostcodeAddress, getProperties().getLanguages())
                .houseNumber("7")
                .importance(1.0)
                .addressType(AddressType.HOUSE);
        instance.add(List.of(sameSuffixPostcodeHouse));
        // A Canadian-style postcode: letters and digits, no space, upper case.
        var alphanumericPostcodeAddress = new HashMap<String, String>();
        alphanumericPostcodeAddress.put("city", CITY);
        alphanumericPostcodeAddress.put("street", ALPHANUMERIC_POSTCODE_STREET);
        var alphanumericPostcodeHouse = new PhotonDoc("22", "N", 22, "building", "yes")
                .countryCode(COUNTRY_CODE)
                .postcode(ALPHANUMERIC_POSTCODE)
                .addAddresses(alphanumericPostcodeAddress, getProperties().getLanguages())
                .houseNumber("1")
                .importance(1.0)
                .addressType(AddressType.HOUSE);
        instance.add(List.of(alphanumericPostcodeHouse));
        instance.finish();
        refresh();
    }

    @AfterAll
    @Override
    public void tearDown() {
        super.tearDown();
    }

    @Test
    void findsDistrictFuzzy() {
        var request = new StructuredSearchRequest();
        request.setCountryCode(COUNTRY_CODE);
        request.setDistrict(DISTRICT + DISTRICT.charAt(DISTRICT.length() - 1));

        var result = search(request);
        Assertions.assertEquals(2, result.get(DocFields.OSM_ID));
    }

    @Test
    void findsPostcode() {
        var request = new StructuredSearchRequest();
        request.setCountryCode(COUNTRY_CODE);
        request.setPostCode(DISTRICT_POST_CODE);

        var result = search(request);
        Assertions.assertEquals("postcode", result.get(DocFields.OSM_VALUE));
        Assertions.assertEquals(DISTRICT_POST_CODE, result.getLocalised("name", "default"));
    }

    @Test
    void findsDistrictByPostcode() {
        var request = new StructuredSearchRequest();
        request.setCountryCode(COUNTRY_CODE);
        request.setCity(CITY);
        request.setPostCode(DISTRICT_POST_CODE);

        var result = search(request);
        Assertions.assertEquals(request.getPostCode(), result.get(DocFields.POSTCODE));
    }

    @Test
    void findsHouseNumberInHamletWithoutStreetName() {
        var request = new StructuredSearchRequest();
        request.setDistrict(HAMLET);
        request.setHouseNumber("2");

        var queryHandler = getServer().createStructuredSearchHandler(1);
        var results = queryHandler.search(request).toList();
        assertEquals(1, results.size());
        var result = results.getFirst();
        assertEquals(request.getHouseNumber(), result.get(DocFields.HOUSENUMBER));
    }

    @Test
    void doesNotReturnBusStops() {
        var request = new StructuredSearchRequest();
        request.setCountryCode(COUNTRY_CODE);
        request.setCity(CITY);
        request.setStreet(STREET);
        var queryHandler = getServer().createStructuredSearchHandler(1);
        var results = queryHandler.search(request).toList();
        for (var result : results)
        {
            assertNotEquals(5, result.get(DocFields.OSM_ID));
        }
    }

    @Test
    void returnsOnlyCountryForCountryRequests() {
        var request = new StructuredSearchRequest();
        request.setCountryCode(COUNTRY_CODE);
        var queryHandler = getServer().createStructuredSearchHandler(1);
        var results = queryHandler.search(request).toList();
        assertEquals(1, results.size());
        var result = results.getFirst();
        assertEquals(0, result.get(DocFields.OSM_ID));
    }

    @Test
    void doesNotReturnHousesForCityRequest() {
        var request = new StructuredSearchRequest();
        request.setCountryCode(COUNTRY_CODE);
        request.setCity(CITY);

        var queryHandler = getServer().createStructuredSearchHandler(1);
        var results = queryHandler.search(request).toList();

        for (var result : results) {
            assertNull(result.getLocalised(DocFields.STREET, LANGUAGE));
            assertNull(result.get(DocFields.HOUSENUMBER));
        }
    }

    @Test
    void testWrongStreet() {
        var request = new StructuredSearchRequest();
        request.setCountryCode(COUNTRY_CODE);
        request.setCity(CITY);
        request.setStreet("totally wrong");
        request.setHouseNumber(HOUSE_NUMBER);

        var result = search(request);
        assertNull(result.getLocalised(DocFields.STREET, LANGUAGE));
        Assertions.assertEquals(request.getCity(), result.getLocalised(DocFields.NAME, LANGUAGE));
    }

    @Test
    void testDistrictAsCity() {
        var request = new StructuredSearchRequest();
        request.setCountryCode(COUNTRY_CODE);
        request.setCity(DISTRICT);
        var result = search(request);
        Assertions.assertEquals(CITY, result.getLocalised(DocFields.CITY, LANGUAGE));
        Assertions.assertEquals(request.getCity(), result.getLocalised(DocFields.NAME, LANGUAGE));
    }

    @Test
    void testWrongHouseNumber() {
        var request = new StructuredSearchRequest();
        request.setCountryCode(COUNTRY_CODE);
        request.setCity(CITY);
        request.setStreet(STREET);
        request.setHouseNumber("1");
        var result = search(request);
        assertNull(result.getLocalised(DocFields.HOUSENUMBER, LANGUAGE));
        Assertions.assertEquals(request.getStreet(), result.getLocalised(DocFields.NAME, LANGUAGE));
        Assertions.assertEquals(request.getCity(), result.getLocalised(DocFields.CITY, LANGUAGE));
    }

    @Test
    void testWrongHouseNumberAndWrongStreet() {
        var request = new StructuredSearchRequest();
        request.setCountryCode(COUNTRY_CODE);
        request.setCity(CITY);
        request.setStreet("does not exist");
        request.setHouseNumber("1");
        var result = search(request);
        assertNull(result.getLocalised(DocFields.HOUSENUMBER, LANGUAGE));
        assertNull(result.getLocalised(DocFields.STREET, LANGUAGE));
        Assertions.assertEquals(request.getCity(), result.getLocalised(DocFields.NAME, LANGUAGE));
    }

    @Test
    void testHouse() {
        var request = new StructuredSearchRequest();
        request.setCountryCode(COUNTRY_CODE);
        request.setCity(CITY);
        request.setStreet(STREET);
        request.setHouseNumber(HOUSE_NUMBER);

        var result = search(request);
        Assertions.assertEquals(request.getCity(), result.getLocalised(DocFields.CITY, LANGUAGE));
        Assertions.assertEquals(request.getStreet(), result.getLocalised(DocFields.STREET, LANGUAGE));
        Assertions.assertEquals(request.getHouseNumber(), result.get(DocFields.HOUSENUMBER));
    }

    @Test
    void findsHouseWithCompoundHouseNumber() {
        // Two tokens after analysis. This used to fail the whole search with
        // "all shards failed": a phrase query on a field indexed without positions.
        var request = new StructuredSearchRequest();
        request.setCountryCode(COUNTRY_CODE);
        request.setCity(CITY);
        request.setStreet(STREET);
        request.setHouseNumber(COMPOUND_HOUSE_NUMBER);

        var result = search(request);

        Assertions.assertEquals(20, result.get(DocFields.OSM_ID));
        Assertions.assertEquals(COMPOUND_HOUSE_NUMBER, result.get(DocFields.HOUSENUMBER));
    }

    @Test
    void findsHouseWithHyphenatedPostcode() {
        // The postcode is indexed as two tokens; the query must be analysed the
        // same way. This used to come back empty for every hyphenated postcode.
        var request = new StructuredSearchRequest();
        request.setCountryCode(COUNTRY_CODE);
        request.setPostCode(HYPHENATED_POSTCODE);
        request.setStreet(HYPHENATED_POSTCODE_STREET);
        request.setHouseNumber("7");

        var result = search(request);

        Assertions.assertEquals(21, result.get(DocFields.OSM_ID));
        Assertions.assertEquals(HYPHENATED_POSTCODE, result.get(DocFields.POSTCODE));
    }

    @Test
    void hyphenatedPostcodeMustMatchAllTokens() {
        // Two houses on the same street with the same number, postcodes
        // 68685-000 and 99999-000. Each query must return its own.
        var request = new StructuredSearchRequest();
        request.setCountryCode(COUNTRY_CODE);
        request.setStreet(HYPHENATED_POSTCODE_STREET);
        request.setHouseNumber("7");

        request.setPostCode(SAME_SUFFIX_POSTCODE);
        Assertions.assertEquals(23, search(request).get(DocFields.OSM_ID));

        request.setPostCode(HYPHENATED_POSTCODE);
        Assertions.assertEquals(21, search(request).get(DocFields.OSM_ID));
    }

    @Test
    void findsHouseWithAlphanumericPostcode() {
        // Indexed lower-cased by the field analyzer; the previous term-level
        // query compared the upper-case string as given and never matched.
        var request = new StructuredSearchRequest();
        request.setCountryCode(COUNTRY_CODE);
        request.setPostCode(ALPHANUMERIC_POSTCODE);
        request.setStreet(ALPHANUMERIC_POSTCODE_STREET);
        request.setHouseNumber("1");

        var result = search(request);

        Assertions.assertEquals(22, result.get(DocFields.OSM_ID));
        Assertions.assertEquals(ALPHANUMERIC_POSTCODE, result.get(DocFields.POSTCODE));
    }

    private PhotonResult search(StructuredSearchRequest request) {
        var queryHandler = getServer().createStructuredSearchHandler(1);
        var results = queryHandler.search(request);

        return results.findFirst().orElseThrow();
    }

    private void addHamletHouse(Importer instance, int id, String houseNumber) {
        var hamletAddress = new HashMap<String, String>();
        hamletAddress.put("city", CITY);
        hamletAddress.put("suburb", HAMLET);

        var doc = new PhotonDoc(Integer.toString(id), "R", id, "place", "house")
                .countryCode(COUNTRY_CODE)
                .addAddresses(hamletAddress, getProperties().getLanguages())
                .houseNumber(houseNumber)
                .importance(1.0)
                .addressType(AddressType.HOUSE);

        instance.add(List.of(doc));
    }
}
