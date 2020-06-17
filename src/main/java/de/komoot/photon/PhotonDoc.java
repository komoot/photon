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
    final private int rankSearch;

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

    public PhotonDoc(long placeId, String osmType, long osmId, String tagKey, String tagValue, Map<String, String> name, String houseNumber, Map<String, String> address, Map<String, String> extratags, Envelope bbox, long parentPlaceId, double importance, CountryCode countryCode, Point centroid, long linkedPlaceId, int rankSearch) {
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
        this.houseNumber = houseNumber;
        this.address = address;
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
        this.rankSearch = other.rankSearch;
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

        this.street = extractAddress(this.street, address, "street");
        this.city = extractAddress(this.city, address, "city");
        this.district = extractAddress(this.district, address, "suburb");
        this.locality = extractAddress(this.locality, address, "neighbourhood");
        this.county = extractAddress(this.county, address, "county");

        String addressPostCode = address.get("postcode");
        if (addressPostCode != null && !addressPostCode.equals(this.postcode)) {
            if (log.isDebugEnabled()) {
                log.debug("Replacing postcode "+this.postcode+" with "+ addressPostCode+ " for osmId #" + osmId);
            }
            this.postcode = addressPostCode;
        }
    }

    private Map<String, String> extractAddress(Map<String, String> existingField, Map<String, String> completeAddress, String addressFieldName) {
        String field = completeAddress.get(addressFieldName);
        if(field != null) {
            Map<String, String> map = nullToEmptyMap(existingField);
            setOrReplace(field, map, addressFieldName);
            return map;
        } else return existingField;
    }

    private Map<String, String> nullToEmptyMap(Map<String, String> map) {
        if (map == null) {
            return new HashMap<>();
        } else return map;
    }

    private void setOrReplace(String name, Map<String, String> namesMap, String field) {
        String existingName = namesMap.get("name");
        if (!name.equals(existingName)) {
            if (log.isDebugEnabled()) {
                log.debug("Replacing "+ field +" name '"+existingName+"' with '"+ name+ "' for osmId #" + osmId);
            }
            // we keep the former name in the context as it might be helpful when looking up typos
            if(!Objects.isNull(existingName)) {
                context.add(ImmutableMap.of("formerName", existingName));
            }
            namesMap.put("name", name);
        }
    }
}
