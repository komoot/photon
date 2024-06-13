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

import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class StructuredQueryTest extends ESBaseTester {

    private static final String CountryCode = "DE";
    private static final String Language = "en";
    private static final String District = "MajorSuburb";
    private static final String HouseNumber = "42";
    private static final String City = "Some City";
    private static final String Hamlet = "Hamlet";
    private static final String Street = "Some street";
    public static final String DistrictPostCode = "12346";

    @TempDir
    private static Path instanceTestDirectory;

    private static int getRank(AddressType type)
    {
        for(int i = 0; i < 50; ++i)
        {
            if (type.coversRank(i)){
                return i;
            }
        }

        return 99;
    }

    @BeforeEach
    void setUp() throws Exception {
        setUpES(instanceTestDirectory, Language, "de", "fr");
        Importer instance = makeImporter();

        var country = new PhotonDoc(0, "R", 0, "place", "country")
                .names(Collections.singletonMap("name", "Germany"))
                .countryCode(CountryCode)
                .importance(1.0)
                .rankAddress(getRank(AddressType.COUNTRY));

        var city = new PhotonDoc(1, "R", 1, "place", "city")
                .names(Collections.singletonMap("name", City))
                .countryCode(CountryCode)
                .postcode("12345")
                .importance(1.0)
                .rankAddress(getRank(AddressType.CITY));

        Map<String, String> address = new HashMap<>();
        address.put("city", City);
        var suburb = new PhotonDoc(2, "N", 2, "place", "suburb")
                .names(Collections.singletonMap("name", District))
                .countryCode(CountryCode)
                .postcode(DistrictPostCode)
                .address(address)
                .importance(1.0)
                .rankAddress(getRank(AddressType.DISTRICT));

        var street = new PhotonDoc(3, "W", 3, "place", "street")
                .names(Collections.singletonMap("name", Street))
                .countryCode(CountryCode)
                .postcode("12345")
                .address(address)
                .importance(1.0)
                .rankAddress(getRank(AddressType.STREET));

        address.put("street", Street);
        var house = new PhotonDoc(4, "R", 4, "place", "house")
                .countryCode(CountryCode)
                .postcode("12345")
                .address(address)
                .houseNumber(HouseNumber)
                .importance(1.0)
                .rankAddress(getRank(AddressType.HOUSE));

        instance.add(country, 0);
        instance.add(city, 1);
        instance.add(suburb, 2);
        instance.add(street, 3);
        instance.add(house, 4);
        addHamletHouse(instance, 5, "1");
        addHamletHouse(instance, 6, "2");
        addHamletHouse(instance, 7, "3");
        instance.finish();
        refresh();
    }

    @Test
    void doesFindDistrictByPostcode()
    {
        var request = new StructuredPhotonRequest(Language);
        request.setCountryCode(CountryCode);
        request.setCity(City);
        request.setPostCode(DistrictPostCode);

        var result = search(request);
        Assertions.assertEquals(request.getPostCode(), result.get(Constants.POSTCODE));
    }

    @Test
    void doesFindHouseNumberInHamletWithoutStreetName() {
        var request = new StructuredPhotonRequest(Language);
        request.setDistrict(Hamlet);
        request.setHouseNumber("2");

        StructuredSearchHandler queryHandler = getServer().createStructuredSearchHandler(new String[]{Language}, 1);
        var results = queryHandler.search(request);
        assertEquals(1, results.size());
        var result = results.get(0);
        assertEquals(request.getHouseNumber(), result.get(Constants.HOUSENUMBER));
    }

    @Test
    void doesNotReturnHousesForCityRequest()
    {
        var request = new StructuredPhotonRequest(Language);
        request.setCountryCode(CountryCode);
        request.setCity(City);

        StructuredSearchHandler queryHandler = getServer().createStructuredSearchHandler(new String[]{Language}, 1);
        var results = queryHandler.search(request);

        for(var result : results)
        {
            assertNull(result.getLocalised(Constants.STREET, Language));
            assertNull(result.get(Constants.HOUSENUMBER));
        }
    }

    @Test
    void testWrongStreet()
    {
        var request = new StructuredPhotonRequest(Language);
        request.setCountryCode(CountryCode);
        request.setCity(City);
        request.setStreet("totally wrong");
        request.setHouseNumber(HouseNumber);

        var result = search(request);
        assertNull(result.getLocalised(Constants.STREET, Language));
        Assertions.assertEquals(request.getCity(), result.getLocalised(Constants.NAME, Language));
    }

    @Test
    void testDistrictAsCity()
    {
        var request = new StructuredPhotonRequest(Language);
        request.setCountryCode(CountryCode);
        request.setCity(District);
        var result = search(request);
        Assertions.assertEquals(City, result.getLocalised(Constants.CITY, Language));
        Assertions.assertEquals(request.getCity(), result.getLocalised(Constants.NAME, Language));
    }

    @Test
    void testWrongHouseNumber()
    {
        var request = new StructuredPhotonRequest(Language);
        request.setCountryCode(CountryCode);
        request.setCity(City);
        request.setStreet(Street);
        request.setHouseNumber("1");
        var result = search(request);
        assertNull(result.getLocalised(Constants.HOUSENUMBER, Language));
        Assertions.assertEquals(request.getStreet(), result.getLocalised(Constants.NAME, Language));
        Assertions.assertEquals(request.getCity(), result.getLocalised(Constants.CITY, Language));
    }

    @Test
    void testWrongHouseNumberAndWrongStreet()
    {
        var request = new StructuredPhotonRequest(Language);
        request.setCountryCode(CountryCode);
        request.setCity(City);
        request.setStreet("does not exist");
        request.setHouseNumber("1");
        var result = search(request);
        assertNull(result.getLocalised(Constants.HOUSENUMBER, Language));
        assertNull(result.getLocalised(Constants.STREET, Language));
        Assertions.assertEquals(request.getCity(), result.getLocalised(Constants.NAME, Language));
    }

    @Test
    void testHouse()
    {
        var request = new StructuredPhotonRequest(Language);
        request.setCountryCode(CountryCode);
        request.setCity(City);
        request.setStreet(Street);
        request.setHouseNumber(HouseNumber);

        var result = search(request);
        Assertions.assertEquals(request.getCity(), result.getLocalised(Constants.CITY, Language));
        Assertions.assertEquals(request.getStreet(), result.getLocalised(Constants.STREET, Language));
        Assertions.assertEquals(request.getHouseNumber(), result.get(Constants.HOUSENUMBER));
    }

    private PhotonResult search(StructuredPhotonRequest request) {
        StructuredSearchHandler queryHandler = getServer().createStructuredSearchHandler(new String[]{Language}, 1);
        var results = queryHandler.search(request);

        return results.get(0);
    }

    private void addHamletHouse(Importer instance, int id, String houseNumber) {
        var hamletAddress = new HashMap<String, String>();
        hamletAddress.put("city", City);
        hamletAddress.put("suburb", Hamlet);

        var doc = new PhotonDoc(id, "R", id, "place", "house")
                .countryCode(CountryCode)
                .address(hamletAddress)
                .houseNumber(houseNumber)
                .importance(1.0)
                .rankAddress(getRank(AddressType.HOUSE));

        instance.add(doc, id);
    }
}
