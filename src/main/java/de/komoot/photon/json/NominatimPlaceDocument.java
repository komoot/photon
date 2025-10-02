package de.komoot.photon.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import de.komoot.photon.ConfigExtraTags;
import de.komoot.photon.PhotonDoc;
import de.komoot.photon.PhotonDocAddressSet;
import de.komoot.photon.nominatim.model.AddressRow;
import de.komoot.photon.nominatim.model.NameMap;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.geojson.GeoJsonReader;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class NominatimPlaceDocument {
    public static final String DOCUMENT_TYPE = "Place";

    private final PhotonDoc doc = new PhotonDoc();
    private Map<String, String> address = Map.of();
    private Map<String, String> names = null;
    private AddressLine[] addressLines = null;

    private static final GeometryFactory factory = new GeometryFactory(new PrecisionModel(10000000), 4326);
    private static final GeoJsonReader jsonReader = new GeoJsonReader();

    public PhotonDoc asSimpleDoc(String[] languages) {
        if (names != null) {
            doc.names(NameMap.makeForPlace(names, languages));
        }

        doc.addAddresses(address, languages);
        return doc;
    }

    public Iterable<PhotonDoc> asMultiAddressDocs(String[] countryFilter, String[] languages) {
        if (names != null) {
            doc.names(NameMap.makeForPlace(names, languages));
        }

        if (countryFilter != null
                && (doc.getCountryCode() == null || Arrays.binarySearch(countryFilter, doc.getCountryCode()) < 0)) {
            return List.of();
        }

        doc.addAddresses(address, languages);
        return new PhotonDocAddressSet(doc, address);
    }

    public AddressRow asAddressRow(String[] languages) {
        if (doc.getRankAddress() > 0 && doc.getRankAddress() < 28 && !names.isEmpty()) {
            final var row = AddressRow.make(
                    names, doc.getTagKey(), doc.getTagValue(),
                    doc.getRankAddress(), languages);
            return row.getName().isEmpty() ? null : row;
        }

        return null;
    }

    public Long getPlaceId() {
        return doc.getPlaceId();
    }

    String getCountryCode() {
        return doc.getCountryCode();
    }

    @JsonProperty(DumpFields.PLACE_ID)
    void setPlaceId(long placeId) {
        doc.placeId(placeId);
    }

    @JsonProperty(DumpFields.PLACE_OBJECT_TYPE)
    void setOsmType(String osmType) {
        doc.osmType(osmType);
    }

    @JsonProperty(DumpFields.PLACE_OBJECT_ID)
    void setOsmId(long osmId) {
        doc.osmId(osmId);
    }

    @JsonProperty(DumpFields.PLACE_CATEGORIES)
    void setCategories(List<String> categories) {
        doc.categories(categories);
        for (var cat : categories) {
            if (cat.startsWith("osm.") && cat.length() > 4) {
                String[] parts = cat.split("\\.");
                doc.tagKey(parts[1]);
                doc.tagValue(parts.length > 2 ? parts[2] : "yes");
                return;
            }
        }
        doc.tagKey("place");
        doc.tagValue("yes");
    }

    @JsonProperty(DumpFields.PLACE_RANK_ADDRESS)
    void setRankAddress(int rankAddress) {
        doc.rankAddress(rankAddress);
    }

    @JsonProperty(DumpFields.PLACE_ADMIN_LEVEL)
    void setAdminLevel(int adminLevel) { doc.adminLevel(adminLevel); }

    @JsonProperty(DumpFields.PLACE_IMPORTANCE)
    void setImportance(double importance) {
        doc.importance(importance);
    }

    @JsonProperty(DumpFields.PLACE_PARENT_PLACE_ID)
    void setParentPlaceId(long parentPlaceId) {
        doc.parentPlaceId(parentPlaceId);
    }

    @JsonProperty(DumpFields.PLACE_NAMES)
    void setNames(Map<String, String> names) {
        this.names = names;
    }

    @JsonProperty(DumpFields.PLACE_HOUSENUMBER)
    void setHousenumber(String housenumber) {
        doc.houseNumber(housenumber);
    }

    @JsonProperty(DumpFields.PLACE_ADDRESS)
    void setAddress(Map<String, String> address) {
        this.address = address;
    }

    @JsonProperty(DumpFields.PLACE_EXTRA_TAGS)
    void setExtratags(Map<String, String> extratags) {
        doc.extraTags(extratags);
    }

    @JsonProperty(DumpFields.PLACE_POSTCODE)
    void setPostcode(String postcode) {
        doc.postcode(postcode);
    }

    @JsonProperty(DumpFields.PLACE_COUNTRY_CODE)
    void setCountryCode(String countryCode) {
        doc.countryCode(countryCode);
    }

    @JsonProperty(DumpFields.PLACE_CENTROID)
    void setCentroid(double[] coordinates) throws IOException {
        if (coordinates.length != 2) {
            throw new IOException("Invalid centroid. Must be an array of two doubles.");
        }
        doc.centroid(factory.createPoint(new Coordinate(coordinates[0], coordinates[1])));
    }

    @JsonProperty(DumpFields.PLACE_BBOX)
    void setBbox(double[] coordinates) throws IOException {
        if (coordinates.length != 4) {
            throw new IOException("Invalid bbox. Must be an array of four doubles.");
        }
        doc.bbox(factory.createMultiPoint(new Point[]{
                factory.createPoint(new Coordinate(coordinates[0], coordinates[1])),
                factory.createPoint(new Coordinate(coordinates[2], coordinates[3]))
        }));
    }

    @JsonProperty(DumpFields.PLACE_GEOMETRY)
    void setGeometry(JsonNode geojson) throws ParseException {
        final var geometry = jsonReader.read(geojson.toString());
        doc.geometry(geometry);
        doc.bbox(geometry.getEnvelope());
    }

    @JsonProperty(DumpFields.PLACE_ADDRESSLINES)
    void setAddressLines(AddressLine[] addressLines) {
        this.addressLines = addressLines;
    }

    public void completeAddressLines(Map<Long, AddressRow> addressCache) {
        if (addressLines != null) {
            doc.addAddresses(
                Arrays.stream(addressLines)
                        .filter(l -> l.isAddress)
                        .map(l -> addressCache.get(l.placeId))
                        .filter(Objects::nonNull)
                        .collect(Collectors.toUnmodifiableList()));
       }
    }

    public void disableGeometries() {
        doc.geometry(null);
    }

    public void filterExtraTags(ConfigExtraTags extraTags) {
        doc.extraTags(extraTags.filterExtraTags(doc.getExtratags()));
    }
}
