package de.komoot.photon.query;

import de.komoot.photon.ESBaseTester;
import org.junit.jupiter.api.io.TempDir;
import org.locationtech.jts.geom.Coordinate;
import de.komoot.photon.Importer;
import de.komoot.photon.PhotonDoc;
import de.komoot.photon.searcher.PhotonResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

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
        return new PhotonDoc()
                .placeId(id).osmType("N").osmId(id).tagKey(key).tagValue(value)
                .names(makeDocNames(names));
    }

    private void setupDocs(PhotonDoc... docs) {
        Importer instance = makeImporter();
        for (var doc : docs) {
            instance.add(List.of(doc));
        }
        instance.finish();
        refresh();
    }

    private List<PhotonResult> search(String query) {
        final var request = new SimpleSearchRequest();
        request.setQuery(query);

        return getServer().createSearchHandler(1).search(request);
    }

    private List<PhotonResult> search(SimpleSearchRequest request) {
        return getServer().createSearchHandler(1).search(request);
    }

    private SimpleSearchRequest createBiasedRequest()
    {
        SimpleSearchRequest result = new SimpleSearchRequest();
        result.setQuery("ham");
        result.setLocationForBias(FACTORY.createPoint(new Coordinate(-9.9, -10)));
        return result;
    }

    private void assertSearchOsmIds(String query, Integer... ids) {
        assertThat(search(query))
                .extracting(p -> p.get("osm_id"))
                .containsExactly((Object[]) ids);
    }

    @Test
    void testShortNamePartialOverMissSpelling() {
        setupDocs(
                createDoc("place", "city", 1000, "name", "Oslo"),
                createDoc("place", "town", 1001, "name", "Olsokava")
        );

        assertSearchOsmIds("olso", 1001, 1000);
        assertSearchOsmIds("Olso", 1001, 1000);
        assertSearchOsmIds("oslo", 1000);
        assertSearchOsmIds("Oslo", 1000);
    }

    @Test
    void testShortNameMissSpellingOverPartialWithImportance() {
        setupDocs(
                createDoc("place", "city", 1000, "name", "Oslo")
                        .importance(0.5),
                createDoc("place", "town", 1001, "name", "Olsokava")
                        .importance(0.1)
        );

        assertSearchOsmIds("olso", 1000, 1001);
        assertSearchOsmIds("Olso", 1000, 1001);
    }

    @Test
    void testRelevanceByImportance() {
        setupDocs(
                createDoc("amenity", "restaurant", 1001, "name", "New York")
                        .importance(0.0),
                createDoc("place", "city", 2000, "name", "New York")
                        .importance(0.5));

        assertSearchOsmIds("new york", 2000, 1001);
    }

    @Test
    void testFullNameOverPartialName() {
        setupDocs(
                createDoc("place", "hamlet", 1000, "name", "Ham"),
                createDoc("place", "hamlet", 1001, "name", "Hamburg"));

        assertSearchOsmIds("ham", 1000, 1001);
    }

    @Test
    void testPartialNameWithImportanceOverFullName() {
        setupDocs(
                createDoc("place", "hamlet", 1000, "name", "Ham")
                        .importance(0.1),
                createDoc("place", "city", 1001, "name", "Hamburg")
                        .importance(0.5));

        assertSearchOsmIds("ham", 1001, 1000);
    }

    @ParameterizedTest
    @ValueSource(strings = {"Ham", "Hamm", "Hamburg"})
    void testLocationPreferenceForEqualImportance(String placeName) {
        setupDocs(
                createDoc("place", "hamlet", 1000, "name", "Ham")
                        .centroid(FACTORY.createPoint(new Coordinate(10, 10))),
                createDoc("place", "hamlet", 1001, "name", placeName)
                        .centroid(FACTORY.createPoint(new Coordinate(-10, -10))));

        assertThat(search(createBiasedRequest()))
                .extracting(p -> p.get("osm_id"))
                .containsExactly(1001, 1000);
    }

    @Test
    void testLocationPreferenceForHigherImportance() {
        setupDocs(
                createDoc("place", "hamlet", 1000, "name", "Ham")
                        .importance(0.8)
                        .centroid(FACTORY.createPoint(new Coordinate(10, 10))),
                createDoc("place", "hamlet", 1001, "name", "Ham")
                        .importance(0.01)
                        .centroid(FACTORY.createPoint(new Coordinate(-10, -10))));

        assertThat(search(createBiasedRequest()))
                .extracting(p -> p.get("osm_id"))
                .containsExactly(1000, 1001);
    }

    @ParameterizedTest
    @ValueSource(doubles = {0.8, 0.6, 0.4, 0.2})
    void testLocationPreferenceScaleForImportance(double scale) {
        setupDocs(
                createDoc("place", "hamlet", 1000, "name", "Ham")
                        .importance(1.0 - scale)
                        .centroid(FACTORY.createPoint(new Coordinate(10, 10))),
                createDoc("place", "hamlet", 1001, "name", "Ham")
                        .importance(0.01)
                        .centroid(FACTORY.createPoint(new Coordinate(-10, -10))));

        final var request = new SimpleSearchRequest();
        request.setQuery("ham");
        request.setLocationForBias(FACTORY.createPoint(new Coordinate(-9.99, -10)));
        request.setScale(scale);

        assertThat(search(request))
                .extracting(p -> p.get("osm_id"))
                .containsExactly(1000, 1001);
    }

    @ParameterizedTest
    @ValueSource(doubles = {0.8, 0.6, 0.4, 0.2})
    void testLocationPreferenceScaleForLocation(double scale) {
        setupDocs(
                createDoc("place", "hamlet", 1000, "name", "Ham")
                        .importance(1.0 - scale)
                        .centroid(FACTORY.createPoint(new Coordinate(10, 10))),
                createDoc("place", "hamlet", 1001, "name", "Ham")
                        .importance(0.01)
                        .centroid(FACTORY.createPoint(new Coordinate(-10, -10))));

        final var request = new SimpleSearchRequest();
        request.setQuery("ham");
        request.setLocationForBias(FACTORY.createPoint(new Coordinate(-9.99, -10)));
        request.setScale(scale - 0.1);

        assertThat(search(request))
                .extracting(p -> p.get("osm_id"))
                .containsExactly(1001, 1000);
    }
}
