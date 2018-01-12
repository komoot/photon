package de.komoot.photon;

import de.komoot.photon.Importer;

/**
 * Connects to a datasource and imports all documents.
 * 
 * @author holger
 */
public interface Connector {

    /**
     * sets the importer which is called for every document to be imported.
     * 
     * @param importer
     */
    public void setImporter(Importer importer);

    /**
     * reads every document from database and must call the {@link #importer}
     * for every document
     */
    public void readEntireDatabase();

}
