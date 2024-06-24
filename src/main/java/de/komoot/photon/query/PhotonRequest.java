package de.komoot.photon.query;

/**
 * Collection of query parameters for a search request.
 */
public class PhotonRequest extends PhotonRequestBase {
    private final String query;

    public PhotonRequest(String query, String language) {
        super(language);
        this.query = query;
    }

    public String getQuery() {
        return query;
    }
}
