package de.komoot.photon.importer.nominatim.model;

import lombok.Data;

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
}
