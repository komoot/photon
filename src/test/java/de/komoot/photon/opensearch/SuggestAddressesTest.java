package de.komoot.photon.opensearch;

import de.komoot.photon.Constants;
import de.komoot.photon.ESBaseTester;
import de.komoot.photon.Importer;
import de.komoot.photon.PhotonDoc;
import de.komoot.photon.query.SimpleSearchRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SuggestAddressesTest extends ESBaseTester {

    private static final String STREET_NAME = "Test Street";

    @BeforeAll
    void setUp(@TempDir Path dataDirectory) throws Exception {
        setUpES(dataDirectory);
        Importer instance = makeImporter();

        // Add a street
        var street = new PhotonDoc("1", "W", 1, "highway", "residential")
                .names(makeDocNames("name", STREET_NAME))
                .countryCode("DE")
                .importance(0.5)
                .rankAddress(26);

        // Add a house on that street
        var address = new HashMap<String, String>();
        address.put("street", STREET_NAME);
        var house = new PhotonDoc("2", "N", 2, "building", "yes")
                .countryCode("DE")
                .houseNumber("42")
                .addAddresses(address, getProperties().getLanguages())
                .importance(0.1)
                .rankAddress(30);

        // Add houses on same street name in different cities (Auelestr scenario)
        var addressTriesen = new HashMap<String, String>();
        addressTriesen.put("street", "Auelestr");
        addressTriesen.put("city", "Triesen");
        var houseTriesen = new PhotonDoc("3", "N", 3, "building", "yes")
                .countryCode("LI")
                .houseNumber("16")
                .addAddresses(addressTriesen, getProperties().getLanguages())
                .importance(0.1)
                .rankAddress(30);

        var addressVaduz = new HashMap<String, String>();
        addressVaduz.put("street", "Auelestr");
        addressVaduz.put("city", "Vaduz");
        var houseVaduz = new PhotonDoc("4", "N", 4, "building", "yes")
                .countryCode("LI")
                .houseNumber("16")
                .addAddresses(addressVaduz, getProperties().getLanguages())
                .importance(0.1)
                .rankAddress(30);

        // Add a street with pure alphabetic name (triggers short query path)
        var alphabeticStreet = new PhotonDoc("5", "W", 5, "highway", "residential")
                .names(makeDocNames("name", "Romsdalsveien"))
                .countryCode("NO")
                .importance(0.5)
                .rankAddress(26);

        // Add a house on that street
        var addressRomsdalsveien = new HashMap<String, String>();
        addressRomsdalsveien.put("street", "Romsdalsveien");
        var houseRomsdalsveien = new PhotonDoc("6", "N", 6, "building", "yes")
                .countryCode("NO")
                .houseNumber("10")
                .addAddresses(addressRomsdalsveien, getProperties().getLanguages())
                .importance(0.1)
                .rankAddress(30);

        // Add a street with spaces in name (triggers full query path)
        var multiWordStreet = new PhotonDoc("7", "W", 7, "highway", "residential")
                .names(makeDocNames("name", "Nils Gotlands veg"))
                .countryCode("NO")
                .importance(0.5)
                .rankAddress(26);

        // Add a house on that street
        var addressNilsGotlands = new HashMap<String, String>();
        addressNilsGotlands.put("street", "Nils Gotlands veg");
        var houseNilsGotlands = new PhotonDoc("8", "N", 8, "building", "yes")
                .countryCode("NO")
                .houseNumber("5")
                .addAddresses(addressNilsGotlands, getProperties().getLanguages())
                .importance(0.1)
                .rankAddress(30);

        instance.add(List.of(street, house, houseTriesen, houseVaduz, alphabeticStreet, houseRomsdalsveien,
                multiWordStreet, houseNilsGotlands));
        instance.finish();
        refresh();
    }

    @AfterAll
    @Override
    public void tearDown() {
        super.tearDown();
    }

    @Test
    void searchWithoutSuggestAddressesReturnsOnlyStreet() {
        var request = new SimpleSearchRequest();
        request.setQuery(STREET_NAME);

        var handler = getServer().createSearchHandler(1);
        var results = handler.search(request);

        assertEquals(1, results.size());
        assertEquals(STREET_NAME, results.getFirst().getLocalised(Constants.NAME, "en"));
        assertNull(results.getFirst().get(Constants.HOUSENUMBER));
    }

    @Test
    void searchWithSuggestAddressesReturnsAddresses() {
        var request = new SimpleSearchRequest();
        request.setQuery(STREET_NAME);
        request.setSuggestAddresses(true);

        var handler = getServer().createSearchHandler(1);
        var results = handler.search(request);

        assertEquals(2, results.size());
        assertEquals("42", results.get(1).get(Constants.HOUSENUMBER));
    }

    @Test
    void searchWithHousenumberInQueryDoesNotTriggerSuggestAddresses() {
        // When query already contains a number, suggest_addresses should not add
        // the alternative housenumber query path (it's redundant since the main query
        // already handles housenumber matching)
        var request = new SimpleSearchRequest();
        request.setQuery(STREET_NAME + " 42");
        request.setSuggestAddresses(true);

        var handler = getServer().createSearchHandler(1);
        var results = handler.search(request);

        assertEquals(1, results.size());
        assertEquals("42", results.getFirst().get(Constants.HOUSENUMBER));
    }

    @Test
    void suggestAddressesRespectsOtherQueryTerms() {
        // When searching for "Auelestr Triesen", should only return addresses in Triesen,
        // not addresses in Vaduz just because they have a housenumber on the same street name
        var request = new SimpleSearchRequest();
        request.setQuery("Auelestr Triesen");
        request.setSuggestAddresses(true);

        var handler = getServer().createSearchHandler(10);
        var results = handler.search(request);

        assertEquals(1, results.size(), "Expected only Triesen result, got: " + results);
        assertEquals("16", results.getFirst().get(Constants.HOUSENUMBER));
        // City is stored in localised format
        assertEquals("Triesen", results.getFirst().getLocalised(Constants.CITY, "en"));
    }

    @Test
    void suggestAddressesWorksForPureAlphabeticStreetNames() {
        // Pure alphabetic queries (like "Romsdalsveien") use the short query path.
        // suggest_addresses should work for these too.
        var request = new SimpleSearchRequest();
        request.setQuery("Romsdalsveien");
        request.setSuggestAddresses(true);

        var handler = getServer().createSearchHandler(10);
        var results = handler.search(request);

        assertEquals(2, results.size(), "Expected street and address, got: " + results);
        assertEquals("Romsdalsveien", results.getFirst().getLocalised(Constants.NAME, "en"));
        assertEquals("10", results.get(1).get(Constants.HOUSENUMBER));
    }

    @Test
    void suggestAddressesWorksForMultiWordStreetNames() {
        // Multi-word queries (like "Nils Gotlands veg") use the full query path.
        var request = new SimpleSearchRequest();
        request.setQuery("Nils Gotlands veg");
        request.setSuggestAddresses(true);

        var handler = getServer().createSearchHandler(10);
        var results = handler.search(request);

        assertEquals(2, results.size(), "Expected street and address, got: " + results);
        assertEquals("Nils Gotlands veg", results.getFirst().getLocalised(Constants.NAME, "en"));
        assertEquals("5", results.get(1).get(Constants.HOUSENUMBER));
    }
}
