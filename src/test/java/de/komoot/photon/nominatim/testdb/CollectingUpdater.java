package de.komoot.photon.nominatim.testdb;

import de.komoot.photon.PhotonDoc;
import de.komoot.photon.Updater;
import org.assertj.core.api.ListAssert;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

public class CollectingUpdater implements Updater {
    private final List<PhotonDoc> created = new ArrayList<>();
    private final List<String> deleted = new ArrayList<>();
    private int finishCalled = 0;

    @Override
    public void addOrUpdate(Iterable<PhotonDoc> docs)
    {
        for (var doc: docs) {
            created.add(doc);
        }
    }

    @Override
    public void delete(String docId) {
        deleted.add(docId);
    }

    @Override
    public void finish() { ++finishCalled; }

    public int getFinishCalled() {
        return finishCalled;
    }

    public ListAssert<String> assertThatDeleted() {
        return assertThat(deleted);
    }

    public ListAssert<PhotonDoc> assertThatCreated() {
        return assertThat(created);
    }
}
