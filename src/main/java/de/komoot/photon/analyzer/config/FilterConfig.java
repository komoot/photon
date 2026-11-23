package de.komoot.photon.analyzer.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;

@NullMarked
public class FilterConfig {

    protected FilterConfig() {}

    @JsonProperty
    @Nullable String lang;

    @JsonProperty
    @Nullable String country;

    @JsonProperty
    List<String> categories = List.of();

    @JsonProperty
    Map<String, String> extra = Map.of();

    public @Nullable String getLang() {
        return lang;
    }

    public @Nullable String getCountry() {
        return country;
    }

    public List<String> getCategories() {
        return categories;
    }

    public Map<String, String> getExtra() {
        return extra;
    }
}
