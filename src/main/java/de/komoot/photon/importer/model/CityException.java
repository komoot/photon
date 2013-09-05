package de.komoot.photon.importer.model;

/**
 * model for a city that deviates from the nation's default admin level
 * <p/>
 * <p/>
 * e.g.: in germany, cities usually have admin_level=8 but in case of berlin admin_level=6 is used
 * <p/>
 * User: christoph Date: 01.08.13
 */
public class CityException {
	public final int adminLevel;
	public final String name;

	public CityException(int adminLevel, String name) {
		this.adminLevel = adminLevel;
		this.name = name;
	}
}
