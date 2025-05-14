package de.komoot.photon.query;

import org.junit.jupiter.api.io.TempDir;
import org.locationtech.jts.geom.Coordinate;
import de.komoot.photon.ESBaseTester;
import de.komoot.photon.Importer;
import de.komoot.photon.PhotonDoc;
import de.komoot.photon.searcher.PhotonResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test that the database backend produces queries that rank the
 * results in the expected order.
 */
class QueryRelevanceTest extends ESBaseTester {

    @BeforeEach
    void setup(@TempDir Path dataDirectory) throws IOException {
        setUpES(dataDirectory);
    }

    private PhotonDoc createDoc(String key, String value, long id, String... names) {
        Map<String, String> nameMap = new HashMap<>();

        for (int i = 0; i < names.length - 1; i += 2) {
            nameMap.put(names[i], names[i+1]);
        }

        return new PhotonDoc(id, "N", id, key, value).names(nameMap);
    }

    private List<PhotonResult> search(String query) {
        return getServer().createSearchHandler(new String[]{"en"}, 1).search(new SimpleSearchRequest(query, "en"));
    }

    private List<PhotonResult> search(SimpleSearchRequest request) {
        return getServer().createSearchHandler(new String[]{"en"}, 1).search(request);
    }

    @Test
    void testRelevanceByImportance() throws IOException {
        Importer instance = makeImporter();
        instance.add(List.of(createDoc("amenity", "restuarant", 1001, "name", "New York").importance(0.0)));
        instance.add(List.of(createDoc("place", "city", 2000, "name", "New York").importance(0.5)));
        instance.finish();
        refresh();

        List<PhotonResult> results = search("new york");

        assertEquals(2, results.size());
        assertEquals(2000, results.get(0).get("osm_id"));
    }

    @Test
    void testFullNameOverPartialName() throws IOException {
        Importer instance = makeImporter();
        instance.add(List.of(createDoc("place", "hamlet", 1000, "name", "Ham")));
        instance.add(List.of(createDoc("place", "hamlet", 1001, "name", "Hamburg")));
        instance.finish();
        refresh();

        List<PhotonResult> results = search("ham");

        assertEquals(2, results.size());
        assertEquals(1000, results.get(0).get("osm_id"));
    }

    @Test
    void testPartialNameWithImportanceOverFullName() throws IOException {
        Importer instance = makeImporter();
        instance.add(List.of(createDoc("place", "hamlet", 1000, "name", "Ham").importance(0.1)));
        instance.add(List.of(createDoc("place", "city", 1001, "name", "Hamburg").importance(0.5)));
        instance.finish();
        refresh();

        List<PhotonResult> results = search("ham");

        assertEquals(2, results.size());
        assertEquals(1001, results.get(0).get("osm_id"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Ham", "Hamm", "Hamburg"})
    void testLocationPreferenceForEqualImportance(String placeName) throws IOException {
        Importer instance = makeImporter();
        instance.add(List.of(
                createDoc("place", "hamlet", 1000, "name", "Ham")
                        .centroid(FACTORY.createPoint(new Coordinate(10, 10)))));
        instance.add(List.of(
                createDoc("place", "hamlet", 1001, "name", placeName)
                        .centroid(FACTORY.createPoint(new Coordinate(-10, -10)))));
        instance.finish();
        refresh();

        List<PhotonResult> results = search(createBiasedRequest());

        assertEquals(2, results.size());
        assertEquals(1001, results.get(0).get("osm_id"));
    }

    @Test
    void testLocationPreferenceForHigherImportance() throws IOException {
        Importer instance = makeImporter();
        instance.add(List.of(
                createDoc("place", "hamlet", 1000, "name", "Ham")
                        .importance(0.8)
                        .centroid(FACTORY.createPoint(new Coordinate(10, 10)))));
        instance.add(List.of(
                createDoc("place", "hamlet", 1001, "name", "Ham")
                        .importance(0.01)
                        .centroid(FACTORY.createPoint(new Coordinate(-10, -10)))));
        instance.finish();
        refresh();

        List<PhotonResult> results = search(createBiasedRequest());

        assertEquals(2, results.size());
        assertEquals(1000, results.get(0).get("osm_id"));
    }

    private SimpleSearchRequest createBiasedRequest()
    {
        SimpleSearchRequest result = new SimpleSearchRequest("ham", "en");
        result.setLocationForBias(FACTORY.createPoint(new Coordinate(-9.9, -10)));
        return result;
    }
}
