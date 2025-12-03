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
class IncludeHousenumbersTest extends ESBaseTester {

    private static final String STREET_NAME = "Test Street";

    @BeforeAll
    void setUp(@TempDir Path dataDirectory) throws Exception {
        setUpES(dataDirectory);
        Importer instance = makeImporter();

        // Add a street
        var street = new PhotonDoc(1, "W", 1, "highway", "residential")
                .names(makeDocNames("name", STREET_NAME))
                .countryCode("DE")
                .importance(0.5)
                .rankAddress(26);

        // Add a house on that street
        var address = new HashMap<String, String>();
        address.put("street", STREET_NAME);
        var house = new PhotonDoc(2, "N", 2, "building", "yes")
                .countryCode("DE")
                .houseNumber("42")
                .addAddresses(address, getProperties().getLanguages())
                .importance(0.1)
                .rankAddress(30);

        instance.add(List.of(street));
        instance.add(List.of(house));
        instance.finish();
        refresh();
    }

    @AfterAll
    @Override
    public void tearDown() {
        super.tearDown();
    }

    @Test
    void searchWithoutIncludeHousenumbersReturnsOnlyStreet() {
        var request = new SimpleSearchRequest();
        request.setQuery(STREET_NAME);

        var handler = getServer().createSearchHandler(1);
        var results = handler.search(request);

        assertEquals(1, results.size());
        assertEquals(STREET_NAME, results.getFirst().getLocalised(Constants.NAME, "en"));
        assertNull(results.getFirst().get(Constants.HOUSENUMBER));
    }

    @Test
    void searchWithIncludeHousenumbersReturnsHousenumbers() {
        var request = new SimpleSearchRequest();
        request.setQuery(STREET_NAME);
        request.setIncludeHousenumbers(true);

        var handler = getServer().createSearchHandler(1);
        var results = handler.search(request);

        assertEquals(2, results.size());
        assertEquals("42", results.get(1).get(Constants.HOUSENUMBER));
    }

    @Test
    void searchWithHousenumberInQueryDoesNotTriggerIncludeHousenumbers() {
        // When query already contains a number, include_housenumbers should not add
        // the alternative housenumber query path (it's redundant since the main query
        // already handles housenumber matching)
        var request = new SimpleSearchRequest();
        request.setQuery(STREET_NAME + " 42");
        request.setIncludeHousenumbers(true);

        var handler = getServer().createSearchHandler(1);
        var results = handler.search(request);

        assertEquals(1, results.size());
        assertEquals("42", results.getFirst().get(Constants.HOUSENUMBER));
    }
}
