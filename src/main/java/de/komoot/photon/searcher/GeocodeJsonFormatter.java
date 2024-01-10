package de.komoot.photon.searcher;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.komoot.photon.Constants;

import java.util.List;

/**
 * Format a database result into a Photon GeocodeJson response.
 */
public class GeocodeJsonFormatter implements ResultFormatter {
    private static final String[] KEYS_LANG_UNSPEC = {Constants.RANK_ADDRESS, Constants.PARENT_PLACE_ID, Constants.PLACE_ID, Constants.OSM_TYPE, Constants.OSM_ID, Constants.OSM_KEY, Constants.OSM_VALUE, Constants.OBJECT_TYPE, Constants.POSTCODE, Constants.HOUSENUMBER, Constants.COUNTRYCODE};
    private static final String[] KEYS_LANG_SPEC = {Constants.NAME, Constants.COUNTRY, Constants.CITY, Constants.DISTRICT, Constants.LOCALITY, Constants.STREET, Constants.STATE, Constants.COUNTY};
    private static final String[] NAME_PRECEDENCE = {"default", "housename", "int", "loc", "reg", "alt", "old"};

    private final boolean addDebugInfo;
    private final String language;
    private final ObjectMapper mapper = new ObjectMapper();

    public GeocodeJsonFormatter(boolean addDebugInfo, String language) {
        this.addDebugInfo = addDebugInfo;
        this.language = language;
    }

    @Override
    public String convert(List<PhotonResult> results, String debugInfo) throws JsonProcessingException {
        final ArrayNode features = mapper.createArrayNode();

        for (PhotonResult result : results) {
            if (result == null) {
                continue;
            }

            final double[] coordinates = result.getCoordinates();

            features.add(mapper.createObjectNode()
                        .put("type", "Feature")
                        .putPOJO("properties", getResultProperties(result))
                        .putPOJO("geometry", mapper.createObjectNode()
                                .put("type", "Point")
                                .putPOJO("coordinates", coordinates)
                        )
            );
        }

        final ObjectNode out = mapper.createObjectNode();
        out.put("type", "FeatureCollection")
           .putPOJO("features", features);

        if (debugInfo != null) {
            out.putPOJO("properties", mapper.createObjectNode()
                    .putPOJO("debug", mapper.readTree(debugInfo))
            );
        }

        return out.toString();
    }

    private ObjectNode getResultProperties(PhotonResult result) {
        ObjectNode props = mapper.createObjectNode();

        if (addDebugInfo) {
            props.put("score", result.getScore());
            props.putPOJO("importance", result.get("importance"));
        }

        for (String key : KEYS_LANG_UNSPEC) {
            props.putPOJO(key, result.get(key));
        }

        for (String key : KEYS_LANG_SPEC) {
            props.put(key, result.getLocalised(key, language));
        }

        final double[] extent = result.getExtent();

        if (extent != null && extent.length != 0) {
            props.putPOJO("extent", extent);
        }

        props.putPOJO("extra", result.get("extra"));
        props.putPOJO("names", result.get("names"));

        return props;
    }

}
