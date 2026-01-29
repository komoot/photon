package de.komoot.photon.json;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Map;

@NullMarked
public class CountryInfo {
    public static final String DOCUMENT_TYPE = "CountryInfo";

    private final String countryCode;
    private final Map<String, @Nullable String> name;

    @JsonCreator
    public CountryInfo(
            @JsonProperty("country_code") String countryCode,
            @JsonProperty("name") Map<String, String> names
    ) {
        this.countryCode = countryCode;
        this.name = names;
    }

    public Map<String, @Nullable String> getName() {
        return name;
    }

    public String getCountryCode() {
        return countryCode;
    }
}
