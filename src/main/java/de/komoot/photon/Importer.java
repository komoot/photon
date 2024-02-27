package de.komoot.photon;

/**
 * Interface for bulk imports from a data source like nominatim
 */
public interface Importer {
    /**
     * Add a new document to the Photon database.
     */
    public void add(PhotonDoc doc, int objectId);

    /**
     * Finish up the import.
     */
    public void finish();
}
