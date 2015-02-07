package de.komoot.photon;

/**
 * Created by Sachin Dole on 2/6/2015.
 */
public class OsmTag {
    private String key;
    private String value;

    public OsmTag(String tagKey, String tagValue) {
        key = tagKey;
        value = tagValue;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }
}
