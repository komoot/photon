package de.komoot.photon.importer;

import de.komoot.photon.importer.model.NominatimEntry;

/**
 * date: 28.09.13
 *
 * @author christoph
 */
public interface Exporter {
	/**
	 * called for every entry that is exported
	 *
	 * @param entry
	 */
	void write(NominatimEntry entry);

	/** called after last entry was exported */
	void finish();
}
