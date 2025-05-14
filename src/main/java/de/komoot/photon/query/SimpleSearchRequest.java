package de.komoot.photon.query;

/**
 * Collection of query parameters for a search request.
 */
public class SimpleSearchRequest extends SearchRequestBase {
    private String query;

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }
}
