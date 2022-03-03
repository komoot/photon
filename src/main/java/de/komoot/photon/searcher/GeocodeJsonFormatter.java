package de.komoot.photon.searcher;

import de.komoot.photon.Constants;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

/**
 * Format a database result into a Photon GeocodeJson response.
 */
public class GeocodeJsonFormatter implements ResultFormatter {
    private static final String[] KEYS_LANG_UNSPEC = {Constants.OSM_TYPE, Constants.OSM_ID, Constants.OSM_KEY, Constants.OSM_VALUE, Constants.OBJECT_TYPE, Constants.POSTCODE, Constants.HOUSENUMBER, Constants.COUNTRYCODE};
    private static final String[] KEYS_LANG_SPEC = {Constants.NAME, Constants.COUNTRY, Constants.CITY, Constants.DISTRICT, Constants.LOCALITY, Constants.STREET, Constants.STATE, Constants.COUNTY};
    private static final String[] NAME_PRECEDENCE = {"default", "housename", "int", "loc", "reg", "alt", "old"};

    private final boolean addDebugInfo;
    private final String language;

    public GeocodeJsonFormatter(boolean addDebugInfo, String language) {
        this.addDebugInfo = addDebugInfo;
        this.language = language;
    }

    @Override
    public String convert(List<PhotonResult> results, String debugInfo) {
        final JSONArray features = new JSONArray(results.size());

        for (PhotonResult result : results) {
            final double[] coordinates = result.getCoordinates();

            features.put(new JSONObject()
                        .put("type", "feature")
                        .put("properties", getResultProperties(result))
                        .put("geometry", new JSONObject()
                                .put("type", "feature")
                                .put("coordinates", coordinates)));
        }

        final JSONObject out = new JSONObject();
        out.put("type", "FeatureCollection")
           .put("features", features);

        if (debugInfo != null) {
            out.put("properties", new JSONObject().put("debug", new JSONObject(debugInfo)));
        }

        if (addDebugInfo) {
            return out.toString(4);
        }

        return out.toString();
    }

    private JSONObject getResultProperties(PhotonResult result) {
        JSONObject props = new JSONObject();
        if (addDebugInfo) {
            props.put("score", result.getScore());
            put(props,"importance", result.get("importance"));
        }

        for (String key : KEYS_LANG_UNSPEC) {
            put(props, key, result.get(key));
        }

        for (String key : KEYS_LANG_SPEC) {
            put(props, key, result.getLocalised(key, language));
        }

        final double[] extent = result.getExtent();
        if (extent != null) {
            props.put("extent", extent);
        }

        put(props, "extra", result.getMap("extra"));

        return props;
    }

    private void put(JSONObject out, String key, Object value) {
        if (value != null) {
            out.put(key, value);
        }
    }
}
