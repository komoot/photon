package de.komoot.photon.opensearch;

import de.komoot.photon.query.StructuredPhotonRequest;
import de.komoot.photon.Constants;
import de.komoot.photon.ESBaseTester;
import de.komoot.photon.Importer;
import de.komoot.photon.PhotonDoc;
import de.komoot.photon.nominatim.model.AddressType;
import de.komoot.photon.searcher.PhotonResult;
import de.komoot.photon.searcher.StructuredSearchHandler;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    private static int getRank(AddressType type) {
        for (int i = 0; i < 50; ++i) {
            if (type.coversRank(i)) {
                return i;
            }
        }

        return 99;
    }

    @BeforeAll
    void setUp(@TempDir Path dataDirectory) throws Exception {
        getProperties().setLanguages(new String[]{LANGUAGE, "de", "fr"});
        getProperties().setSupportStructuredQueries(true);
        setUpES(dataDirectory);
        Importer instance = makeImporter();

        var country = new PhotonDoc(0, "R", 0, "place", "country")
                .names(Collections.singletonMap("name", "Germany"))
                .countryCode(COUNTRY_CODE)
                .importance(1.0)
                .rankAddress(getRank(AddressType.COUNTRY));

        var city = new PhotonDoc(1, "R", 1, "place", "city")
                .names(Collections.singletonMap("name", CITY))
                .countryCode(COUNTRY_CODE)
                .postcode("12345")
                .importance(1.0)
                .rankAddress(getRank(AddressType.CITY));

        Map<String, String> address = new HashMap<>();
        address.put("city", CITY);
        var suburb = new PhotonDoc(2, "N", 2, "place", "suburb")
                .names(Collections.singletonMap("name", DISTRICT))
                .countryCode(COUNTRY_CODE)
                .postcode(DISTRICT_POST_CODE)
                .address(address)
                .importance(1.0)
                .rankAddress(getRank(AddressType.DISTRICT));

        var street = new PhotonDoc(3, "W", 3, "place", "street")
                .names(Collections.singletonMap("name", STREET))
                .countryCode(COUNTRY_CODE)
                .postcode("12345")
                .address(address)
                .importance(1.0)
                .rankAddress(getRank(AddressType.STREET));

        address.put("street", STREET);
        var house = new PhotonDoc(4, "R", 4, "place", "house")
                .countryCode(COUNTRY_CODE)
                .postcode("12345")
                .address(address)
                .houseNumber(HOUSE_NUMBER)
                .importance(1.0)
                .rankAddress(getRank(AddressType.HOUSE));

        var busStop = new PhotonDoc(8, "N", 8, "highway", "house")
                .names(Collections.singletonMap("name", CITY + ' ' + STREET))
                .countryCode(COUNTRY_CODE)
                .postcode("12345")
                .address(address)
                .houseNumber(HOUSE_NUMBER)
                .importance(1.0)
                .rankAddress(getRank(AddressType.HOUSE));

        instance.add(List.of(country));
        instance.add(List.of(city));
        instance.add(List.of(suburb));
        instance.add(List.of(street));
        instance.add(List.of(house));
        addHamletHouse(instance, 5, "1");
        addHamletHouse(instance, 6, "2");
        addHamletHouse(instance, 7, "3");
        instance.add(List.of(busStop));
        instance.finish();
        refresh();
    }

    @AfterAll
    @Override
    public void tearDown() throws IOException {
        super.tearDown();
    }

    @Test
    void findsDistrictFuzzy() {
        var request = new StructuredPhotonRequest(LANGUAGE);
        request.setCountryCode(COUNTRY_CODE);
        request.setDistrict(DISTRICT + DISTRICT.charAt(DISTRICT.length() - 1));

        var result = search(request);
        Assertions.assertEquals(2, result.get(Constants.OSM_ID));
    }

    @Test
    void findsDistrictByPostcode() {
        var request = new StructuredPhotonRequest(LANGUAGE);
        request.setCountryCode(COUNTRY_CODE);
        request.setCity(CITY);
        request.setPostCode(DISTRICT_POST_CODE);

        var result = search(request);
        Assertions.assertEquals(request.getPostCode(), result.get(Constants.POSTCODE));
    }

    @Test
    void findsHouseNumberInHamletWithoutStreetName() {
        var request = new StructuredPhotonRequest(LANGUAGE);
        request.setDistrict(HAMLET);
        request.setHouseNumber("2");

        StructuredSearchHandler queryHandler = getServer().createStructuredSearchHandler(new String[]{LANGUAGE}, 1);
        var results = queryHandler.search(request);
        assertEquals(1, results.size());
        var result = results.get(0);
        assertEquals(request.getHouseNumber(), result.get(Constants.HOUSENUMBER));
    }

    @Test
    void doesNotReturnBusStops() {
        var request = new StructuredPhotonRequest(LANGUAGE);
        request.setCountryCode(COUNTRY_CODE);
        request.setCity(CITY);
        request.setStreet(STREET);
        StructuredSearchHandler queryHandler = getServer().createStructuredSearchHandler(new String[]{LANGUAGE}, 1);
        var results = queryHandler.search(request);
        for (var result : results)
        {
            assertNotEquals(5, result.get(Constants.OSM_ID));
        }
    }

    @Test
    void returnsOnlyCountryForCountryRequests() {
        var request = new StructuredPhotonRequest(LANGUAGE);
        request.setCountryCode(COUNTRY_CODE);
        StructuredSearchHandler queryHandler = getServer().createStructuredSearchHandler(new String[]{LANGUAGE}, 1);
        var results = queryHandler.search(request);
        assertEquals(1, results.size());
        var result = results.get(0);
        assertEquals(0, result.get(Constants.OSM_ID));
    }

    @Test
    void doesNotReturnHousesForCityRequest() {
        var request = new StructuredPhotonRequest(LANGUAGE);
        request.setCountryCode(COUNTRY_CODE);
        request.setCity(CITY);

        StructuredSearchHandler queryHandler = getServer().createStructuredSearchHandler(new String[]{LANGUAGE}, 1);
        var results = queryHandler.search(request);

        for (var result : results) {
            assertNull(result.getLocalised(Constants.STREET, LANGUAGE));
            assertNull(result.get(Constants.HOUSENUMBER));
        }
    }

    @Test
    void testWrongStreet() {
        var request = new StructuredPhotonRequest(LANGUAGE);
        request.setCountryCode(COUNTRY_CODE);
        request.setCity(CITY);
        request.setStreet("totally wrong");
        request.setHouseNumber(HOUSE_NUMBER);

        var result = search(request);
        assertNull(result.getLocalised(Constants.STREET, LANGUAGE));
        Assertions.assertEquals(request.getCity(), result.getLocalised(Constants.NAME, LANGUAGE));
    }

    @Test
    void testDistrictAsCity() {
        var request = new StructuredPhotonRequest(LANGUAGE);
        request.setCountryCode(COUNTRY_CODE);
        request.setCity(DISTRICT);
        var result = search(request);
        Assertions.assertEquals(CITY, result.getLocalised(Constants.CITY, LANGUAGE));
        Assertions.assertEquals(request.getCity(), result.getLocalised(Constants.NAME, LANGUAGE));
    }

    @Test
    void testWrongHouseNumber() {
        var request = new StructuredPhotonRequest(LANGUAGE);
        request.setCountryCode(COUNTRY_CODE);
        request.setCity(CITY);
        request.setStreet(STREET);
        request.setHouseNumber("1");
        var result = search(request);
        assertNull(result.getLocalised(Constants.HOUSENUMBER, LANGUAGE));
        Assertions.assertEquals(request.getStreet(), result.getLocalised(Constants.NAME, LANGUAGE));
        Assertions.assertEquals(request.getCity(), result.getLocalised(Constants.CITY, LANGUAGE));
    }

    @Test
    void testWrongHouseNumberAndWrongStreet() {
        var request = new StructuredPhotonRequest(LANGUAGE);
        request.setCountryCode(COUNTRY_CODE);
        request.setCity(CITY);
        request.setStreet("does not exist");
        request.setHouseNumber("1");
        var result = search(request);
        assertNull(result.getLocalised(Constants.HOUSENUMBER, LANGUAGE));
        assertNull(result.getLocalised(Constants.STREET, LANGUAGE));
        Assertions.assertEquals(request.getCity(), result.getLocalised(Constants.NAME, LANGUAGE));
    }

    @Test
    void testHouse() {
        var request = new StructuredPhotonRequest(LANGUAGE);
        request.setCountryCode(COUNTRY_CODE);
        request.setCity(CITY);
        request.setStreet(STREET);
        request.setHouseNumber(HOUSE_NUMBER);

        var result = search(request);
        Assertions.assertEquals(request.getCity(), result.getLocalised(Constants.CITY, LANGUAGE));
        Assertions.assertEquals(request.getStreet(), result.getLocalised(Constants.STREET, LANGUAGE));
        Assertions.assertEquals(request.getHouseNumber(), result.get(Constants.HOUSENUMBER));
    }

    private PhotonResult search(StructuredPhotonRequest request) {
        StructuredSearchHandler queryHandler = getServer().createStructuredSearchHandler(new String[]{LANGUAGE}, 1);
        var results = queryHandler.search(request);

        return results.get(0);
    }

    private void addHamletHouse(Importer instance, int id, String houseNumber) {
        var hamletAddress = new HashMap<String, String>();
        hamletAddress.put("city", CITY);
        hamletAddress.put("suburb", HAMLET);

        var doc = new PhotonDoc(id, "R", id, "place", "house")
                .countryCode(COUNTRY_CODE)
                .address(hamletAddress)
                .houseNumber(houseNumber)
                .importance(1.0)
                .rankAddress(getRank(AddressType.HOUSE));

        instance.add(List.of(doc));
    }
}
