package de.komoot.photon.opensearch;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.komoot.photon.Constants;
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

        var fields = node.fields();
        while (fields.hasNext()) {
            final var entry = fields.next();
            final String key = entry.getKey();
            final JsonNode value = entry.getValue();
            if (value.isTextual()) {
                tags.put(key, value.asText());
            } else if (value.isInt()) {
                tags.put(entry.getKey(), value.asInt());
            } else if (value.isBigInteger()) {
                tags.put(entry.getKey(), value.asLong());
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

        return new OpenSearchResult(extent, coordinates, tags, localeTags);
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

}
