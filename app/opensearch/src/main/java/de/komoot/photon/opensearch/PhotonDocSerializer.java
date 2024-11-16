package de.komoot.photon.opensearch;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
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
    private final String[] languages;
    private final String[] extraTags;

    public PhotonDocSerializer(String[] languages, String[] extraTags) {
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

        if (value.getGeometry() != null) {
            GeoJsonWriter g = new GeoJsonWriter();
            gen.writeObjectField("geometry", g.write(value.getGeometry()));
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

        writeContext(gen, value.getContext());
        writeExtraTags(gen, value.getExtratags());
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

    private void writeContext(JsonGenerator gen, Set<Map<String, String>> contexts) throws IOException {
        final Map<String, Set<String>> multimap = new HashMap<>();

        for (Map<String, String> context : contexts) {
            if (context.get("name") != null) {
                multimap.computeIfAbsent("default", k -> new HashSet<>()).add(context.get("name"));
            }

            for (String language : languages) {
                if (context.get("name:" + language) != null) {
                    multimap.computeIfAbsent("default", k -> new HashSet<>()).add(context.get("name:" + language));
                }
            }
        }

        if (!multimap.isEmpty()) {
            gen.writeObjectFieldStart("context");
            for (Map.Entry<String, Set<String>> entry : multimap.entrySet()) {
                gen.writeStringField(entry.getKey(), String.join(", ", entry.getValue()));
            }
            gen.writeEndObject();
        }
    }

    private void writeExtraTags(JsonGenerator gen, Map<String, String> docTags) throws IOException {
        boolean foundTag = false;

        for (String tag: extraTags) {
            String value = docTags.get(tag);
            if (value != null) {
                if (!foundTag) {
                    gen.writeObjectFieldStart("extra");
                    foundTag = true;
                }
                gen.writeStringField(tag, value);
            }
        }

        if (foundTag) {
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
