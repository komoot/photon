package de.komoot.photon.elasticsearch;

import co.elastic.clients.elasticsearch.core.search.Hit;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.komoot.photon.Constants;
import de.komoot.photon.PhotonDoc;
import de.komoot.photon.searcher.PhotonResult;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

@Slf4j
public class ElasticResult implements PhotonResult {
    private static final String[] NAME_PRECEDENCE = {"default", "housename", "int", "loc", "reg", "alt", "old"};

    private final ObjectNode result;
    private Double score = 0.0;

    ElasticResult(Hit<ObjectNode> result) {
        this.result = result.source();
        this.score = result.score();
    }

    ElasticResult(ObjectNode result) {
        this.result = result;
    }

    @Override
    public Object get(String key) {
        return result.get(key);
    }

    @Override
    public String getLocalised(String key, String language) {
        final ObjectNode map = (ObjectNode) result.get(key);

        if (map == null) return null;

        if (map.get(language) != null) {
            // language specific field
            return map.get(language).asText();
        }

        if ("name".equals(key)) {
            for (String name : NAME_PRECEDENCE) {
                if (map.hasNonNull(name))
                    return map.get(name).asText();
            }
        }

        return map.get("default").asText();
    }

    @Override
    public double[] getCoordinates() {
        final ObjectNode coordinate = (ObjectNode) result.get("coordinate");
        if (coordinate == null) {
            log.error(String.format("invalid data [id=%s, type=%s], coordinate is missing!",
                    result.get(Constants.OSM_ID),
                    result.get(Constants.OSM_VALUE)));
            return INVALID_COORDINATES;
        }

        return new double[]{coordinate.get(Constants.LON).asDouble(), coordinate.get(Constants.LAT).asDouble()};
    }

    @Override
    public double[] getExtent() {
        final ObjectNode extent = (ObjectNode) result.get("extent");
        if (extent == null) {
            return null;
        }

        final ArrayNode coords = (ArrayNode) extent.get("coordinates");
        final ArrayNode nw = (ArrayNode) coords.get(0);
        final ArrayNode se = (ArrayNode) coords.get(1);

        return new double[]{ nw.get(0).asDouble(), nw.get(1).asDouble(), se.get(0).asDouble(), se.get(1).asDouble() };
    }

    @Override
    public double getScore() {
        return score;
    }
}
