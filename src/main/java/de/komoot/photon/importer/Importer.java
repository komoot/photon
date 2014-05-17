package de.komoot.photon.importer;
import de.komoot.photon.importer.model.PhotonDoc;

/**
 *
 * @author felix
 */
public interface Importer {

    public void addDocument(PhotonDoc doc);

    public void finish();
}
