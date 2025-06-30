package de.komoot.photon.query;

import de.komoot.photon.ESBaseTester;
import de.komoot.photon.PhotonDoc;
import org.locationtech.jts.geom.Coordinate;
import de.komoot.photon.Importer;
import de.komoot.photon.searcher.PhotonResult;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.locationtech.jts.geom.Point;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class QueryReverseTest extends ESBaseTester {
    final Point[] TEST_POINTS = {
            makePoint(10, 10),
            makePoint(10, 10.1),
            makePoint(10, 10.2),
            makePoint(-10, -10)
    };

    @BeforeAll
    void setup(@TempDir Path dataDirectory) throws IOException {
        setUpES(dataDirectory);

        Importer instance = makeImporter();

        int id = 100;
        for (Point pt : TEST_POINTS) {
            instance.add(List.of(new PhotonDoc()
                    .placeId(id).osmType("N").osmId(id++).tagKey("place").tagValue("house")
                    .centroid(pt)
                    .names(makeDocNames("name", "some house"))
            ));
        }

        instance.finish();
        refresh();
    }

    @AfterAll
    @Override
    public void tearDown() {
        super.tearDown();
    }

    private List<PhotonResult> reverse(double lon, double lat, double radius, Integer limit) {
        final var request = new ReverseRequest();
        request.setLocation(FACTORY.createPoint(new Coordinate(lon, lat)));
        request.setRadius(radius);
        if (limit != null) {
            request.setLimit(limit, limit);
        }

        return getServer().createReverseHandler(1).search(request);
    }

    @Test
    void testReverse() {
        assertThat(reverse(10, 10, 0.1, 1))
                .satisfiesExactly(p -> assertThat(p.get("osm_id")).isEqualTo(100));
    }

    @Test
    void testDefaultLimitIsOne() {
        assertThat(reverse(10, 10, 20, null))
                .satisfiesExactly(
                        p -> assertThat(p.get("osm_id")).isEqualTo(100));
    }

    @ParameterizedTest
    @ValueSource(ints = {2, 3, 10})
    void testReverseMultiple(int limit) {
        assertThat(reverse(10, 10, 20, limit))
                .satisfiesExactly(
                        p -> assertThat(p.get("osm_id")).isEqualTo(100),
                        p -> assertThat(p.get("osm_id")).isEqualTo(101));
    }
}
