package de.komoot.photon.json;

import java.util.Collections;
import java.util.Map;

public class CountryInfo {

    private Map<String, String> names = Collections.emptyMap();

    void setNames(Map<String, String> names) {
        this.names = names;
    }

    public Map<String, String> getNames() {
        return names;
    }

}
