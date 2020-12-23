package de.komoot.photon;

import com.google.common.collect.ImmutableMap;
import com.neovisionaries.i18n.CountryCode;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Point;
import de.komoot.photon.nominatim.model.AddressType;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * denormalized doc with all information needed be dumped to elasticsearch
 *
 * @author christoph
 */
@Getter
@Slf4j
public class PhotonDoc {
    private final long placeId;
    private final String osmType;
    private final long osmId;
    private final String tagKey;
    private final String tagValue;
    private final Map<String, String> name;
    private String postcode;
    private final Map<String, String> address;
    private final Map<String, String> extratags;
    private final Envelope bbox;
    private final long parentPlaceId; // 0 if unset
    private final double importance;
    private final CountryCode countryCode;
    private final long linkedPlaceId; // 0 if unset
    private final int rankAddress;

    private Map<AddressType, Map<String, String>> addressParts = new EnumMap<>(AddressType.class);
    private Set<Map<String, String>> context = new HashSet<>();
    private String houseNumber;
    private Point centroid;

    public PhotonDoc(long placeId, String osmType, long osmId, String tagKey, String tagValue, Map<String, String> name, String houseNumber, Map<String, String> address, Map<String, String> extratags, Envelope bbox, long parentPlaceId, double importance, String countryCode, Point centroid, long linkedPlaceId, int rankAddress) {
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
        this.countryCode = CountryCode.getByCode(countryCode, false);
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
        this.addressParts = other.addressParts;
        this.context = other.context;
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

    public AddressType getAddressType() {
        return AddressType.fromRank(rankAddress);
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

        extractAddress(AddressType.STREET, "street");
        extractAddress(AddressType.CITY, "city");
        extractAddress(AddressType.DISTRICT, "suburb");
        extractAddress(AddressType.LOCALITY, "neighbourhood");
        extractAddress(AddressType.COUNTY, "county");
        extractAddress(AddressType.STATE, "state");

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
     * @param addressType The type of address field to fill.
     * @param addressFieldName The name of the address tag to use (without the 'addr:' prefix).
     *
     * @return 'existingField' potentially with the name field replaced. If existingField was null and
     *         the address field could be found, then a new map with the address as single entry is returned.
     */
    private void extractAddress(AddressType addressType, String addressFieldName) {
        String field = address.get(addressFieldName);

        if (field != null) {
            Map<String, String> map = addressParts.computeIfAbsent(addressType, k -> new HashMap<>());

            String existingName = map.get("name");
            if (!field.equals(existingName)) {
                if (log.isDebugEnabled()) {
                    log.debug("Replacing " + addressFieldName + " name '" + existingName + "' with '" + field + "' for osmId #" + osmId);
                }
                // we keep the former name in the context as it might be helpful when looking up typos
                if (!Objects.isNull(existingName)) {
                    context.add(ImmutableMap.of("formerName", existingName));
                }
                map.put("name", field);
            }
        }
    }

    public void setPostcode(String postcode) {
        this.postcode = postcode;
    }

    /**
     * Set names for the given address part if it is not already set.
     *
     * @return True, if the address was inserted.
     */
    public boolean setAddressPartIfNew(AddressType addressType, Map<String, String> names) {
        return addressParts.computeIfAbsent(addressType, k -> names) == names;
    }

    public void setCountry(Map<String, String> names) {
        addressParts.put(AddressType.COUNTRY, names);
    }

    public void setHouseNumber(String houseNumber) {
        this.houseNumber = houseNumber;
    }

    public void setCentroid(Point centroid) {
        this.centroid = centroid;
    }
}
