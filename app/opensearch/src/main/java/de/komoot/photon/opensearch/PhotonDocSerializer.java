package de.komoot.photon.opensearch;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import de.komoot.photon.ConfigExtraTags;
import de.komoot.photon.Constants;
import de.komoot.photon.PhotonDoc;
import de.komoot.photon.Utils;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.io.geojson.GeoJsonWriter;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class PhotonDocSerializer extends StdSerializer<PhotonDoc> {
    // Versioning of the json output format produced. This version appears
    // in JSON dumps and allows to track changes.
    public static final String FORMAT_VERSION = "1.0.0";

    private final String[] languages;
    private final ConfigExtraTags extraTags;

    public PhotonDocSerializer(String[] languages, ConfigExtraTags extraTags) {
        super(PhotonDoc.class);
        this.languages = languages;
        this.extraTags = extraTags;
    }

    @Override
    public void serialize(PhotonDoc value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        final var atype = value.getAddressType();

        gen.writeStartObject();
        gen.writeNumberField(Constants.OSM_ID, value.getOsmId());
        gen.writeStringField(Constants.OSM_TYPE, value.getOsmType());
        gen.writeStringField(Constants.OSM_KEY, value.getTagKey());
        gen.writeStringField(Constants.OSM_VALUE, value.getTagValue());
        gen.writeStringField(Constants.OBJECT_TYPE, atype == null ? "locality" : atype.getName());
        gen.writeNumberField(Constants.IMPORTANCE, value.getImportance());

        String classification = Utils.buildClassificationString(value.getTagKey(), value.getTagValue());
        if (classification != null) {
            gen.writeStringField(Constants.CLASSIFICATION, classification);
        }

        if (value.getCentroid() != null) {
            gen.writeObjectFieldStart("coordinate");
            gen.writeNumberField("lat", value.getCentroid().getY());
            gen.writeNumberField("lon", value.getCentroid().getX());
            gen.writeEndObject();
        }

        if (value.getGeometry() != null && !value.getGeometry().getGeometryType().equals("Point")) {
            // Convert JTS Geometry to GeoJSON
            GeoJsonWriter geoJsonWriter = new GeoJsonWriter();
            String geoJson = geoJsonWriter.write(value.getGeometry());

            gen.writeFieldName("geometry");
            gen.writeRawValue(geoJson);
        }

        if (value.getHouseNumber() != null) {
            gen.writeStringField("housenumber", value.getHouseNumber());
        }

        if (value.getPostcode() != null) {
            gen.writeStringField("postcode", value.getPostcode());
        }

        writeName(gen, value, languages);

        for (var entry : value.getAddressParts().keySet()) {
            Map<String, String> fNames = new HashMap<>();

            value.copyAddressName(fNames, "default", entry, "name");

            for (String language : languages) {
                value.copyAddressName(fNames, language, entry, "name:" + language);
            }

            gen.writeObjectField(entry.getName(), fNames);
        }

        String countryCode = value.getCountryCode();
        if (countryCode != null) {
            gen.writeStringField(Constants.COUNTRYCODE, countryCode);
        }

        writeContext(gen, value.getContextByLanguage(languages));
        extraTags.writeFilteredExtraTags(gen, "extra", value.getExtratags());
        writeExtent(gen, value.getBbox());

        gen.writeEndObject();
    }

    private void writeName(JsonGenerator gen, PhotonDoc doc, String[] languages) throws IOException {
        Map<String, String> fNames = new HashMap<>();

        doc.copyName(fNames, "default", "name");

        for (String language : languages) {
            doc.copyName(fNames, language, "name:" + language);
        }

        doc.copyName(fNames, "alt", "alt_name");
        doc.copyName(fNames, "int", "int_name");
        doc.copyName(fNames, "loc", "loc_name");
        doc.copyName(fNames, "old", "old_name");
        doc.copyName(fNames, "reg", "reg_name");
        doc.copyName(fNames, "housename", "addr:housename");

        gen.writeObjectField("name", fNames);
    }

    private void writeContext(JsonGenerator gen, Map<String, Set<String>> contexts) throws IOException {
        if (!contexts.isEmpty()) {
            gen.writeObjectFieldStart("context");
            for (Map.Entry<String, Set<String>> entry : contexts.entrySet()) {
                gen.writeStringField(entry.getKey(), String.join(", ", entry.getValue()));
            }
            gen.writeEndObject();
        }
    }

    private static void writeExtent(JsonGenerator gen, Envelope bbox) throws IOException {
        if (bbox == null || bbox.getArea() == 0.) return;

        //https://opensearch.org/docs/latest/field-types/supported-field-types/geo-shape/#envelope
        gen.writeObjectFieldStart("extent");
        gen.writeStringField("type", "envelope");

        gen.writeArrayFieldStart("coordinates");

        gen.writeStartArray();
        gen.writeNumber(bbox.getMinX());
        gen.writeNumber(bbox.getMaxY());
        gen.writeEndArray();
        gen.writeStartArray();
        gen.writeNumber(bbox.getMaxX());
        gen.writeNumber(bbox.getMinY());
        gen.writeEndArray();

        gen.writeEndArray();
        gen.writeEndObject();
    }

}
