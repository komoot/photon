package de.komoot.photon.importer.model;

import com.neovisionaries.i18n.CountryCode;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Point;
import lombok.Data;

import java.util.Map;

/**
 * date: 16.05.14
 *
 * @author christoph
 */
@Data
public class PhotonDoc {
	final private long placeId;
	final private String osmType;
	final private long osmId;
	final private String tagKey;
	final private String tagValue;
	final private Map<String, String> name;
	final private String houseNumber;
	private String postcode;
	final private Map<String, String> extratags;
	final private Envelope bbox;
	final private Long parentPlaceId;
	final private double importance;
	final private CountryCode countryCode;
	final private Point centroid;
	final private Long linkedPlaceId;

	private Map<String, String> street;
	private Map<String, String> city;
	private Map<String, Map<String, String>> context;
}
