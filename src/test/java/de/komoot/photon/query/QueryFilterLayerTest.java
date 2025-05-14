package de.komoot.photon.query;

import de.komoot.photon.ESBaseTester;
import de.komoot.photon.Importer;
import de.komoot.photon.PhotonDoc;
import de.komoot.photon.searcher.PhotonResult;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class QueryFilterLayerTest extends ESBaseTester {
    @BeforeAll
    void setUp(@TempDir Path dataDirectory) throws Exception {
        setUpES(dataDirectory);
        Importer instance = makeImporter();

        int id = 0;

        int[] docRanks = {10, 13, 14, 22}; // state, city * 2, locality
        for (int rank : docRanks) {
            instance.add(List.of(createDoc(++id, rank)));
        }

        instance.finish();
        refresh();
    }

    @AfterAll
    @Override
    public void tearDown() throws IOException {
        super.tearDown();
    }

    private PhotonDoc createDoc(int id, int rank) {
        return new PhotonDoc(id, "W", id, "place", "value").names(Collections.singletonMap("name", "berlin")).rankAddress(rank);
    }

    @Test
    void testSingleLayer() {
        assertEquals(2, searchWithLayers("city").size());
    }

    @Test
    void testMultipleLayers() {
        assertEquals(3, searchWithLayers("city", "locality").size());
    }

    private List<PhotonResult> searchWithLayers(String... layers) {
        SimpleSearchRequest request = new SimpleSearchRequest("berlin", "en");
        request.setLimit(50);
        request.setLayerFilter(Arrays.stream(layers).collect(Collectors.toSet()));

        return getServer().createSearchHandler(new String[]{"en"}, 1).search(request);
    }
}
