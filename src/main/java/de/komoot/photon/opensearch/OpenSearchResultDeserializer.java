package de.komoot.photon.opensearch;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.komoot.photon.Constants;
import de.komoot.photon.searcher.PhotonResult;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@NullMarked
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

        String geometry = null;
        if (node.get("geometry") != null) {
            geometry = node.get("geometry").toString();
        }

        var fieldNames = node.fieldNames();
        while (fieldNames.hasNext()) {
            String key = fieldNames.next();
            final JsonNode value = node.get(key);
            if (value.isTextual()) {
                tags.put(key, value.asText());
            } else if (value.isInt()) {
                tags.put(key, value.asInt());
            } else if (value.isLong()) {
                tags.put(key, value.asLong());
            } else if (value.isFloatingPointNumber()) {
                tags.put(key, value.asDouble());
            } else if (value.isObject()) {
                Map<String, String> vtags = new HashMap<>();
                ObjectNode objValue = (ObjectNode) value;
                var subFieldNames = objValue.fieldNames();
                while (subFieldNames.hasNext()) {
                    String subKey = subFieldNames.next();
                    JsonNode subValue = objValue.get(subKey);
                    if (subValue.isTextual()) {
                        vtags.put(subKey, subValue.asText());
                    }
                }
                localeTags.put(key, vtags);
            }
        }

        return new OpenSearchResult(extent, coordinates, tags, localeTags, geometry);
    }


    private double @Nullable [] extractExtent(@Nullable ObjectNode node) {
        if (node == null || !node.has("coordinates")) {
            return null;
        }

        final var coords = ((ArrayNode) node.get("coordinates"));
        final var nw = ((ArrayNode) coords.get(0));
        final var se = ((ArrayNode) coords.get(1));

        return new double[]{nw.get(0).doubleValue(), nw.get(1).doubleValue(),
                            se.get(0).doubleValue(), se.get(1).doubleValue()};
    }

    private double[] extractCoordinate(@Nullable ObjectNode node) {
        if (node == null) {
            return PhotonResult.INVALID_COORDINATES;
        }

        return new double[]{node.get(Constants.LON).doubleValue(), node.get(Constants.LAT).doubleValue()};
    }
}
