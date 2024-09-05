package de.komoot.photon.query;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Point;
import de.komoot.photon.ESBaseTester;
import de.komoot.photon.Importer;
import de.komoot.photon.PhotonDoc;
import de.komoot.photon.searcher.PhotonResult;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class QueryReverseFilterLayerTest extends ESBaseTester {
    @TempDir
    private static Path instanceTestDirectory;

    @BeforeAll
    void setup() throws IOException {
        setUpES(instanceTestDirectory, false,"en");

        Importer instance = makeImporter();

        int id = 0;

        int[] docRanks = {10, 13, 14, 22}; // state, city * 2, locality
        for (int rank : docRanks) {
            instance.add(createDoc(++id, rank), 0);
        }

        instance.finish();
        refresh();
    }

    private PhotonDoc createDoc(int id, int rank) {
        Point location = FACTORY.createPoint(new Coordinate(10, 10));
        return new PhotonDoc(id, "W", id, "place", "value").centroid(location).rankAddress(rank);
    }

    private List<PhotonResult> reverse(String... layers) {
        Point pt = FACTORY.createPoint(new Coordinate(10, 10));

        Set<String> layerSet = Arrays.stream(layers).collect(Collectors.toSet());
        ReverseRequest request = new ReverseRequest(pt, "en", 1.0, "", 10, true, layerSet, false, false);

        return getServer().createReverseHandler(1).reverse(request);
    }

    @Test
    void testSingleLayer() {
        assertEquals(2, reverse("city").size());
    }

    @Test
    void testMultipleLayers() {
        assertEquals(3, reverse("city", "locality").size());
    }

    @AfterAll
    @Override
    public void tearDown() throws IOException {
        super.tearDown();
    }
}
