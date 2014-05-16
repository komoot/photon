package de.komoot.photon.importer;

import de.komoot.photon.importer.model.PhotonDoc;

/**
 *
 * @author felix
 */
public interface Updater {
    public void create(PhotonDoc doc);

    public void update(PhotonDoc doc);

    public void delete(Long id);

    public void finish();
}
