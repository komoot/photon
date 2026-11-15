package de.komoot.photon;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.Objects;

@NullMarked
public record ConfigClassificationTerm(String key, String value, String[] terms) {

    @JsonCreator
    public ConfigClassificationTerm(
            @JsonProperty("key") String key,
            @JsonProperty("value") String value,
            @JsonProperty("terms") @Nullable String[] terms
    ) {
        this.key = key;
        this.value = value;
        this.terms = Arrays.stream(terms).filter(Objects::nonNull).toArray(String[]::new);
    }

    public boolean isValidCategory() {
        return PhotonDoc.isValidCategoryLabel(key) && PhotonDoc.isValidCategoryLabel(value);
    }

    public String getClassificationString() {
        return String.format("#osm.%s.%s", key, value);
    }
}
