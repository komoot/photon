package de.komoot.photon.query;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Point;
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

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class QueryReverseTest extends ESBaseTester {
    @TempDir
    private static Path instanceTestDirectory;

    @BeforeAll
    public void setup() throws IOException {
        setUpES(instanceTestDirectory, "en");

        Importer instance = makeImporter();
        instance.add(createDoc(10,10, 100, 100, "place", "house"));
        instance.add(createDoc(10,10.1, 101, 101, "place", "house"));
        instance.add(createDoc(10,10.2, 102, 102, "place", "house"));
        instance.add(createDoc(-10,-10, 202, 102, "place", "house"));
        instance.finish();
        refresh();
    }

    @AfterAll
    @Override
    public void tearDown() {
        super.tearDown();
    }

    private List<PhotonResult> reverse(double lon, double lat, double radius, int limit) {
        Point pt = FACTORY.createPoint(new Coordinate(lon, lat));

        return getServer().createReverseHandler().reverse(
            new ReverseRequest(pt, "en", radius, "", limit, true, new HashSet<>(), false)
        );
    }

    @Test
    public void testReverse() {
        List<PhotonResult> results = reverse(10, 10, 0.1, 1);

        assertEquals(1, results.size());
        assertEquals(100, results.get(0).get("osm_id"));
    }

    @ParameterizedTest
    @ValueSource(ints = {2, 3, 10})
    public void testReverseMultiple(int limit) {
        List<PhotonResult> results = reverse(10, 10, 20, limit);

        assertEquals(2, results.size());
        assertEquals(100, results.get(0).get("osm_id"));
        assertEquals(101, results.get(1).get("osm_id"));
    }


}
