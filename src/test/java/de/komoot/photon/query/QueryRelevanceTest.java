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

    @Test
    void testRelevanceByImportance() {
        Importer instance = makeImporter();
        instance.add(List.of(createDoc("amenity", "restaurant", 1001, "name", "New York").importance(0.0)));
        instance.add(List.of(createDoc("place", "city", 2000, "name", "New York").importance(0.5)));
        instance.finish();
        refresh();

        assertThat(search("new york"))
                .hasSize(2)
                .element(0)
                .satisfies(p -> assertThat(p.get("osm_id")).isEqualTo(2000));
    }

    @Test
    void testFullNameOverPartialName() {
        Importer instance = makeImporter();
        instance.add(List.of(createDoc("place", "hamlet", 1000, "name", "Ham")));
        instance.add(List.of(createDoc("place", "hamlet", 1001, "name", "Hamburg")));
        instance.finish();
        refresh();

        assertThat(search("ham"))
                .hasSize(2)
                .element(0)
                .satisfies(p -> assertThat(p.get("osm_id")).isEqualTo(1000));
    }

    @Test
    void testPartialNameWithImportanceOverFullName() {
        Importer instance = makeImporter();
        instance.add(List.of(createDoc("place", "hamlet", 1000, "name", "Ham").importance(0.1)));
        instance.add(List.of(createDoc("place", "city", 1001, "name", "Hamburg").importance(0.5)));
        instance.finish();
        refresh();

        assertThat(search("ham"))
                .hasSize(2)
                .element(0)
                .satisfies(p -> assertThat(p.get("osm_id")).isEqualTo(1001));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Ham", "Hamm", "Hamburg"})
    void testLocationPreferenceForEqualImportance(String placeName) {
        Importer instance = makeImporter();
        instance.add(List.of(
                createDoc("place", "hamlet", 1000, "name", "Ham")
                        .centroid(FACTORY.createPoint(new Coordinate(10, 10)))));
        instance.add(List.of(
                createDoc("place", "hamlet", 1001, "name", placeName)
                        .centroid(FACTORY.createPoint(new Coordinate(-10, -10)))));
        instance.finish();
        refresh();

        assertThat(search(createBiasedRequest()))
                .hasSize(2)
                .element(0)
                .satisfies(p -> assertThat(p.get("osm_id")).isEqualTo(1001));
    }

    @Test
    void testLocationPreferenceForHigherImportance() {
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

        assertThat(search(createBiasedRequest()))
                .hasSize(2)
                .element(0)
                .satisfies(p -> assertThat(p.get("osm_id")).isEqualTo(1000));
    }

    @ParameterizedTest
    @ValueSource(doubles = {0.8, 0.6, 0.4, 0.2})
    void testLocationPreferenceScaleForImportance(double scale) {
        Importer instance = makeImporter();
        instance.add(List.of(
                createDoc("place", "hamlet", 1000, "name", "Ham")
                        .importance(1.0 - scale)
                        .centroid(FACTORY.createPoint(new Coordinate(10, 10)))));
        instance.add(List.of(
                createDoc("place", "hamlet", 1001, "name", "Ham")
                        .importance(0.01)
                        .centroid(FACTORY.createPoint(new Coordinate(-10, -10)))));
        instance.finish();
        refresh();

        final var request = new SimpleSearchRequest();
        request.setQuery("ham");
        request.setLocationForBias(FACTORY.createPoint(new Coordinate(-9.99, -10)));
        request.setScale(scale);

        assertThat(search(request))
                .hasSize(2)
                .element(0)
                .satisfies(p -> assertThat(p.get("osm_id")).isEqualTo(1000));
    }

    @ParameterizedTest
    @ValueSource(doubles = {0.8, 0.6, 0.4, 0.2})
    void testLocationPreferenceScaleForLocation(double scale) {
        Importer instance = makeImporter();
        instance.add(List.of(
                createDoc("place", "hamlet", 1000, "name", "Ham")
                        .importance(1.0 - scale)
                        .centroid(FACTORY.createPoint(new Coordinate(10, 10)))));
        instance.add(List.of(
                createDoc("place", "hamlet", 1001, "name", "Ham")
                        .importance(0.01)
                        .centroid(FACTORY.createPoint(new Coordinate(-10, -10)))));
        instance.finish();
        refresh();

        final var request = new SimpleSearchRequest();
        request.setQuery("ham");
        request.setLocationForBias(FACTORY.createPoint(new Coordinate(-9.99, -10)));
        request.setScale(scale - 0.1);

        assertThat(search(request))
                .hasSize(2)
                .element(0)
                .satisfies(p -> assertThat(p.get("osm_id")).isEqualTo(1001));
    }
}
