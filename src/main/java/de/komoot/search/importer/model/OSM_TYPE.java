package de.komoot.search.importer.model;

/**
 * representation of nominatim's osm_type column
 *
 * @author christoph
 */
public enum OSM_TYPE {
	R, // relation
	N, // node
	P, // postcodes
	W; // way

	@Override
	public String toString() {
		return String.format("OSM_TYPE [%s]", this.name());
	}

	public static OSM_TYPE get(String identifier) throws Exception {
		for(OSM_TYPE osmType : values()) {
			if(osmType.name().equals(identifier)) {
				return osmType;
			}
		}

		return null;
	}
}
