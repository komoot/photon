package de.komoot.photon;

import com.neovisionaries.i18n.CountryCode;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Point;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * denormalized doc with all information needed be dumped to elasticsearch
 *
 * @author christoph
 */
@Getter
@Setter
public class PhotonDoc {
    final private long placeId;
    final private String osmType;
    final private long osmId;
    final private String tagKey;
    final private String tagValue;
    final private Map<String, String> name;
    private String postcode;
    final private Map<String, String> extratags;
    final private Envelope bbox;
    final private long parentPlaceId; // 0 if unset
    final private double importance;
    final private CountryCode countryCode;
    final private long linkedPlaceId; // 0 if unset
    final private int rankSearch;

    private Map<String, String> street;
    private Map<String, String> city;
    private Set<Map<String, String>> context = new HashSet<Map<String, String>>();
    private Map<String, String> country;
    private Map<String, String> state;
    private List<String> houseNumbers;
    private Point centroid;
    private boolean isExpanded = false;

    public PhotonDoc(long placeId, String osmType, long osmId, String tagKey, String tagValue, Map<String, String> name, @Nullable List<String> houseNumbers, Map<String, String> extratags, Envelope bbox, long parentPlaceId, double importance, CountryCode countryCode, Point centroid, long linkedPlaceId, int rankSearch) {
        String place = extratags != null ? extratags.get("place") : null;
        if (place != null) {
            // take more specific extra tag information
            tagKey = "place";
            tagValue = place;
        }

        this.placeId = placeId;
        this.osmType = osmType;
        this.osmId = osmId;
        this.tagKey = tagKey;
        this.tagValue = tagValue;
        this.name = name;
        this.houseNumbers = houseNumbers;
        this.extratags = extratags;
        this.bbox = bbox;
        this.parentPlaceId = parentPlaceId;
        this.importance = importance;
        this.countryCode = countryCode;
        this.centroid = centroid;
        this.linkedPlaceId = linkedPlaceId;
        this.rankSearch = rankSearch;
    }

    public PhotonDoc(PhotonDoc other) {
        this.placeId = other.placeId;
        this.osmType = other.osmType;
        this.osmId = other.osmId;
        this.tagKey = other.tagKey;
        this.tagValue = other.tagValue;
        this.name = other.name;
        this.houseNumbers = other.houseNumbers;
        this.postcode = other.postcode;
        this.extratags = other.extratags;
        this.bbox = other.bbox;
        this.parentPlaceId = other.parentPlaceId;
        this.importance = other.importance;
        this.countryCode = other.countryCode;
        this.centroid = other.centroid;
        this.linkedPlaceId = other.linkedPlaceId;
        this.rankSearch = other.rankSearch;
        this.street = other.street;
        this.city = other.city;
        this.context = other.context;
        this.country = other.country;
        this.state = other.state;
        this.isExpanded = other.isExpanded;
    }

    /**
     * Get an id suitable for using as an ES document id
     * 
     * @return an id
     */
    @Nonnull
    public String getUid() {
        String id = getBaseId(osmType, osmId, tagKey);
        if (houseNumbers == null || houseNumbers.isEmpty()) {
            return id;
        } else {
            return id + (isExpanded ? "." + houseNumbers.get(0) : "");
        }
    }

    /**
     * Construct a base id for a document
     * 
     * @param elementType the OSM element type (N, W, R)
     * @param id the id of the OSM object
     * @param key the key typically "place" or the key of the tag 
     * @return an id String
     */
    public static String getBaseId(@Nonnull String elementType, long id, @Nonnull String key) {
        return elementType + Long.toString(id) + key;
    }

    /**
     * Used for testing - really all variables required (final)?
     */
    public static PhotonDoc create(long placeId, String osmType, long osmId, Map<String, String> nameMap) {
        return new PhotonDoc(placeId, osmType, osmId, "", "", nameMap,
                null, null, null, 0, 0, null, null, 0, 0);
    }

    public boolean isUsefulForIndex() {
        if ("place".equals(tagKey) && "houses".equals(tagValue)) return false;

        if (houseNumbers != null && !houseNumbers.isEmpty()) {
            return true;
        }

        if (name.isEmpty()) return false;

        if (linkedPlaceId > 0) return false;

        return true;
    }
}
