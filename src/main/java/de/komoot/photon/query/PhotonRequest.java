package de.komoot.photon.query;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Point;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Collection of query parameters for a search request.
 */
public class PhotonRequest implements Serializable {
    private String query;
    private Integer limit;
    private Point locationForBias;
    private String language;
    private final double scale;
    private Envelope bbox;
    private boolean debug;

    private Set<String> excludeKeys;
    private Set<String> includeKeys;
    private Set<String> excludeValues;
    private Set<String> includeValues;
    private Map<String, Set<String>> includeTags;
    private Map<String, Set<String>> excludeTags;
    private Map<String, Set<String>> excludeTagValues;


    public PhotonRequest(String query, int limit, Envelope bbox, Point locationForBias, double scale, String language, boolean debug) {
        this.query = query;
        this.limit = limit;
        this.locationForBias = locationForBias;
        this.scale = scale;
        this.language = language;
        this.bbox = bbox;
        this.debug = debug;
    }

    public String getQuery() {
        return query;
    }

    public Integer getLimit() {
        return limit;
    }
    
    public Envelope getBbox() {
        return bbox;
    }

    public Point getLocationForBias() {
        return locationForBias;
    }

    public double getScaleForBias() {
        return scale;
    }

    public String getLanguage() {
        return language;
    }

    public boolean getDebug() { return debug; }

    public Set<String> keys() {
        return includeKeys;
    }

    public Set<String> values() {
        return includeValues;
    }

    public Map<String, Set<String>> tags() {
        return includeTags;
    }

    public Set<String> notValues() {
        return excludeValues;
    }

    public Map<String, Set<String>> notTags() {
        return excludeTags;
    }

    public Set<String> notKeys() {
        return excludeKeys;
    }

    public Map<String, Set<String>> tagNotValues() {
        return excludeTagValues;

    }

    private void addExcludeValues(String value) {
        if (excludeValues == null) {
            excludeValues = new HashSet<>(3);
        }
        excludeValues.add(value);
    }

    private void addIncludeValues(String value) {
        if (includeValues == null) {
            includeValues = new HashSet<>(3);
        }
        includeValues.add(value);
    }

    private void addExcludeKeys(String value) {
        if (excludeKeys == null) {
            excludeKeys = new HashSet<>(3);
        }
        excludeKeys.add(value);
    }

    private void addIncludeKeys(String value) {
        if (includeKeys == null) {
            includeKeys = new HashSet<>(3);
        }
        includeKeys.add(value);
    }

    private void addExcludeTags(String key, String value) {
        if (excludeTags == null) {
            excludeTags = new HashMap<>(3);
        }

        excludeTags.computeIfAbsent(key, k -> new HashSet<>()).add(value);
    }

    private void addExcludeTagValues(String key, String value) {
        if (excludeTagValues == null) {
            excludeTagValues = new HashMap<>(3);
        }

        excludeTagValues.computeIfAbsent(key, k -> new HashSet<>()).add(value);
    }

    private void addIncludeTags(String key, String value) {
        if (includeTags == null) {
            includeTags = new HashMap<>(3);
        }

        includeTags.computeIfAbsent(key, k -> new HashSet<>()).add(value);
    }

   public void setUpTagFilters(String[] tagFilters) {
        for (String tagFilter : tagFilters) {
            if (tagFilter.contains(":")) {
                //might be tag and value OR just value.
                if (tagFilter.startsWith("!")) {
                    //exclude
                    String keyValueCandidate = tagFilter.substring(1);
                    if (keyValueCandidate.startsWith(":")) {
                        //just value
                        addExcludeValues(keyValueCandidate.substring(1));
                    } else {
                        //key and value
                        String[] keyAndValue = keyValueCandidate.split(":");
                        String excludeKey = keyAndValue[0];
                        String value = keyAndValue[1].startsWith("!") ? keyAndValue[1].substring(1) : keyAndValue[1];
                        addExcludeTags(excludeKey, value);
                    }
                } else {
                    //include key, not sure about value
                    if (tagFilter.startsWith(":")) {
                        //just value

                        String valueCandidate = tagFilter.substring(1);
                        if (valueCandidate.startsWith("!")) {
                            addExcludeValues(valueCandidate.substring(1));
                        } else {
                            addIncludeValues(valueCandidate);
                        }
                    } else {
                        //key and value
                        String[] keyAndValue = tagFilter.split(":");

                        String key = keyAndValue[0];
                        String value = keyAndValue[1];
                        if (value.startsWith("!")) {
                            addExcludeTagValues(key, value.substring(1));
                        } else {
                            addIncludeTags(key, value);
                        }
                    }
                }
            } else {
                //only tag
                if (tagFilter.startsWith("!")) {
                    addExcludeKeys(tagFilter.substring(1));
                } else {
                    addIncludeKeys(tagFilter);
                }
            }
        }
    }
}
