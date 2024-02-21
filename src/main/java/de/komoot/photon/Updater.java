package de.komoot.photon;

/**
 * Interface for classes accepting database updates.
 */
public interface Updater {
    void create(PhotonDoc doc, int object_id);

    void delete(long doc_id, int object_id);

    boolean exists(long doc_id, int object_id);

    void finish();
}
