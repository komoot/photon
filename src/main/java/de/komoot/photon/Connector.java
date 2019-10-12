package de.komoot.photon;

/**
 * Connects to a datasource and imports all documents.
 *
 * @author holger
 */
public interface Connector {

    /**
     * reads every document from database and must call the Importer
     * for every document
     */
    void readEntireDatabase();
}
