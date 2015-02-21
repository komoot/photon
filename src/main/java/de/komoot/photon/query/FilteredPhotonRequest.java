package de.komoot.photon.query;

import com.vividsolutions.jts.geom.Point;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by Sachin Dole on 2/12/2015.
 */
public class FilteredPhotonRequest extends PhotonRequest {
    public FilteredPhotonRequest(String query, Integer limit, Point locationForBias,String language) {
        super(query, limit, locationForBias, language);
    }

    private Set<String> excludeKeys = new HashSet<String>(3);
    private Set<String> includeKeys = new HashSet<String>(3);
    private Set<String> excludeValues = new HashSet<String>(3);
    private Set<String> includeValues =  new HashSet<String>();
    private Map<String,String> includeTags = new HashMap<String, String>(3);
    private Map<String,String> excludeTags = new HashMap<String, String>(3);

    void notKey(String excludeKey) {
        excludeKeys.add(excludeKey);
    }

    void notTag(String excludeKey,String excludeValue) {
        excludeTags.put(excludeKey,excludeValue);
    }
    void notValue(String excludeValue) {
        excludeValues.add(excludeValue);
    }
    
    public Set<String> notKey(){
        return excludeKeys;
    }

    public Set<String> key() {
        return includeKeys;
    }

    public void key(String includeKey) {
        this.includeKeys.add(includeKey);
    }

    public Set<String> notValue() {
        return excludeValues;
    }

    public Map<String, String> notTag() {
        return excludeTags;
    }

    public void value(String keyToInclude) {
        includeKeys.add(keyToInclude);
    }
    public Set<String> value() {
        return includeKeys;
    }

    public void tag(String aKey, String aValue) {
        this.includeTags.put(aKey,aValue);
    }

    public Map<String, String> tag() {
        return includeTags;
    }
}
