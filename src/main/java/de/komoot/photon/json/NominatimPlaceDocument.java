package de.komoot.photon.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import de.komoot.photon.ConfigExtraTags;
import de.komoot.photon.PhotonDoc;
import de.komoot.photon.PhotonDocAddressSet;
import de.komoot.photon.nominatim.model.AddressRow;
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
    private AddressLine[] addressLines = null;

    private static final GeometryFactory factory = new GeometryFactory(new PrecisionModel(10000000), 4326);
    private static final GeoJsonReader jsonReader = new GeoJsonReader();

    public PhotonDoc asSimpleDoc() {
        doc.address(address);
        return doc;
    }

    public Iterable<PhotonDoc> asMultiAddressDocs(String[] countryFilter) {
        if (countryFilter != null
                && (doc.getCountryCode() == null || Arrays.binarySearch(countryFilter, doc.getCountryCode()) < 0)) {
            return List.of();
        }

        doc.address(address);
        return new PhotonDocAddressSet(doc, address);
    }

    @JsonProperty("place_id")
    void setPlaceId(long placeId) {
        doc.placeId(placeId);
    }

    @JsonProperty("object_type")
    void setOsmType(String osmType) {
        doc.osmType(osmType);
    }

    @JsonProperty("object_id")
    void setOsmId(long osmId) {
        doc.osmId(osmId);
    }

    @JsonProperty("categories")
    void setCategories(String[] categories) {
        for (var cat : categories) {
            if (cat.startsWith("osm.")) {
                String[] parts = cat.split("\\.");
                doc.tagKey(parts[1]);
                doc.tagValue(parts.length > 2 ? parts[2] : "yes");
                return;
            }
        }
        doc.tagKey("place");
        doc.tagValue("yes");
    }

    @JsonProperty("tag_key")
    void setTagKey(String tagKey) {
        doc.tagKey(tagKey);
    }

    @JsonProperty("tag_value")
    void setTagValue(String tagValue) {
        doc.tagValue(tagValue);
    }

    @JsonProperty("rank_address")
    void setRankAddress(int rankAddress) {
        doc.rankAddress(rankAddress);
    }

    @JsonProperty("admin_level")
    void setAdminLevel(int adminLevel) { doc.adminLevel(adminLevel); }

    @JsonProperty("importance")
    void setImportance(double importance) {
        doc.importance(importance);
    }

    @JsonProperty("parent_place_id")
    void setParentPlaceId(long parentPlaceId) {
        doc.parentPlaceId(parentPlaceId);
    }

    @JsonProperty("name")
    void setNames(Map<String, String> names) {
        doc.names(names);
    }

    @JsonProperty("housenumber")
    void setHousenumber(String housenumber) {
        doc.houseNumber(housenumber);
    }

    @JsonProperty("address")
    void setAddress(Map<String, String> address) {
        this.address = address;
    }

    @JsonProperty("extratags")
    void setExtratags(Map<String, String> extratags) {
        doc.extraTags(extratags);
    }

    @JsonProperty("postcode")
    void setPostcode(String postcode) {
        doc.postcode(postcode);
    }

    @JsonProperty("country_code")
    void setCountryCode(String countryCode) {
        doc.countryCode(countryCode);
    }

    @JsonProperty("centroid")
    void setCentroid(double[] coordinates) throws IOException {
        if (coordinates.length != 2) {
            throw new IOException("Invalid centroid. Must be an array of two doubles.");
        }
        doc.centroid(factory.createPoint(new Coordinate(coordinates[0], coordinates[1])));
    }

    @JsonProperty("bbox")
    void setBbox(double[] coordinates) throws IOException {
        if (coordinates.length != 4) {
            throw new IOException("Invalid bbox. Must be an array of four doubles.");
        }
        doc.bbox(factory.createMultiPoint(new Point[]{
                factory.createPoint(new Coordinate(coordinates[0], coordinates[1])),
                factory.createPoint(new Coordinate(coordinates[2], coordinates[3]))
        }));
    }

    @JsonProperty("geometry")
    void setGeometry(JsonNode geojson) throws ParseException {
        final var geometry = jsonReader.read(geojson.toString());
        doc.geometry(geometry);
        doc.bbox(geometry.getEnvelope());
    }

    @JsonProperty("addresslines")
    void setAddressLines(AddressLine[] addressLines) {
        this.addressLines = addressLines;
    }

    public void completeAddressLines(Map<Long, AddressRow> addressCache) {
        if (addressLines != null) {
            doc.completePlace(
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
