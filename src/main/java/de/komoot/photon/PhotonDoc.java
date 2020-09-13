package de.komoot.photon;

import com.google.common.collect.ImmutableMap;
import com.neovisionaries.i18n.CountryCode;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Point;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * denormalized doc with all information needed be dumped to elasticsearch
 *
 * @author christoph
 */
@Getter
@Setter
@Slf4j
public class PhotonDoc {
    final private long placeId;
    final private String osmType;
    final private long osmId;
    final private String tagKey;
    final private String tagValue;
    final private Map<String, String> name;
    private String postcode;
    final private Map<String, String> address;
    final private Map<String, String> extratags;
    final private Envelope bbox;
    final private long parentPlaceId; // 0 if unset
    final private double importance;
    final private CountryCode countryCode;
    final private long linkedPlaceId; // 0 if unset
    final private int rankAddress;

    private Map<String, String> street;
    private Map<String, String> locality;
    private Map<String, String> district;
    private Map<String, String> city;
    private Map<String, String> county; // careful, this is county not count_r_y
    private Set<Map<String, String>> context = new HashSet<Map<String, String>>();
    private Map<String, String> country;
    private Map<String, String> state;
    private String houseNumber;
    private Point centroid;

    public PhotonDoc(long placeId, String osmType, long osmId, String tagKey, String tagValue, Map<String, String> name, String houseNumber, Map<String, String> address, Map<String, String> extratags, Envelope bbox, long parentPlaceId, double importance, CountryCode countryCode, Point centroid, long linkedPlaceId, int rankAddress) {
        if (extratags != null) {
            String place = extratags.get("place");
            if (place == null) {
                place = extratags.get("linked_place");
            }
            if (place != null) {
                // take more specific extra tag information
                tagKey = "place";
                tagValue = place;
            }
        }

        this.placeId = placeId;
        this.osmType = osmType;
        this.osmId = osmId;
        this.tagKey = tagKey;
        this.tagValue = tagValue;
        this.name = name;
        this.houseNumber = houseNumber;
        this.address = address;
        this.extratags = extratags;
        this.bbox = bbox;
        this.parentPlaceId = parentPlaceId;
        this.importance = importance;
        this.countryCode = countryCode;
        this.centroid = centroid;
        this.linkedPlaceId = linkedPlaceId;
        this.rankAddress = rankAddress;
    }

    public PhotonDoc(PhotonDoc other) {
        this.placeId = other.placeId;
        this.osmType = other.osmType;
        this.osmId = other.osmId;
        this.tagKey = other.tagKey;
        this.tagValue = other.tagValue;
        this.name = other.name;
        this.houseNumber = other.houseNumber;
        this.postcode = other.postcode;
        this.address = other.address;
        this.extratags = other.extratags;
        this.bbox = other.bbox;
        this.parentPlaceId = other.parentPlaceId;
        this.importance = other.importance;
        this.countryCode = other.countryCode;
        this.centroid = other.centroid;
        this.linkedPlaceId = other.linkedPlaceId;
        this.rankAddress = other.rankAddress;
        this.street = other.street;
        this.locality = other.locality;
        this.district = other.district;
        this.city = other.city;
        this.county= other.county;
        this.context = other.context;
        this.country = other.country;
        this.state = other.state;
    }

    public String getUid() {
        if (houseNumber == null || houseNumber.isEmpty())
            return String.valueOf(placeId);
        else
            return String.valueOf(placeId) + "." + houseNumber;
    }

    /**
     * Used for testing - really all variables required (final)?
     */
    public static PhotonDoc create(long placeId, String osmType, long osmId, Map<String, String> nameMap) {
        return new PhotonDoc(placeId, osmType, osmId, "", "", nameMap,
                "", null, null, null, 0, 0, null, null, 0, 0);
    }

    /**
     * Return the GeocodeJSON place type.
     *
     * @return A string representation of the type
     */
    public final String getObjectType() {
        if (rankAddress >= 28) return "house";
        if (rankAddress >= 26) return "street";
        if (rankAddress >= 13 && rankAddress <= 16) return "city";
        if (rankAddress >= 5 && rankAddress <= 12) return "region";
        if (rankAddress == 4) return "country";

        return "locality";
    }

    public boolean isUsefulForIndex() {
        if ("place".equals(tagKey) && "houses".equals(tagValue)) return false;

        if (houseNumber != null) return true;

        if (name.isEmpty()) return false;

        if (linkedPlaceId > 0) return false;

        return true;
    }
    
    /**
     * Complete doc from nominatim address information.
     */
    public void completeFromAddress() {
        if (address == null) return;

        street = extractAddress(street, "street");
        city = extractAddress(city, "city");
        district = extractAddress(district, "suburb");
        locality = extractAddress(locality, "neighbourhood");
        county = extractAddress(county, "county");
        state = extractAddress(state, "state");

        String addressPostCode = address.get("postcode");
        if (addressPostCode != null && !addressPostCode.equals(postcode)) {
            if (log.isDebugEnabled()) {
                log.debug("Replacing postcode " + postcode + " with "+ addressPostCode + " for osmId #" + osmId);
            }
            postcode = addressPostCode;
        }
    }


    /**
     * Extract an address field from an address tag and replace the appropriate address field in the document.
     *
     * @param existingField The current value of the document's address field.
     * @param addressFieldName The name of the address tag to use (without the 'addr:' prefix).
     *
     * @return 'existingField' potentially with the name field replaced. If existingField was null and
     *         the address field could be found, then a new map with the address as single entry is returned.
     */
    private Map<String, String> extractAddress(Map<String, String> existingField, String addressFieldName) {
        String field = address.get(addressFieldName);

        if (field == null) return existingField;

        Map<String, String> map = (existingField == null) ? new HashMap<>() : existingField;

        String existingName = map.get("name");
        if (!field.equals(existingName)) {
            if (log.isDebugEnabled()) {
                log.debug("Replacing " + addressFieldName + " name '" + existingName + "' with '" + field + "' for osmId #" + osmId);
            }
            // we keep the former name in the context as it might be helpful when looking up typos
            if(!Objects.isNull(existingName)) {
                context.add(ImmutableMap.of("formerName", existingName));
            }
            map.put("name", field);
        }

        return map;
    }
}
