package de.komoot.photon;

/**
 * interface for bulk imports from a data source like nominatim
 *
 * @author felix
 */
public interface Importer {
    /**
     * a new document was imported
     *
     * @param doc
     */
    void add(PhotonDoc doc);

    /**
     * import is finished
     */
    void finish();
}
