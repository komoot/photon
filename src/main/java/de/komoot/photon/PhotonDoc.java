    package de.komoot.photon;

import de.komoot.photon.nominatim.model.AddressRow;
import de.komoot.photon.nominatim.model.ContextMap;
import de.komoot.photon.nominatim.model.NameMap;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import de.komoot.photon.nominatim.model.AddressType;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Denormalized document with all information needed for saving in the Photon database.
 */
@NullMarked
public class PhotonDoc {
    public static final String CATEGORY_VALID_CHARS = "a-zA-Z0-9_-";
    public static final Pattern CATEGORY_PATTERN = Pattern.compile(
            String.format("[%1$s]+(\\.[%1$s]+)+(,[%1$s]+(\\.[%1$s]+)+)*", PhotonDoc.CATEGORY_VALID_CHARS));
    public static final String DEFAULT_OSM_KEY = "place";
    public static final String DEFAULT_OSM_VALUE = "yes";
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

    @Nullable private String placeId = null;
    @Nullable private String osmType = null;
    private long osmId = -1;
    private String tagKey = DEFAULT_OSM_KEY;
    private String tagValue = DEFAULT_OSM_VALUE;

    private NameMap name = new NameMap();
    @Nullable private String postcode = null;
    private Map<String, String> extratags = Map.of();
    private Set<String> categorySet = Set.of();
    @Nullable private Envelope bbox = null;
    private double importance = 0;
    @Nullable private String countryCode = null;
    private int rankAddress = 30;

    private Map<AddressType, Map<String, String>> addressParts = new EnumMap<>(AddressType.class);
    private ContextMap context = new ContextMap();
    @Nullable private String houseNumber = null;
    @Nullable private Point centroid = null;
    @Nullable private Geometry geometry = null;

    public PhotonDoc() {}

    public PhotonDoc(@Nullable String placeId, @Nullable String osmType, long osmId, String tagKey, String tagValue) {
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
        this.categorySet = other.categorySet;
        this.bbox = other.bbox;
        this.importance = other.importance;
        this.countryCode = other.countryCode;
        this.centroid = other.centroid;
        this.rankAddress = other.rankAddress;
        this.addressParts = other.addressParts;
        this.context = other.context;
        this.geometry = other.geometry;
    }

    public PhotonDoc placeId(@Nullable String placeId) {
        this.placeId = placeId;
        return this;
    }

    public PhotonDoc osmType(@Nullable String osmType) {
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

    public PhotonDoc houseNumber(@Nullable String houseNumber) {
        this.houseNumber = (houseNumber == null || houseNumber.isEmpty()) ? null : houseNumber;
        return this;
    }

    public PhotonDoc bbox(@Nullable Geometry geom) {
        if (geom != null) {
            this.bbox = geom.getEnvelopeInternal();
        }
        return this;
    }

    public PhotonDoc centroid(Geometry centroid) {
        this.centroid = (Point) centroid;
        return this;
    }

    public PhotonDoc geometry(@Nullable Geometry geometry) {
        this.geometry = geometry;
        return this;
    }

    public PhotonDoc countryCode(@Nullable String countryCode) {
        if (countryCode != null) {
            this.countryCode = countryCode.toUpperCase();
        }
        return this;
    }

    public PhotonDoc extraTags(@Nullable Map<String, String> extratags) {
        this.extratags = extratags == null ? Map.of() : extratags;

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

    public PhotonDoc categories(Collection<@Nullable String> collection) {
        this.categorySet = collection.stream()
                .filter(Objects::nonNull)
                .filter(s -> CATEGORY_PATTERN.matcher(s).matches())
                .collect(Collectors.toSet());

        return this;
    }

    public PhotonDoc importance(double importance) {
        this.importance = importance;

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

    public static String makeUid(String placeId, int objectId) {
        return (objectId <= 0) ? placeId : String.format("%s.%d", placeId, objectId);
    }

    @Nullable
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
    public PhotonDoc addAddresses(@Nullable Map<String, String> address, String[] languages) {
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

    public void setCountry(@Nullable Map<String, String> names) {
        if (names != null) {
            addressParts.put(AddressType.COUNTRY, names);
        }
    }

    @Nullable
    public String getPlaceId() {
        return this.placeId;
    }

    @Nullable
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

    @Nullable
    public String getPostcode() {
        return this.postcode;
    }

    public Map<String, String> getExtratags() {
        return this.extratags;
    }

    public Set<String> getCategories() {
            return categorySet;
    }

    @Nullable
    public Envelope getBbox() {
        return this.bbox;
    }

    public double getImportance() {
        return this.importance;
    }

    @Nullable
    public String getCountryCode() {
        return this.countryCode;
    }

    public int getRankAddress() {
        return this.rankAddress;
    }

    public Map<AddressType, Map<String, String>> getAddressParts() {
        return this.addressParts;
    }

    public ContextMap getContext() {
        return this.context;
    }

    @Nullable
    public String getHouseNumber() {
        return this.houseNumber;
    }

    @Nullable
    public Point getCentroid() {
        return this.centroid;
    }

    @Nullable
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
