package de.komoot.photon.nominatim.testdb;

import org.locationtech.jts.io.ParseException;
import de.komoot.photon.Importer;
import de.komoot.photon.PhotonDoc;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class CollectingImporter implements Importer {
    private List<Map.Entry<Integer, PhotonDoc>> docs = new ArrayList<>();
    private int finishCalled = 0;


    @Override
    public void add(PhotonDoc doc, int objectId)
    {
        docs.add(Map.entry(objectId, doc));
    }

    @Override
    public void finish() {
        ++finishCalled;
    }

    public void assertFinishCalled(int num) {
        assertEquals(num, finishCalled);
    }

    public int size() {
        return docs.size();
    }

    public PhotonDoc get(PlacexTestRow row) {
        return get(row.getPlaceId());
    }

    public PhotonDoc get(long placeId) {
        for (Map.Entry<Integer, PhotonDoc> doc : docs) {
            if (doc.getValue().getPlaceId() == placeId) {
                return doc.getValue();
            }
        }

        fail("No document found with that place_id.");
        return null;
    }

    public void assertContains(PlacexTestRow row) throws ParseException {
        PhotonDoc doc = null;
        for (Map.Entry<Integer, PhotonDoc> outdoc : docs) {
            if (outdoc.getValue().getPlaceId() == row.getPlaceId()) {
                assertNull(doc, "Row is contained multiple times");
                doc = outdoc.getValue();
            }
        }

        assertNotNull(doc, "Row not found");

        row.assertEquals(doc);
    }

    public void assertContains(PlacexTestRow row, String housenumber) throws ParseException {
        PhotonDoc doc = null;
        for (Map.Entry<Integer, PhotonDoc> outdoc : docs) {
            if (outdoc.getValue().getPlaceId() == row.getPlaceId()
                    && housenumber.equals(outdoc.getValue().getHouseNumber())) {
                assertNull(doc, "Row is contained multiple times");
                doc = outdoc.getValue();
            }
        }

        assertNotNull(doc, "Row not found");

        row.assertEquals(doc);
    }}
