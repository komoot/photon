package de.komoot.photon.query;

import org.locationtech.jts.geom.Coordinate;
import de.komoot.photon.ESBaseTester;
import de.komoot.photon.Importer;
import de.komoot.photon.searcher.PhotonResult;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.locationtech.jts.io.ParseException;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class QueryReverseTest extends ESBaseTester {
    @BeforeAll
    void setup(@TempDir Path dataDirectory) throws IOException, ParseException {
        setUpES(dataDirectory);

        Importer instance = makeImporter();
        instance.add(List.of(createDoc(10,10, 100, 100, "place", "house")));
        instance.add(List.of(createDoc(10,10.1, 101, 101, "place", "house")));
        instance.add(List.of(createDoc(10,10.2, 102, 102, "place", "house")));
        instance.add(List.of(createDoc(-10,-10, 202, 102, "place", "house")));
        instance.finish();
        refresh();
    }

    @AfterAll
    @Override
    public void tearDown() throws IOException {
        super.tearDown();
    }

    private List<PhotonResult> reverse(double lon, double lat, double radius, int limit) {
        final var request = new ReverseRequest();
        request.setLocation(FACTORY.createPoint(new Coordinate(lon, lat)));
        request.setRadius(radius);
        request.setLimit(limit, limit);

        return getServer().createReverseHandler(1).search(request);
    }

    @Test
    void testReverse() {
        List<PhotonResult> results = reverse(10, 10, 0.1, 1);

        assertEquals(1, results.size());
        assertEquals(100, results.get(0).get("osm_id"));
    }

    @ParameterizedTest
    @ValueSource(ints = {2, 3, 10})
    void testReverseMultiple(int limit) {
        List<PhotonResult> results = reverse(10, 10, 20, limit);

        assertEquals(2, results.size());
        assertEquals(100, results.get(0).get("osm_id"));
        assertEquals(101, results.get(1).get("osm_id"));
    }


}
