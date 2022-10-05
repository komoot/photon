package de.komoot.photon;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
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
    private String tagKey;
    private String tagValue;

    private Map<String, String> name = Collections.emptyMap();
    private String postcode = null;
    private Map<String, String> extratags = Collections.emptyMap();
    private Envelope bbox = null;
    private long parentPlaceId = 0; // 0 if unset
    private double importance = 0;
    private String countryCode = null;
    private long linkedPlaceId = 0; // 0 if unset
    private int rankAddress = 30;

    private Map<AddressType, Map<String, String>> addressParts = new EnumMap<>(AddressType.class);
    private Set<Map<String, String>> context = new HashSet<>();
    private String houseNumber = null;
    private Point centroid = null;

    public PhotonDoc(long placeId, String osmType, long osmId, String tagKey, String tagValue) {
        this.placeId = placeId;
        this.osmType = osmType;
        this.osmId = osmId;
        this.tagKey = tagKey;
        this.tagValue = tagValue;
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

    public PhotonDoc names(Map<String, String> names) {
        this.name = names;
        return this;
    }

    public PhotonDoc houseNumber(String houseNumber) {
        this.houseNumber = (houseNumber == null || houseNumber.isEmpty()) ? null : houseNumber;
        return this;
    }

    public PhotonDoc bbox(Geometry geom) {
        if (geom != null) {
            this.bbox = geom.getEnvelopeInternal();
        }
        return this;
    }

    public PhotonDoc centroid(Geometry centroid) {
        this.centroid = (Point) centroid;
        return this;
    }

    public PhotonDoc countryCode(String countryCode) {
        if (countryCode != null) {
            this.countryCode = countryCode.toUpperCase();
        }
        return this;
    }


    public PhotonDoc address(Map<String, String> address) {
        if (address != null) {
            extractAddress(address, AddressType.STREET, "street");
            extractAddress(address, AddressType.CITY, "city");
            extractAddress(address, AddressType.DISTRICT, "suburb");
            extractAddress(address, AddressType.LOCALITY, "neighbourhood");
            extractAddress(address, AddressType.COUNTY, "county");
            extractAddress(address, AddressType.STATE, "state");

            String addressPostCode = address.get("postcode");
            if (addressPostCode != null && !addressPostCode.equals(postcode)) {
                if (log.isDebugEnabled()) {
                    log.debug("Replacing postcode " + postcode + " with " + addressPostCode + " for osmId #" + osmId);
                }
                postcode = addressPostCode;
            }
        }
        return this;
    }

    public PhotonDoc extraTags(Map<String, String> extratags) {
        this.extratags = extratags;

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

        return this;
    }

    public PhotonDoc parentPlaceId(long parentPlaceId) {
        this.parentPlaceId = parentPlaceId;
        return this;
    }

    public PhotonDoc importance(Double importance) {
        this.importance = importance;

        return this;
    }

    public PhotonDoc linkedPlaceId(long linkedPlaceId) {
        this.linkedPlaceId = linkedPlaceId;
        return this;
    }

    public PhotonDoc rankAddress(int rank) {
        this.rankAddress = rank;
        return this;
    }

    public PhotonDoc postcode(String postcode) {
        this.postcode = postcode;
        return this;
    }

    public String getUid() {
        if (houseNumber == null)
            return String.valueOf(placeId);

        return String.valueOf(placeId) + "." + houseNumber;
    }

    public void copyName(Map<String, String> target, String target_field, String name_field) {
        String outname = name.get("_place_" + name_field);
        if (outname == null) {
            outname = name.get(name_field);
        }

        if (outname != null) {
            target.put(target_field, outname);
        }
    }

    public void copyAddressName(Map<String, String> target, String target_field, AddressType address_field, String name_field) {
        Map<String, String> names = addressParts.get(address_field);

        if (names != null) {
            String outname = names.get("_place_" + name_field);
            if (outname == null) {
                outname = names.get(name_field);
            }

            if (outname != null) {
                target.put(target_field, outname);
            }
        }
    }

    public AddressType getAddressType() {
        return AddressType.fromRank(rankAddress);
    }

    public boolean isUsefulForIndex() {
        if ("place".equals(tagKey) && "houses".equals(tagValue)) return false;

        if (linkedPlaceId > 0) return false;

        return houseNumber != null || !name.isEmpty();
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
    private void extractAddress(Map<String, String> address, AddressType addressType, String addressFieldName) {
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
                    context.add(Collections.singletonMap("formerName", existingName));
                }
                map.put("name", field);
            }
        }
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

}
