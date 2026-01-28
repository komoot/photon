package de.komoot.photon.query;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Collection of query parameters for a search request.
 */
@NullMarked
public class SimpleSearchRequest extends SearchRequestBase {
    @Nullable private String query;

    @Nullable public String getQuery() {
        return query;
    }

    public void setQuery(@Nullable String query) {
        this.query = query;
    }
}
