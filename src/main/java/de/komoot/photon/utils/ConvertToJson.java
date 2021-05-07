package de.komoot.photon.utils;

import com.google.common.collect.Lists;
import de.komoot.photon.Constants;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;
import java.util.Map;

/**
 * Convert a elasticsearch {@link SearchResponse} into a list of {@link JSONObject}s
 * <p/>
 * Created by Sachin Dole on 2/20/2015.
 */
@Slf4j
public class ConvertToJson {
    private static final String[] KEYS_LANG_UNSPEC = {Constants.OSM_ID, Constants.OSM_VALUE, Constants.OSM_KEY, Constants.POSTCODE, Constants.HOUSENUMBER, Constants.COUNTRYCODE, Constants.OSM_TYPE};
    private static final String[] KEYS_LANG_SPEC = {Constants.NAME, Constants.COUNTRY, Constants.CITY, Constants.DISTRICT, Constants.LOCALITY, Constants.STREET, Constants.STATE, Constants.COUNTY};
    private static final String[] NAME_PRECEDENCE = {"default", "housename", "int", "loc", "reg", "alt", "old"};
    private final String lang;

    public ConvertToJson(String lang) {
        this.lang = lang;
    }

    public List<JSONObject> convert(SearchResponse searchResponse, boolean debugMode) {
        SearchHit[] hits = searchResponse.getHits().getHits();
        final List<JSONObject> list = Lists.newArrayListWithExpectedSize(hits.length);
        for (SearchHit hit : hits) {
            final Map<String, Object> source = hit.getSource();

            final JSONObject feature = new JSONObject();
            if (debugMode) {
                feature.put("score", hit.getScore());
                feature.put("importance", source.get("importance"));
            }
            feature.put(Constants.TYPE, Constants.FEATURE);
            feature.put(Constants.GEOMETRY, getPoint(source));

            // populate properties
            final JSONObject properties = new JSONObject();

            // language unspecific properties
            for (String key : KEYS_LANG_UNSPEC) {
                if (source.containsKey(key))
                    properties.put(key, source.get(key));
            }

            // language specific properties
            for (String key : KEYS_LANG_SPEC) {
                if (source.containsKey(key))
                    properties.put(key, getLocalised(source, key, lang));
            }

            // place type
            properties.put("type", source.get(Constants.OBJECT_TYPE));

            // add extent of geometry
            final Map<String, Object> extent = (Map<String, Object>) source.get("extent");
            if (extent != null) {
                List<List<Double>> coords = (List<List<Double>>) extent.get("coordinates");
                final List<Double> nw = coords.get(0);
                final List<Double> se = coords.get(1);
                properties.put("extent", new JSONArray(Lists.newArrayList(nw.get(0), nw.get(1), se.get(0), se.get(1))));
            }

            final Map<String, String> extraTags = (Map<String, String>) source.get("extra");
            if (extraTags != null) {
                properties.put("extra", extraTags);
            }

            feature.put(Constants.PROPERTIES, properties);

            list.add(feature);
        }
        return list;
    }

    private String getLocalised(Map<String, Object> source, String fieldName, String lang) {
        final Map<String, String> map = (Map<String, String>) source.get(fieldName);
        if (map == null) return null;

        if (map.get(lang) != null) {
            // language specific field
            return map.get(lang);
        }

        if (fieldName.equals("name")) {
            for (String key : NAME_PRECEDENCE) {
                if (map.containsKey(key))
                    return map.get(key);
            }
        }

        return map.get("default");
    }

    private JSONObject getPoint(Map<String, Object> source) {
        JSONObject point = new JSONObject();

        final Map<String, Double> coordinate = (Map<String, Double>) source.get("coordinate");
        if (coordinate != null) {
            point.put(Constants.TYPE, Constants.POINT);
            point.put(Constants.COORDINATES, new JSONArray("[" + coordinate.get(Constants.LON) + "," + coordinate.get(Constants.LAT) + "]"));
        } else {
            log.error(String.format("invalid data [id=%s, type=%s], coordinate is missing!", source.get(Constants.OSM_ID), source.get(Constants.OSM_VALUE)));
        }

        return point;
    }
}
