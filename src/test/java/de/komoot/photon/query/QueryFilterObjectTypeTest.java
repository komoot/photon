package de.komoot.photon.query;

import de.komoot.photon.ESBaseTester;
import de.komoot.photon.Importer;
import de.komoot.photon.PhotonDoc;
import de.komoot.photon.searcher.PhotonResult;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class QueryFilterObjectTypeTest extends ESBaseTester {
    @TempDir
    private static Path instanceTestDirectory;

    @BeforeAll
    public void setUp() throws Exception {
        setUpES(instanceTestDirectory, "en", "de", "fr");
        Importer instance = makeImporter();

        int id = 0;

        int[] docRanks = {10, 13, 14, 22}; // state, city * 2, locality
        for (int rank : docRanks) {
            instance.add(createDoc(++id, rank));
        }

        instance.finish();
        refresh();
    }

    private PhotonDoc createDoc(int id, int rank) {
        return new PhotonDoc(id, "W", id, "place", "value").names(Collections.singletonMap("name", "berlin")).rankAddress(rank);
    }

    @Test
    public void testSingleType() {
        assertEquals(2, searchWithTypes("city").size());
    }

    @Test
    public void testMultipleTypes() {
        assertEquals(3, searchWithTypes("city", "locality").size());
    }

    private List<PhotonResult> searchWithTypes(String... types) {
        PhotonRequest request = new PhotonRequest("berlin", "en").setLimit(50);
        for (String type : types) {
            request.addObjectTypeFilter(type);
        }

        return getServer().createSearchHandler(new String[]{"en"}).search(request);
    }

    @AfterAll
    @Override
    public void tearDown() {
        super.tearDown();
    }
}
