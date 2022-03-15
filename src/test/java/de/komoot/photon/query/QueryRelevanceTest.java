package de.komoot.photon.query;

import com.vividsolutions.jts.geom.Coordinate;
import de.komoot.photon.ESBaseTester;
import de.komoot.photon.Importer;
import de.komoot.photon.PhotonDoc;
import de.komoot.photon.searcher.PhotonResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test that the database backend produces queries that rank the
 * results in the expected order.
 */
public class QueryRelevanceTest extends ESBaseTester {

    @BeforeEach
    public void setup() throws IOException {
        setUpES();
    }

    private PhotonDoc createDoc(String key, String value, long id, String... names) {
        Map<String, String> nameMap = new HashMap<>();

        for (int i = 0; i < names.length - 1; i += 2) {
            nameMap.put(names[i], names[i+1]);
        }

        return new PhotonDoc(id, "N", id, key, value).names(nameMap);
    }

    private List<PhotonResult> search(String query) {
        return getServer().createSearchHandler(new String[]{"en"}).search(new PhotonRequest(query, "en"));
    }

    private List<PhotonResult> search(PhotonRequest request) {
        return getServer().createSearchHandler(new String[]{"en"}).search(request);
    }

    @Test
    public void testRelevanceByImportance() {
        Importer instance = makeImporter();
        instance.add(createDoc("amenity", "restuarant", 1001, "name", "New York").importance(0.0));
        instance.add(createDoc("place", "city", 2000, "name", "New York").importance(0.5));
        instance.finish();
        refresh();

        List<PhotonResult> results = search("new york");

        assertEquals(2, results.size());
        assertEquals(2000, results.get(0).get("osm_id"));
    }

    @Test
    public void testFullNameOverPartialName() {
        Importer instance = makeImporter();
        instance.add(createDoc("place", "hamlet", 1000, "name", "Ham"));
        instance.add(createDoc("place", "hamlet", 1001, "name", "Hamburg"));
        instance.finish();
        refresh();

        List<PhotonResult> results = search("ham");

        assertEquals(2, results.size());
        assertEquals(1000, results.get(0).get("osm_id"));
    }

    @Test
    public void testPartialNameWithImportanceOverFullName() {
        Importer instance = makeImporter();
        instance.add(createDoc("place", "hamlet", 1000, "name", "Ham").importance(0.1));
        instance.add(createDoc("place", "city", 1001, "name", "Hamburg").importance(0.5));
        instance.finish();
        refresh();

        List<PhotonResult> results = search("ham");

        assertEquals(2, results.size());
        assertEquals(1001, results.get(0).get("osm_id"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Ham", "Hamm", "Hamburg"})
    void testLocationPreferenceForEqualImportance(String placeName) {
        Importer instance = makeImporter();
        instance.add(createDoc("place", "hamlet", 1000, "name", "Ham")
                .centroid(FACTORY.createPoint(new Coordinate(10, 10))));
        instance.add(createDoc("place", "hamlet", 1001, "name", placeName)
                .centroid(FACTORY.createPoint(new Coordinate(-10, -10))));
        instance.finish();
        refresh();

        List<PhotonResult> results = search(new PhotonRequest("ham", "en")
                .setLocationForBias(FACTORY.createPoint(new Coordinate(-9.9, -10))));

        assertEquals(2, results.size());
        assertEquals(1001, results.get(0).get("osm_id"));
    }

    @Test
    void testLocationPreferenceForHigherImportance() {
        Importer instance = makeImporter();
        instance.add(createDoc("place", "hamlet", 1000, "name", "Ham")
                        .importance(0.8)
                .centroid(FACTORY.createPoint(new Coordinate(10, 10))));
        instance.add(createDoc("place", "hamlet", 1001, "name", "Ham")
                        .importance(0.01)
                .centroid(FACTORY.createPoint(new Coordinate(-10, -10))));
        instance.finish();
        refresh();

        List<PhotonResult> results = search(new PhotonRequest("ham", "en")
                .setLocationForBias(FACTORY.createPoint(new Coordinate(-9.9, -10))));

        assertEquals(2, results.size());
        assertEquals(1000, results.get(0).get("osm_id"));
    }
}
