package de.komoot.photon.json;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.Map;

public class CountryInfo {
    public static final String DOCUMENT_TYPE = "CountryInfo";

    private String countryCode;

    private Map<String, String> name = Collections.emptyMap();

    @JsonProperty("name")
    public void setName(Map<String, String> names) {
        this.name = names;
    }

    @JsonProperty("country_code")
    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public Map<String, String> getName() {
        return name;
    }

    public String getCountryCode() {
        return countryCode;
    }
}
