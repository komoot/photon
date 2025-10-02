    package de.komoot.photon;

import de.komoot.photon.nominatim.model.AddressRow;
import de.komoot.photon.nominatim.model.ContextMap;
import de.komoot.photon.nominatim.model.NameMap;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import de.komoot.photon.nominatim.model.AddressType;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Denormalized document with all information needed for saving in the Photon database.
 */
public class PhotonDoc {
    public static final Pattern CATEGORY_INVALID_CHARS = Pattern.compile("[^a-zA-Z0-9_-]");
    private static final Pattern CATEGORY_PATTERN = Pattern.compile("[^a-zA-Z0-9._-]");

    private static final List<Map.Entry<AddressType, String>> ADDRESS_TYPE_TAG_MAP = List.of(
            Map.entry(AddressType.STREET, "street"),
            Map.entry(AddressType.CITY, "city"),
            Map.entry(AddressType.DISTRICT, "suburb"),
            Map.entry(AddressType.LOCALITY, "neighbourhood"),
            Map.entry(AddressType.COUNTY, "county"),
            Map.entry(AddressType.STATE, "state"),
            Map.entry(AddressType.STATE, "province"),
            Map.entry(AddressType.OTHER, "other"),
            Map.entry(AddressType.OTHER, "district"),
            Map.entry(AddressType.OTHER, "hamlet"),
            Map.entry(AddressType.OTHER, "subdistrict"),
            Map.entry(AddressType.OTHER, "municipality"),
            Map.entry(AddressType.OTHER, "region"),
            Map.entry(AddressType.OTHER, "ward"),
            Map.entry(AddressType.OTHER, "village"),
            Map.entry(AddressType.OTHER, "subward"),
            Map.entry(AddressType.OTHER, "block"),
            Map.entry(AddressType.OTHER, "quarter")
            );

    private long placeId = -1;
    private String osmType = null;
    private long osmId = -1;
    private String tagKey = null;
    private String tagValue = null;

    private NameMap name = new NameMap();
    private String postcode = null;
    private Map<String, String> extratags = Map.of();
    private Set<String> categorySet = new HashSet<>();
    private Envelope bbox = null;
    private long parentPlaceId = 0; // 0 if unset
    private double importance = 0;
    private String countryCode = null;
    private int rankAddress = 30;
    private Integer adminLevel = null;

    private Map<AddressType, Map<String, String>> addressParts = new EnumMap<>(AddressType.class);
    private ContextMap context = new ContextMap();
    private String houseNumber = null;
    private Point centroid = null;
    private Geometry geometry = null;

