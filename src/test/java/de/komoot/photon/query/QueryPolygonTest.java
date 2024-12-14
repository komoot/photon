package de.komoot.photon.query;

import de.komoot.photon.ESBaseTester;
import de.komoot.photon.Importer;
import de.komoot.photon.PhotonDoc;
import de.komoot.photon.searcher.PhotonResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Tests that the database backend produces queries which can find all
 * expected results. These tests do not check relevance.
 */
class QueryPolygonTest extends ESBaseTester {
    private int testDocId = 10000;

    @BeforeEach
    void setup() throws IOException {
        setUpESWithPolygons();
    }

    private PhotonDoc createDoc(String... names) throws ParseException {
        Map<String, String> nameMap = new HashMap<>();

        for (int i = 0; i < names.length - 1; i += 2) {
            nameMap.put(names[i], names[i+1]);
        }

        ++testDocId;
        return new PhotonDoc(testDocId, "N", testDocId, "place", "city", new WKTReader().read("POLYGON ((1 1, 1 2, 2 1, 1 1))")).names(nameMap);
    }

    private List<PhotonResult> search(String query) {
        return getServer().createSearchHandler(new String[]{"en"}, 1).search(new PhotonRequest(query, "en"));
    }


    @Test
    void testSearchGetPolygon() throws IOException, ParseException {
        Importer instance = makeImporter();
        instance.add(createDoc("name", "Muffle Flu"), 0);
        instance.finish();
        refresh();
        List<PhotonResult> s = search("muffle flu");

        if (s.get(0).getClass().getName().equals("de.komoot.photon.opensearch.OpenSearchResult")) {
            assertNotNull(s.get(0).getGeometry());
        } else {
            assertNotNull(s.get(0).get("geometry"));
        }
    }
}
