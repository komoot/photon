package de.komoot.photon.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import de.komoot.photon.ConfigExtraTags;
import de.komoot.photon.PhotonDoc;
import de.komoot.photon.PhotonDocAddressSet;
import de.komoot.photon.nominatim.model.AddressRow;
import de.komoot.photon.nominatim.model.AddressType;
import de.komoot.photon.nominatim.model.NameMap;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.geojson.GeoJsonReader;

import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

@NullMarked
public class NominatimPlaceDocument {
    public static final String DOCUMENT_TYPE = "Place";

    private static final Pattern PLACEID_PATTERN = Pattern.compile("[0-9a-zA-Z_-]{1,60}");

    private final PhotonDoc doc = new PhotonDoc();
    @Nullable private AddressMap address = null;
    private Map<String, String> names = Map.of();
    private AddressLine @Nullable [] addressLines = null;

    private static final GeometryFactory factory = new GeometryFactory(new PrecisionModel(10000000), 4326);
    private static final GeoJsonReader jsonReader = new GeoJsonReader();

    public PhotonDoc asSimpleDoc(Set<String> languages) {
        if (!names.isEmpty()) {
            doc.names(NameMap.makeForPlace(names, languages));
        }

        if (address != null) {
            doc.addAddresses(address.get(), languages);
        }
        return doc;
    }

    public Iterable<PhotonDoc> asMultiAddressDocs(String @Nullable [] countryFilter, Set<String> languages) {
        if (!names.isEmpty()) {
            doc.names(NameMap.makeForPlace(names, languages));
        }

        if (countryFilter != null
                && (doc.getCountryCode() == null || Arrays.binarySearch(countryFilter, doc.getCountryCode()) < 0)) {
            return List.of();
        }

        if (address != null) {
            doc.addAddresses(address.get(), languages);
        }

        return new PhotonDocAddressSet(doc, address == null ? Map.of() : address.getMainAddress());
    }

    @Nullable
    public AddressRow asAddressRow(Set<String> languages) {
        var addressType = doc.getAddressType();
        if (!names.isEmpty() && addressType != AddressType.HOUSE && addressType != AddressType.OTHER) {
            final var row = AddressRow.make(
                    names, doc.getTagKey(), doc.getTagValue(),
                    addressType, languages);
            return row.getName().isEmpty() ? null : row;
        }

        return null;
    }

    @Nullable
    public String getPlaceId() {
        return doc.getPlaceId();
    }

    @Nullable
    String getCountryCode() {
        return doc.getCountryCode();
    }

    @JsonProperty(DumpFields.PLACE_ID)
    void setPlaceId(@Nullable Long placeId) {
        if (placeId != null) {
            doc.placeId(Long.toString(placeId));
        }
    }

    @JsonProperty(DumpFields.PLACE_ID)
    void setPlaceIdAsString(@Nullable String placeId) throws IOException {
        if (placeId != null) {
            if (!PLACEID_PATTERN.matcher(placeId).matches()) {
                throw new IOException("PlaceID must only consist of letters, numbers, dash and underscore and not exceed 60 characters.");
            }
            doc.placeId(placeId);
        }
    }

    @JsonProperty(DumpFields.PLACE_OBJECT_TYPE)
    void setOsmType(String osmType) {
        doc.osmType(osmType);
    }

    @JsonProperty(DumpFields.PLACE_OBJECT_ID)
    void setOsmId(@Nullable Long osmId) {
        if (osmId != null) {
            doc.osmId(osmId);
        }
    }

    @JsonProperty(DumpFields.PLACE_OSM_KEY)
    void setOsmKey(@Nullable String key) {
        if (key != null) {
            doc.tagKey(key);
        }
    }

    @JsonProperty(DumpFields.PLACE_OSM_VALUE)
    void setOsmValue(@Nullable String value) {
        if (value != null) {
            doc.tagValue(value);
        }
    }

    @JsonProperty(DumpFields.PLACE_CATEGORIES)
    void setCategories(@Nullable List<@Nullable String> categories) {
        if (categories != null) {
            doc.categories(categories);
            // Equal comparison is intentional here. We only want to replace the
            // tagKey/Value, if it hasn't been set to something custom yet.
            //noinspection StringEquality
            if (doc.getTagKey() == PhotonDoc.DEFAULT_OSM_KEY) {
                for (var cat : categories) {
                    if (cat != null && cat.startsWith("osm.") && cat.length() > 4) {
                        String[] parts = cat.split("\\.");
                        doc.tagKey(parts[1]);
                        doc.tagValue(parts.length > 2 ? parts[2] : "yes");
                        return;
                    }
                }
            }
        }
    }

