package de.komoot.photon.importer;

import de.komoot.photon.importer.model.PhotonDoc;

/**
 * interface for bulk imports from a data source like nominatim
 *
 * @author felix
 */
public interface Importer {
	public void add(PhotonDoc doc);

	public void finish();
}
