package de.komoot.photon;

/**
 * Interface for bulk imports from a data source like nominatim
 */
public interface Importer {
    /**
     * Add a new set of document to the Photon database.
     *
     * The document set must have been created from the same base document
     * and each document must have the same place ID.
     */
    public void add(Iterable<PhotonDoc> docs);

    /**
     * Finish up the import.
     */
    public void finish();
}
