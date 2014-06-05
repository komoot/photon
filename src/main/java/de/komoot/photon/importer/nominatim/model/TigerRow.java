package de.komoot.photon.importer.nominatim.model;

import lombok.Data;

import java.util.Arrays;
import java.util.Map;
import com.vividsolutions.jts.geom.Point;

/**
 * representation of an address as returned by nominatim's get_addressdata PL/pgSQL function
 *
 * @author christoph
 */
@Data
public class TigerRow {
	final private long placeId;
	final private String houseNumber;
	final private Point centroid;
	final private String postcode;

}
