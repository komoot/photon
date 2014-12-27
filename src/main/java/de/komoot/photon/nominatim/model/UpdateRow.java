package de.komoot.photon.nominatim.model;

import lombok.Data;

/**
 * @author felix
 */
@Data
public class UpdateRow {

	public Long placeId;
	public Integer indexdStatus; // 1 - index, 2 - update, 100 - delete
}
