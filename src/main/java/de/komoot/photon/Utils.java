package de.komoot.photon;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.vividsolutions.jts.geom.Envelope;
import de.komoot.photon.nominatim.model.AddressType;

import java.util.*;

/**
 * helper functions to create convert a photon document to XContentBuilder object / JSON
 *
 * @author christoph
 */
public class Utils {
    private static final ObjectMapper mapper = new ObjectMapper();

    public static ObjectNode convert(PhotonDoc doc, String[] languages, String[] extraTags, boolean allExtraTags, boolean includeExtraNames) {
        final AddressType addressType = doc.getAddressType();

        ObjectNode rootNode = mapper
                .createObjectNode()
                .put(Constants.OSM_ID, doc.getOsmId())
                .put(Constants.OSM_TYPE, doc.getOsmType())
                .put(Constants.OSM_KEY, doc.getTagKey())
                .put(Constants.OSM_VALUE, doc.getTagValue())
                .put(Constants.PLACE_ID, doc.getPlaceId())
                .put(Constants.PARENT_PLACE_ID, doc.getParentPlaceId())
                .put(Constants.OBJECT_TYPE, addressType == null ? "locality" : addressType.getName())
                .put(Constants.IMPORTANCE, doc.getImportance());

        String classification = buildClassificationString(doc.getTagKey(), doc.getTagValue());
        if (classification != null) {
            rootNode.put(Constants.CLASSIFICATION, classification);
        }

        if (doc.getCentroid() != null) {
            ObjectNode coordinateNode = mapper
                    .createObjectNode()
                    .put("lat", doc.getCentroid().getY())
                    .put("lon", doc.getCentroid().getX());
            rootNode.set("coordinate", coordinateNode);
        }

        if (doc.getHouseNumber() != null) {
            rootNode.put("housenumber", doc.getHouseNumber());
        }

        if (doc.getPostcode() != null) {
            rootNode.put("postcode", doc.getPostcode());
        }

        writeName(rootNode, doc, languages);

        for (AddressType entry : doc.getAddressParts().keySet()) {
            Map<String, String> fNames = new HashMap<>();

            doc.copyAddressName(fNames, "default", entry, "name");

            for (String language : languages) {
                doc.copyAddressName(fNames, language, entry, "name:" + language);
            }

            write(rootNode, fNames, entry.getName());
        }

        String countryCode = doc.getCountryCode();
        if (countryCode != null)
            rootNode.put(Constants.COUNTRYCODE, countryCode);

        writeContext(rootNode, doc.getContext(), languages);
        writeExtraTags(rootNode, doc.getExtratags(), extraTags, allExtraTags);
        writeExtraNames(rootNode, doc.getName(), includeExtraNames);
        writeExtent(rootNode, doc.getBbox());

        return rootNode;
    }

    private static void writeExtraNames(ObjectNode objectNode, Map<String, String> docNames, boolean includeExtraNames) {
        ObjectNode extraNamesNode = mapper.createObjectNode();

        if (includeExtraNames) {
            for (Map.Entry<String, String> entry : docNames.entrySet()) {
                String name = entry.getKey();
                String value = entry.getValue();

                if (value != null) {
                    extraNamesNode.put(name, value);
                }
            }
        }

        if (!extraNamesNode.isEmpty()) {
            objectNode.set("names", extraNamesNode);
        }
    }

    private static void writeExtraTags(ObjectNode objectNode, Map<String, String> docTags, String[] extraTags, boolean allExtraTags) {
        ObjectNode extraNode = mapper.createObjectNode();

        if (allExtraTags) {
            for (Map.Entry<String, String> entry : docTags.entrySet()) {
                String tag = entry.getKey();
                String value = entry.getValue();

                if (value != null) {
                    extraNode.put(tag, value);
                }
            }
        } else {
            for (String tag : extraTags) {
                String value = docTags.get(tag);
                if (value != null) {
                    extraNode.put(tag, value);
                }
            }
        }

        if (!extraNode.isEmpty()) {
            objectNode.set("extra", extraNode);
        }
    }

    private static void writeExtent(ObjectNode objectNode, Envelope bbox) {
        if (bbox == null) return;

        if (bbox.getArea() == 0.) return;

        // http://www.elasticsearch.org/guide/en/elasticsearch/reference/current/mapping-geo-shape-type.html#_envelope
        ObjectNode extentNode = mapper
                .createObjectNode()
                .put("type", "envelope");

        ArrayNode coordinateNode = mapper
                .createArrayNode()
                .add(mapper.createArrayNode().add(bbox.getMinX()).add(bbox.getMaxY()))
                .add(mapper.createArrayNode().add(bbox.getMaxX()).add(bbox.getMinY()));

        extentNode.set("coordinates", coordinateNode);
        objectNode.set("extent", extentNode);
    }

    private static void writeName(ObjectNode objectNode, PhotonDoc doc, String[] languages) {
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

        write(objectNode, fNames, "name");
    }

    private static void write(ObjectNode objectNode, Map<String, String> fNames, String name) {
        if (fNames.isEmpty()) return;

        ObjectNode childNode = mapper.createObjectNode();
        for (Map.Entry<String, String> entry : fNames.entrySet()) {
            childNode.put(entry.getKey(), entry.getValue());
        }
        objectNode.set(name, childNode);
    }

    protected static void writeContext(ObjectNode objectNode, Set<Map<String, String>> contexts, String[] languages) {
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
            ObjectNode contextNode = mapper.createObjectNode();
            for (Map.Entry<String, Set<String>> entry : multimap.entrySet()) {
                contextNode.put(entry.getKey(), String.join(", ", entry.getValue()));
            }
            objectNode.set("context", contextNode);
        }
    }

    // http://stackoverflow.com/a/4031040/1437096
    public static String stripNonDigits(
            final CharSequence input /* inspired by seh's comment */) {
        final StringBuilder sb = new StringBuilder(
                input.length() /* also inspired by seh's comment */);
        for (int i = 0; i < input.length(); i++) {
            final char c = input.charAt(i);
            if (c > 47 && c < 58) {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    public static String buildClassificationString(String key, String value) {
        if ("place".equals(key) || "building".equals(key)) {
            return null;
        }

        if ("highway".equals(key)
            && ("unclassified".equals(value) || "residential".equals(value))) {
            return null;
        }

        for (char c : value.toCharArray()) {
            if (!(c == '_'
                  || ((c >= 'a') && (c <= 'z'))
                  || ((c >= 'A') && (c <= 'Z'))
                  || ((c >= '0') && (c <= '9')))) {
                return null;
            }
        }

        return "tpfld" + value.replaceAll("_", "").toLowerCase() + "clsfld" + key.replaceAll("_", "").toLowerCase();
    }
}
