package de.komoot.photon.elasticsearch;

import de.komoot.photon.Constants;
import de.komoot.photon.PhotonDoc;
import de.komoot.photon.nominatim.model.AddressType;

import org.elasticsearch.common.xcontent.*;
import org.elasticsearch.common.xcontent.json.JsonXContent;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.io.geojson.GeoJsonWriter;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static de.komoot.photon.Utils.buildClassificationString;

public class PhotonDocConverter {
    public static XContentBuilder convert(PhotonDoc doc, String[] languages, String[] extraTags) throws IOException {
        final AddressType atype = doc.getAddressType();
        XContentBuilder builder = XContentFactory.jsonBuilder().startObject()
                .field(Constants.OSM_ID, doc.getOsmId())
                .field(Constants.OSM_TYPE, doc.getOsmType())
                .field(Constants.OSM_KEY, doc.getTagKey())
                .field(Constants.OSM_VALUE, doc.getTagValue())
                .field(Constants.OBJECT_TYPE, atype == null ? "locality" : atype.getName())
                .field(Constants.IMPORTANCE, doc.getImportance());

        String classification = buildClassificationString(doc.getTagKey(), doc.getTagValue());
        if (classification != null) {
            builder.field(Constants.CLASSIFICATION, classification);
        }

        if (doc.getCentroid() != null) {
            builder.startObject("coordinate")
                    .field("lat", doc.getCentroid().getY())
                    .field("lon", doc.getCentroid().getX())
                    .endObject();
        }

        if (doc.getGeometry() != null) {
            GeoJsonWriter g = new GeoJsonWriter();

            XContentParser parser = JsonXContent
                .jsonXContent
                .createParser(NamedXContentRegistry.EMPTY, g.write(doc.getGeometry()));

            builder.field("geometry");
            builder.copyCurrentStructure(parser);
        }

        if (doc.getHouseNumber() != null) {
            builder.field("housenumber", doc.getHouseNumber());
        }

        if (doc.getPostcode() != null) {
            builder.field("postcode", doc.getPostcode());
        }

        writeName(builder, doc, languages);

        for (AddressType entry : doc.getAddressParts().keySet()) {
            Map<String, String> fNames = new HashMap<>();

            doc.copyAddressName(fNames, "default", entry, "name");

            for (String language : languages) {
                doc.copyAddressName(fNames, language, entry, "name:" + language);
            }

            write(builder, fNames, entry.getName());
        }

        String countryCode = doc.getCountryCode();
        if (countryCode != null)
            builder.field(Constants.COUNTRYCODE, countryCode);
        writeContext(builder, doc.getContext(), languages);
        writeExtraTags(builder, doc.getExtratags(), extraTags);
        writeExtent(builder, doc.getBbox());

        builder.endObject();


        return builder;
    }

    private static void writeExtraTags(XContentBuilder builder, Map<String, String> docTags, String[] extraTags) throws IOException {
        boolean foundTag = false;

        for (String tag : extraTags) {
            String value = docTags.get(tag);
            if (value != null) {
                if (!foundTag) {
                    builder.startObject("extra");
                    foundTag = true;
                }
                builder.field(tag, value);
            }
        }

        if (foundTag) {
            builder.endObject();
        }
    }

    private static void writeExtent(XContentBuilder builder, Envelope bbox) throws IOException {
        if (bbox == null) return;

        if (bbox.getArea() == 0.) return;

        // http://www.elasticsearch.org/guide/en/elasticsearch/reference/current/mapping-geo-shape-type.html#_envelope
        builder.startObject("extent");
        builder.field("type", "envelope");

        builder.startArray("coordinates");
        builder.startArray().value(bbox.getMinX()).value(bbox.getMaxY()).endArray();
        builder.startArray().value(bbox.getMaxX()).value(bbox.getMinY()).endArray();

        builder.endArray();
        builder.endObject();
    }

    private static void writeName(XContentBuilder builder, PhotonDoc doc, String[] languages) throws IOException {
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

        write(builder, fNames, "name");
    }

    private static void write(XContentBuilder builder, Map<String, String> fNames, String name) throws IOException {
        if (fNames.isEmpty()) return;

        builder.startObject(name);
        for (Map.Entry<String, String> entry : fNames.entrySet()) {
            builder.field(entry.getKey(), entry.getValue());
        }
        builder.endObject();
    }

    protected static void writeContext(XContentBuilder builder, Set<Map<String, String>> contexts, String[] languages) throws IOException {
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
            builder.startObject("context");
            for (Map.Entry<String, Set<String>> entry : multimap.entrySet()) {
                builder.field(entry.getKey(), String.join(", ", entry.getValue()));
            }
            builder.endObject();
        }
    }
}
