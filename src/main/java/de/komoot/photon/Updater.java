package de.komoot.photon;

import org.jspecify.annotations.NullMarked;

/**
 * Interface for classes accepting database updates.
 */
@NullMarked
public interface Updater {
    void addOrUpdate(Iterable<PhotonDoc> docs);

    void delete(String docId);

    void finish();
}
