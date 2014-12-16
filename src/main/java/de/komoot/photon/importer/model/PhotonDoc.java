package de.komoot.photon.importer.model;

import com.neovisionaries.i18n.CountryCode;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Point;
import java.util.Arrays;
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
        final private int admin_level;

	private Map<String, String> street;
	private Map<String, String> city;
	private Set<Map<String, String>> context = new HashSet<Map<String, String>>();
	private Map<String, String> country;

        /**
         * Used for testing - really all variables required (final)?         
         */
        public static PhotonDoc create(long placeId, String osmType, long osmId, Map<String, String> nameMap) {
            return new PhotonDoc(placeId, osmType, osmId, "", "", nameMap,
            		"", null, null, 0, 0, null, null, 0, 0, 0);
        }

	public boolean isUsefulForIndex() {
                // Falk specific accepted tags
                Set<String> acceptedKeys = new HashSet<String>(Arrays.asList("place,highway,landuse,leisure,boundary".split(",")));
                if (!acceptedKeys.contains(tagKey)) return false;
            
                Set<String> acceptedPlaceTags = new HashSet<String>(Arrays.asList("locality,village,hamlet,island,town,isolated_dwelling,suburb,islet,neighbourhood,city,municipality,region,county,country,state,city_block,borough,national_park".split(",")));
                if("place".equals(tagKey) && !acceptedPlaceTags.contains(tagValue)) return false;
                
                Set<String> acceptedHighwayTags = new HashSet<String>(Arrays.asList("residential,unclassified,tertiary,secondary,primary,trunk,pedestrian".split(",")));
                if("highway".equals(tagKey) && !acceptedHighwayTags.contains(tagValue)) return false;
                
                Set<String> acceptedLanduseTags = new HashSet<String>(Arrays.asList("forest,park,nature_reserve".split(",")));
                if("landuse".equals(tagKey) && !acceptedLanduseTags.contains(tagValue)) return false;
                
                Set<String> acceptedLeisureTags = new HashSet<String>(Arrays.asList("park,nature_reserve".split(",")));
                if("leisure".equals(tagKey) && !acceptedLeisureTags.contains(tagValue)) return false;
                
                Set<String> acceptedBoundaryTags = new HashSet<String>(Arrays.asList("administrative,national_park,protected_area".split(",")));
                if("boundary".equals(tagKey) && !acceptedBoundaryTags.contains(tagValue)) return false;
                
                // Filter boundaries that are not admin_level 8
                if("boundary".equals(tagKey) && tagValue.equals("administrative") && (admin_level > 8 || admin_level < 0)) return false;
                // End Falk specific accepted tags
                                
                // Not used when using the Falk place whitelist
		//if("place".equals(tagKey) && "houses".equals(tagValue)) return false;

		if(houseNumber != null) return true;

		if(name.isEmpty()) return false;

		if(linkedPlaceId > 0) return false;

		return true;
	}
}
