package de.komoot.photon;

import com.neovisionaries.i18n.CountryCode;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Point;
import lombok.Data;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * denormalized doc with all information needed be dumped to elasticsearch
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
	final private long parentPlaceId; // 0 if unset
	final private double importance;
	final private CountryCode countryCode;
	final private Point centroid;
	final private long linkedPlaceId; // 0 if unset
	final private int rankSearch;

	private Map<String, String> street;
	private Map<String, String> city;
	private Set<Map<String, String>> context = new HashSet<Map<String, String>>();
	private Map<String, String> country;
	private Map<String, String> state;

	/**
	 * Used for testing - really all variables required (final)?
	 */
	public static PhotonDoc create(long placeId, String osmType, long osmId, Map<String, String> nameMap) {
		return new PhotonDoc(placeId, osmType, osmId, "", "", nameMap,
				"", null, null, 0, 0, null, null, 0, 0);
	}

	public boolean isUsefulForIndex() {
		if("place".equals(tagKey) && "houses".equals(tagValue)) return false;

		if(houseNumber != null) return true;

		if(name.isEmpty()) return false;

		if(linkedPlaceId > 0) return false;

		return true;
	}
}
