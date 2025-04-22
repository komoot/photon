package de.komoot.photon;

/**
 * Interface for classes accepting database updates.
 */
public interface Updater {
    void create(PhotonDoc doc, int objectId);

    void delete(long docId, int objectId);
    void delete(String id);

    boolean exists(long docId, int objectId);

    void finish();

    void cleanManualRecords(String prefix);
}
