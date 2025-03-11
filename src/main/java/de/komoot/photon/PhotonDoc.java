    package de.komoot.photon;

import de.komoot.photon.nominatim.model.AddressRow;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import de.komoot.photon.nominatim.model.AddressType;
import org.slf4j.Logger;

import java.util.*;

/**
 * Denormalized document with all information needed for saving in the Photon database.
 */
public class PhotonDoc {
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(PhotonDoc.class);

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
    private Geometry geometry = null;

    public PhotonDoc(long placeId, String osmType, long osmId, String tagKey, String tagValue) {
        this.placeId = placeId;
        this.osmType = osmType;
        this.osmId = osmId;
        this.tagKey = tagKey;
        this.tagValue = tagValue;
    }

    public PhotonDoc(long placeId, String osmType, long osmId, String tagKey, String tagValue, Geometry geometry) {
        this.placeId = placeId;
        this.osmType = osmType;
        this.osmId = osmId;
        this.tagKey = tagKey;
        this.tagValue = tagValue;
        this.geometry = geometry;
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
        this.geometry = other.geometry;
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

    public PhotonDoc geometry(Geometry geometry) {
        this.geometry = (Geometry) geometry;
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
                LOGGER.debug("Replacing postcode {} with {} for osmId #{}", postcode, addressPostCode, osmId);
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

    public String getUid(int objectId) {
        return makeUid(placeId, objectId);
    }

    public static String makeUid(long placeId, int objectId) {
        if (objectId <= 0)
            return String.valueOf(placeId);

        return String.format("%d.%d", placeId, objectId);
    }

    public void copyName(Map<String, String> target, String targetField, String nameField) {
        String outname = name.get("_place_" + nameField);
        if (outname == null) {
            outname = name.get(nameField);
        }

        if (outname != null) {
            target.put(targetField, outname);
        }
    }

    public void copyAddressName(Map<String, String> target, String targetField, AddressType addressType, String nameField) {
        Map<String, String> names = addressParts.get(addressType);

        if (names != null) {
            String outname = names.get("_place_" + nameField);
            if (outname == null) {
                outname = names.get(nameField);
            }

            if (outname != null) {
                target.put(targetField, outname);
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

        if (field == null) {
            return;
        }

        Map<String, String> map = addressParts.get(addressType);
        if (map == null) {
            map = new HashMap<>();
            map.put("name", field);
            addressParts.put(addressType, map);
        } else {
            String existingName = map.get("name");
            if (!field.equals(existingName)) {
                // Make a copy of the original name map because the map is reused for other addresses.
                map = new HashMap<>(map);
                LOGGER.debug("Replacing {} name '{}' with '{}' for osmId #{}", addressFieldName, existingName, field, osmId);
                // we keep the former name in the context as it might be helpful when looking up typos
                if (!Objects.isNull(existingName)) {
                    context.add(Collections.singletonMap("formerName", existingName));
                }
                map.put("name", field);
                addressParts.put(addressType, map);
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

    /**
     * Complete address data from a list of address rows.
     */
    public void completePlace(List<AddressRow> addresses) {
        final AddressType doctype = getAddressType();
        for (AddressRow address : addresses) {
            final AddressType atype = address.getAddressType();

            if (atype != null
                    && (atype == doctype || !setAddressPartIfNew(atype, address.getName()))
                    && address.isUsefulForContext()) {
                // no specifically handled item, check if useful for context
                getContext().add(address.getName());
            }
        }
    }

    public void setCountry(Map<String, String> names) {
        addressParts.put(AddressType.COUNTRY, names);
    }

    public long getPlaceId() {
        return this.placeId;
    }

    public String getOsmType() {
        return this.osmType;
    }

    public long getOsmId() {
        return this.osmId;
    }

    public String getTagKey() {
        return this.tagKey;
    }

    public String getTagValue() {
        return this.tagValue;
    }

    public Map<String, String> getName() {
        return this.name;
    }

    public String getPostcode() {
        return this.postcode;
    }

    public Map<String, String> getExtratags() {
        return this.extratags;
    }

    public Envelope getBbox() {
        return this.bbox;
    }

    public long getParentPlaceId() {
        return this.parentPlaceId;
    }

    public double getImportance() {
        return this.importance;
    }

    public String getCountryCode() {
        return this.countryCode;
    }

    public int getRankAddress() {
        return this.rankAddress;
    }

    public Map<AddressType, Map<String, String>> getAddressParts() {
        return this.addressParts;
    }

    public Set<Map<String, String>> getContext() {
        return this.context;
    }

    public String getHouseNumber() {
        return this.houseNumber;
    }

    public Point getCentroid() {
        return this.centroid;
    }

    public Geometry getGeometry() {
        return this.geometry;
    }

    @Override
    public String toString() {
        return "PhotonDoc{" +
                "placeId=" + placeId +
                ", osmType='" + osmType + '\'' +
                ", osmId=" + osmId +
                ", tagKey='" + tagKey + '\'' +
                ", tagValue='" + tagValue + '\'' +
                ", name=" + name +
                ", postcode='" + postcode + '\'' +
                ", extratags=" + extratags +
                ", bbox=" + bbox +
                ", parentPlaceId=" + parentPlaceId +
                ", importance=" + importance +
                ", countryCode='" + countryCode + '\'' +
                ", linkedPlaceId=" + linkedPlaceId +
                ", rankAddress=" + rankAddress +
                ", addressParts=" + addressParts +
                ", context=" + context +
                ", houseNumber='" + houseNumber + '\'' +
                ", centroid=" + centroid +
                ", geometry=" + geometry +
                '}';
    }
}
