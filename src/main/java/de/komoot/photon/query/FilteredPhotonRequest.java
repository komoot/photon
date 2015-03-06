package de.komoot.photon.query;

import com.vividsolutions.jts.geom.Point;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A photon request that can hold filter parameters requested by client.
 * Created by Sachin Dole on 2/12/2015.
 */
public class FilteredPhotonRequest extends PhotonRequest {
    private Set<String> excludeKeys = new HashSet<String>(3);
    private Set<String> includeKeys = new HashSet<String>(3);
    private Set<String> excludeValues = new HashSet<String>(3);
    private Set<String> includeValues = new HashSet<String>();
    private Map<String, Set<String>> includeTags = new HashMap<String, Set<String>>(3);
    private Map<String, Set<String>> excludeTags = new HashMap<String, Set<String>>(3);
    private Map<String, Set<String>> excludeTagValues = new HashMap<String, Set<String>>(3);
    FilteredPhotonRequest(String query, Integer limit, Point locationForBias, String language) {
        super(query, limit, locationForBias, language);
    }

    public Set<String> keys() {
        return includeKeys;
    }

    void keys(String includeKey) {
        this.includeKeys.add(includeKey);
    }

    public Set<String> values() {
        return includeValues;
    }

    void values(String keyToInclude) {
        includeValues.add(keyToInclude);
    }

    public Map<String, Set<String>> tags() {
        return includeTags;
    }

    void tags(String aKey, Set<String> manyValues) {
        this.includeTags.put(aKey, manyValues);
    }

    public Set<String> notValues() {
        return excludeValues;
    }

    public Map<String, Set<String>> notTags() {
        return excludeTags;
    }

    void notKeys(String excludeKey) {
        excludeKeys.add(excludeKey);
    }

    void notTags(String excludeKey, Set<String> excludeManyValues) {
        excludeTags.put(excludeKey, excludeManyValues);
    }

    void notValues(String excludeValue) {
        excludeValues.add(excludeValue);
    }

    public Set<String> notKeys() {
        return excludeKeys;
    }

    void tagNotValues(String key, Set<String> excludeValues) {
        excludeTagValues.put(key,excludeValues);
    }
    public Map<String,Set<String>> tagNotValues(){
        return excludeTagValues;
        
    }
}
