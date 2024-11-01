package de.komoot.photon.nominatim.testdb;

import de.komoot.photon.PhotonDoc;
import de.komoot.photon.Updater;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class CollectingUpdater implements Updater {
    private List<Map.Entry<Integer, PhotonDoc>> created = new ArrayList<>();
    private List<Map.Entry<Integer, Long>> deleted = new ArrayList<>();
    private List<Map.Entry<Integer, Long>> existing = new ArrayList<>();
    private int finishCalled = 0;

    @Override
    public void create(PhotonDoc doc, int objectId) {
        created.add(Map.entry(objectId, doc));
    }

    @Override
    public void delete(long docId, int objectId) {
        deleted.add(Map.entry(objectId, docId));
    }

    @Override
    public boolean exists(long docId, int objectId) {
        for (Map.Entry<Integer, Long> entry: existing) {
            if (entry.getKey() == objectId && docId == entry.getValue()) {
                return true;
            }
        }

        return false;
    }

    @Override
    public void finish() { ++finishCalled; }


    public void addExisting(long placeId, int ... objectId)
    {
        for (int o: objectId) {
            existing.add(Map.entry(o, placeId));
        }
    }


    public void assertFinishCalled() {
        assertEquals(1, finishCalled);
    }

    public int numDeleted() {
        return deleted.size();
    }

    public int numCreated() {
        return created.size();
    }


    public void assertHasCreated(long id) {
        int objectId = -1;
        for (Map.Entry<Integer, PhotonDoc> outdoc : created) {
            if (outdoc.getValue().getPlaceId() == id) {
                assertEquals(-1, objectId, "Row is contained multiple times");
                objectId = outdoc.getKey();
            }
        }

        assertTrue(objectId >= 0, "Row not found");
        assertEquals(0, objectId, "Row inserted with a non-zero object id");
    }

    public void assertHasCreated(long id, String housenumber) {
        int objectId = -1;
        for (Map.Entry<Integer, PhotonDoc> outdoc : created) {
            PhotonDoc doc = outdoc.getValue();
            if (doc.getPlaceId() == id && housenumber.equals(doc.getHouseNumber())) {
                assertEquals(-1, objectId, "Row is contained multiple times");
                objectId = outdoc.getKey();
            }
        }

        assertTrue(objectId >= 0, "Row not found");
    }

    public void assertHasDeleted(long id) {
        assertHasDeleted(id, 1);
    }

    public void assertHasDeleted(long id, int num) {
        int numFound = 0;
        for (Map.Entry<Integer, Long> outdoc : deleted) {
            if (outdoc.getValue() == id) {
                ++numFound;
            }
        }

        assertEquals(num, numFound);
    }

}
