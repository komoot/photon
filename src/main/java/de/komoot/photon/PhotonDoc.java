    package de.komoot.photon;

import de.komoot.photon.nominatim.model.AddressRow;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import de.komoot.photon.nominatim.model.AddressType;

import java.util.*;

/**
 * Denormalized document with all information needed for saving in the Photon database.
 */
public class PhotonDoc {
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
        this.linkedPlaceId = other.linkedPlaceId;
        this.rankAddress = other.rankAddress;
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
        this.geometry = geometry;
        return this;
    }

    public PhotonDoc countryCode(String countryCode) {
        if (countryCode != null) {
            this.countryCode = countryCode.toUpperCase();
        }
        return this;
    }

    public PhotonDoc address(Map<String, String> address) {
        Map<AddressType, Map<String, String>> overlay = new EnumMap<>(AddressType.class);
        if (address != null) {
            for (var entry : address.entrySet()) {
                final String key = entry.getKey();

                if (key.equals("postcode")) {
                    postcode = entry.getValue();
                } else {
                    var match = ADDRESS_TYPE_TAG_MAP
                            .stream()
                            .filter(e -> key.startsWith(e.getValue()))
                            .findFirst();
                    if (match.isPresent()) {
                        var atype = match.get().getKey();
                        if (atype == AddressType.OTHER) {
                            final String[] parts = key.split(":");
                            context.add(Map.of(parts.length == 1 ? "name" : ("name:" + parts[parts.length - 1]), entry.getValue()));
                        } else {
                            var newKey = "name" + key.substring(match.get().getValue().length());
                            overlay.computeIfAbsent(atype, k -> new HashMap<>()).put(newKey, entry.getValue());
                        }
                    }
                }
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
                        context.add(Map.of(newEntry.getKey(), oldValue));
                        newMap.put(newEntry.getKey(), newEntry.getValue());
                    }
                }
                addressParts.put(atype, newMap);
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
    public void completePlace(List<AddressRow> addresses) {
        final AddressType doctype = getAddressType();
        for (AddressRow address : addresses) {
            if (address.isPostcode()) {
                this.postcode = address.getName().getOrDefault("ref", this.postcode);
            } else {
                final AddressType atype = address.getAddressType();

                if (atype != null
                        && (atype == doctype || !setAddressPartIfNew(atype, address.getName()))
                        && address.isUsefulForContext()) {
                    // no specifically handled item, check if useful for context
                    getContext().add(address.getName());
                }
            }
        }
    }

    public Map<String, Set<String>> getContextByLanguage(String[] languages) {
        final Map<String, Set<String>> multimap = new HashMap<>();

        for (Map<String, String> cmap : context) {
            String locName = cmap.get("name");
            if (locName != null) {
                multimap.computeIfAbsent("default", k -> new HashSet<>()).add(locName);
            }

            for (String language : languages) {
                locName = cmap.get("name:" + language);
                if (locName != null) {
                    multimap.computeIfAbsent(language, k -> new HashSet<>()).add(locName);
                }
            }
        }

        return multimap;
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
