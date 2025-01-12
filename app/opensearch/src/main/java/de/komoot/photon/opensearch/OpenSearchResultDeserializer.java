package de.komoot.photon.opensearch;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.komoot.photon.Constants;
import de.komoot.photon.searcher.GeometryType;
import de.komoot.photon.searcher.PhotonResult;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class OpenSearchResultDeserializer extends StdDeserializer<OpenSearchResult> {

    public OpenSearchResultDeserializer() {
        super(OpenSearchResult.class);
    }

    @Override
    public OpenSearchResult deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        final var node = (ObjectNode) p.getCodec().readTree(p);

        final double[] extent = extractExtent((ObjectNode) node.get("extent"));
        final double[] coordinates = extractCoordinate((ObjectNode) node.get("coordinate"));

        final Map<String, Object> tags = new HashMap<>();
        final Map<String, Map<String, String>> localeTags = new HashMap<>();

        double[][] geometry = new double[0][];
        GeometryType geometryType = GeometryType.UNKNOWN;

        if (node.get("geometry") != null && node.get("geometry").get("type") != null) {
            if (node.get("geometry").get("type").asText().equals("Polygon")) {
                geometry = extractPolygon((ObjectNode) node.get("geometry"));
                geometryType = GeometryType.POLYGON;
            } else if (node.get("geometry").get("type").asText().equals("LineString")) {
                geometry = extractLineString((ObjectNode) node.get("geometry"));
                geometryType = GeometryType.LINESTRING;
            }
        }

        var fields = node.fields();
        while (fields.hasNext()) {
            final var entry = fields.next();
            final String key = entry.getKey();
            final JsonNode value = entry.getValue();
            if (value.isTextual()) {
                tags.put(key, value.asText());
            } else if (value.isInt()) {
                tags.put(entry.getKey(), value.asInt());
            } else if (value.isLong()) {
                tags.put(entry.getKey(), value.asLong());
            } else if (value.isFloatingPointNumber()) {
                tags.put(entry.getKey(), value.asDouble());
            } else if (value.isObject()) {
                Map<String, String> vtags = new HashMap<>();
                var subfields = value.fields();
                while (subfields.hasNext()) {
                    final var subentry = subfields.next();
                    if (subentry.getValue().isTextual()) {
                        vtags.put(subentry.getKey(), subentry.getValue().asText());
                    }
                    localeTags.put(key, vtags);
                }
            }
        }

        return new OpenSearchResult(extent, coordinates, tags, localeTags, geometry, geometryType);
    }

    private double[] extractExtent(ObjectNode node) {
        if (node == null || !node.has("coordinates")) {
            return null;
        }

        final var coords = ((ArrayNode) node.get("coordinates"));
        final var nw = ((ArrayNode) coords.get(0));
        final var se = ((ArrayNode) coords.get(1));

        return new double[]{nw.get(0).doubleValue(), nw.get(1).doubleValue(),
                            se.get(0).doubleValue(), se.get(1).doubleValue()};
    }

    private double[] extractCoordinate(ObjectNode node) {
        if (node == null) {
            return PhotonResult.INVALID_COORDINATES;
        }

        return new double[]{node.get(Constants.LON).doubleValue(), node.get(Constants.LAT).doubleValue()};
    }

    private double[][] extractPolygon(ObjectNode node) {
        if (node == null) {
            return PhotonResult.INVALID_GEOMETRY;
        }

        double[][] coordinates = new double[node.get("coordinates").get(0).size()][];
        for(int i=0; i<node.get("coordinates").get(0).size(); i++) {
            double[] coordinate = new double[] {node.get("coordinates").get(0).get(i).get(0).doubleValue(), node.get("coordinates").get(0).get(i).get(1).doubleValue()};
            coordinates[i] = coordinate;
        }

        return coordinates;
    }

    private double[][] extractLineString(ObjectNode node) {
        if (node == null) {
            return PhotonResult.INVALID_GEOMETRY;
        }

        double[][] coordinates = new double[node.get("coordinates").size()][];
        for(int i=0; i<node.get("coordinates").size(); i++) {
            double[] coordinate = new double[] {node.get("coordinates").get(i).get(0).doubleValue(), node.get("coordinates").get(i).get(1).doubleValue()};
            coordinates[i] = coordinate;
        }

        return coordinates;
    }
}
