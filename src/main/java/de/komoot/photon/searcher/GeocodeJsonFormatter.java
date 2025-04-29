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

    private final boolean addDebugInfo;
    private final String language;
    private final boolean useGeometryColumn;

    public GeocodeJsonFormatter(boolean addDebugInfo, String language, boolean useGeometryColumn) {
        this.addDebugInfo = addDebugInfo;
        this.language = language;
        this.useGeometryColumn = useGeometryColumn;
    }

    @Override
    public String convert(List<PhotonResult> results, String debugInfo) {
        final JSONArray features = new JSONArray(results.size());

        for (PhotonResult result : results) {
            if (useGeometryColumn && (result.get("geometry") != null || result.getGeometry() != null)) {
                if (result.get("geometry") != null) {
                    features.put(new JSONObject()
                            .put("type", "Feature")
                            .put("properties", getResultProperties(result))
                            .put("geometry", result.get("geometry")));
                }
                else {
                    // We need to un-escape the JSON String first
                    JSONObject jsonObject = new JSONObject(result.getGeometry());

                    features.put(new JSONObject()
                            .put("type", "Feature")
                            .put("properties", getResultProperties(result))
                            .put("geometry", jsonObject));
                }
            } else {
                final double[] coordinates = result.getCoordinates();

                features.put(new JSONObject()
                        .put("type", "Feature")
                        .put("properties", getResultProperties(result))
                        .put("geometry", new JSONObject()
                                .put("type", "Point")
                                .put("coordinates", coordinates)));
            }
        }

        final JSONObject out = new JSONObject();
        out.put("type", "FeatureCollection")
           .put("features", features);

        if (addDebugInfo) {
            final JSONObject extraProps = new JSONObject();
            if (debugInfo != null) {
                extraProps.put("debug", new JSONObject(debugInfo));
            }
            final JSONArray rawResults = new JSONArray();
            results.forEach(res -> rawResults.put(res.getRawData()));
            extraProps.put("raw_data", rawResults);
            out.put("properties", extraProps);

            return out.toString(4);
        }

        return out.toString();
    }

    private JSONObject getResultProperties(PhotonResult result) {
        JSONObject props = new JSONObject();

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
