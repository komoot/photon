package de.komoot.photon.nominatim.testdb;

import de.komoot.photon.PhotonDoc;
import de.komoot.photon.Updater;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class CollectingUpdater implements Updater {
    private List<PhotonDoc> created = new ArrayList<>();
    private List<Long> deleted = new ArrayList<>();
    private int finishCalled = 0;

    @Override
    public void create(PhotonDoc doc) {
        created.add(doc);
    }

    @Override
    public void delete(Long id) {
        deleted.add(id);
    }

    @Override
    public void finish() {
        ++finishCalled;
    }

    public void assertFinishCalled() {
        assertEquals(finishCalled, 1);
    }

    public void assertDeleted(Long ... ids) {
        assertArrayEquals(deleted.toArray(new Long[0]), ids);
    }

    public void assertCreatedPlaceIds(Long ... ids) {
        Long[] expected = new Long[created.size()];
        for (int i = 0; i < created.size(); ++i) {
            expected[i] = created.get(i).getPlaceId();
        }

        assertArrayEquals(expected, ids);
    }

}
