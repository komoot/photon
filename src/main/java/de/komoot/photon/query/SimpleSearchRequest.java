package de.komoot.photon.query;

/**
 * Collection of query parameters for a search request.
 */
public class SimpleSearchRequest extends SearchRequestBase {
    private final String query;

    public SimpleSearchRequest(String query, String language) {
        super(language);
        this.query = query;
    }

    public String getQuery() {
        return query;
    }
}
