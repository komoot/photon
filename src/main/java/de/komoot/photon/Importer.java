package de.komoot.photon;

import org.jspecify.annotations.NullMarked;

/**
 * Interface for bulk imports from a data source like nominatim
 */
@NullMarked
public interface Importer {
    /**
     * Add a new set of document to the Photon database.
     * <p>
     * The document set must have been created from the same base document
     * and each document must have the same place ID.
     */
    void add(Iterable<PhotonDoc> docs);

    /**
     * Finish up the import.
     */
    void finish();
}