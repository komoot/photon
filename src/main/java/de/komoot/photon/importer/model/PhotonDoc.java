package de.komoot.photon.importer.model;

import com.neovisionaries.i18n.CountryCode;
import com.sun.xml.internal.messaging.saaj.soap.Envelope;
import com.vividsolutions.jts.geom.Point;
import lombok.Value;

import java.util.Map;

/**
 * date: 16.05.14
 *
 * @author christoph
 */
@Value
public class PhotonDoc {
	final private long placeId;
	final private String osmType;
	final private String osmId;
	final private String tagKey;
	final private String tagValue;
	final private Map<String, String> name;
	final private String houseNumber;
	final private String postcode;
	final private Map<String, String> extratags;
	final private Envelope bbox;
	final private Long parentPlaceId;
	final private double importance;
	final private CountryCode countryCode;
	final private Point centroid;
	final private Long linkedPlaceId;
}
