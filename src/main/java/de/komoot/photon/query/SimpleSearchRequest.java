package de.komoot.photon.query;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Collection of query parameters for a search request.
 */
@NullMarked
public class SimpleSearchRequest extends SearchRequestBase {
    @Nullable private String query;
    private List<String> countryCodes = List.of();

    @Nullable public String getQuery() {
        return query;
    }

    public void setQuery(@Nullable String query) {
        this.query = query;
    }

    public List<String> getCountryCodes() {
        return countryCodes;
    }

    public void setCountryCodes(List<String> countryCodes) {
        this.countryCodes = countryCodes;
    }
}