    @JsonProperty(DumpFields.PLACE_RANK_ADDRESS)
    void setRankAddress(@Nullable Integer rankAddress) {
        if (rankAddress != null) {
            doc.addressType(AddressType.fromRank(rankAddress));
        }
    }

    @JsonProperty(DumpFields.PLACE_ADDRESS_TYPE)
    void setAddressType(@Nullable String addressType) {
        if (addressType != null) {
            try {
                doc.addressType(AddressType.valueOf(addressType.toUpperCase()));
            } catch (IllegalArgumentException e) {
                doc.addressType(AddressType.OTHER);
            }
        }
    }

    @JsonProperty(DumpFields.PLACE_IMPORTANCE)
    void setImportance(@Nullable Double importance) {
        if (importance != null && !importance.isNaN() && !importance.isInfinite()) {
            doc.importance(importance);
        }
    }

    @JsonProperty(DumpFields.PLACE_NAMES)
    void setNames(@Nullable Map<String, String> names) {
        if (names != null) {
            this.names = names;
        }
    }

    @JsonProperty(DumpFields.PLACE_HOUSENUMBER)
    void setHousenumber(@Nullable String housenumber) {
        doc.houseNumber(housenumber);
    }

    @JsonProperty(DumpFields.PLACE_ADDRESS)
    void setAddress(@Nullable AddressMap address) {
        if (address != null) {
            this.address = address;
        }
    }

    @JsonProperty(DumpFields.PLACE_EXTRA_TAGS)
    void setExtratags(@Nullable Map<String, Object> extratags) {
        if (extratags != null) {
            //noinspection ConstantValue
            extratags.values().removeIf(Objects::isNull);
            doc.extraTags(extratags);
        }
    }

    @JsonProperty(DumpFields.PLACE_POSTCODE)
    void setPostcode(@Nullable String postcode) {
        if (postcode != null) {
            doc.postcode(postcode);
        }
    }

    @JsonProperty(DumpFields.PLACE_COUNTRY_CODE)
    void setCountryCode(@Nullable String countryCode) {
        if (countryCode != null) {
            doc.countryCode(countryCode);
        }
    }

    @JsonProperty(DumpFields.PLACE_CENTROID)
    void setCentroid(@Nullable Double @Nullable [] coordinates) throws IOException {
        if (coordinates != null) {
            if (coordinates.length != 2 || coordinates[0] == null || coordinates[1] == null) {
                throw new IOException("Invalid centroid. Must be an array of two doubles.");
            }
            doc.centroid(factory.createPoint(new Coordinate(coordinates[0], coordinates[1])));
        }
    }

    @JsonProperty(DumpFields.PLACE_BBOX)
    void setBbox(Double @Nullable [] coordinates) throws IOException {
        if (coordinates != null) {
            if (coordinates.length != 4
                    || coordinates[0] == null || coordinates[1] == null
                    || coordinates[2] == null || coordinates[3] == null) {
                throw new IOException("Invalid bbox. Must be an array of four doubles.");
            }
            doc.bbox(new Envelope(coordinates[0], coordinates[2], coordinates[1], coordinates[3]));
        }
    }

    @JsonProperty(DumpFields.PLACE_GEOMETRY)
    void setGeometry(@Nullable JsonNode geojson) throws ParseException {
        if (geojson != null) {
            final var geometry = jsonReader.read(geojson.toString());
            doc.geometry(geometry);
            doc.bbox(geometry.getEnvelope());
        }
    }

    @JsonProperty(DumpFields.PLACE_ADDRESSLINES)
    void setAddressLines(AddressLine @Nullable [] addressLines) {
        if (addressLines != null) {
            this.addressLines = addressLines;
        }
    }

    public void completeAddressLines(Map<String, AddressRow> addressCache) {
        if (addressLines != null) {
            List<AddressRow> rows = new ArrayList<>();
            for (AddressLine line : addressLines) {
                if (line.isAddress && line.placeId != null) {
                    AddressRow row = addressCache.get(line.placeId);
                    if (row != null) {
                        rows.add(row);
                    }
                }
            }
            if (!rows.isEmpty()) {
                doc.addAddresses(rows);
            }
       }
    }

    public void disableGeometries() {
        doc.geometry(null);
    }

    public void filterExtraTags(ConfigExtraTags extraTags) {
        doc.extraTags(extraTags.filterExtraTags(doc.getExtratags()));
    }
}
