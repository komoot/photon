package de.komoot.photon;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.Objects;
import java.util.regex.Pattern;

@NullMarked
public record ConfigClassificationTerm(String key, String value, String[] terms) {
    private static final Pattern LABEL_PATTERN = Pattern.compile(
            String.format("[%s]+", PhotonDoc.CATEGORY_VALID_CHARS)
    );

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
        return LABEL_PATTERN.matcher(key).matches() && LABEL_PATTERN.matcher(value).matches();
    }

    public String getClassificationString() {
        return String.format("#osm.%s.%s", key, value);
    }
}
