package de.komoot.photon;

/**
 * Interface for classes accepting database updates.
 */
public interface Updater {
    void addOrUpdate(Iterable<PhotonDoc> docs);

    void delete(String docId);

    void finish();
}
