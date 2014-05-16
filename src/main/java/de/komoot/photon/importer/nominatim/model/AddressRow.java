package de.komoot.photon.importer.nominatim.model;

import lombok.Data;

import java.util.Arrays;
import java.util.Map;

/**
 * date: 16.05.14
 *
 * @author christoph
 */
@Data
public class AddressRow {
	final private long placeId;
	final private Map<String, String> name;
	final private String osmKey;
	final private String osmValue;
	final private int rankAddress;
	final Integer adminLevel;

	static final String[] CITY_PLACE_VALUES = new String[]{"city", "hamlet", "town", "village"}; // must be in alphabetic order to speed up lookup
	static final String[] USEFUL_CONTEXT_KEYS = new String[]{"boundary", "landuse", "place"}; // must be in alphabetic order to speed up lookup

	public boolean isStreet() {
		return 26 <= rankAddress && rankAddress <= 28;
	}

	public boolean isCity() {
		if("place".equals(osmKey) && Arrays.binarySearch(CITY_PLACE_VALUES, osmValue) >= 0) {
			return true;
		}

		if(adminLevel == 8 && "boundary".equals(osmKey) && "administrative".equals(osmValue)) {
			return true;
		}

		return false;
	}

	public boolean isPostcode() {
		if("place".equals(osmKey) && "postcode".equals(osmValue)) {
			return true;
		}

		if("boundary".equals(osmKey) && "postal_code".equals(osmValue)) {
			return true;
		}

		return false;
	}

	public boolean isUsefulForContext() {
		if(name.isEmpty()) {
			return false;
		}

		if(isPostcode()) {
			return false;
		}

		if(rankAddress < 4) {
			// continent, sea, ...
			return false;
		}

		if(Arrays.binarySearch(USEFUL_CONTEXT_KEYS, osmKey) >= 0) {
			return true;
		}

		return false;
	}
}
