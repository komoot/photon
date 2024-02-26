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
    public void create(PhotonDoc doc, int object_id) {
        created.add(Map.entry(object_id, doc));
    }

    @Override
    public void delete(long id, int object_id) {
        deleted.add(Map.entry(object_id, id));
    }

    @Override
    public boolean exists(long id, int object_id) {
        for (Map.Entry<Integer, Long> entry: existing) {
            if (entry.getKey() == object_id && id == entry.getValue()) {
                return true;
            }
        }

        return false;
    }

    @Override
    public void finish() { ++finishCalled; }


    public void add_existing(long place_id, int ... object_id)
    {
        for (int o: object_id) {
            existing.add(Map.entry(o, place_id));
        }
    }


    public void assertFinishCalled() {
        assertEquals(finishCalled, 1);
    }

    public int numDeleted() {
        return deleted.size();
    }

    public int numCreated() {
        return created.size();
    }


    public void assertHasCreated(long id) {
        int object_id = -1;
        for (Map.Entry<Integer, PhotonDoc> outdoc : created) {
            if (outdoc.getValue().getPlaceId() == id) {
                assertTrue(object_id == -1, "Row is contained multiple times");
                object_id = outdoc.getKey();
            }
        }

        assertTrue(object_id >= 0, "Row not found");
        assertTrue(object_id == 0, "Row inserted with a non-zero object id");
    }

    public void assertHasCreated(long id, String housenumber) {
        int object_id = -1;
        for (Map.Entry<Integer, PhotonDoc> outdoc : created) {
            PhotonDoc doc = outdoc.getValue();
            if (doc.getPlaceId() == id && housenumber.equals(doc.getHouseNumber())) {
                assertTrue(object_id == -1, "Row is contained multiple times");
                object_id = outdoc.getKey();
            }
        }

        assertTrue(object_id >= 0, "Row not found");
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