    public PhotonDoc() {}

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
        this.rankAddress = other.rankAddress;
        this.adminLevel = other.adminLevel;
        this.addressParts = other.addressParts;
        this.context = other.context;
        this.geometry = other.geometry;
    }

    public PhotonDoc placeId(long placeId) {
        this.placeId = placeId;
        return this;
    }

    public PhotonDoc osmType(String osmType) {
        this.osmType = osmType;
        return this;
    }

    public PhotonDoc osmId(long osmId) {
        this.osmId = osmId;
        return this;
    }

    public PhotonDoc tagKey(String tagKey) {
        this.tagKey = tagKey;
        return this;
    }

    public PhotonDoc tagValue(String tagValue) {
        this.tagValue = tagValue;
        return this;
    }

    public PhotonDoc names(NameMap names) {
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
        this.geometry = geometry;
        return this;
    }

    public PhotonDoc countryCode(String countryCode) {
        if (countryCode != null) {
            this.countryCode = countryCode.toUpperCase();
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

    public PhotonDoc categories(Collection<String> collection) {
        this.categorySet.clear();

        for (var category : collection) {
            if (!CATEGORY_PATTERN.matcher(category).find()) {
                categorySet.add(category);
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

    public PhotonDoc rankAddress(int rank) {
        this.rankAddress = rank;
        return this;
    }

    public PhotonDoc adminLevel(int level) {
        this.adminLevel = (level < 1 || level >= 15) ? null : level;
        return this;
    }

    public PhotonDoc postcode(String postcode) {
        this.postcode = postcode;
        return this;
    }

    public static String makeUid(long placeId, int objectId) {
        if (objectId <= 0)
            return String.valueOf(placeId);

        return String.format("%d.%d", placeId, objectId);
    }

    public AddressType getAddressType() {
        return AddressType.fromRank(rankAddress);
    }

    public boolean isUsefulForIndex() {
        return houseNumber != null || !name.isEmpty();
    }
    
    /**
     * Set names for the given address part if it is not already set.
     *
     * @return True, if the address was inserted.
     */
    public boolean setAddressPartIfNew(AddressType addressType, Map<String, String> names) {
        return addressParts.computeIfAbsent(addressType, k -> names) == names;
    }

    public PhotonDoc replaceAddress(Map<AddressType, Map<String, String>> addressParts) {
        this.addressParts = addressParts;
        return this;
    }

    /**
     * Complete address data from a list of address rows.
     */
    public void addAddresses(List<AddressRow> addresses) {
        final AddressType doctype = getAddressType();
        for (AddressRow address : addresses) {
            if (address.isPostcode()) {
                this.postcode = address.getName().getOrDefault("ref", this.postcode);
            } else {
                if (!address.getName().isEmpty()) {
                    final AddressType atype = address.getAddressType();

                    if (atype != null
                            && (atype == doctype || !setAddressPartIfNew(atype, address.getName()))
                            && address.isUsefulForContext()) {
                        // if address level already exists but item still looks interesting,
                        // add to context
                        context.addAll(address.getName());
                    }
                }

                context.addAll(address.getContext());
            }
        }
    }

    /**
     * Complete address data from a map of address terms.
     */
    public PhotonDoc addAddresses(Map<String, String> address, String[] languages) {
        if (address == null || address.isEmpty()) {
            return this;
        }

        List<String> langList = Arrays.asList(languages);
        Map<AddressType, Map<String, String>> overlay = new EnumMap<>(AddressType.class);
        for (var entry : address.entrySet()) {
            final String key = entry.getKey();

            if (key.equals("postcode")) {
                postcode = entry.getValue();
            } else {
                ADDRESS_TYPE_TAG_MAP
                        .stream()
                        .filter(e -> key.startsWith(e.getValue()))
                        .findFirst()
                        .ifPresent(e -> {
                            var atype = e.getKey();
                            if (atype == AddressType.OTHER) {
                                final String[] parts = key.split(":", 0);
                                final String intKey = parts[parts.length - 1];
                                if (parts.length == 1) {
                                    context.addName("default", entry.getValue());
                                } else if (langList.contains(intKey)) {
                                    context.addName(intKey, entry.getValue());
                                }
                            } else {
                                int prefixLen = e.getValue().length();
                                if (key.length() == prefixLen) {
                                    overlay.computeIfAbsent(atype, k -> new HashMap<>()).put("default", entry.getValue());
                                } else if (key.charAt(prefixLen) == ':') {
                                    final String intKey = key.substring(prefixLen + 1);
                                    if (langList.contains(intKey)) {
                                        overlay.computeIfAbsent(atype, k -> new HashMap<>()).put(intKey, entry.getValue());
                                    }
                                }
                            }
                        });
            }
        }

        for (var entry : overlay.entrySet()) {
            final var atype = entry.getKey();
            if (!setAddressPartIfNew(atype, entry.getValue())) {
                final var origMap = addressParts.get(atype);
                final var newMap = new HashMap<>(origMap);
                for (var newEntry : entry.getValue().entrySet()) {
                    final var oldValue = origMap.get(newEntry.getKey());
                    if (oldValue == null) {
                        newMap.put(newEntry.getKey(), newEntry.getValue());
                    } else if (!newEntry.getValue().equals(oldValue)) {
                        context.addName(newEntry.getKey(), oldValue);
                        newMap.put(newEntry.getKey(), newEntry.getValue());
                    }
                }
                addressParts.put(atype, newMap);
            }
        }

        return this;
    }

    public void setCountry(Map<String, String> names) {
        if (names != null) {
            addressParts.put(AddressType.COUNTRY, names);
        }
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

    public NameMap getName() {
        return this.name;
    }

    public String getPostcode() {
        return this.postcode;
    }

    public Map<String, String> getExtratags() {
        return this.extratags;
    }

    public Set<String> getCategories() { return this.categorySet; }

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

    public Integer getAdminLevel() {
        return this.adminLevel;
    }

    public Map<AddressType, Map<String, String>> getAddressParts() {
        return this.addressParts;
    }

    public ContextMap getContext() {
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
                ", rankAddress=" + rankAddress +
                ", addressParts=" + addressParts +
                ", context=" + context +
                ", houseNumber='" + houseNumber + '\'' +
                ", centroid=" + centroid +
                ", geometry=" + geometry +
                '}';
    }
}
