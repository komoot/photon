package de.komoot.photon.query;

public class LookupRequest {
    private String placeId;
    private String language;

    public LookupRequest(String placeId, String language) {
        this.placeId = placeId;
        this.language = language;
    }

    public String getPlaceId() {
        return this.placeId;
    }

    public String getLanguage() {
        return this.language;
    }
}
